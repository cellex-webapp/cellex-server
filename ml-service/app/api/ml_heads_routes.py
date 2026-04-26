"""
ML Heads API Routes
-------------------
FastAPI endpoints cho 4 ML Heads:
  - Demand Forecast
  - Stockout Risk
  - Churn Risk
  - Coupon Uplift

Base path: /api/v1/ml-heads

Tất cả endpoints đều hoạt động với cả PostgreSQL (đầy đủ) lẫn MongoDB-only (fallback).
"""

from typing import Optional
from fastapi import APIRouter, HTTPException, Query, Depends
from loguru import logger

from ..ml_heads.demand_forecast import DemandForecastService
from ..ml_heads.stockout_risk import StockoutRiskService
from ..ml_heads.churn_risk import ChurnRiskService
from ..ml_heads.coupon_uplift import CouponUpliftService
from ..database.postgres import get_postgres_client

router = APIRouter(prefix="/api/v1/ml-heads", tags=["ml-heads"])

# ── Global service instances (initialized in main.py) ──────────────────────
demand_service: Optional[DemandForecastService] = None
stockout_service: Optional[StockoutRiskService] = None
churn_service: Optional[ChurnRiskService] = None
coupon_service: Optional[CouponUpliftService] = None


def _check_demand():
    if demand_service is None:
        raise HTTPException(503, "DemandForecastService not initialized")
    return demand_service


def _check_stockout():
    if stockout_service is None:
        raise HTTPException(503, "StockoutRiskService not initialized")
    return stockout_service


def _check_churn():
    if churn_service is None:
        raise HTTPException(503, "ChurnRiskService not initialized")
    return churn_service


def _check_coupon():
    if coupon_service is None:
        raise HTTPException(503, "CouponUpliftService not initialized")
    return coupon_service


# ══════════════════════════════════════════════════════════════════════════════
# DEMAND FORECAST
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/demand-forecast/product/{product_id}")
async def forecast_product_demand(
    product_id: str,
    shop_id: str = Query(..., description="Shop ID (UUID)"),
    forecast_days: int = Query(default=14, ge=1, le=90, description="Số ngày cần dự báo"),
    history_days: int = Query(default=60, ge=7, le=180, description="Số ngày lịch sử sử dụng"),
    svc: DemandForecastService = Depends(_check_demand),
):
    """
    Dự báo nhu cầu cho một sản phẩm cụ thể.
    
    Returns:
    - daily_forecast: danh sách dự báo theo ngày
    - days_until_stockout: ước tính ngày hết hàng
    - stockout_risk: CRITICAL / HIGH / MEDIUM / LOW
    - avg_daily_demand: nhu cầu trung bình mỗi ngày
    
    Example:
        GET /api/v1/ml-heads/demand-forecast/product/prod123?shop_id=uuid&forecast_days=14
    """
    try:
        result = svc.forecast_product(product_id, shop_id, forecast_days, history_days)
        return result
    except Exception as e:
        logger.error(f"forecast_product_demand failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/demand-forecast/shop/{shop_id}")
async def forecast_shop_demand(
    shop_id: str,
    forecast_days: int = Query(default=14, ge=1, le=90),
    history_days: int = Query(default=90, ge=7, le=180),
    top_n_products: int = Query(default=10, ge=1, le=50),
    svc: DemandForecastService = Depends(_check_demand),
):
    """
    Dự báo nhu cầu tổng hợp cho toàn bộ shop.
    
    Returns:
    - shop_level_forecast: tổng nhu cầu theo ngày
    - top_products_by_demand: sản phẩm có nhu cầu cao nhất
    
    Dùng cho SELLER dashboard để lập kế hoạch nhập hàng.
    """
    try:
        result = svc.forecast_shop(shop_id, forecast_days, history_days, top_n_products)
        return result
    except Exception as e:
        logger.error(f"forecast_shop_demand failed: {e}")
        raise HTTPException(500, str(e))


# ══════════════════════════════════════════════════════════════════════════════
# STOCKOUT RISK
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/stockout-risk/shop/{shop_id}")
async def analyze_shop_stockout(
    shop_id: str,
    analysis_days: int = Query(default=30, ge=7, le=90, description="Số ngày phân tích velocity"),
    svc: StockoutRiskService = Depends(_check_stockout),
):
    """
    Phân tích stockout risk cho tất cả sản phẩm của shop.
    
    Returns:
    - summary: số lượng sản phẩm theo risk level (CRITICAL/HIGH/MEDIUM/LOW/SAFE)
    - products_by_risk: chi tiết theo từng nhóm
    - urgent_action_required: danh sách sản phẩm cần xử lý ngay
    
    Dùng cho SELLER inventory management.
    """
    try:
        result = svc.analyze_shop(shop_id, analysis_days)
        return result
    except Exception as e:
        logger.error(f"analyze_shop_stockout failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/stockout-risk/product/{product_id}")
