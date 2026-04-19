"""
RBAC - Role-Based Access Control
---------------------------------
Quan ly quyen han truy cap tools theo role.
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel


class Role(str, Enum):
    """User roles trong he thong."""

    BUYER = "BUYER"
    SELLER = "SELLER"
    ADMIN = "ADMIN"


class Permission(str, Enum):
    """Danh sach permissions."""

    # Product permissions
    SEARCH_PRODUCTS = "search_products"
    VIEW_PRODUCT_DETAILS = "view_product_details"
    COMPARE_PRODUCTS = "compare_products"
    GET_TOP_SELLING = "get_top_selling"

    # Order permissions
    VIEW_OWN_ORDERS = "view_own_orders"
    VIEW_ALL_ORDERS = "view_all_orders"
    ANALYZE_ORDERS = "analyze_orders"

    # KPI permissions
    VIEW_SHOP_KPI = "view_shop_kpi"
    VIEW_SYSTEM_KPI = "view_system_kpi"
    ANALYZE_SHOP_HEALTH = "analyze_shop_health"

    # Coupon permissions
    VIEW_COUPONS = "view_coupons"
    SUGGEST_COUPON_STRATEGY = "suggest_coupon_strategy"

    # Inventory permissions
    VIEW_INVENTORY = "view_inventory"
    ANALYZE_STOCKOUT_RISK = "analyze_stockout_risk"


# Tool -> Required Permission mapping
TOOL_PERMISSIONS = {
    # BUYER tools
    "search_products": Permission.SEARCH_PRODUCTS,
    "get_product_details": Permission.VIEW_PRODUCT_DETAILS,
    "compare_products": Permission.COMPARE_PRODUCTS,
    "get_top_selling": Permission.GET_TOP_SELLING,
    "get_my_orders": Permission.VIEW_OWN_ORDERS,
    "get_order_status": Permission.VIEW_OWN_ORDERS,
    # SELLER tools
    "get_shop_kpi": Permission.VIEW_SHOP_KPI,
    "get_bestsellers": Permission.VIEW_SHOP_KPI,
    "analyze_inventory": Permission.ANALYZE_STOCKOUT_RISK,
    "suggest_coupon_strategy": Permission.SUGGEST_COUPON_STRATEGY,
    # ADMIN tools
    "get_system_metrics": Permission.VIEW_SYSTEM_KPI,
    "get_shop_health": Permission.ANALYZE_SHOP_HEALTH,
    "get_all_orders": Permission.VIEW_ALL_ORDERS,
    "analyze_order_issues": Permission.ANALYZE_ORDERS,
}


# Role -> Permissions mapping
ROLE_PERMISSIONS = {
    Role.BUYER: [
        Permission.SEARCH_PRODUCTS,
        Permission.VIEW_PRODUCT_DETAILS,
        Permission.COMPARE_PRODUCTS,
        Permission.GET_TOP_SELLING,
        Permission.VIEW_OWN_ORDERS,
        Permission.VIEW_COUPONS,
    ],
    Role.SELLER: [
        Permission.SEARCH_PRODUCTS,
        Permission.VIEW_PRODUCT_DETAILS,
        Permission.VIEW_SHOP_KPI,
        Permission.VIEW_INVENTORY,
        Permission.ANALYZE_STOCKOUT_RISK,
        Permission.SUGGEST_COUPON_STRATEGY,
    ],
    Role.ADMIN: [
        # Admin co tat ca permissions
        Permission.SEARCH_PRODUCTS,
        Permission.VIEW_PRODUCT_DETAILS,
        Permission.COMPARE_PRODUCTS,
        Permission.VIEW_ALL_ORDERS,
        Permission.ANALYZE_ORDERS,
        Permission.VIEW_SYSTEM_KPI,
        Permission.ANALYZE_SHOP_HEALTH,
        Permission.VIEW_INVENTORY,
    ],
}


class RBACGuard:
    """
    RBAC Guard de kiem tra permissions.
    """

    @staticmethod
    def has_permission(role: Role, permission: Permission) -> bool:
        """
        Kiem tra role co permission hay khong.

        Args:
            role: User role
            permission: Required permission

        Returns:
            True neu co quyen, False neu khong
        """
        allowed_permissions = ROLE_PERMISSIONS.get(role, [])
        return permission in allowed_permissions

    @staticmethod
    def can_use_tool(role: Role, tool_name: str) -> bool:
        """
        Kiem tra role co the su dung tool hay khong.

        Args:
            role: User role
            tool_name: Ten cua tool

        Returns:
            True neu co quyen, False neu khong
        """
        required_permission = TOOL_PERMISSIONS.get(tool_name)
        if required_permission is None:
            # Tool khong yeu cau permission (public)
            return True

        return RBACGuard.has_permission(role, required_permission)

    @staticmethod
    def get_available_tools(role: Role) -> list[str]:
        """
        Lay danh sach tools ma role co the su dung.

        Args:
            role: User role

        Returns:
            List cac tool names
        """
        available = []
        for tool_name in TOOL_PERMISSIONS.keys():
            if RBACGuard.can_use_tool(role, tool_name):
                available.append(tool_name)
        return available


class UserContext(BaseModel):
    """
    Context cua user trong conversation.
    """

    user_id: str
    role: Role
    session_id: Optional[str] = None
    metadata: dict = {}

    def can_use_tool(self, tool_name: str) -> bool:
        """Check if user can use tool."""
        return RBACGuard.can_use_tool(self.role, tool_name)
