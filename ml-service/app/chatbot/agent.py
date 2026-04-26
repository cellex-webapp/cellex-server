"""
Chatbot Agent
-------------
Main orchestrator cho LLM + RAG + Tool-calling chatbot.
Uses Gemini API (Google Generative AI).

Cap nhat:
- Them tools moi: GetShopHealthTool, GetAnomaliesReportTool, SuggestCouponStrategyTool, AnalyzeInventoryTool
- Them role_map_header cho Spring Boot integration
- Fix inject shop_id cho tat ca SELLER tools
"""

from typing import List, Dict, Any, Optional
import json
from loguru import logger
from pymongo import MongoClient

from ..config import settings
from .prompts.templates import get_system_prompt, format_tool_error
from .guardrails.rbac import Role, UserContext, RBACGuard
from .guardrails.validator import OutputValidator
from .rag.vectorstore import VectorStore
from .rag.retriever import Retriever
from .llm.gemini_client import GeminiClient
from .tools.product_tools import (
    SearchProductsTool,
    GetProductDetailsTool,
    CompareProductsTool,
    GetTopSellingTool,
)
from .tools.order_tools import GetMyOrdersTool, GetOrderStatusTool
from .tools.kpi_tools import (
    GetShopKPITool,
    GetSystemMetricsTool,
    GetBestsellersTool,
    GetShopHealthTool,
    GetAnomaliesReportTool,
    SuggestCouponStrategyTool,
    AnalyzeInventoryTool,
)


