"""
KPI Tools
---------
Tools de lay metrics va phan tich KPI (cho SELLER va ADMIN).

Thay doi:
- GetShopKPITool: goi retriever.retrieve_shop_metrics() thật (MongoDB aggregation)
- GetBestsellersTool: goi retriever.retrieve_shop_bestsellers() filter theo shopId
- GetSystemMetricsTool: goi retriever.retrieve_system_metrics() thật
- GetShopHealthTool: NEW — phan tich suc khoe shop (ADMIN)
- GetAnomaliesReportTool: NEW — phat hien bat thuong (ADMIN)
- SuggestCouponStrategyTool: NEW — de xuat coupon (SELLER)
"""

from typing import Any, Dict, Optional
from loguru import logger

from .base import BaseTool, ToolParameter
from ..rag.retriever import Retriever


class GetShopKPITool(BaseTool):
    """Tool lay KPI cua shop (SELLER role)."""

    name = "get_shop_kpi"
    description = (
        "Lay KPI cua shop: san pham, luot xem, gio hang, don mua, "
        "ty le chuyen doi, ước tinh doanh thu trong N ngay. SELLER only."
    )
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID (se tu dong dien tu context)",
            required=True,
        ),
        ToolParameter(
            name="days",
            type="integer",
            description="So ngay look back (7, 14, 30)",
            required=False,
            default=7,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, days: int = 7, **kwargs) -> Dict[str, Any]:
        """Lay shop KPI tu MongoDB aggregation."""
        try:
            metrics = self.retriever.retrieve_shop_metrics(shop_id, days)

            return {
                "success": True,
                "shop_id": shop_id,
                "period_days": days,
                "metrics": metrics,
                "message": f"KPI shop trong {days} ngay qua",
            }

        except Exception as e:
            logger.error(f"GetShopKPITool failed: {e}")
            return {"success": False, "message": "Loi khi lay KPI shop", "error": str(e)}


class GetSystemMetricsTool(BaseTool):
    """Tool lay system metrics (ADMIN role)."""

    name = "get_system_metrics"
    description = (
        "Lay metrics toan he thong: tong product, tuong tac, review, "
        "phan tich category, san pham het hang. ADMIN only."
    )
    parameters = []

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, **kwargs) -> Dict[str, Any]:
        """Lay system metrics tu MongoDB."""
        try:
            metrics = self.retriever.retrieve_system_metrics()

            return {
                "success": True,
                "metrics": metrics,
                "message": "System metrics overview",
            }

        except Exception as e:
            logger.error(f"GetSystemMetricsTool failed: {e}")
            return {"success": False, "message": "Loi khi lay system metrics", "error": str(e)}


class GetBestsellersTool(BaseTool):
    """Tool lay bestsellers cua shop (SELLER role)."""

    name = "get_bestsellers"
    description = "Lay san pham ban chay nhat cua shop theo purchaseCount va reviewCount. SELLER only."
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID (se tu dong dien tu context)",
            required=True,
        ),
        ToolParameter(
            name="limit",
            type="integer",
            description="So luong san pham tra ve",
            required=False,
            default=10,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, limit: int = 10, **kwargs) -> Dict[str, Any]:
        """Lay bestsellers theo shopId."""
        try:
            bestsellers = self.retriever.retrieve_shop_bestsellers(shop_id, limit)

            formatted = []
            for p in bestsellers:
                formatted.append({
                    "id": p.get("id", ""),
                    "name": p.get("name", ""),
                    "price": p.get("finalPrice") or p.get("price", 0),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                    "stockQuantity": p.get("stockQuantity"),
                })

            return {
                "success": True,
                "shop_id": shop_id,
                "bestsellers": formatted,
                "total": len(formatted),
                "message": f"Top {len(formatted)} san pham ban chay cua shop",
            }

        except Exception as e:
            logger.error(f"GetBestsellersTool failed: {e}")
            return {"success": False, "message": "Loi khi lay bestsellers", "error": str(e)}


