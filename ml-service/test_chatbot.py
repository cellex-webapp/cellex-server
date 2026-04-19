"""
Test script for chatbot fix
-----------------------------
Test function calling with get_top_selling tool.
"""

import asyncio
import sys
from pathlib import Path

# Add app to path
sys.path.insert(0, str(Path(__file__).parent))

from app.chatbot.agent import ChatbotAgent
from app.chatbot.guardrails.rbac import Role, UserContext
from pymongo import MongoClient
from app.config import settings


async def test_chatbot_function_calling():
    """Test chatbot with function calling."""
    print("=" * 60)
    print("Testing Chatbot Function Calling Fix")
    print("=" * 60)

    # Initialize MongoDB client
    print(f"\n1. Connecting to MongoDB: {settings.mongo_uri}")
    mongo_client = MongoClient(settings.mongo_uri)

    # Initialize agent
    print("2. Initializing ChatbotAgent...")
    agent = ChatbotAgent(mongo_client=mongo_client)

    # Create user context
    user_context = UserContext(
        user_id="test_user_001",
        role=Role.BUYER,
        session_id="test_session_001",
        metadata={}
    )

    # Test message that should trigger get_top_selling function
    test_message = "Cho toi xem 5 san pham ban chay nhat"

    print(f"\n3. Testing with message: '{test_message}'")
    print(f"   User role: {user_context.role.value}")
    print(f"   Expected: Gemini should call get_top_selling tool")

    try:
        print("\n4. Sending request to agent...")
        response = await agent.chat(
            user_message=test_message,
            user_context=user_context,
            session_id="test_session_001"
        )

        print("\n" + "=" * 60)
        print("RESULT:")
        print("=" * 60)
        print(f"Success: {response.get('success', False)}")
        print(f"Model: {response.get('model', 'N/A')}")
        print(f"\nMessage:\n{response.get('message', '')}")

        if response.get('usage'):
            print(f"\nToken usage: {response['usage']}")

        if response.get('error'):
            print(f"\n❌ ERROR: {response['error']}")
            return False

        print("\n✅ Test PASSED - No KeyError 'content'!")
        return True

    except Exception as e:
        print("\n" + "=" * 60)
        print(f"❌ Test FAILED with error: {e}")
        print("=" * 60)
        import traceback
        traceback.print_exc()
        return False

    finally:
        mongo_client.close()


async def test_simple_chat():
    """Test simple chat without function calling."""
    print("\n\n" + "=" * 60)
    print("Testing Simple Chat (No Function Calling)")
    print("=" * 60)

    mongo_client = MongoClient(settings.mongo_uri)
    agent = ChatbotAgent(mongo_client=mongo_client)

    user_context = UserContext(
        user_id="test_user_002",
        role=Role.BUYER,
        session_id="test_session_002",
        metadata={}
    )

    test_message = "Xin chao! Ban la ai?"

    print(f"\nTesting with message: '{test_message}'")

    try:
        response = await agent.chat(
            user_message=test_message,
            user_context=user_context,
            session_id="test_session_002"
        )

        print(f"\nResponse: {response.get('message', '')}")
        print(f"Success: {response.get('success', False)}")

        if response.get('error'):
            print(f"❌ ERROR: {response['error']}")
            return False

        print("✅ Simple chat test PASSED!")
        return True

    except Exception as e:
        print(f"❌ Test FAILED: {e}")
        import traceback
        traceback.print_exc()
        return False

    finally:
        mongo_client.close()


async def main():
    """Run all tests."""
    print("\n\n")
    print("╔" + "=" * 58 + "╗")
    print("║" + " " * 15 + "CHATBOT FIX TEST SUITE" + " " * 21 + "║")
    print("╚" + "=" * 58 + "╝")

    results = []

    # Test 1: Function calling (the bug we fixed)
    results.append(await test_chatbot_function_calling())

    # Test 2: Simple chat
    results.append(await test_simple_chat())

    # Summary
    print("\n\n" + "=" * 60)
    print("TEST SUMMARY")
    print("=" * 60)
    passed = sum(results)
    total = len(results)
    print(f"Passed: {passed}/{total}")

    if passed == total:
        print("\n🎉 All tests PASSED! The fix is working correctly.")
    else:
        print(f"\n⚠️  {total - passed} test(s) failed. Please review the errors above.")

    return passed == total


if __name__ == "__main__":
    success = asyncio.run(main())
    sys.exit(0 if success else 1)
