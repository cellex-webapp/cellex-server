"""
Gemini Client
-------------
Client de goi Gemini API (Google Generative AI).
"""

import json
from typing import List, Dict, Any, Optional
import httpx
from loguru import logger

from ...config import settings


class GeminiClient:
    """
    Client de giao tiep voi Gemini API.
    Format tuong tu AIService.java trong Spring Boot backend.
    """

    API_BASE = "https://generativelanguage.googleapis.com/v1beta/models"

    def __init__(self, api_key: Optional[str] = None, model: Optional[str] = None):
        """
        Khoi tao Gemini client.

        Args:
            api_key: Gemini API key (optional, defaults to settings)
            model: Model name (optional, defaults to settings)
        """
        self.api_key = api_key or settings.gemini_api_key
        self.model = model or settings.gemini_model

        if not self.api_key:
            raise ValueError("Gemini API key is required. Set GEMINI_API_KEY in .env")

    async def generate_content(
        self,
        messages: List[Dict[str, Any]],
        system_prompt: Optional[str] = None,
        tools: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        """
        Goi Gemini API de generate content.

        Args:
            messages: List cac messages (role: "user" hoac "model", parts: [{"text": "..."}])
            system_prompt: System instruction
            tools: Function declarations cho tool calling

        Returns:
            Response dict voi text va function_call (neu co)
        """
        url = f"{self.API_BASE}/{self.model}:generateContent?key={self.api_key}"

        # Build request body
        request_body = {
            "contents": messages,
            "generation_config": {
                "temperature": settings.gemini_temperature,
                "topK": settings.gemini_top_k,
                "topP": settings.gemini_top_p,
                "maxOutputTokens": settings.gemini_max_tokens,
            },
        }

        # Add system instruction
        if system_prompt:
            request_body["system_instruction"] = {
                "parts": [{"text": system_prompt}]
            }

        # Add tools (function declarations)
        if tools:
            request_body["tools"] = [{"function_declarations": tools}]

            # Tool config - AUTO mode de uu tien function calling
            request_body["tool_config"] = {
                "function_calling_config": {"mode": "AUTO"}
            }

        # Call API
        async with httpx.AsyncClient(timeout=60.0) as client:
            try:
                response = await client.post(url, json=request_body)
                response.raise_for_status()

                data = response.json()
                return self._parse_response(data)

            except httpx.HTTPStatusError as e:
                logger.error(f"Gemini API error: {e.response.status_code} - {e.response.text}")
                raise Exception(f"Gemini API error: {e.response.text}")
            except Exception as e:
                logger.error(f"Gemini request failed: {e}")
                raise

    def _parse_response(self, response_data: Dict[str, Any]) -> Dict[str, Any]:
        """
        Parse Gemini response de extract text va function_call.

        Args:
            response_data: Raw response tu Gemini API

        Returns:
            Dict voi:
            - text: Generated text
            - function_call: Function call (neu co)
            - usage: Token usage stats
        """
        try:
            candidates = response_data.get("candidates", [])
            if not candidates:
                return {
                    "text": "",
                    "function_call": None,
                    "usage": {},
                }

            candidate = candidates[0]
            content = candidate.get("content", {})
            parts = content.get("parts", [])

            result = {
                "text": "",
                "function_call": None,
                "usage": response_data.get("usageMetadata", {}),
            }

            # Chi parse part dau tien (text hoac function_call)
            if parts:
                first_part = parts[0]

                # Check if function call
                if "functionCall" in first_part:
                    func_call = first_part["functionCall"]
                    result["function_call"] = {
                        "name": func_call.get("name"),
                        "arguments": func_call.get("args", {}),
                    }
                # Check if text response
                elif "text" in first_part:
                    result["text"] = first_part["text"]

            return result

        except Exception as e:
            logger.error(f"Failed to parse Gemini response: {e}")
            return {
                "text": "",
                "function_call": None,
                "usage": {},
            }

    @staticmethod
    def format_messages(
        history: List[Dict[str, str]],
        new_message: str,
    ) -> List[Dict[str, Any]]:
        """
        Format conversation history thanh Gemini messages format.

        Args:
            history: List cac messages truoc do (role: "user"/"assistant", content: "...")
            new_message: Message moi cua user

        Returns:
            List messages theo Gemini format
        """
        messages = []

        # Add history
        for msg in history:
            role = "user" if msg["role"] == "user" else "model"
            messages.append({
                "role": role,
                "parts": [{"text": msg["content"]}],
            })

        # Add new user message
        messages.append({
            "role": "user",
            "parts": [{"text": new_message}],
        })

        return messages

    @staticmethod
    def format_function_result(
        function_name: str,
        function_response: Dict[str, Any],
    ) -> Dict[str, Any]:
        """
        Format function result thanh Gemini message part.

        Args:
            function_name: Ten function
            function_response: Ket qua tu function

        Returns:
            Message part voi functionResponse
        """
        return {
            "role": "model",
            "parts": [
                {
                    "functionResponse": {
                        "name": function_name,
                        "response": {
                            "content": json.dumps(function_response, ensure_ascii=False),
                        },
                    }
                }
            ],
        }
