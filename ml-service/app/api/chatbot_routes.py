"""
Chatbot API Routes
------------------
FastAPI endpoints cho chatbot service.
"""

from typing import Optional
from fastapi import APIRouter, HTTPException, Depends
from pydantic import BaseModel, Field
from loguru import logger

from ..chatbot.agent import ChatbotAgent
from ..chatbot.guardrails.rbac import Role, UserContext


router = APIRouter(prefix="/api/v1/chatbot", tags=["chatbot"])


# Request/Response models
class ChatRequest(BaseModel):
    """Chat request schema."""

    message: str = Field(..., min_length=1, max_length=2000, description="User message")
    user_id: str = Field(..., description="User ID")
    role: Role = Field(..., description="User role: BUYER, SELLER, ADMIN")
    session_id: Optional[str] = Field(None, description="Session ID for conversation continuity")
    metadata: dict = Field(default_factory=dict, description="Additional context (shop_id, etc)")


class ChatResponse(BaseModel):
    """Chat response schema."""

    message: str
    success: bool
    model: Optional[str] = None
    session_id: Optional[str] = None
    usage: dict = Field(default_factory=dict)


class IndexRequest(BaseModel):
    """Index products request schema."""

    limit: Optional[int] = Field(None, description="Limit number of products to index")


class IndexResponse(BaseModel):
    """Index response schema."""

    success: bool
    indexed_count: int
    message: str


# Global agent instance (will be set in main.py)
chatbot_agent: Optional[ChatbotAgent] = None


def get_agent() -> ChatbotAgent:
    """Dependency de lay agent instance."""
    if chatbot_agent is None:
        raise HTTPException(status_code=503, detail="Chatbot agent not initialized")
    return chatbot_agent


@router.post("/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Chat endpoint.

    Gui tin nhan va nhan response tu chatbot.
    Agent se tu dong goi tools dua tren context va role.

    Example:
        ```json
        {
          "message": "Tim dien thoai duoi 20 trieu",
          "user_id": "user123",
          "role": "BUYER",
          "session_id": "session_abc"
        }
        ```
    """
    try:
        # Tao user context
        user_context = UserContext(
            user_id=request.user_id,
            role=request.role,
            session_id=request.session_id,
            metadata=request.metadata,
        )

        # Call agent
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
    Index products vao vector store.

    Chay endpoint nay khi:
    - Lan dau setup chatbot
    - Co them san pham moi
    - Muon refresh index

    Example:
        ```json
        {
          "limit": 100
        }
        ```
    """
    try:
        count = await agent.index_products(limit=request.limit)

        return IndexResponse(
            success=count > 0,
            indexed_count=count,
            message=f"Indexed {count} products successfully",
        )

    except Exception as e:
        logger.error(f"Index failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/conversation/{session_id}")
async def clear_conversation(
    session_id: str,
    agent: ChatbotAgent = Depends(get_agent),
):
    """
    Xoa conversation history.

    Dung de reset conversation context.
    """
    try:
        agent.clear_conversation(session_id)
        return {"success": True, "message": "Conversation cleared"}

    except Exception as e:
        logger.error(f"Clear conversation failed: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/stats")
async def get_stats(agent: ChatbotAgent = Depends(get_agent)):
    """
    Lay thong tin thong ke agent.

    Returns:
        Agent stats: active conversations, tools, vector store info
    """
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