async def analyze_product_stockout(
    product_id: str,
    shop_id: Optional[str] = Query(default=None),
    analysis_days: int = Query(default=30, ge=7, le=90),
    svc: StockoutRiskService = Depends(_check_stockout),
):
    """
    Phân tích stockout risk cho một sản phẩm cụ thể.
    
    Returns:
    - risk_level, days_until_stockout, daily_velocity
    - reorder_suggestion: gợi ý số lượng và thời điểm đặt hàng
    """
    try:
        result = svc.analyze_product(product_id, shop_id, analysis_days)
        return result
    except Exception as e:
        logger.error(f"analyze_product_stockout failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/stockout-risk/system")
async def system_stockout_report(
    svc: StockoutRiskService = Depends(_check_stockout),
):
    """
    Báo cáo stockout toàn hệ thống (ADMIN).
    Trả về danh sách sản phẩm đã hết hàng và sắp hết hàng.
    """
    try:
        result = svc.get_system_stockout_report()
        return result
    except Exception as e:
        logger.error(f"system_stockout_report failed: {e}")
        raise HTTPException(500, str(e))


# ══════════════════════════════════════════════════════════════════════════════
# CHURN RISK
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/churn-risk/buyer/{user_id}")
async def analyze_buyer_churn(
    user_id: str,
    svc: ChurnRiskService = Depends(_check_churn),
):
    """
    Phân tích churn risk cho một buyer cụ thể.
    
    Returns:
    - risk_level: ACTIVE / LOW / MEDIUM / HIGH
    - churn_probability: 0.0 - 1.0
    - rfm: Recency, Frequency, Monetary metrics
    - recommendations: gợi ý hành động giữ chân user
    
    Dùng trong SELLER / ADMIN dashboard để identify at-risk users.
    """
    try:
        result = svc.analyze_buyer(user_id)
        return result
    except Exception as e:
        logger.error(f"analyze_buyer_churn failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/churn-risk/buyers")
async def analyze_all_buyers_churn(
    limit: int = Query(default=100, ge=1, le=500),
    svc: ChurnRiskService = Depends(_check_churn),
):
    """
    Phân tích churn risk cho tất cả buyers (ADMIN).
    
    Returns:
    - summary: số lượng theo risk level
    - high_risk_users: danh sách 20 users có nguy cơ churn cao nhất
    - churn_rate_estimate: ước tính tỷ lệ churn %
    
    Requires PostgreSQL connection.
    """
    try:
        result = svc.analyze_all_buyers(limit)
        return result
    except Exception as e:
        logger.error(f"analyze_all_buyers_churn failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/churn-risk/shop/{shop_id}")
async def analyze_shop_churn(
    shop_id: str,
    svc: ChurnRiskService = Depends(_check_churn),
):
    """
    Phân tích churn risk của một shop (ADMIN).
    Kiểm tra xem shop có đang hoạt động không hay đã "churn" (bỏ bán).
    
    Returns:
    - risk_level: ACTIVE / LOW / MEDIUM / HIGH
    - days_since_last_order
    - recommendation: gợi ý can thiệp
    """
    try:
        result = svc.analyze_shop_churn_risk(shop_id)
        return result
    except Exception as e:
        logger.error(f"analyze_shop_churn failed: {e}")
        raise HTTPException(500, str(e))


# ══════════════════════════════════════════════════════════════════════════════
# COUPON UPLIFT
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/coupon-uplift/shop/{shop_id}")
async def shop_coupon_effectiveness(
    shop_id: str,
    days: int = Query(default=90, ge=14, le=365),
    svc: CouponUpliftService = Depends(_check_coupon),
):
    """
    Phân tích hiệu quả coupon của shop.
    
    Returns:
    - uplift_analysis: tỷ lệ hoàn thành đơn có/không có coupon
    - coupon_economics: ROI, average discount, order value lift
    - recommendation: loại coupon tối ưu và mức giảm giá gợi ý
    
    Dùng cho SELLER để đánh giá và tối ưu chiến lược coupon.
    """
    try:
        result = svc.analyze_coupon_effectiveness(shop_id, days)
        return result
    except Exception as e:
        logger.error(f"shop_coupon_effectiveness failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/coupon-uplift/system")
async def system_coupon_effectiveness(
    days: int = Query(default=90, ge=14, le=365),
    svc: CouponUpliftService = Depends(_check_coupon),
):
    """
    Phân tích hiệu quả coupon toàn hệ thống (ADMIN).
    """
    try:
        result = svc.analyze_coupon_effectiveness(None, days)
        return result
    except Exception as e:
        logger.error(f"system_coupon_effectiveness failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/coupon-uplift/user/{user_id}")
