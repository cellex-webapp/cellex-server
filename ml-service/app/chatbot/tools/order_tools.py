"""
Order Tools
-----------
Tools de truy van thong tin don hang.
"""

from typing import Any, Dict, Optional
from loguru import logger

from .base import BaseTool, ToolParameter
from ..rag.retriever import Retriever


class GetMyOrdersTool(BaseTool):
    """Tool lay don hang cua user."""

    name = "get_my_orders"
    description = "Lay danh sach don hang cua khach hang (chi permission BUYER)."
    parameters = [
        ToolParameter(
            name="user_id",
            type="string",
            description="User ID (tu context)",
            required=True,
        ),
        ToolParameter(
            name="limit",
            type="integer",
            description="So luong don hang",
            required=False,
            default=10,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, user_id: str, limit: int = 10, **kwargs) -> Dict[str, Any]:
        """Lay orders cua user."""
        try:
            orders = self.retriever.retrieve_order_info(user_id)

            if not orders:
                return {
                    "success": True,
                    "message": "Ban chua co don hang nao",
                    "orders": [],
                }

            # Format orders
            formatted = []
            for order in orders[:limit]:
                formatted.append(
                    {
                        "id": str(order.get("_id")),
                        "status": order.get("status", "unknown"),
                        "total": order.get("total", 0),
                        "items_count": len(order.get("items", [])),
                        "created_at": str(order.get("created_at", "")),
                    }
                )

            return {
                "success": True,
                "message": f"Ban co {len(formatted)} don hang",
                "orders": formatted,
            }

        except Exception as e:
            logger.error(f"GetMyOrdersTool failed: {e}")
            return {"success": False, "message": "Loi khi lay don hang"}


class GetOrderStatusTool(BaseTool):
    """Tool kiem tra trang thai don hang."""

    name = "get_order_status"
    description = "Kiem tra trang thai cua mot don hang cu the."
    parameters = [
        ToolParameter(
            name="order_id",
            type="string",
            description="Ma don hang",
            required=True,
        ),
        ToolParameter(
            name="user_id",
            type="string",
            description="User ID de verify",
            required=True,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, order_id: str, user_id: str, **kwargs) -> Dict[str, Any]:
        """Kiem tra order status."""
        try:
            orders = self.retriever.retrieve_order_info(user_id, order_id)

            if not orders:
                return {
                    "success": False,
                    "message": "Khong tim thay don hang",
                }

            order = orders[0]

            return {
                "success": True,
                "order": {
                    "id": str(order.get("_id")),
                    "status": order.get("status"),
                    "status_message": self._get_status_message(order.get("status")),
                    "total": order.get("total"),
                    "items": order.get("items", []),
                    "created_at": str(order.get("created_at")),
                    "updated_at": str(order.get("updated_at")),
                },
            }

        except Exception as e:
            logger.error(f"GetOrderStatusTool failed: {e}")
            return {"success": False, "message": "Loi khi kiem tra trang thai"}

    @staticmethod
    def _get_status_message(status: str) -> str:
        """Map status to Vietnamese message."""
        messages = {
            "pending": "Don hang dang cho xu ly",
            "processing": "Don hang dang duoc xu ly",
            "shipped": "Don hang dang tren duong giao",
            "delivered": "Don hang da giao thanh cong",
            "cancelled": "Don hang da bi huy",
        }
        return messages.get(status, "Trang thai khong xac dinh")
