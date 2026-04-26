"""
Product Tools
-------------
Tools de truy van thong tin san pham.
Cap nhat: dung dung field names cua Product.java (MongoDB):
  name, description, images, price, finalPrice, averageRating, reviewCount, shopId, categoryId
"""

from typing import Any, Dict, List, Optional
from loguru import logger

from .base import BaseTool, ToolParameter
from ..rag.retriever import Retriever


class SearchProductsTool(BaseTool):
    """Tool tim kiem san pham."""

    name = "search_products"
    description = "Tim kiem san pham theo tu khoa. Su dung cho cau hoi 'tim dien thoai', 'laptop gia re', etc."
    parameters = [
        ToolParameter(
            name="query",
            type="string",
            description="Tu khoa tim kiem (vd: 'iPhone 15', 'laptop gaming')",
            required=True,
        ),
        ToolParameter(
            name="limit",
            type="integer",
            description="So luong ket qua toi da",
            required=False,
            default=5,
        ),
        ToolParameter(
            name="min_price",
            type="number",
            description="Gia thap nhat",
            required=False,
        ),
        ToolParameter(
            name="max_price",
            type="number",
            description="Gia cao nhat",
            required=False,
        ),
        ToolParameter(
            name="category_id",
            type="string",
            description="ID danh muc",
            required=False,
        ),
        ToolParameter(
            name="brand",
            type="string",
            description="Thuong hieu (vd: 'Apple', 'Samsung')",
            required=False,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(
        self,
        query: str,
        limit: int = 5,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        category_id: Optional[str] = None,
        brand: Optional[str] = None,
        **kwargs
    ) -> Dict[str, Any]:
        """Tim kiem san pham."""
        try:
            # Construct filters for ChromaDB
            filters = {}
            filter_list = []
            
            if min_price is not None:
                filter_list.append({"final_price": {"$gte": min_price}})
            if max_price is not None:
                filter_list.append({"final_price": {"$lte": max_price}})
            if category_id:
                filter_list.append({"category_id": category_id})
            if brand:
                filter_list.append({"brand": brand})
                
            if len(filter_list) > 1:
                filters = {"$and": filter_list}
            elif len(filter_list) == 1:
                filters = filter_list[0]
            else:
                filters = None

            products = self.retriever.retrieve_products(query, top_k=limit, filters=filters)

            if not products:
                return {
                    "success": False,
                    "message": "Khong tim thay san pham phu hop",
                    "products": [],
                }

            formatted = []
            for p in products:
                formatted.append({
                    "id": p.get("id", ""),
                    "name": p.get("name", ""),
                    "price": p.get("price"),
                    "finalPrice": p.get("finalPrice"),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                    "image": p.get("image"),
                    "shopId": p.get("shopId"),
                    "categoryId": p.get("categoryId"),
                    "relevance_score": round(p.get("relevance_score", 0), 3),
                })

            return {
                "success": True,
                "message": f"Tim thay {len(formatted)} san pham",
                "products": formatted,
            }

        except Exception as e:
            logger.error(f"SearchProductsTool failed: {e}")
            return {"success": False, "message": "Loi khi tim kiem", "products": []}


class GetProductDetailsTool(BaseTool):
    """Tool lay chi tiet san pham."""

    name = "get_product_details"
    description = "Lay thong tin chi tiet cua san pham theo ID. Bao gom specs, rating, reviews."
    parameters = [
        ToolParameter(
            name="product_id",
            type="string",
            description="ID cua san pham",
            required=True,
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, product_id: str, **kwargs) -> Dict[str, Any]:
        """Lay chi tiet san pham."""
        try:
            context = self.retriever.retrieve_product_context(product_id)

            if not context["product"]:
                return {
                    "success": False,
                    "message": "Khong tim thay san pham",
                }

            p = context["product"]

            return {
                "success": True,
                "product": {
                    "id": p.get("id", ""),
                    "name": p.get("name", ""),
                    "description": p.get("description", ""),
                    "price": p.get("price"),
                    "finalPrice": p.get("finalPrice"),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                    "stockQuantity": p.get("stockQuantity"),
                    "shopId": p.get("shopId"),
                    "categoryId": p.get("categoryId"),
                    "images": p.get("images", []),
                    "attributeValues": p.get("attributeValues", []),
                },
                "reviews_count": len(context["reviews"]),
                "similar_products": [
                    {
                        "id": s.get("id", ""),
                        "name": s.get("name", ""),
                        "finalPrice": s.get("finalPrice"),
                        "averageRating": s.get("averageRating", 0),
                    }
                    for s in context["similar_products"][:3]
                ],
            }

        except Exception as e:
            logger.error(f"GetProductDetailsTool failed: {e}")
            return {"success": False, "message": "Loi khi lay thong tin"}


class CompareProductsTool(BaseTool):
    """Tool so sanh san pham."""

    name = "compare_products"
    description = "So sanh 2 hoac nhieu san pham voi nhau (gia, specs, rating)."
    parameters = [
        ToolParameter(
            name="product_ids",
            type="array",
            description="Danh sach product IDs can so sanh (toi thieu 2)",
            required=True,
            items={"type": "STRING"},
        ),
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, product_ids: List[str], **kwargs) -> Dict[str, Any]:
        """So sanh san pham."""
        if len(product_ids) < 2:
            return {
                "success": False,
                "message": "Can it nhat 2 san pham de so sanh",
            }

        try:
            products = []
            for pid in product_ids:
                context = self.retriever.retrieve_product_context(pid)
                if context["product"]:
                    products.append(context["product"])

            if len(products) < 2:
                return {
                    "success": False,
                    "message": "Khong du san pham de so sanh",
                }

            comparison = []
            for p in products:
                comparison.append({
                    "id": p.get("id", ""),
                    "name": p.get("name", ""),
                    "price": p.get("price"),
                    "finalPrice": p.get("finalPrice"),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                    "stockQuantity": p.get("stockQuantity"),
                })

            return {
                "success": True,
                "message": f"So sanh {len(comparison)} san pham",
                "products": comparison,
            }

        except Exception as e:
            logger.error(f"CompareProductsTool failed: {e}")
            return {"success": False, "message": "Loi khi so sanh"}


class GetTopSellingTool(BaseTool):
    """Tool lay san pham ban chay."""

    name = "get_top_selling"
    description = "Lay danh sach san pham ban chay nhat theo category hoac toan he thong."
    parameters = [
        ToolParameter(
            name="category_id",
            type="string",
            description="Category ID (optional). De trong de lay toan bo.",
            required=False,
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

    async def execute(
        self, category_id: Optional[str] = None, limit: int = 10, **kwargs
    ) -> Dict[str, Any]:
        """Lay top selling products."""
        try:
            db = self.retriever.db
            query: Dict = {"isPublished": True}
            if category_id:
                query["categoryId"] = category_id

            # Sort by purchaseCount desc, then reviewCount desc
            products = list(
                db.products.find(query)
                .sort([("purchaseCount", -1), ("reviewCount", -1)])
                .limit(limit)
            )

            formatted = []
            for p in products:
                formatted.append({
                    "id": str(p.get("_id", "")),
                    "name": p.get("name", ""),
                    "price": p.get("price"),
                    "finalPrice": p.get("finalPrice"),
                    "averageRating": p.get("averageRating", 0),
                    "reviewCount": p.get("reviewCount", 0),
                    "purchaseCount": p.get("purchaseCount", 0),
                    "shopId": p.get("shopId"),
                    "categoryId": p.get("categoryId"),
                })

            return {
                "success": True,
                "message": f"Top {len(formatted)} san pham ban chay",
                "products": formatted,
                "category_id": category_id or "tat_ca",
            }

        except Exception as e:
            logger.error(f"GetTopSellingTool failed: {e}")
            return {"success": False, "message": "Loi khi lay top selling"}
