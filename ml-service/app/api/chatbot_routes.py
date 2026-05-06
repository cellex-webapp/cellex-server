"""
Chatbot API Routes
------------------
FastAPI endpoints cho chatbot service.

Endpoints:
  POST /api/v1/chatbot/chat         - Main chat
  POST /api/v1/chatbot/index-products - Index san pham vao vector store
  DELETE /api/v1/chatbot/conversation/{session_id}
  GET  /api/v1/chatbot/stats
  GET  /api/v1/chatbot/health

KPI Endpoints (goi truc tiep, khong qua LLM):
  GET  /api/v1/chatbot/kpi/shop/{shop_id}          - KPI cua shop (SELLER/ADMIN)
  GET  /api/v1/chatbot/kpi/system                  - System metrics (ADMIN)
  GET  /api/v1/chatbot/kpi/shop/{shop_id}/bestsellers
  GET  /api/v1/chatbot/kpi/shop/{shop_id}/inventory
  GET  /api/v1/chatbot/kpi/shop/{shop_id}/coupon-strategy
  GET  /api/v1/chatbot/kpi/shop/{shop_id}/health
  GET  /api/v1/chatbot/kpi/anomalies
"""

from typing import Optional
from fastapi import APIRouter, HTTPException, Depends, Query
from pydantic import BaseModel, Field
from loguru import logger

from ..chatbot.agent import ChatbotAgent
from ..chatbot.guardrails.rbac import Role, UserContext, RBACGuard


router = APIRouter(prefix="/api/v1/chatbot", tags=["chatbot"])


# ══════════════════════════════════════════════════════════════════════════════
# Request / Response schemas
# ══════════════════════════════════════════════════════════════════════════════

class ChatRequest(BaseModel):
    """Chat request schema."""
    message: str = Field(..., min_length=1, max_length=2000, description="User message")
    user_id: str = Field(..., description="User ID")
    role: str = Field(..., description="User role: BUYER, SELLER, ADMIN (hoac VENDOR, USER de khop voi Spring Boot)")
    session_id: Optional[str] = Field(None, description="Session ID for conversation continuity")
    metadata: dict = Field(default_factory=dict, description="Additional context (shop_id, shopId, etc.)")


class ChatResponse(BaseModel):
    """Chat response schema."""
    message: str
    success: bool
    model: Optional[str] = None
    session_id: Optional[str] = None
    usage: dict = Field(default_factory=dict)


class IndexRequest(BaseModel):
    """Index products request schema."""
    limit: Optional[int] = Field(None, description="Limit so luong products can index (None = all)")


class IndexResponse(BaseModel):
    """Index response schema."""
    success: bool
    indexed_count: int
    message: str


# ══════════════════════════════════════════════════════════════════════════════
# Global agent instance
# ══════════════════════════════════════════════════════════════════════════════

chatbot_agent: Optional[ChatbotAgent] = None


def get_agent() -> ChatbotAgent:
    """Dependency de lay agent instance."""
    if chatbot_agent is None:
        raise HTTPException(status_code=503, detail="Chatbot agent chua duoc khoi tao")
    return chatbot_agent


# ══════════════════════════════════════════════════════════════════════════════
# Main chat endpoints
# ══════════════════════════════════════════════════════════════════════════════

@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Chat endpoint.

    Role mapping tu Spring Boot:
      USER   -> BUYER
      VENDOR -> SELLER
      ADMIN  -> ADMIN (giu nguyen)

    Example:
        ```json
        {
          "message": "Tim dien thoai duoi 10 trieu",
          "user_id": "user-uuid-here",
          "role": "USER",
          "session_id": "session_abc",
          "metadata": {}
        }
        ```
    """
    try:
        # Map Spring Boot roles sang chatbot roles
        mapped_role = RBACGuard.map_spring_boot_role(request.role)

        user_context = UserContext(
            user_id=request.user_id,
            role=mapped_role,
            session_id=request.session_id,
            metadata=request.metadata,
        )

        response = await agent.chat(
            user_message=request.message,
            user_context=user_context,
            session_id=request.session_id,
        )

        return ChatResponse(
            message=response.get("message", ""),
            success=response.get("success", False),
            model=response.get("model"),
            session_id=request.session_id,
            usage=response.get("usage", {}),
        )

    except Exception as e:
        logger.error(f"Chat endpoint failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.post("/index-products", response_model=IndexResponse)
async def index_products(
    request: IndexRequest,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Index published products vao vector store cho RAG.

    Chay endpoint nay khi:
    - Lan dau setup chatbot
    - Co them san pham moi
    - Muon refresh index

    Example:
        ```json
        { "limit": null }
        ```
    """
    try:
        count = await agent.index_products(limit=request.limit)
        return IndexResponse(
            success=count > 0,
            indexed_count=count,
            message=f"Da index {count} san pham thanh cong",
        )

    except Exception as e:
        logger.error(f"Index failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/conversation/{session_id}")
async def clear_conversation(
    session_id: str,
    agent: ChatbotAgent = Depends(get_agent),
):
    """Xoa conversation history de reset context."""
    try:
        agent.clear_conversation(session_id)
        return {"success": True, "message": "Conversation cleared"}
    except Exception as e:
        logger.error(f"Clear conversation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stats")
async def get_stats(agent: ChatbotAgent = Depends(get_agent)):
    """Lay thong tin thong ke agent: conversations, tools, vector store."""
    try:
        stats = agent.get_stats()
        return {"success": True, "stats": stats}
    except Exception as e:
        logger.error(f"Get stats failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "healthy",
        "service": "chatbot",
        "agent_initialized": chatbot_agent is not None,
    }


