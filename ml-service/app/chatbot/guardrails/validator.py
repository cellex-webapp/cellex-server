"""
Output Validator
----------------
Validate va sanitize chatbot outputs.
"""

import re
import json
from typing import Any
from jsonschema import validate, ValidationError as JsonSchemaValidationError


# PII patterns de mask
PII_PATTERNS = {
    "email": r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b",
    "phone": r"\b(?:\+84|0)[0-9]{9,10}\b",
    "card": r"\b[0-9]{4}[\s-]?[0-9]{4}[\s-]?[0-9]{4}[\s-]?[0-9]{4}\b",
}


# Response schema
RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "message": {"type": "string", "maxLength": 2000},
        "tool_calls": {"type": "array"},
        "metadata": {"type": "object"},
    },
    "required": ["message"],
}


class OutputValidator:
    """
    Validator cho chatbot outputs.
    """

    @staticmethod
    def mask_pii(text: str, enable: bool = True) -> str:
        """
        Mask PII (Personally Identifiable Information) trong text.

        Args:
            text: Input text
            enable: Enable PII masking

        Returns:
            Text da mask PII
        """
        if not enable:
            return text

        masked = text
        for pii_type, pattern in PII_PATTERNS.items():
            if pii_type == "email":
                masked = re.sub(pattern, "***@***.***", masked)
            elif pii_type == "phone":
                masked = re.sub(pattern, "***********", masked)
            elif pii_type == "card":
                masked = re.sub(pattern, "****-****-****-****", masked)

        return masked

    @staticmethod
    def validate_response_schema(response: dict) -> tuple[bool, str]:
        """
        Validate response theo schema.

        Args:
            response: Response dict

        Returns:
            (is_valid, error_message)
        """
        try:
            validate(instance=response, schema=RESPONSE_SCHEMA)
            return True, ""
        except JsonSchemaValidationError as e:
            return False, str(e)

    @staticmethod
    def truncate_response(text: str, max_length: int = 2000) -> str:
        """
        Truncate response neu qua dai.

        Args:
            text: Response text
            max_length: Max length

        Returns:
            Truncated text
        """
        if len(text) <= max_length:
            return text

        truncated = text[: max_length - 50]
        return truncated + "\n\n... (response truncated)"

    @staticmethod
    def sanitize_output(
        response: str,
        enable_pii_masking: bool = True,
        max_length: int = 2000,
    ) -> str:
        """
        Sanitize output: mask PII + truncate.

        Args:
            response: Raw response
            enable_pii_masking: Enable PII masking
            max_length: Max response length

        Returns:
            Sanitized response
        """
        sanitized = OutputValidator.mask_pii(response, enable_pii_masking)
        sanitized = OutputValidator.truncate_response(sanitized, max_length)
        return sanitized

    @staticmethod
    def validate_tool_params(tool_name: str, params: dict) -> tuple[bool, str]:
        """
        Validate tool parameters (extensible).

        Args:
            tool_name: Tool name
            params: Parameters dict

        Returns:
            (is_valid, error_message)
        """
        # Basic validation - can be extended with schema per tool
        if not isinstance(params, dict):
            return False, "Parameters phai la dict"

        # Tool-specific validation
        if tool_name == "search_products":
            if "query" not in params or not params["query"]:
                return False, "query parameter la bat buoc"

        if tool_name == "compare_products":
            if "product_ids" not in params or len(params["product_ids"]) < 2:
                return False, "can it nhat 2 product_ids de so sanh"

        return True, ""
