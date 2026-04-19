"""
Product Tools
-------------
Tools de truy van thong tin san pham.
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
    ]

    def __init__(self, retriever: Retriever):
        super().__init__()
        self.retriever = retriever

    async def execute(self, query: str, limit: int = 5, **kwargs) -> Dict[str, Any]:
        """Tim kiem san pham."""
        try:
            products = self.retriever.retrieve_products(query, top_k=limit)

            if not products:
                return {
                    "success": False,
                    "message": "Khong tim thay san pham phu hop",
                    "products": [],
                }

            # Format response
            formatted_products = []
            for p in products:
                formatted_products.append(
                    {
                        "id": str(p.get("_id", "")),
                        "title": p.get("title", ""),
                        "brand": p.get("brand", ""),
                        "category": p.get("category", ""),
                        "price": p.get("price", 0),
                        "original_price": p.get("original_price"),
                        "rating": p.get("rating", 0),
                        "review_count": p.get("review_count", 0),
                        "relevance_score": p.get("relevance_score", 0),
                    }
                )

            return {
                "success": True,
                "message": f"Tim thay {len(formatted_products)} san pham",
                "products": formatted_products,
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

            product = context["product"]

            return {
                "success": True,
                "product": {
                    "id": str(product.get("_id")),
                    "title": product.get("title"),
                    "brand": product.get("brand"),
                    "category": product.get("category"),
                    "price": product.get("price"),
                    "description": product.get("description"),
                    "specifications": product.get("specifications", {}),
                    "rating": product.get("rating"),
                    "review_count": product.get("review_count"),
                    "stock": product.get("stock", 0),
                },
                "reviews_count": len(context["reviews"]),
                "similar_products_count": len(context["similar_products"]),
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

            # Format comparison
            comparison = []
            for p in products:
                comparison.append(
                    {
                        "id": str(p.get("_id")),
                        "title": p.get("title"),
                        "brand": p.get("brand"),
                        "price": p.get("price"),
                        "rating": p.get("rating"),
                        "specifications": p.get("specifications", {}),
                    }
                )

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
            name="category",
            type="string",
            description="Category (optional). Vi du: 'Dien thoai', 'Laptop'",
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
        self, category: Optional[str] = None, limit: int = 10, **kwargs
    ) -> Dict[str, Any]:
        """Lay top selling products."""
        try:
            db = self.retriever.db
            query = {}
            if category:
                query["category"] = category

            # Sort by review_count + rating
            products = list(
                db.products.find(query)
                .sort([("review_count", -1), ("rating", -1)])
                .limit(limit)
            )

            formatted = []
            for p in products:
                formatted.append(
                    {
                        "id": str(p.get("_id")),
                        "title": p.get("title"),
                        "brand": p.get("brand"),
                        "category": p.get("category"),
                        "price": p.get("price"),
                        "rating": p.get("rating"),
                        "review_count": p.get("review_count"),
                    }
                )

            return {
                "success": True,
                "message": f"Top {len(formatted)} san pham ban chay",
                "products": formatted,
                "category": category or "Tat ca",
            }

        except Exception as e:
            logger.error(f"GetTopSellingTool failed: {e}")
            return {"success": False, "message": "Loi khi lay top selling"}
