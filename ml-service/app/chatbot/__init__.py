"""
Chatbot Module
--------------
LLM + RAG + Tool-calling chatbot cho Cellex E-commerce.
Ho tro 3 roles: BUYER, SELLER, ADMIN voi tool whitelist va guardrails.
"""

from .agent import ChatbotAgent
from .guardrails.rbac import Role, RBACGuard

__all__ = ["ChatbotAgent", "Role", "RBACGuard"]
