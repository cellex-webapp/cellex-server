"""
Order Tools
-----------
Tools de truy van thong tin don hang.

Note ve kien truc:
- Orders luu trong PostgreSQL (Spring Boot). ml-service truy cap qua
  user_interactions (MongoDB) de lay thong tin hanh vi mua hang.
- Neu can thong tin don hang chinh xac (status, total, items), Spring Boot
  should proxy the chatbot request va inject order context.
"""

from typing import Any, Dict, Optional
from loguru import logger

from .base import BaseTool, ToolParameter
from ..rag.retriever import Retriever


class GetMyOrdersTool(BaseTool):
    """Tool lay don hang cua user."""

    name = "get_my_orders"
    description = (
        "Lay danh sach san pham da mua cua khach hang "
        "dua tren lich su tuong tac. Chi danh cho BUYER."
    )
    parameters = [
        ToolParameter(
            name="user_id",
            type="string",
            description="User ID (tu dong dien tu context)",
            required=True,
        ),
        ToolParameter(
            name="limit",
            type="integer",
            description="So luong don hang tra ve",
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
                    "message": "Ban chua co lich su mua hang nao",
                    "orders": [],
                }

            formatted = []
            for order in orders[:limit]:
                formatted.append({
                    "productName": order.get("productName", ""),
                    "purchaseCount": order.get("purchaseCount", 0),
                    "estimatedTotal": order.get("estimatedTotal", 0),
                    "updatedAt": order.get("updatedAt", ""),
                    "note": "Du lieu tu lich su tuong tac. De xem don hang chinh xac, vui long kiem tra trong phan Quan ly don hang.",
                })

            return {
                "success": True,
                "message": f"Ban da mua {len(formatted)} san pham",
                "orders": formatted,
                "data_note": "Thong tin lay tu lich su tuong tac MongoDB. Status don hang chinh xac nam trong PostgreSQL.",
            }

        except Exception as e:
            logger.error(f"GetMyOrdersTool failed: {e}")
            return {"success": False, "message": "Loi khi lay lich su mua hang"}


class GetOrderStatusTool(BaseTool):
    """Tool kiem tra trang thai don hang."""

    name = "get_order_status"
    description = (
        "Kiem tra trang thai don hang. "
        "Luu y: trang thai chinh xac nam trong he thong quan ly don hang."
    )
    parameters = [
        ToolParameter(
            name="order_id",
            type="string",
            description="Ma don hang (order code hoac ID)",
            required=True,
        ),
        ToolParameter(
            name="user_id",
            type="string",
            description="User ID de xac thuc",
            required=True,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, order_id: str, user_id: str, **kwargs) -> Dict[str, Any]:
        """Kiem tra order status."""
        try:
            # Try to find in user_interactions
            orders = self.retriever.retrieve_order_info(user_id)

            # Filter matching order_id if specified
            matching = [
                o for o in orders
                if order_id.lower() in str(o.get("_id", "")).lower()
                or order_id.lower() in str(o.get("productName", "")).lower()
            ]

            if not matching:
                return {
                    "success": False,
                    "message": (
                        f"Khong tim thay don hang '{order_id}' cho user nay. "
                        "Don hang co the da duoc tao truoc khi he thong theo doi, "
                        "hoac ma don hang khong chinh xac. "
                        "Vui long kiem tra trong phan 'Don hang cua toi'."
                    ),
                    "suggestion": "Truy cap vao man hinh Quan ly don hang de xem trang thai chinh xac.",
                }

            order = matching[0]
            return {
                "success": True,
                "order": {
                    "productName": order.get("productName", ""),
                    "purchaseCount": order.get("purchaseCount", 0),
                    "estimatedTotal": order.get("estimatedTotal", 0),
                    "status": "purchased",
                    "updatedAt": order.get("updatedAt", ""),
                },
                "message": "Thong tin san pham da mua",
                "data_note": "De xem trang thai van chuyen chinh xac, vui long kiem tra trong phan Quan ly don hang.",
            }

        except Exception as e:
            logger.error(f"GetOrderStatusTool failed: {e}")
            return {"success": False, "message": "Loi khi kiem tra trang thai"}

    @staticmethod
    def _get_status_message(status: str) -> str:
        """Map status to Vietnamese message."""
        messages = {
            "PENDING": "Don hang dang cho xac nhan",
            "CONFIRMED": "Don hang da duoc xac nhan",
            "SHIPPING": "Don hang dang tren duong giao",
            "DELIVERED": "Don hang da giao thanh cong",
            "CANCELLED": "Don hang da bi huy",
        }
        return messages.get(status.upper() if status else "", "Trang thai khong xac dinh")