# ══════════════════════════════════════════════════════════════════════════════
# KPI Direct Endpoints (khong qua LLM, goi thang tu retriever)
# Dung cho frontend muon lay raw KPI data ma khong qua chat interface
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/kpi/shop/{shop_id}")
async def get_shop_kpi(
    shop_id: str,
    days: int = Query(default=7, ge=1, le=90, description="So ngay look back"),
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Lay KPI cua shop (san pham, tuong tac, uoc tinh doanh thu).
    Dung cho SELLER dashboard hoac Spring Boot goi de lay data.

    Args:
        shop_id: ID cua shop (UUID string)
        days: So ngay look back (1-90)
    """
    try:
        metrics = agent.retriever.retrieve_shop_metrics(shop_id, days)
        return {"success": True, "shop_id": shop_id, "period_days": days, "metrics": metrics}
    except Exception as e:
        logger.error(f"get_shop_kpi failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/shop/{shop_id}/bestsellers")
async def get_shop_bestsellers(
    shop_id: str,
    limit: int = Query(default=10, ge=1, le=50),
    agent: ChatbotAgent = Depends(get_agent),
):
    """Lay bestsellers cua shop (theo purchaseCount)."""
    try:
        bestsellers = agent.retriever.retrieve_shop_bestsellers(shop_id, limit)
        return {"success": True, "shop_id": shop_id, "bestsellers": bestsellers, "total": len(bestsellers)}
    except Exception as e:
        logger.error(f"get_shop_bestsellers failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/shop/{shop_id}/inventory")
async def get_shop_inventory(
    shop_id: str,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Phan tich ton kho shop: out_of_stock, low_stock, overstock, healthy.
    """
    try:
        inventory = agent.retriever.retrieve_inventory_analysis(shop_id)
        return {"success": True, "shop_id": shop_id, "inventory": inventory}
    except Exception as e:
        logger.error(f"get_shop_inventory failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/shop/{shop_id}/coupon-strategy")
async def get_coupon_strategy(
    shop_id: str,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Phan tich san pham de de xuat chien luoc coupon.
    Tra ve danh sach san pham nhieu view nhung conversion thap.
    """
    try:
        strategy = agent.retriever.retrieve_coupon_strategy(shop_id)
        return {"success": True, "shop_id": shop_id, "strategy": strategy}
    except Exception as e:
        logger.error(f"get_coupon_strategy failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/shop/{shop_id}/health")
async def get_shop_health(
    shop_id: str,
    days: int = Query(default=30, ge=1, le=90),
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Phan tich suc khoe tong the shop: health_score, issues, KPI summary, inventory summary.
    """
    try:
        kpi = agent.retriever.retrieve_shop_metrics(shop_id, days)
        inventory = agent.retriever.retrieve_inventory_analysis(shop_id)

        score = 100
        issues = []

        published = kpi.get("published_products", 0)
        if published == 0:
            score -= 40
            issues.append("Shop chua co san pham nao duoc publish")

        out_of_stock_count = len(inventory.get("out_of_stock", []))
        if out_of_stock_count > 0:
            penalty = min(30, out_of_stock_count * 5)
            score -= penalty
            issues.append(f"{out_of_stock_count} san pham het hang")

        avg_rating = kpi.get("avg_product_rating", 0)
        if avg_rating < 3.0 and kpi.get("total_reviews", 0) >= 5:
            score -= 20
            issues.append(f"Rating trung binh thap ({avg_rating:.1f}/5)")

        conversion = kpi.get("conversion_rate", 0)
        if kpi.get("total_views", 0) > 50 and conversion < 1.0:
            score -= 10
            issues.append(f"Ty le chuyen doi thap ({conversion:.1f}%)")

        health_status = (
            "EXCELLENT" if score >= 80 else
            "GOOD" if score >= 60 else
            "FAIR" if score >= 40 else
            "POOR"
        )

        return {
            "success": True,
            "shop_id": shop_id,
            "period_days": days,
            "health_score": max(0, score),
            "health_status": health_status,
            "issues": issues,
            "kpi_summary": {
                "published_products": published,
                "avg_rating": avg_rating,
                "total_views": kpi.get("total_views", 0),
                "total_orders": kpi.get("total_orders", 0),
                "conversion_rate": kpi.get("conversion_rate", 0),
                "estimated_revenue": kpi.get("estimated_revenue", 0),
            },
            "inventory_summary": inventory.get("summary", {}),
        }
    except Exception as e:
        logger.error(f"get_shop_health failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/system")
async def get_system_metrics(
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Lay system-level metrics cho ADMIN:
    - Tong san pham, tuong tac, review
    - Breakdown theo category
    - San pham het hang / sap het hang
    - Recent activity (7 ngay)
    """
    try:
        metrics = agent.retriever.retrieve_system_metrics()
        return {"success": True, "metrics": metrics}
    except Exception as e:
        logger.error(f"get_system_metrics failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/kpi/anomalies")
async def get_anomalies(
    days: int = Query(default=7, ge=1, le=30),
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Phat hien bat thuong trong he thong:
    - San pham het hang nhung van published
    - San pham rating thap
    - San pham khong co tuong tac
    """
    try:
        anomalies = agent.retriever.retrieve_anomalies(days)
        return {"success": True, "period_days": days, "anomalies": anomalies}
    except Exception as e:
        logger.error(f"get_anomalies failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))
