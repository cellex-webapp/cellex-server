@echo off
REM ============================================
REM Cellex MongoDB Seed Data Import Script (Windows)
REM ============================================
REM Script tự động import seed data vào MongoDB
REM ============================================

echo ==========================================
echo   CELLEX MONGODB SEED DATA IMPORTER
echo ==========================================
echo.

REM Cấu hình
set DB_NAME=cellex
set MONGO_URI=mongodb://localhost:27017
set SCRIPT_DIR=%~dp0

REM Kiểm tra MongoDB
echo [*] Checking MongoDB connection...
mongosh "%MONGO_URI%/%DB_NAME%" --eval "db.runCommand({ ping: 1 })" >nul 2>&1
if errorlevel 1 (
    echo [X] Cannot connect to MongoDB at %MONGO_URI%
    echo Please make sure MongoDB is running and accessible.
    pause
    exit /b 1
)
echo [OK] MongoDB connection successful
echo.

REM Xác nhận trước khi xóa dữ liệu
echo WARNING: This will DELETE all existing data in the following collections:
echo    - customer_segments
echo    - categories
echo    - category_attributes
echo    - users
echo    - shops
echo    - products
echo    - orders
echo    - user_interactions
echo    - reviews
echo.
set /p CONFIRM="Do you want to continue? (yes/no): "
if /i not "%CONFIRM%"=="yes" (
    echo Import cancelled.
    pause
    exit /b 0
)
echo.

REM Import Part 1: Core Data
echo [*] Importing Part 1: Core Data (Segments, Categories, Users, Shops, Products)...
mongosh "%MONGO_URI%/%DB_NAME%" "%SCRIPT_DIR%mongodb_seed_data.js" >nul 2>&1
if errorlevel 1 (
    echo [X] Failed to import Part 1
    pause
    exit /b 1
)
echo [OK] Part 1 imported successfully
echo.

REM Import Part 2: Transactional Data
echo [*] Importing Part 2: Transactional Data (Orders, Interactions, Reviews)...
mongosh "%MONGO_URI%/%DB_NAME%" "%SCRIPT_DIR%mongodb_seed_data_part2.js" >nul 2>&1
if errorlevel 1 (
    echo [X] Failed to import Part 2
    pause
    exit /b 1
)
echo [OK] Part 2 imported successfully
echo.

REM Verify data
echo [*] Verifying imported data...
echo.

REM PowerShell commands to get counts (more reliable on Windows)
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.customer_segments.countDocuments()"') do set SEGMENTS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.categories.countDocuments()"') do set CATEGORIES=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.category_attributes.countDocuments()"') do set ATTRIBUTES=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.users.countDocuments()"') do set USERS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.shops.countDocuments()"') do set SHOPS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.products.countDocuments()"') do set PRODUCTS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.orders.countDocuments()"') do set ORDERS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.user_interactions.countDocuments()"') do set INTERACTIONS=%%i
for /f %%i in ('mongosh "%MONGO_URI%/%DB_NAME%" --quiet --eval "db.reviews.countDocuments()"') do set REVIEWS=%%i

echo Import Summary:
echo    Customer Segments: %SEGMENTS% (expected: 3)
echo    Categories: %CATEGORIES% (expected: 4)
echo    Category Attributes: %ATTRIBUTES% (expected: 18)
echo    Users: %USERS% (expected: 14)
echo    Shops: %SHOPS% (expected: 3)
echo    Products: %PRODUCTS% (expected: 22)
echo    Orders: %ORDERS% (expected: 10)
echo    User Interactions: %INTERACTIONS% (expected: 50)
echo    Reviews: %REVIEWS% (expected: 12)
echo.

REM Check if all counts match
if "%SEGMENTS%"=="3" if "%CATEGORIES%"=="4" if "%ATTRIBUTES%"=="18" if "%USERS%"=="14" if "%SHOPS%"=="3" if "%PRODUCTS%"=="22" if "%ORDERS%"=="10" if "%INTERACTIONS%"=="50" if "%REVIEWS%"=="12" (
    echo [OK] All data imported successfully!
    echo.
    echo You can now use the following credentials:
    echo    Admin:    admin@cellex.vn / admin123
    echo    Vendor:   hung.vendor@cellex.vn / admin123
    echo    Customer: tuan.customer@gmail.com / admin123
) else (
    echo [!] Some counts don't match expected values. Please check the logs.
)

echo.
echo ==========================================
echo   Import completed!
echo ==========================================
echo.
pause
