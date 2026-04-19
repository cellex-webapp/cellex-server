# Quick Demo Script cho Cellex Chatbot (Windows) - Gemini Edition
# Chay script nay de test chatbot nhanh chong với Gemini 2.5 Flash

$BASE_URL = "http://localhost:8000"

Write-Host "==================================" -ForegroundColor Green
Write-Host "CELLEX CHATBOT QUICK DEMO" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
Write-Host ""

# 1. Health check
Write-Host "[1/5] Checking service health..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "$BASE_URL/health" -Method Get
$response | ConvertTo-Json -Depth 10
Write-Host ""
Start-Sleep -Seconds 1

# 2. Index products
Write-Host "[2/5] Indexing products into vector store..." -ForegroundColor Yellow
$body = @{} | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/chatbot/index-products" -Method Post -Body $body -ContentType "application/json"
$response | ConvertTo-Json -Depth 10
Write-Host ""
Start-Sleep -Seconds 2

# 3. Test BUYER chat
Write-Host "[3/5] Testing BUYER role - Product search..." -ForegroundColor Yellow
$body = @{
    message = "Tim dien thoai duoi 25 trieu"
    user_id = "demo_buyer"
    role = "BUYER"
} | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/chatbot/chat" -Method Post -Body $body -ContentType "application/json"
$response | ConvertTo-Json -Depth 10
Write-Host ""
Start-Sleep -Seconds 2

# 4. Test product comparison
Write-Host "[4/5] Testing BUYER role - Product comparison..." -ForegroundColor Yellow
$body = @{
    message = "So sanh iPhone 15 va Samsung S24"
    user_id = "demo_buyer"
    role = "BUYER"
    session_id = "demo_session"
} | ConvertTo-Json
$response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/chatbot/chat" -Method Post -Body $body -ContentType "application/json"
$response | ConvertTo-Json -Depth 10
Write-Host ""
Start-Sleep -Seconds 2

# 5. Check stats
Write-Host "[5/5] Checking chatbot stats..." -ForegroundColor Yellow
$response = Invoke-RestMethod -Uri "$BASE_URL/api/v1/chatbot/stats" -Method Get
$response | ConvertTo-Json -Depth 10
Write-Host ""

Write-Host "==================================" -ForegroundColor Green
Write-Host "DEMO COMPLETED!" -ForegroundColor Green
Write-Host "Chatbot is ready for production" -ForegroundColor Green
Write-Host "==================================" -ForegroundColor Green
