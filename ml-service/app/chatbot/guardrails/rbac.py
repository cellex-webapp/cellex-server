"""
RBAC - Role-Based Access Control
---------------------------------
Quan ly quyen han truy cap tools theo role.

Role mapping voi Spring Boot enums:
  Spring Boot Role.ADMIN  -> ChatbotRole.ADMIN
  Spring Boot Role.VENDOR -> ChatbotRole.SELLER
  Spring Boot Role.USER   -> ChatbotRole.BUYER
"""

from enum import Enum
from typing import Optional
from pydantic import BaseModel


class Role(str, Enum):
    """User roles cho chatbot (khop voi Spring Boot Role enum qua mapping)."""

    BUYER = "BUYER"    # maps to Spring Boot Role.USER
    SELLER = "SELLER"  # maps to Spring Boot Role.VENDOR
    ADMIN = "ADMIN"    # maps to Spring Boot Role.ADMIN


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
    VIEW_ANOMALIES = "view_anomalies"

    # Coupon permissions
    VIEW_COUPONS = "view_coupons"
    SUGGEST_COUPON_STRATEGY = "suggest_coupon_strategy"

    # Inventory permissions
    VIEW_INVENTORY = "view_inventory"
    ANALYZE_STOCKOUT_RISK = "analyze_stockout_risk"


# Tool -> Required Permission mapping
TOOL_PERMISSIONS: dict[str, Permission] = {
    # ── BUYER tools ────────────────────────────────────
    "search_products": Permission.SEARCH_PRODUCTS,
    "get_product_details": Permission.VIEW_PRODUCT_DETAILS,
    "compare_products": Permission.COMPARE_PRODUCTS,
    "get_top_selling": Permission.GET_TOP_SELLING,
    "get_my_orders": Permission.VIEW_OWN_ORDERS,
    "get_order_status": Permission.VIEW_OWN_ORDERS,

    # ── SELLER tools ───────────────────────────────────
    "get_shop_kpi": Permission.VIEW_SHOP_KPI,
    "get_bestsellers": Permission.VIEW_SHOP_KPI,
    "suggest_coupon_strategy": Permission.SUGGEST_COUPON_STRATEGY,
    "analyze_inventory": Permission.ANALYZE_STOCKOUT_RISK,

    # ── ADMIN tools ────────────────────────────────────
    "get_system_metrics": Permission.VIEW_SYSTEM_KPI,
    "get_shop_health": Permission.ANALYZE_SHOP_HEALTH,
    "get_anomalies_report": Permission.VIEW_ANOMALIES,
}


# Role -> Permissions mapping
ROLE_PERMISSIONS: dict[Role, list[Permission]] = {
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
        Permission.GET_TOP_SELLING,
        Permission.VIEW_ALL_ORDERS,
        Permission.ANALYZE_ORDERS,
        Permission.VIEW_SYSTEM_KPI,
        Permission.ANALYZE_SHOP_HEALTH,
        Permission.VIEW_INVENTORY,
        Permission.VIEW_ANOMALIES,
        Permission.VIEW_COUPONS,
        Permission.SUGGEST_COUPON_STRATEGY,
        Permission.ANALYZE_STOCKOUT_RISK,
    ],
}


class RBACGuard:
    """RBAC Guard de kiem tra permissions."""

    @staticmethod
    def has_permission(role: Role, permission: Permission) -> bool:
        allowed_permissions = ROLE_PERMISSIONS.get(role, [])
        return permission in allowed_permissions

    @staticmethod
    def can_use_tool(role: Role, tool_name: str) -> bool:
        required_permission = TOOL_PERMISSIONS.get(tool_name)
        if required_permission is None:
            return True  # Public tool
        return RBACGuard.has_permission(role, required_permission)

    @staticmethod
    def get_available_tools(role: Role) -> list[str]:
        available = []
        for tool_name, permission in TOOL_PERMISSIONS.items():
            if RBACGuard.has_permission(role, permission):
                available.append(tool_name)
        return available

    @staticmethod
    def map_spring_boot_role(spring_role: str) -> Role:
        """
        Map Spring Boot Role enum to chatbot Role.
        Spring Boot: ADMIN, VENDOR, USER
        """
        mapping = {
            "ADMIN": Role.ADMIN,
            "VENDOR": Role.SELLER,
            "USER": Role.BUYER,
            # Direct mappings (if frontend sends chatbot roles)
            "SELLER": Role.SELLER,
            "BUYER": Role.BUYER,
        }
        return mapping.get(spring_role.upper() if spring_role else "USER", Role.BUYER)


class UserContext(BaseModel):
    """Context cua user trong conversation."""

    user_id: str
    role: Role
    session_id: Optional[str] = None
    metadata: dict = {}

    def can_use_tool(self, tool_name: str) -> bool:
        """Check if user can use tool."""
        return RBACGuard.can_use_tool(self.role, tool_name)
