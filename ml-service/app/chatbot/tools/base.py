"""
Base Tool
---------
Abstract base class cho cac tools.
"""

from abc import ABC, abstractmethod
from typing import Any, Dict, Optional
from pydantic import BaseModel


class ToolParameter(BaseModel):
    """Schema cho tool parameter."""

    name: str
    type: str
    description: str
    required: bool = False
    default: Any = None
    items: Optional[Dict[str, str]] = None  # For array types


class BaseTool(ABC):
    """
    Abstract base class cho tools.
    """

    name: str
    description: str
    parameters: list[ToolParameter]

    def __init__(self):
        """Initialize tool."""
        pass

    @abstractmethod
    async def execute(self, **kwargs) -> Dict[str, Any]:
        """
        Execute tool logic.

        Args:
            **kwargs: Tool parameters

        Returns:
            Tool execution result
        """
        pass

    def to_openai_function(self) -> Dict[str, Any]:
        """
        Convert tool to OpenAI function calling format.

        Returns:
            Function schema
        """
        properties = {}
        required = []

        for param in self.parameters:
            properties[param.name] = {
                "type": param.type,
                "description": param.description,
            }
            if param.default is not None:
                properties[param.name]["default"] = param.default

            if param.required:
                required.append(param.name)

        return {
            "name": self.name,
            "description": self.description,
            "parameters": {
                "type": "object",
                "properties": properties,
                "required": required,
            },
        }

    def to_gemini_function(self) -> Dict[str, Any]:
        """
        Convert tool to Gemini function declaration format.
        Format follows AIService.java buildFunction() method.

        Returns:
            Gemini function declaration
        """
        properties = {}
        required = []

        for param in self.parameters:
            param_schema = {
                "type": param.type.upper(),  # Gemini uses uppercase: STRING, INTEGER, NUMBER
                "description": param.description,
            }

            # Handle array types - MUST have items
            if param.type == "array":
                param_schema["type"] = "ARRAY"
                # Default to STRING items if not specified
                param_schema["items"] = param.items or {"type": "STRING"}

            properties[param.name] = param_schema

            if param.required:
                required.append(param.name)

        return {
            "name": self.name,
            "description": self.description,
            "parameters": {
                "type": "OBJECT",
                "properties": properties,
                "required": required,
            },
        }

    def to_langchain_tool(self) -> Dict[str, Any]:
        """
        Convert tool to LangChain tool format.

        Returns:
            Tool dict
        """
        return {
            "name": self.name,
            "description": self.description,
            "parameters": [p.dict() for p in self.parameters],
            "func": self.execute,
        }