class GetShopHealthTool(BaseTool):
    """Tool phan tich suc khoe shop (ADMIN role)."""

    name = "get_shop_health"
    description = (
        "Phan tich suc khoe tong the cua mot shop: "
        "san pham, tuong tac, ton kho, doanh thu uoc tinh. ADMIN only."
    )
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID can kiem tra",
            required=True,
        ),
        ToolParameter(
            name="days",
            type="integer",
            description="So ngay de phan tich",
            required=False,
            default=30,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, days: int = 30, **kwargs) -> Dict[str, Any]:
        """Phan tich shop health."""
        try:
            kpi = self.retriever.retrieve_shop_metrics(shop_id, days)
            inventory = self.retriever.retrieve_inventory_analysis(shop_id)

            # Health score (0-100)
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
                issues.append(f"Rating trung binh san pham thap ({avg_rating:.1f}/5)")

            conversion = kpi.get("conversion_rate", 0)
            if kpi.get("total_views", 0) > 50 and conversion < 1.0:
                score -= 10
                issues.append(f"Ty le chuyen doi thap ({conversion:.1f}%)")

            health_status = "EXCELLENT" if score >= 80 else "GOOD" if score >= 60 else "FAIR" if score >= 40 else "POOR"

            return {
                "success": True,
                "shop_id": shop_id,
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
                "message": f"Health check shop {shop_id}: {health_status} (score: {max(0, score)}/100)",
            }

        except Exception as e:
            logger.error(f"GetShopHealthTool failed: {e}")
            return {"success": False, "message": "Loi khi kiem tra shop health", "error": str(e)}


class GetAnomaliesReportTool(BaseTool):
    """Tool phat hien bat thuong trong he thong (ADMIN role)."""

    name = "get_anomalies_report"
    description = (
        "Phat hien bat thuong trong he thong: san pham het hang, "
        "rating thap, san pham khong co tuong tac. ADMIN only."
    )
    parameters = [
        ToolParameter(
            name="days",
            type="integer",
            description="So ngay de phan tich (mac dinh 7)",
            required=False,
            default=7,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, days: int = 7, **kwargs) -> Dict[str, Any]:
        """Phat hien bat thuong."""
        try:
            anomalies = self.retriever.retrieve_anomalies(days)
            return {
                "success": True,
                "anomalies": anomalies,
                "message": f"Ket qua phat hien bat thuong trong {days} ngay qua",
            }

        except Exception as e:
            logger.error(f"GetAnomaliesReportTool failed: {e}")
            return {"success": False, "message": "Loi khi phat hien bat thuong", "error": str(e)}


class SuggestCouponStrategyTool(BaseTool):
    """Tool de xuat chien luoc coupon cho shop (SELLER role)."""

    name = "suggest_coupon_strategy"
    description = (
        "Phan tich san pham va tuong tac de de xuat chien luoc coupon "
        "phu hop cho shop. SELLER only."
    )
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID (se tu dong dien tu context)",
            required=True,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, **kwargs) -> Dict[str, Any]:
        """De xuat coupon strategy."""
        try:
            strategy = self.retriever.retrieve_coupon_strategy(shop_id)
            return {
                "success": True,
                "shop_id": shop_id,
                "coupon_strategy": strategy,
                "message": "De xuat chien luoc coupon",
            }

        except Exception as e:
            logger.error(f"SuggestCouponStrategyTool failed: {e}")
            return {"success": False, "message": "Loi khi phan tich coupon strategy", "error": str(e)}


class AnalyzeInventoryTool(BaseTool):
    """Tool phan tich ton kho va stockout risk (SELLER role)."""

    name = "analyze_inventory"
    description = (
        "Phan tich ton kho shop: san pham het hang, sap het hang, "
        "ton kho qua nhieu, toc do ban hang. SELLER only."
    )
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID (se tu dong dien tu context)",
            required=True,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, **kwargs) -> Dict[str, Any]:
        """Phan tich inventory."""
        try:
            inventory = self.retriever.retrieve_inventory_analysis(shop_id)
            return {
                "success": True,
                "shop_id": shop_id,
                "inventory": inventory,
                "message": "Phan tich ton kho shop",
            }

        except Exception as e:
            logger.error(f"AnalyzeInventoryTool failed: {e}")
            return {"success": False, "message": "Loi khi phan tich ton kho", "error": str(e)}