async def user_coupon_uplift(
    user_id: str,
    shop_id: Optional[str] = Query(default=None, description="Filter theo shop cụ thể (optional)"),
    svc: CouponUpliftService = Depends(_check_coupon),
):
    """
    Dự báo khả năng uplift nếu gửi coupon cho user.
    
    Returns:
    - uplift_score: 0.0 - 1.0 (cao = nên gửi coupon)
    - uplift_tier: MINIMAL / LOW / MEDIUM / HIGH
    - recommended_coupon: loại và giá trị coupon phù hợp
    - reason: giải thích scoring
    
    Dùng cho targeted coupon campaigns.
    """
    try:
        result = svc.predict_user_uplift(user_id, shop_id)
        return result
    except Exception as e:
        logger.error(f"user_coupon_uplift failed: {e}")
        raise HTTPException(500, str(e))


@router.get("/coupon-uplift/shop/{shop_id}/high-potential-products")
async def shop_high_uplift_products(
    shop_id: str,
    limit: int = Query(default=10, ge=1, le=50),
    svc: CouponUpliftService = Depends(_check_coupon),
):
    """
    Tìm sản phẩm có high uplift potential (nhiều view, ít mua).
    Đây là target tốt nhất cho coupon campaign.
    
    Returns:
    - high_uplift_products: sorted by uplift_potential_score desc
    - suggested_discount_pct: % giảm giá gợi ý cho từng sản phẩm
    - reason: lý do sản phẩm được chọn
    """
    try:
        result = svc.get_high_uplift_products(shop_id, limit)
        return result
    except Exception as e:
        logger.error(f"shop_high_uplift_products failed: {e}")
        raise HTTPException(500, str(e))


# ══════════════════════════════════════════════════════════════════════════════
# HEALTH & INFO
# ══════════════════════════════════════════════════════════════════════════════

@router.get("/health")
async def ml_heads_health():
    """Health check cho ML Heads service."""
    pg = get_postgres_client()
    return {
        "status": "healthy",
        "services": {
            "demand_forecast": demand_service is not None,
            "stockout_risk": stockout_service is not None,
            "churn_risk": churn_service is not None,
            "coupon_uplift": coupon_service is not None,
        },
        "data_sources": {
            "postgresql": pg.is_connected,
            "mongodb": True,  # Always available (required)
        },
        "note": "Tất cả ML heads hoạt động với MongoDB. PostgreSQL cung cấp thêm dữ liệu đơn hàng thực tế.",
    }


@router.get("/info")
async def ml_heads_info():
    """Thông tin về các ML Heads endpoints."""
    return {
        "ml_heads": {
            "demand_forecast": {
                "description": "Dự báo nhu cầu sản phẩm theo ngày",
                "endpoints": [
                    "GET /api/v1/ml-heads/demand-forecast/product/{product_id}",
                    "GET /api/v1/ml-heads/demand-forecast/shop/{shop_id}",
                ],
                "data_required": "PostgreSQL (orders, order_items) + MongoDB (products)",
            },
            "stockout_risk": {
                "description": "Phân tích rủi ro hết hàng",
                "endpoints": [
                    "GET /api/v1/ml-heads/stockout-risk/product/{product_id}",
                    "GET /api/v1/ml-heads/stockout-risk/shop/{shop_id}",
                    "GET /api/v1/ml-heads/stockout-risk/system",
                ],
                "data_required": "MongoDB (products.stockQuantity) + PostgreSQL (order velocity)",
            },
            "churn_risk": {
                "description": "Dự báo nguy cơ churn buyer/seller (RFM model)",
                "endpoints": [
                    "GET /api/v1/ml-heads/churn-risk/buyer/{user_id}",
                    "GET /api/v1/ml-heads/churn-risk/buyers",
                    "GET /api/v1/ml-heads/churn-risk/shop/{shop_id}",
                ],
                "data_required": "PostgreSQL (orders RFM data) + MongoDB (user_interactions fallback)",
            },
            "coupon_uplift": {
                "description": "Phân tích và dự báo hiệu quả coupon",
                "endpoints": [
                    "GET /api/v1/ml-heads/coupon-uplift/shop/{shop_id}",
                    "GET /api/v1/ml-heads/coupon-uplift/system",
                    "GET /api/v1/ml-heads/coupon-uplift/user/{user_id}",
                    "GET /api/v1/ml-heads/coupon-uplift/shop/{shop_id}/high-potential-products",
                ],
                "data_required": "PostgreSQL (orders, user_coupons) + MongoDB (user_interactions)",
            },
        }
    }
