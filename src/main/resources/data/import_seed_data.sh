#!/bin/bash

# ============================================
# Cellex MongoDB Seed Data Import Script
# ============================================
# Script tự động import seed data vào MongoDB
# ============================================

echo "=========================================="
echo "  CELLEX MONGODB SEED DATA IMPORTER"
echo "=========================================="
echo ""

# Cấu hình
DB_NAME="cellex"
MONGO_URI="mongodb://localhost:27017"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Màu sắc cho output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Kiểm tra MongoDB
echo "🔍 Checking MongoDB connection..."
if ! mongosh "$MONGO_URI/$DB_NAME" --eval "db.runCommand({ ping: 1 })" > /dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to MongoDB at $MONGO_URI${NC}"
    echo "Please make sure MongoDB is running and accessible."
    exit 1
fi
echo -e "${GREEN}✅ MongoDB connection successful${NC}"
echo ""

# Xác nhận trước khi xóa dữ liệu
echo -e "${YELLOW}⚠️  WARNING: This will DELETE all existing data in the following collections:${NC}"
echo "   - customer_segments"
echo "   - categories"
echo "   - category_attributes"
echo "   - users"
echo "   - shops"
echo "   - products"
echo "   - orders"
echo "   - user_interactions"
echo "   - reviews"
echo ""
read -p "Do you want to continue? (yes/no): " -r
echo ""
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo "Import cancelled."
    exit 0
fi

# Import Part 1: Core Data
echo "📦 Importing Part 1: Core Data (Segments, Categories, Users, Shops, Products)..."
if mongosh "$MONGO_URI/$DB_NAME" "$SCRIPT_DIR/mongodb_seed_data.js" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Part 1 imported successfully${NC}"
else
    echo -e "${RED}❌ Failed to import Part 1${NC}"
    exit 1
fi
echo ""

# Import Part 2: Transactional Data
echo "📦 Importing Part 2: Transactional Data (Orders, Interactions, Reviews)..."
if mongosh "$MONGO_URI/$DB_NAME" "$SCRIPT_DIR/mongodb_seed_data_part2.js" > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Part 2 imported successfully${NC}"
else
    echo -e "${RED}❌ Failed to import Part 2${NC}"
    exit 1
fi
echo ""

# Verify data
echo "🔍 Verifying imported data..."
echo ""

# Count documents
SEGMENTS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.customer_segments.countDocuments()")
CATEGORIES=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.categories.countDocuments()")
ATTRIBUTES=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.category_attributes.countDocuments()")
USERS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.users.countDocuments()")
SHOPS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.shops.countDocuments()")
PRODUCTS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.products.countDocuments()")
ORDERS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.orders.countDocuments()")
INTERACTIONS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.user_interactions.countDocuments()")
REVIEWS=$(mongosh "$MONGO_URI/$DB_NAME" --quiet --eval "db.reviews.countDocuments()")

echo "📊 Import Summary:"
echo "   Customer Segments: $SEGMENTS (expected: 3)"
echo "   Categories: $CATEGORIES (expected: 4)"
echo "   Category Attributes: $ATTRIBUTES (expected: 18)"
echo "   Users: $USERS (expected: 14)"
echo "   Shops: $SHOPS (expected: 3)"
echo "   Products: $PRODUCTS (expected: 22)"
echo "   Orders: $ORDERS (expected: 10)"
echo "   User Interactions: $INTERACTIONS (expected: 50)"
echo "   Reviews: $REVIEWS (expected: 12)"
echo ""

# Check if all counts match
if [ "$SEGMENTS" -eq 3 ] && [ "$CATEGORIES" -eq 4 ] && [ "$ATTRIBUTES" -eq 18 ] && \
   [ "$USERS" -eq 14 ] && [ "$SHOPS" -eq 3 ] && [ "$PRODUCTS" -eq 22 ] && \
   [ "$ORDERS" -eq 10 ] && [ "$INTERACTIONS" -eq 50 ] && [ "$REVIEWS" -eq 12 ]; then
    echo -e "${GREEN}✅ All data imported successfully!${NC}"
    echo ""
    echo "🎉 You can now use the following credentials:"
    echo "   Admin:    admin@cellex.vn / admin123"
    echo "   Vendor:   hung.vendor@cellex.vn / admin123"
    echo "   Customer: tuan.customer@gmail.com / admin123"
else
    echo -e "${YELLOW}⚠️  Some counts don't match expected values. Please check the logs.${NC}"
fi

echo ""
echo "=========================================="
echo "  Import completed!"
echo "=========================================="