class ChatbotAgent:
    """
    Main chatbot agent voi LLM + RAG + Tool-calling.
    Uses Gemini model.
    """

    def __init__(
        self,
        mongo_client: MongoClient,
        vector_store: Optional[VectorStore] = None,
    ):
        self.mongo_client = mongo_client
        self.vector_store = vector_store or VectorStore(settings.vector_store_path)
        self.retriever = Retriever(
            self.vector_store, mongo_client, settings.mongo_db
        )
        self.gemini_client = GeminiClient()
        self.tools = self._init_tools()
        self.conversations: Dict[str, list] = {}  # session_id -> messages

        logger.info("ChatbotAgent initialized with Gemini API")

    def _init_tools(self) -> Dict[str, Any]:
        """Khoi tao tat ca tools."""
        return {
            # ── Product tools (ALL roles) ───────────────────────────────
            "search_products": SearchProductsTool(self.retriever),
            "get_product_details": GetProductDetailsTool(self.retriever),
            "compare_products": CompareProductsTool(self.retriever),
            "get_top_selling": GetTopSellingTool(self.retriever),

            # ── Order tools (BUYER) ─────────────────────────────────────
            "get_my_orders": GetMyOrdersTool(self.retriever),
            "get_order_status": GetOrderStatusTool(self.retriever),

            # ── KPI tools (SELLER) ──────────────────────────────────────
            "get_shop_kpi": GetShopKPITool(self.retriever),
            "get_bestsellers": GetBestsellersTool(self.retriever),
            "suggest_coupon_strategy": SuggestCouponStrategyTool(self.retriever),
            "analyze_inventory": AnalyzeInventoryTool(self.retriever),

            # ── Admin tools (ADMIN) ─────────────────────────────────────
            "get_system_metrics": GetSystemMetricsTool(self.retriever),
            "get_shop_health": GetShopHealthTool(self.retriever),
            "get_anomalies_report": GetAnomaliesReportTool(self.retriever),
        }

    def get_available_tools_for_role(self, role: Role) -> List[Dict[str, Any]]:
        """Lay danh sach tools ma role co the su dung (Gemini function declarations format)."""
        available_tool_names = RBACGuard.get_available_tools(role)
        schemas = []

        for tool_name in available_tool_names:
            if tool_name in self.tools:
                tool = self.tools[tool_name]
                schemas.append(tool.to_gemini_function())

        return schemas

    async def chat(
        self,
        user_message: str,
        user_context: UserContext,
        session_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        """Main chat interface."""
        try:
            if not session_id:
                session_id = f"{user_context.user_id}_{user_context.role.value}"

            if session_id not in self.conversations:
                self.conversations[session_id] = []

            system_prompt = get_system_prompt(user_context.role.value)

            messages = [{"role": "system", "content": system_prompt}]
            messages.extend(self.conversations[session_id])
            messages.append({"role": "user", "content": user_message})

            available_tools = self.get_available_tools_for_role(user_context.role)

            response = await self._call_llm(
                messages, available_tools, user_context, system_prompt
            )

            self.conversations[session_id].append(
                {"role": "user", "content": user_message}
            )
            self.conversations[session_id].append(
                {"role": "assistant", "content": response.get("message", "")}
            )

            # Truncate conversation memory
            if len(self.conversations[session_id]) > settings.conversation_memory_size * 2:
                self.conversations[session_id] = self.conversations[session_id][
                    -settings.conversation_memory_size * 2:
                ]

            return response

        except Exception as e:
            logger.error(f"Chat failed: {e}")
            return {
                "message": "Xin loi, da xay ra loi. Vui long thu lai.",
                "success": False,
                "error": str(e),
            }

    async def _call_llm(
        self,
        messages: List[Dict[str, str]],
        tools: List[Dict],
        user_context: UserContext,
        system_prompt: str,
        iteration: int = 0,
        gemini_formatted: bool = False,
    ) -> Dict[str, Any]:
        """Goi Gemini LLM voi tool calling."""
        if iteration >= settings.max_tool_iterations:
            return {
                "message": "Da vuot qua so lan goi tool toi da.",
                "success": False,
            }

        try:
            if gemini_formatted:
                gemini_messages = [msg for msg in messages if msg["role"] != "system"]
            else:
                history_messages = [msg for msg in messages if msg["role"] != "system"]
                user_message = history_messages[-1]["content"]
                history = history_messages[:-1]
                gemini_messages = GeminiClient.format_messages(history, user_message)

            gemini_response = await self.gemini_client.generate_content(
                messages=gemini_messages,
                system_prompt=system_prompt,
                tools=tools if tools else None,
            )

            if gemini_response["function_call"]:
                func_call = gemini_response["function_call"]
                tool_name = func_call["name"]
                tool_args = func_call["arguments"]

                logger.info(f"Gemini called function: {tool_name} with args: {tool_args}")

                # Permission check
                if not user_context.can_use_tool(tool_name):
                    tool_result = {
                        "success": False,
                        "message": format_tool_error("permission_denied"),
                    }
                else:
                    tool_result = await self._execute_tool(
                        tool_name, tool_args, user_context
                    )

                # Add function call to messages (Gemini format)
                gemini_messages.append({
                    "role": "model",
                    "parts": [{"functionCall": func_call}],
                })

                function_result_msg = GeminiClient.format_function_result(
                    tool_name, tool_result
                )
                gemini_messages.append(function_result_msg)

                new_messages = [{"role": "system", "content": system_prompt}]
                new_messages.extend(gemini_messages)

                return await self._call_llm(
                    new_messages, tools, user_context, system_prompt,
                    iteration + 1, gemini_formatted=True
                )

            else:
                content = gemini_response["text"] or ""
                sanitized = OutputValidator.sanitize_output(
                    content,
                    settings.enable_pii_masking,
                    settings.max_response_length,
                )
                return {
                    "message": sanitized,
                    "success": True,
                    "model": settings.gemini_model,
                    "usage": gemini_response.get("usage", {}),
                }

        except Exception as e:
            logger.error(f"Gemini call failed: {e}")
            return {
                "message": format_tool_error("system_error"),
                "success": False,
                "error": str(e),
            }

    async def _execute_tool(
        self, tool_name: str, tool_args: Dict, user_context: UserContext
    ) -> Dict[str, Any]:
        """Thuc thi tool."""
        if tool_name not in self.tools:
            return {
                "success": False,
                "message": format_tool_error("tool_not_found", tool_name=tool_name),
            }

        tool = self.tools[tool_name]

        is_valid, error_msg = OutputValidator.validate_tool_params(tool_name, tool_args)
        if not is_valid:
            return {
                "success": False,
                "message": format_tool_error("invalid_input", details=error_msg),
            }

        try:
            # Inject user_id cho BUYER tools
            if tool_name in ["get_my_orders", "get_order_status"]:
                tool_args["user_id"] = user_context.user_id

            # Inject shop_id cho tat ca SELLER tools
            if tool_name in [
                "get_shop_kpi", "get_bestsellers",
                "suggest_coupon_strategy", "analyze_inventory",
                "get_shop_health",  # ADMIN cung co the goi voi shop_id cu the
            ]:
                if "shop_id" not in tool_args or not tool_args.get("shop_id"):
                    tool_args["shop_id"] = user_context.metadata.get(
                        "shop_id",
                        user_context.metadata.get("shopId", ""),
                    )

            result = await tool.execute(**tool_args)
            return result

        except Exception as e:
            logger.error(f"Tool {tool_name} execution failed: {e}")
            return {
                "success": False,
                "message": format_tool_error("system_error"),
                "error": str(e),
            }

    async def index_products(self, limit: Optional[int] = None) -> int:
        """Index products tu MongoDB vao vector store."""
        try:
            db = self.mongo_client[settings.mongo_db]
            query = {"isPublished": True}  # Chỉ index published products

            if limit:
                products = list(db.products.find(query).limit(limit))
            else:
                products = list(db.products.find(query))

            count = self.vector_store.index_products(products)
            logger.info(f"Indexed {count} products into vector store")
            return count

        except Exception as e:
            logger.error(f"Failed to index products: {e}")
            return 0

    def clear_conversation(self, session_id: str):
        """Xoa conversation history."""
        if session_id in self.conversations:
            del self.conversations[session_id]
            logger.info(f"Cleared conversation: {session_id}")

    def get_stats(self) -> Dict[str, Any]:
        """Lay thong tin thong ke agent."""
        return {
            "active_conversations": len(self.conversations),
            "available_tools": len(self.tools),
            "tools_by_role": {
                "BUYER": RBACGuard.get_available_tools(Role.BUYER),
                "SELLER": RBACGuard.get_available_tools(Role.SELLER),
                "ADMIN": RBACGuard.get_available_tools(Role.ADMIN),
            },
            "vector_store_stats": self.vector_store.get_collection_stats(),
        }
