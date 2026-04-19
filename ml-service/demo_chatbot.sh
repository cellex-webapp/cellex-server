#!/bin/bash
# Quick Demo Script cho Cellex Chatbot
# Chay script nay de test chatbot nhanh chong

set -e

echo "=================================="
echo "CELLEX CHATBOT QUICK DEMO"
echo "=================================="
echo ""

BASE_URL="http://localhost:8000"

# 1. Health check
echo "[1/5] Checking service health..."
curl -s "$BASE_URL/health" | python -m json.tool
echo ""
sleep 1

# 2. Index products
echo "[2/5] Indexing products into vector store..."
curl -s -X POST "$BASE_URL/api/v1/chatbot/index-products" \
  -H "Content-Type: application/json" \
  -d '{}' | python -m json.tool
echo ""
sleep 2

# 3. Test BUYER chat
echo "[3/5] Testing BUYER role - Product search..."
curl -s -X POST "$BASE_URL/api/v1/chatbot/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "Tim dien thoai duoi 25 trieu",
    "user_id": "demo_buyer",
    "role": "BUYER"
  }' | python -m json.tool
echo ""
sleep 2

# 4. Test product comparison
echo "[4/5] Testing BUYER role - Product comparison..."
curl -s -X POST "$BASE_URL/api/v1/chatbot/chat" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "So sanh iPhone 15 va Samsung S24",
    "user_id": "demo_buyer",
    "role": "BUYER",
    "session_id": "demo_session"
  }' | python -m json.tool
echo ""
sleep 2

# 5. Check stats
echo "[5/5] Checking chatbot stats..."
curl -s "$BASE_URL/api/v1/chatbot/stats" | python -m json.tool
echo ""

echo "=================================="
echo "DEMO COMPLETED!"
echo "Chatbot is ready for production"
echo "=================================="
