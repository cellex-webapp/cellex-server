@echo off
chcp 65001 >nul
echo ==========================================
echo CELLEX MONGODB ATLAS SEED DATA IMPORTER
echo ==========================================
echo.

REM MongoDB Atlas Connection String from .env
set MONGO_URI=mongodb+srv://minhthien250405:Minhthien1!@cellex-db.yn6tv0f.mongodb.net/cellex_prod?retryWrites=true^&w=majority^&appName=Cellex-db

echo [*] Checking mongosh installation...
where mongosh >nul 2>&1
if %errorlevel% neq 0 (
    echo [X] mongosh is not installed!
    echo.
    echo Please install MongoDB Shell:
    echo https://www.mongodb.com/try/download/shell
    echo.
    echo After installation, add mongosh to PATH and restart terminal.
    pause
    exit /b 1
)
echo [√] mongosh is installed
echo.

echo [*] Checking MongoDB Atlas connection...
mongosh "%MONGO_URI%" --quiet --eval "db.version()" >nul 2>&1
if %errorlevel% neq 0 (
    echo [X] Cannot connect to MongoDB Atlas
    echo.
    echo Please check:
    echo - Internet connection
    echo - MongoDB Atlas cluster is running
    echo - IP address is whitelisted (0.0.0.0/0 for testing)
    echo - Username/password is correct
    pause
    exit /b 1
)
echo [√] Connected to MongoDB Atlas successfully
echo.

echo ==========================================
echo WARNING: This will DELETE ALL existing data
echo in the following collections:
echo - customer_segments
echo - categories
echo - category_attributes
echo - users
echo - shops
echo - products
echo - orders
echo - user_interactions
echo - reviews
echo ==========================================
echo.
set /p CONFIRM="Are you sure you want to continue? (yes/no): "
if /i not "%CONFIRM%"=="yes" (
    echo Operation cancelled.
    pause
    exit /b 0
)

echo.
echo ==========================================
echo STEP 1: Importing Core Data
echo ==========================================
echo [*] Inserting customer_segments, categories, users, shops, products...
mongosh "%MONGO_URI%" --file mongodb_seed_data_atlas.js
if %errorlevel% neq 0 (
    echo [X] Failed to import core data
    pause
    exit /b 1
)
echo [√] Core data imported successfully
echo.

echo ==========================================
echo STEP 2: Importing Transactional Data
echo ==========================================
echo [*] Inserting orders, user_interactions, reviews...
mongosh "%MONGO_URI%" --file mongodb_seed_data_part2_atlas.js
if %errorlevel% neq 0 (
    echo [X] Failed to import transactional data
    pause
    exit /b 1
)
echo [√] Transactional data imported successfully
echo.

echo ==========================================
echo VERIFYING DATA IMPORT
echo ==========================================
echo [*] Checking collection counts...
mongosh "%MONGO_URI%" --quiet --eval "use('cellex_prod'); print('customer_segments:', db.customer_segments.countDocuments()); print('categories:', db.categories.countDocuments()); print('category_attributes:', db.category_attributes.countDocuments()); print('users:', db.users.countDocuments()); print('shops:', db.shops.countDocuments()); print('products:', db.products.countDocuments()); print('orders:', db.orders.countDocuments()); print('user_interactions:', db.user_interactions.countDocuments()); print('reviews:', db.reviews.countDocuments());"
echo.

echo ==========================================
echo SUCCESS! All data imported to MongoDB Atlas
echo ==========================================
echo.
echo Next steps:
echo 1. Start your backend: mvn spring-boot:run
echo 2. Check logs to verify MongoDB Atlas connection
echo 3. Test API endpoints
echo.
pause
