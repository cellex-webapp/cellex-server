"""
Chatbot Agent
-------------
Main orchestrator cho LLM + RAG + Tool-calling chatbot.
Uses Gemini API (Google Generative AI).
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
from .tools.kpi_tools import GetShopKPITool, GetSystemMetricsTool, GetBestsellersTool


class ChatbotAgent:
    """
    Main chatbot agent voi LLM + RAG + Tool-calling.
    Uses Gemini 2.5 Flash model.
    """

    def __init__(
        self,
        mongo_client: MongoClient,
        vector_store: Optional[VectorStore] = None,
    ):
        """
        Khoi tao chatbot agent.

        Args:
            mongo_client: MongoDB client
            vector_store: VectorStore instance (optional)
        """
        self.mongo_client = mongo_client
        self.vector_store = vector_store or VectorStore(settings.vector_store_path)
        self.retriever = Retriever(
            self.vector_store, mongo_client, settings.mongo_db
        )

        # Gemini client
        self.gemini_client = GeminiClient()

        # Initialize tools
        self.tools = self._init_tools()

        # Conversation memory
        self.conversations = {}  # session_id -> messages

        logger.info("ChatbotAgent initialized with Gemini API")

    def _init_tools(self) -> Dict[str, Any]:
        """
        Khoi tao tools cho cac roles.

        Returns:
            Dict mapping tool_name -> tool instance
        """
        tools = {
            # Product tools (BUYER, SELLER, ADMIN)
            "search_products": SearchProductsTool(self.retriever),
            "get_product_details": GetProductDetailsTool(self.retriever),
            "compare_products": CompareProductsTool(self.retriever),
            "get_top_selling": GetTopSellingTool(self.retriever),
            # Order tools (BUYER)
            "get_my_orders": GetMyOrdersTool(self.retriever),
            "get_order_status": GetOrderStatusTool(self.retriever),
            # KPI tools (SELLER, ADMIN)
            "get_shop_kpi": GetShopKPITool(self.retriever),
            "get_bestsellers": GetBestsellersTool(self.retriever),
            "get_system_metrics": GetSystemMetricsTool(self.retriever),
        }

        return tools

    def get_available_tools_for_role(self, role: Role) -> List[Dict[str, Any]]:
        """
        Lay danh sach tools ma role co the su dung.

        Args:
            role: User role

        Returns:
            List tool schemas (Gemini function declarations format)
        """
        available_tool_names = RBACGuard.get_available_tools(role)
        schemas = []

        for tool_name in available_tool_names:
            if tool_name in self.tools:
                tool = self.tools[tool_name]
                # Convert to Gemini format
                schemas.append(tool.to_gemini_function())

        return schemas

    async def chat(
        self,
        user_message: str,
        user_context: UserContext,
        session_id: Optional[str] = None,
    ) -> Dict[str, Any]:
        """
        Main chat interface.

        Args:
            user_message: User's message
            user_context: User context (user_id, role, etc)
            session_id: Optional session ID for conversation memory

        Returns:
            Response dict
        """
        try:
            # Get or create conversation history
            if not session_id:
                session_id = f"{user_context.user_id}_{user_context.role.value}"

            if session_id not in self.conversations:
                self.conversations[session_id] = []

            # System prompt
            system_prompt = get_system_prompt(user_context.role.value)

            # Messages
            messages = [{"role": "system", "content": system_prompt}]

            ## Add conversation history
            messages.extend(self.conversations[session_id])

            # Add user message
            messages.append({"role": "user", "content": user_message})

            # Get tools cho role nay
            available_tools = self.get_available_tools_for_role(user_context.role)

            # Call Gemini
            response = await self._call_llm(
                messages, available_tools, user_context, system_prompt
            )

            # Update conversation memory
            self.conversations[session_id].append(
                {"role": "user", "content": user_message}
            )
            self.conversations[session_id].append(
                {"role": "assistant", "content": response["message"]}
            )

            # Truncate conversation memory
            if len(self.conversations[session_id]) > settings.conversation_memory_size * 2:
                self.conversations[session_id] = self.conversations[session_id][
                    -settings.conversation_memory_size * 2 :
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
        """
        Goi Gemini LLM voi tool calling - format follows AIService.java.

        Args:
            messages: Conversation messages
            tools: Available tools (Gemini function declarations)
            user_context: User context
            system_prompt: System instruction
            iteration: Current iteration (prevent infinite loops)
            gemini_formatted: If True, messages are already in Gemini format

        Returns:
            Response dict
        """
        if iteration >= settings.max_tool_iterations:
            return {
                "message": "Da vuot qua so lan goi tool toi da.",
                "success": False,
            }

        try:
            # Format messages for Gemini if not already formatted
            if gemini_formatted:
                # Messages already in Gemini format (from function calling)
                gemini_messages = [msg for msg in messages if msg["role"] != "system"]
            else:
                # Extract history (exclude system prompt) and format for Gemini
                history_messages = [msg for msg in messages if msg["role"] != "system"]
                user_message = history_messages[-1]["content"]  # Current user message
                history = history_messages[:-1]  # Previous messages

                # Format messages for Gemini
                gemini_messages = GeminiClient.format_messages(history, user_message)

            # Call Gemini
            gemini_response = await self.gemini_client.generate_content(
                messages=gemini_messages,
                system_prompt=system_prompt,
                tools=tools if tools else None,
            )

            # Check if function call
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
                    # Execute tool
                    tool_result = await self._execute_tool(
                        tool_name, tool_args, user_context
                    )

                # Add function call to messages (Gemini format)
                # Model message with function call (correct Gemini format)
                gemini_messages.append({
                    "role": "model",
                    "parts": [{"functionCall": func_call}],
                })

                # Function result message
                function_result_msg = GeminiClient.format_function_result(
                    tool_name, tool_result
                )
                gemini_messages.append(function_result_msg)

                # Create new messages list with system prompt + formatted messages
                new_messages = [{"role": "system", "content": system_prompt}]
                new_messages.extend(gemini_messages)

                # Recursive call - let Gemini process function result
                return await self._call_llm(
                    new_messages, tools, user_context, system_prompt, iteration + 1, gemini_formatted=True
                )

            else:
                # Final text response from Gemini
                content = gemini_response["text"] or ""

                # Sanitize output
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
        """
        Thuc thi tool.

        Args:
            tool_name: Tool name
            tool_args: Tool arguments
            user_context: User context

        Returns:
            Tool execution result
        """
        if tool_name not in self.tools:
            return {
                "success": False,
                "message": format_tool_error("tool_not_found", tool_name=tool_name),
            }

        tool = self.tools[tool_name]

        # Validate parameters
        is_valid, error_msg = OutputValidator.validate_tool_params(
            tool_name, tool_args
        )
        if not is_valid:
            return {
                "success": False,
                "message": format_tool_error("invalid_input", details=error_msg),
            }

        try:
            # Inject user_id cho order tools
            if tool_name in ["get_my_orders", "get_order_status"]:
                tool_args["user_id"] = user_context.user_id

            # Inject shop_id cho seller tools (from metadata)
            if tool_name in ["get_shop_kpi", "get_bestsellers"]:
                tool_args["shop_id"] = user_context.metadata.get(
                    "shop_id", "default_shop"
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
        """
        Index products tu MongoDB vao vector store.

        Args:
            limit: Gioi han so luong products (None = all)

        Returns:
            So luong products da index
        """
        try:
            db = self.mongo_client[settings.mongo_db]
            query = {}

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
        """
        Lay thong tin thong ke agent.

        Returns:
            Stats dict
        """
        return {
            "active_conversations": len(self.conversations),
            "available_tools": len(self.tools),
            "vector_store_stats": self.vector_store.get_collection_stats(),
        }
