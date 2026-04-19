"""
KPI Tools
---------
Tools de lay metrics va phan tich KPI (cho SELLER va ADMIN).
"""

from typing import Any, Dict, Optional
from loguru import logger

from .base import BaseTool, ToolParameter
from ..rag.retriever import Retriever


class GetShopKPITool(BaseTool):
    """Tool lay KPI cua shop (SELLER role)."""

    name = "get_shop_kpi"
    description = "Lay KPI cua shop: doanh thu, don hang, conversion rate (SELLER only)."
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID",
            required=True,
        ),
        ToolParameter(
            name="days",
            type="integer",
            description="So ngay look back",
            required=False,
            default=7,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, days: int = 7, **kwargs) -> Dict[str, Any]:
        """Lay shop KPI."""
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
            return {"success": False, "message": "Loi khi lay KPI shop"}


class GetSystemMetricsTool(BaseTool):
    """Tool lay system metrics (ADMIN role)."""

    name = "get_system_metrics"
    description = "Lay metrics toan he thong: tong product, order, user (ADMIN only)."
    parameters = []

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, **kwargs) -> Dict[str, Any]:
        """Lay system metrics."""
        try:
            metrics = self.retriever.retrieve_system_metrics()

            return {
                "success": True,
                "metrics": metrics,
                "message": "System metrics overview",
            }

        except Exception as e:
            logger.error(f"GetSystemMetricsTool failed: {e}")
            return {"success": False, "message": "Loi khi lay system metrics"}


class GetBestsellersTool(BaseTool):
    """Tool lay bestsellers (SELLER role)."""

    name = "get_bestsellers"
    description = "Lay san pham ban chay nhat cua shop (SELLER only)."
    parameters = [
        ToolParameter(
            name="shop_id",
            type="string",
            description="Shop ID",
            required=True,
        ),
        ToolParameter(
            name="limit",
            type="integer",
            description="So luong san pham",
            required=False,
            default=10,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, shop_id: str, limit: int = 10, **kwargs) -> Dict[str, Any]:
        """Lay bestsellers."""
        try:
            # Placeholder - implement khi co data
            db = self.retriever.db

            products = list(
                db.products.find({})  # Add shop_id filter khi co
                .sort([("review_count", -1)])
                .limit(limit)
            )

            formatted = []
            for p in products:
                formatted.append(
                    {
                        "id": str(p.get("_id")),
                        "title": p.get("title"),
                        "price": p.get("price"),
                        "sales_count": p.get("review_count", 0),  # Proxy for sales
                    }
                )

            return {
                "success": True,
                "shop_id": shop_id,
                "bestsellers": formatted,
                "message": f"Top {len(formatted)} san pham ban chay",
            }

        except Exception as e:
            logger.error(f"GetBestsellersTool failed: {e}")
            return {"success": False, "message": "Loi khi lay bestsellers"}
