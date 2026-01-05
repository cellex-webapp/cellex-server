// ============================================
// CELLEX E-COMMERCE MONGODB SEED DATA (PART 2)
// Orders, UserInteractions, Reviews
// ============================================
// Chạy file này sau mongodb_seed_data.js
// ============================================

use('cellex');

// ============================================
// 7. ORDERS (Đơn hàng)
// ============================================
print("\nInserting Orders...");
db.orders.deleteMany({});
db.orders.insertMany([
    // Order 1: user_customer_006 (Kim cương) mua iPhone 15 Pro Max
    {
        "_id": "order_001",
        "order_code": "ORD-2024-001",
        "user_id": "user_customer_006",
        "shop_id": "shop_001",
        "shop_name": "TechZone Store",
        "items": [
            {
                "product_id": "product_phone_001",
                "product_name": "iPhone 15 Pro Max 256GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555584/dienthoai1_bkz3yn.jpg",
                "price": 28490500,
                "quantity": 1,
                "subtotal": 28490500
            }
        ],
        "shipping_address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00003",
            "commune_name": "Quận Cầu Giấy",
            "detail_address": "102 Xuân Thủy"
        },
        "subtotal": 28490500,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 28520500,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2024-12-22T10:30:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-22T10:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-22T11:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-23T09:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-25T14:00:00Z") }
        ],
        "note": "Giao giờ hành chính",
        "is_from_cart": true,
        "confirmed_at": new Date("2024-12-22T11:00:00Z"),
        "shipping_at": new Date("2024-12-23T09:00:00Z"),
        "delivered_at": new Date("2024-12-25T14:00:00Z"),
        "created_at": new Date("2024-12-22T10:00:00Z"),
        "updated_at": new Date("2024-12-25T14:00:00Z")
    },
    // Order 2: user_customer_007 (Kim cương) mua Samsung S24 Ultra
    {
        "_id": "order_002",
        "order_code": "ORD-2024-002",
        "user_id": "user_customer_007",
        "shop_id": "shop_001",
        "shop_name": "TechZone Store",
        "items": [
            {
                "product_id": "product_phone_002",
                "product_name": "Samsung Galaxy S24 Ultra 512GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555582/dienthoai2_wrid4u.jpg",
                "price": 31610700,
                "quantity": 1,
                "subtotal": 31610700
            }
        ],
        "shipping_address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79003",
            "commune_name": "Quận 7",
            "detail_address": "250 Nguyễn Văn Linh"
        },
        "subtotal": 31610700,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 31640700,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2024-12-20T15:20:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-20T15:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-20T16:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-21T08:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-23T10:00:00Z") }
        ],
        "note": "",
        "is_from_cart": true,
        "confirmed_at": new Date("2024-12-20T16:00:00Z"),
        "shipping_at": new Date("2024-12-21T08:00:00Z"),
        "delivered_at": new Date("2024-12-23T10:00:00Z"),
        "created_at": new Date("2024-12-20T15:00:00Z"),
        "updated_at": new Date("2024-12-23T10:00:00Z")
    },
    // Order 3: user_customer_003 (Vàng) mua Xiaomi 14 Pro
    {
        "_id": "order_003",
        "order_code": "ORD-2024-003",
        "user_id": "user_customer_003",
        "shop_id": "shop_001",
        "shop_name": "TechZone Store",
        "items": [
            {
                "product_id": "product_phone_003",
                "product_name": "Xiaomi 14 Pro 256GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555581/dienthoai3_d8rgnj.jpg",
                "price": 17991000,
                "quantity": 1,
                "subtotal": 17991000
            }
        ],
        "shipping_address": {
            "province_code": "48",
            "province_name": "Đà Nẵng",
            "commune_code": "48001",
            "commune_name": "Quận Hải Châu",
            "detail_address": "789 Trần Phú"
        },
        "subtotal": 17991000,
        "shipping_fee": 35000,
        "discount_amount": 0,
        "total_amount": 18026000,
        "payment_method": "COD",
        "is_paid": true,
        "paid_at": new Date("2024-12-28T16:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-26T09:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-26T10:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-27T08:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-28T16:00:00Z") }
        ],
        "note": "",
        "is_from_cart": false,
        "confirmed_at": new Date("2024-12-26T10:00:00Z"),
        "shipping_at": new Date("2024-12-27T08:00:00Z"),
        "delivered_at": new Date("2024-12-28T16:00:00Z"),
        "created_at": new Date("2024-12-26T09:00:00Z"),
        "updated_at": new Date("2024-12-28T16:00:00Z")
    },
    // Order 4: user_customer_005 (Vàng) mua Galaxy A54
    {
        "_id": "order_004",
        "order_code": "ORD-2024-004",
        "user_id": "user_customer_005",
        "shop_id": "shop_001",
        "shop_name": "TechZone Store",
        "items": [
            {
                "product_id": "product_phone_006",
                "product_name": "Samsung Galaxy A54 5G 128GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555579/dienthoai6_q2u7zb.jpg",
                "price": 8491500,
                "quantity": 1,
                "subtotal": 8491500
            }
        ],
        "shipping_address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79002",
            "commune_name": "Quận 3",
            "detail_address": "88 Võ Văn Tần"
        },
        "subtotal": 8491500,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 8521500,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2024-12-18T11:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-18T10:30:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-18T12:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-19T09:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-20T15:00:00Z") }
        ],
        "note": "Giao nhanh",
        "is_from_cart": true,
        "confirmed_at": new Date("2024-12-18T12:00:00Z"),
        "shipping_at": new Date("2024-12-19T09:00:00Z"),
        "delivered_at": new Date("2024-12-20T15:00:00Z"),
        "created_at": new Date("2024-12-18T10:30:00Z"),
        "updated_at": new Date("2024-12-20T15:00:00Z")
    },
    // Order 5: user_customer_010 (Kim cương) mua MacBook Pro
    {
        "_id": "order_005",
        "order_code": "ORD-2024-005",
        "user_id": "user_customer_010",
        "shop_id": "shop_003",
        "shop_name": "Digital World",
        "items": [
            {
                "product_id": "product_laptop_001",
                "product_name": "MacBook Pro 14 inch M3 Pro 512GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555587/laptop1_jkjynm.jpg",
                "price": 50340500,
                "quantity": 1,
                "subtotal": 50340500
            }
        ],
        "shipping_address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79004",
            "commune_name": "Quận Bình Thạnh",
            "detail_address": "199 Điện Biên Phủ"
        },
        "subtotal": 50340500,
        "shipping_fee": 0,
        "discount_amount": 0,
        "total_amount": 50340500,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2025-01-02T09:30:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2025-01-02T09:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2025-01-02T10:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2025-01-03T08:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2025-01-04T11:00:00Z") }
        ],
        "note": "Giao trong giờ hành chính",
        "is_from_cart": false,
        "confirmed_at": new Date("2025-01-02T10:00:00Z"),
        "shipping_at": new Date("2025-01-03T08:00:00Z"),
        "delivered_at": new Date("2025-01-04T11:00:00Z"),
        "created_at": new Date("2025-01-02T09:00:00Z"),
        "updated_at": new Date("2025-01-04T11:00:00Z")
    },
    // Order 6: user_customer_003 (Vàng) mua ASUS ROG
    {
        "_id": "order_006",
        "order_code": "ORD-2024-006",
        "user_id": "user_customer_003",
        "shop_id": "shop_003",
        "shop_name": "Digital World",
        "items": [
            {
                "product_id": "product_laptop_002",
                "product_name": "ASUS ROG Strix G16 G614JV i7-13650HX",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555586/laptop2_n6ysje.jpg",
                "price": 32391000,
                "quantity": 1,
                "subtotal": 32391000
            }
        ],
        "shipping_address": {
            "province_code": "48",
            "province_name": "Đà Nẵng",
            "commune_code": "48001",
            "commune_name": "Quận Hải Châu",
            "detail_address": "789 Trần Phú"
        },
        "subtotal": 32391000,
        "shipping_fee": 0,
        "discount_amount": 0,
        "total_amount": 32391000,
        "payment_method": "COD",
        "is_paid": true,
        "paid_at": new Date("2024-12-30T14:30:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-29T10:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-29T11:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-30T08:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-30T14:30:00Z") }
        ],
        "note": "",
        "is_from_cart": true,
        "confirmed_at": new Date("2024-12-29T11:00:00Z"),
        "shipping_at": new Date("2024-12-30T08:00:00Z"),
        "delivered_at": new Date("2024-12-30T14:30:00Z"),
        "created_at": new Date("2024-12-29T10:00:00Z"),
        "updated_at": new Date("2024-12-30T14:30:00Z")
    },
    // Order 7: user_customer_004 (Vàng) mua iPad Pro
    {
        "_id": "order_007",
        "order_code": "ORD-2024-007",
        "user_id": "user_customer_004",
        "shop_id": "shop_002",
        "shop_name": "Gadget Paradise",
        "items": [
            {
                "product_id": "product_tablet_001",
                "product_name": "iPad Pro 12.9 inch M2 WiFi 256GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/maytinhbang1_a2b9sv.jpg",
                "price": 31340500,
                "quantity": 1,
                "subtotal": 31340500
            }
        ],
        "shipping_address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00002",
            "commune_name": "Quận Hoàn Kiếm",
            "detail_address": "15 Hàng Bài"
        },
        "subtotal": 31340500,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 31370500,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2024-12-24T13:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-24T12:30:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-24T14:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-25T09:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-26T16:00:00Z") }
        ],
        "note": "",
        "is_from_cart": true,
        "confirmed_at": new Date("2024-12-24T14:00:00Z"),
        "shipping_at": new Date("2024-12-25T09:00:00Z"),
        "delivered_at": new Date("2024-12-26T16:00:00Z"),
        "created_at": new Date("2024-12-24T12:30:00Z"),
        "updated_at": new Date("2024-12-26T16:00:00Z")
    },
    // Order 8: user_customer_006 (Kim cương) mua Galaxy Tab S9 Ultra
    {
        "_id": "order_008",
        "order_code": "ORD-2024-008",
        "user_id": "user_customer_006",
        "shop_id": "shop_002",
        "shop_name": "Gadget Paradise",
        "items": [
            {
                "product_id": "product_tablet_002",
                "product_name": "Samsung Galaxy Tab S9 Ultra 5G 512GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/maytinhbang2_nnclqx.jpg",
                "price": 26991000,
                "quantity": 1,
                "subtotal": 26991000
            }
        ],
        "shipping_address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00003",
            "commune_name": "Quận Cầu Giấy",
            "detail_address": "102 Xuân Thủy"
        },
        "subtotal": 26991000,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 27021000,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2024-12-15T10:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2024-12-15T09:30:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2024-12-15T11:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2024-12-16T08:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2024-12-17T15:00:00Z") }
        ],
        "note": "",
        "is_from_cart": false,
        "confirmed_at": new Date("2024-12-15T11:00:00Z"),
        "shipping_at": new Date("2024-12-16T08:00:00Z"),
        "delivered_at": new Date("2024-12-17T15:00:00Z"),
        "created_at": new Date("2024-12-15T09:30:00Z"),
        "updated_at": new Date("2024-12-17T15:00:00Z")
    },
    // Order 9: user_customer_001 (Đồng) mua Redmi Note 13 Pro
    {
        "_id": "order_009",
        "order_code": "ORD-2024-009",
        "user_id": "user_customer_001",
        "shop_id": "shop_001",
        "shop_name": "TechZone Store",
        "items": [
            {
                "product_id": "product_phone_007",
                "product_name": "Xiaomi Redmi Note 13 Pro 256GB",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555578/dienthoai7_nqgfp9.jpg",
                "price": 7191000,
                "quantity": 1,
                "subtotal": 7191000
            }
        ],
        "shipping_address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00001",
            "commune_name": "Quận Ba Đình",
            "detail_address": "123 Đường Láng"
        },
        "subtotal": 7191000,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 7221000,
        "payment_method": "COD",
        "is_paid": true,
        "paid_at": new Date("2025-01-03T14:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2025-01-03T10:00:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2025-01-03T11:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2025-01-03T12:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2025-01-03T14:00:00Z") }
        ],
        "note": "",
        "is_from_cart": true,
        "confirmed_at": new Date("2025-01-03T11:00:00Z"),
        "shipping_at": new Date("2025-01-03T12:00:00Z"),
        "delivered_at": new Date("2025-01-03T14:00:00Z"),
        "created_at": new Date("2025-01-03T10:00:00Z"),
        "updated_at": new Date("2025-01-03T14:00:00Z")
    },
    // Order 10: user_customer_002 (Đồng) mua AirPods Pro 2
    {
        "_id": "order_010",
        "order_code": "ORD-2024-010",
        "user_id": "user_customer_002",
        "shop_id": "shop_002",
        "shop_name": "Gadget Paradise",
        "items": [
            {
                "product_id": "product_headphone_001",
                "product_name": "AirPods Pro 2 USB-C",
                "product_image": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555568/tainghe1_s3cyo5.jpg",
                "price": 5841000,
                "quantity": 1,
                "subtotal": 5841000
            }
        ],
        "shipping_address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79001",
            "commune_name": "Quận 1",
            "detail_address": "456 Nguyễn Huệ"
        },
        "subtotal": 5841000,
        "shipping_fee": 30000,
        "discount_amount": 0,
        "total_amount": 5871000,
        "payment_method": "VNPAY",
        "is_paid": true,
        "paid_at": new Date("2025-01-01T16:00:00Z"),
        "status": "DELIVERED",
        "status_history": [
            { "status": "PENDING", "timestamp": new Date("2025-01-01T15:30:00Z") },
            { "status": "CONFIRMED", "timestamp": new Date("2025-01-01T17:00:00Z") },
            { "status": "SHIPPING", "timestamp": new Date("2025-01-02T09:00:00Z") },
            { "status": "DELIVERED", "timestamp": new Date("2025-01-02T16:00:00Z") }
        ],
        "note": "",
        "is_from_cart": false,
        "confirmed_at": new Date("2025-01-01T17:00:00Z"),
        "shipping_at": new Date("2025-01-02T09:00:00Z"),
        "delivered_at": new Date("2025-01-02T16:00:00Z"),
        "created_at": new Date("2025-01-01T15:30:00Z"),
        "updated_at": new Date("2025-01-02T16:00:00Z")
    }
]);
print("✓ Orders inserted: 10");

// Update product purchase_count
db.products.updateOne({ "_id": "product_phone_001" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_phone_002" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_phone_003" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_phone_006" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_phone_007" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_laptop_001" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_laptop_002" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_tablet_001" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_tablet_002" }, { $inc: { "purchase_count": 1 } });
db.products.updateOne({ "_id": "product_headphone_001" }, { $inc: { "purchase_count": 1 } });
print("✓ Product purchase_count updated");

// ============================================
// 8. USER INTERACTIONS (Tương tác người dùng)
// ============================================
print("\nInserting User Interactions...");
db.user_interactions.deleteMany({});
db.user_interactions.insertMany([
    // User 6 (Kim cương) - Fan Apple
    {
        "_id": "interaction_001",
        "user_id": "user_customer_006",
        "product_id": "product_phone_001",
        "category_id": "cat_phone_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 17, // 5*1 + 1*3 + 1*5 + 1*4 = 17
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-26T00:00:00Z")
    },
    {
        "_id": "interaction_002",
        "user_id": "user_customer_006",
        "product_id": "product_tablet_001",
        "category_id": "cat_tablet_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3, // 3*1 = 3
        "created_at": new Date("2024-12-21T00:00:00Z"),
        "updated_at": new Date("2024-12-21T00:00:00Z")
    },
    {
        "_id": "interaction_003",
        "user_id": "user_customer_006",
        "product_id": "product_tablet_002",
        "category_id": "cat_tablet_001",
        "view_count": 4,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 16, // 4*1 + 1*3 + 1*5 + 1*4 = 16
        "created_at": new Date("2024-12-14T00:00:00Z"),
        "updated_at": new Date("2024-12-18T00:00:00Z")
    },
    {
        "_id": "interaction_004",
        "user_id": "user_customer_006",
        "product_id": "product_laptop_001",
        "category_id": "cat_laptop_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2, // 2*1 = 2
        "created_at": new Date("2024-12-22T00:00:00Z"),
        "updated_at": new Date("2024-12-22T00:00:00Z")
    },

    // User 7 (Kim cương) - Fan Samsung
    {
        "_id": "interaction_005",
        "user_id": "user_customer_007",
        "product_id": "product_phone_002",
        "category_id": "cat_phone_001",
        "view_count": 6,
        "cart_count": 2,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 21, // 6*1 + 2*3 + 1*5 + 1*4 = 21
        "created_at": new Date("2024-12-18T00:00:00Z"),
        "updated_at": new Date("2024-12-24T00:00:00Z")
    },
    {
        "_id": "interaction_006",
        "user_id": "user_customer_007",
        "product_id": "product_phone_006",
        "category_id": "cat_phone_001",
        "view_count": 4,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 7, // 4*1 + 1*3 = 7
        "created_at": new Date("2024-12-19T00:00:00Z"),
        "updated_at": new Date("2024-12-19T00:00:00Z")
    },
    {
        "_id": "interaction_007",
        "user_id": "user_customer_007",
        "product_id": "product_tablet_002",
        "category_id": "cat_tablet_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3, // 3*1 = 3
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-20T00:00:00Z")
    },

    // User 3 (Vàng) - Quan tâm Xiaomi và Gaming
    {
        "_id": "interaction_008",
        "user_id": "user_customer_003",
        "product_id": "product_phone_003",
        "category_id": "cat_phone_001",
        "view_count": 7,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 19, // 7*1 + 1*3 + 1*5 + 1*4 = 19
        "created_at": new Date("2024-12-24T00:00:00Z"),
        "updated_at": new Date("2024-12-29T00:00:00Z")
    },
    {
        "_id": "interaction_009",
        "user_id": "user_customer_003",
        "product_id": "product_phone_007",
        "category_id": "cat_phone_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3, // 3*1 = 3
        "created_at": new Date("2024-12-25T00:00:00Z"),
        "updated_at": new Date("2024-12-25T00:00:00Z")
    },
    {
        "_id": "interaction_010",
        "user_id": "user_customer_003",
        "product_id": "product_laptop_002",
        "category_id": "cat_laptop_001",
        "view_count": 8,
        "cart_count": 2,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 23, // 8*1 + 2*3 + 1*5 + 1*4 = 23
        "created_at": new Date("2024-12-27T00:00:00Z"),
        "updated_at": new Date("2024-12-31T00:00:00Z")
    },
    {
        "_id": "interaction_011",
        "user_id": "user_customer_003",
        "product_id": "product_laptop_003",
        "category_id": "cat_laptop_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2, // 2*1 = 2
        "created_at": new Date("2024-12-28T00:00:00Z"),
        "updated_at": new Date("2024-12-28T00:00:00Z")
    },

    // User 4 (Vàng) - Quan tâm Tablet cao cấp
    {
        "_id": "interaction_012",
        "user_id": "user_customer_004",
        "product_id": "product_tablet_001",
        "category_id": "cat_tablet_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 17, // 5*1 + 1*3 + 1*5 + 1*4 = 17
        "created_at": new Date("2024-12-22T00:00:00Z"),
        "updated_at": new Date("2024-12-27T00:00:00Z")
    },
    {
        "_id": "interaction_013",
        "user_id": "user_customer_004",
        "product_id": "product_tablet_003",
        "category_id": "cat_tablet_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4, // 4*1 = 4
        "created_at": new Date("2024-12-23T00:00:00Z"),
        "updated_at": new Date("2024-12-23T00:00:00Z")
    },
    {
        "_id": "interaction_014",
        "user_id": "user_customer_004",
        "product_id": "product_headphone_001",
        "category_id": "cat_accessories_001",
        "view_count": 3,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 6, // 3*1 + 1*3 = 6
        "created_at": new Date("2024-12-24T00:00:00Z"),
        "updated_at": new Date("2024-12-24T00:00:00Z")
    },

    // User 5 (Vàng) - Quan tâm điện thoại tầm trung
    {
        "_id": "interaction_015",
        "user_id": "user_customer_005",
        "product_id": "product_phone_006",
        "category_id": "cat_phone_001",
        "view_count": 6,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 18, // 6*1 + 1*3 + 1*5 + 1*4 = 18
        "created_at": new Date("2024-12-16T00:00:00Z"),
        "updated_at": new Date("2024-12-21T00:00:00Z")
    },
    {
        "_id": "interaction_016",
        "user_id": "user_customer_005",
        "product_id": "product_phone_007",
        "category_id": "cat_phone_001",
        "view_count": 5,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 5, // 5*1 = 5
        "created_at": new Date("2024-12-17T00:00:00Z"),
        "updated_at": new Date("2024-12-17T00:00:00Z")
    },
    {
        "_id": "interaction_017",
        "user_id": "user_customer_005",
        "product_id": "product_phone_008",
        "category_id": "cat_phone_001",
        "view_count": 4,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 7, // 4*1 + 1*3 = 7
        "created_at": new Date("2024-12-18T00:00:00Z"),
        "updated_at": new Date("2024-12-18T00:00:00Z")
    },

    // User 10 (Kim cương) - Quan tâm MacBook
    {
        "_id": "interaction_018",
        "user_id": "user_customer_010",
        "product_id": "product_laptop_001",
        "category_id": "cat_laptop_001",
        "view_count": 9,
        "cart_count": 2,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 24, // 9*1 + 2*3 + 1*5 + 1*4 = 24
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-05T00:00:00Z")
    },
    {
        "_id": "interaction_019",
        "user_id": "user_customer_010",
        "product_id": "product_phone_001",
        "category_id": "cat_phone_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4, // 4*1 = 4
        "created_at": new Date("2025-01-02T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    {
        "_id": "interaction_020",
        "user_id": "user_customer_010",
        "product_id": "product_tablet_001",
        "category_id": "cat_tablet_001",
        "view_count": 3,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 6, // 3*1 + 1*3 = 6
        "created_at": new Date("2025-01-03T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },

    // User 1 (Đồng) - Quan tâm điện thoại giá rẻ
    {
        "_id": "interaction_021",
        "user_id": "user_customer_001",
        "product_id": "product_phone_007",
        "category_id": "cat_phone_001",
        "view_count": 8,
        "cart_count": 2,
        "purchase_count": 1,
        "review_count": 0,
        "total_score": 19, // 8*1 + 2*3 + 1*5 = 19
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },
    {
        "_id": "interaction_022",
        "user_id": "user_customer_001",
        "product_id": "product_phone_006",
        "category_id": "cat_phone_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 8, // 5*1 + 1*3 = 8
        "created_at": new Date("2025-01-02T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    {
        "_id": "interaction_023",
        "user_id": "user_customer_001",
        "product_id": "product_phone_008",
        "category_id": "cat_phone_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3, // 3*1 = 3
        "created_at": new Date("2025-01-02T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },

    // User 2 (Đồng) - Quan tâm tai nghe
    {
        "_id": "interaction_024",
        "user_id": "user_customer_002",
        "product_id": "product_headphone_001",
        "category_id": "cat_accessories_001",
        "view_count": 6,
        "cart_count": 1,
        "purchase_count": 1,
        "review_count": 1,
        "total_score": 18, // 6*1 + 1*3 + 1*5 + 1*4 = 18
        "created_at": new Date("2024-12-30T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },
    {
        "_id": "interaction_025",
        "user_id": "user_customer_002",
        "product_id": "product_headphone_002",
        "category_id": "cat_accessories_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4, // 4*1 = 4
        "created_at": new Date("2024-12-31T00:00:00Z"),
        "updated_at": new Date("2024-12-31T00:00:00Z")
    },
    {
        "_id": "interaction_026",
        "user_id": "user_customer_002",
        "product_id": "product_headphone_003",
        "category_id": "cat_accessories_001",
        "view_count": 3,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 6, // 3*1 + 1*3 = 6
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-01T00:00:00Z")
    },

    // User 8 (Đồng) - Xem nhiều sản phẩm
    {
        "_id": "interaction_027",
        "user_id": "user_customer_008",
        "product_id": "product_phone_007",
        "category_id": "cat_phone_001",
        "view_count": 5,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 5, // 5*1 = 5
        "created_at": new Date("2024-12-15T00:00:00Z"),
        "updated_at": new Date("2024-12-15T00:00:00Z")
    },
    {
        "_id": "interaction_028",
        "user_id": "user_customer_008",
        "product_id": "product_tablet_004",
        "category_id": "cat_tablet_001",
        "view_count": 6,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 9, // 6*1 + 1*3 = 9
        "created_at": new Date("2024-12-16T00:00:00Z"),
        "updated_at": new Date("2024-12-16T00:00:00Z")
    },
    {
        "_id": "interaction_029",
        "user_id": "user_customer_008",
        "product_id": "product_tablet_003",
        "category_id": "cat_tablet_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4, // 4*1 = 4
        "created_at": new Date("2024-12-17T00:00:00Z"),
        "updated_at": new Date("2024-12-17T00:00:00Z")
    },

    // User 9 (Vàng) - Quan tâm laptop văn phòng
    {
        "_id": "interaction_030",
        "user_id": "user_customer_009",
        "product_id": "product_laptop_004",
        "category_id": "cat_laptop_001",
        "view_count": 7,
        "cart_count": 2,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 13, // 7*1 + 2*3 = 13
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-20T00:00:00Z")
    },
    {
        "_id": "interaction_031",
        "user_id": "user_customer_009",
        "product_id": "product_laptop_003",
        "category_id": "cat_laptop_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 8, // 5*1 + 1*3 = 8
        "created_at": new Date("2024-12-21T00:00:00Z"),
        "updated_at": new Date("2024-12-21T00:00:00Z")
    },
    {
        "_id": "interaction_032",
        "user_id": "user_customer_009",
        "product_id": "product_laptop_005",
        "category_id": "cat_laptop_001",
        "view_count": 6,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 9, // 6*1 + 1*3 = 9
        "created_at": new Date("2024-12-22T00:00:00Z"),
        "updated_at": new Date("2024-12-22T00:00:00Z")
    },

    // Thêm interactions để đủ 50
    // User khác xem sản phẩm phụ kiện
    {
        "_id": "interaction_033",
        "user_id": "user_customer_001",
        "product_id": "product_charger_001",
        "category_id": "cat_accessories_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2,
        "created_at": new Date("2025-01-04T00:00:00Z"),
        "updated_at": new Date("2025-01-04T00:00:00Z")
    },
    {
        "_id": "interaction_034",
        "user_id": "user_customer_002",
        "product_id": "product_charger_002",
        "category_id": "cat_accessories_001",
        "view_count": 3,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 6,
        "created_at": new Date("2025-01-02T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    {
        "_id": "interaction_035",
        "user_id": "user_customer_003",
        "product_id": "product_phone_004",
        "category_id": "cat_phone_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4,
        "created_at": new Date("2024-12-26T00:00:00Z"),
        "updated_at": new Date("2024-12-26T00:00:00Z")
    },
    {
        "_id": "interaction_036",
        "user_id": "user_customer_004",
        "product_id": "product_phone_001",
        "category_id": "cat_phone_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3,
        "created_at": new Date("2024-12-25T00:00:00Z"),
        "updated_at": new Date("2024-12-25T00:00:00Z")
    },
    {
        "_id": "interaction_037",
        "user_id": "user_customer_005",
        "product_id": "product_laptop_004",
        "category_id": "cat_laptop_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2,
        "created_at": new Date("2024-12-19T00:00:00Z"),
        "updated_at": new Date("2024-12-19T00:00:00Z")
    },
    {
        "_id": "interaction_038",
        "user_id": "user_customer_006",
        "product_id": "product_headphone_001",
        "category_id": "cat_accessories_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 8,
        "created_at": new Date("2024-12-16T00:00:00Z"),
        "updated_at": new Date("2024-12-16T00:00:00Z")
    },
    {
        "_id": "interaction_039",
        "user_id": "user_customer_007",
        "product_id": "product_laptop_005",
        "category_id": "cat_laptop_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3,
        "created_at": new Date("2024-12-21T00:00:00Z"),
        "updated_at": new Date("2024-12-21T00:00:00Z")
    },
    {
        "_id": "interaction_040",
        "user_id": "user_customer_008",
        "product_id": "product_phone_006",
        "category_id": "cat_phone_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4,
        "created_at": new Date("2024-12-18T00:00:00Z"),
        "updated_at": new Date("2024-12-18T00:00:00Z")
    },
    {
        "_id": "interaction_041",
        "user_id": "user_customer_009",
        "product_id": "product_phone_005",
        "category_id": "cat_phone_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2,
        "created_at": new Date("2024-12-23T00:00:00Z"),
        "updated_at": new Date("2024-12-23T00:00:00Z")
    },
    {
        "_id": "interaction_042",
        "user_id": "user_customer_010",
        "product_id": "product_laptop_003",
        "category_id": "cat_laptop_001",
        "view_count": 4,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 7,
        "created_at": new Date("2025-01-04T00:00:00Z"),
        "updated_at": new Date("2025-01-04T00:00:00Z")
    },
    {
        "_id": "interaction_043",
        "user_id": "user_customer_001",
        "product_id": "product_tablet_004",
        "category_id": "cat_tablet_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3,
        "created_at": new Date("2025-01-03T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },
    {
        "_id": "interaction_044",
        "user_id": "user_customer_002",
        "product_id": "product_phone_008",
        "category_id": "cat_phone_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2,
        "created_at": new Date("2025-01-02T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    {
        "_id": "interaction_045",
        "user_id": "user_customer_003",
        "product_id": "product_tablet_004",
        "category_id": "cat_tablet_001",
        "view_count": 5,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 8,
        "created_at": new Date("2024-12-30T00:00:00Z"),
        "updated_at": new Date("2024-12-30T00:00:00Z")
    },
    {
        "_id": "interaction_046",
        "user_id": "user_customer_004",
        "product_id": "product_phone_002",
        "category_id": "cat_phone_001",
        "view_count": 2,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 2,
        "created_at": new Date("2024-12-26T00:00:00Z"),
        "updated_at": new Date("2024-12-26T00:00:00Z")
    },
    {
        "_id": "interaction_047",
        "user_id": "user_customer_005",
        "product_id": "product_charger_001",
        "category_id": "cat_accessories_001",
        "view_count": 1,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 1,
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-20T00:00:00Z")
    },
    {
        "_id": "interaction_048",
        "user_id": "user_customer_006",
        "product_id": "product_phone_005",
        "category_id": "cat_phone_001",
        "view_count": 3,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 3,
        "created_at": new Date("2024-12-17T00:00:00Z"),
        "updated_at": new Date("2024-12-17T00:00:00Z")
    },
    {
        "_id": "interaction_049",
        "user_id": "user_customer_007",
        "product_id": "product_charger_002",
        "category_id": "cat_accessories_001",
        "view_count": 2,
        "cart_count": 1,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 5,
        "created_at": new Date("2024-12-22T00:00:00Z"),
        "updated_at": new Date("2024-12-22T00:00:00Z")
    },
    {
        "_id": "interaction_050",
        "user_id": "user_customer_008",
        "product_id": "product_headphone_003",
        "category_id": "cat_accessories_001",
        "view_count": 4,
        "cart_count": 0,
        "purchase_count": 0,
        "review_count": 0,
        "total_score": 4,
        "created_at": new Date("2024-12-19T00:00:00Z"),
        "updated_at": new Date("2024-12-19T00:00:00Z")
    }
]);
print("✓ User Interactions inserted: 50");

// ============================================
// 9. REVIEWS (Đánh giá sản phẩm)
// ============================================
// 85% reviews tốt (APPROVED), 15% có vấn đề (PENDING_MODERATION)
print("\nInserting Reviews...");
db.reviews.deleteMany({});
db.reviews.insertMany([
    // REVIEWS TỐT - APPROVED (85%)
    // Review 1: iPhone 15 Pro Max
    {
        "_id": "review_001",
        "product_id": "product_phone_001",
        "user_id": "user_customer_006",
        "user_name": "Ngô Thị Hương",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_001",
        "order_item_id": null,
        "shop_id": "shop_001",
        "rating": 5,
        "comment": "iPhone 15 Pro Max thật sự xuất sắc! Chip A17 Pro xử lý mượt mà, camera chụp ảnh ban đêm rất đẹp. Thiết kế titan sang trọng, cầm nặng tay nhưng chắc chắn. Pin dùng cả ngày không lo hết. Đáng đồng tiền bát gạo!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 12,
        "helpful_voted_user_ids": ["user_customer_001", "user_customer_002", "user_customer_003"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-26T10:00:00Z"),
        "updated_at": new Date("2024-12-26T10:00:00Z")
    },
    // Review 2: Samsung S24 Ultra
    {
        "_id": "review_002",
        "product_id": "product_phone_002",
        "user_id": "user_customer_007",
        "user_name": "Đinh Quốc Anh",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_002",
        "order_item_id": null,
        "shop_id": "shop_001",
        "rating": 5,
        "comment": "Samsung S24 Ultra xứng đáng là flagship! Camera 200MP cực nét, zoom 100x ấn tượng. Bút S-Pen rất tiện lợi cho công việc. Màn hình đẹp, hiển thị sắc nét. Hiệu năng Snapdragon 8 Gen 3 mạnh mẽ, chơi game không lag.",
        "images": [],
        "videos": [],
        "vendor_response": {
            "comment": "Cảm ơn anh đã tin tưởng sản phẩm! Chúc anh sử dụng máy vui vẻ.",
            "responded_at": new Date("2024-12-24T14:00:00Z")
        },
        "is_verified_purchase": true,
        "helpful_count": 8,
        "helpful_voted_user_ids": ["user_customer_004", "user_customer_005"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-24T11:00:00Z"),
        "updated_at": new Date("2024-12-24T14:00:00Z")
    },
    // Review 3: Xiaomi 14 Pro
    {
        "_id": "review_003",
        "product_id": "product_phone_003",
        "user_id": "user_customer_003",
        "user_name": "Đỗ Quang Minh",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_003",
        "order_item_id": null,
        "shop_id": "shop_001",
        "rating": 4,
        "comment": "Xiaomi 14 Pro chất lượng tốt với tầm giá. Camera Leica chụp đẹp, màu sắc tự nhiên. Sạc nhanh 120W tiện lợi. Tuy nhiên MIUI còn nhiều ứng dụng thừa, cần gỡ bớt. Nhìn chung đáng mua nếu thích Android.",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 5,
        "helpful_voted_user_ids": ["user_customer_001"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-29T09:00:00Z"),
        "updated_at": new Date("2024-12-29T09:00:00Z")
    },
    // Review 4: Galaxy A54
    {
        "_id": "review_004",
        "product_id": "product_phone_006",
        "user_id": "user_customer_005",
        "user_name": "Bùi Văn Đức",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_004",
        "order_item_id": null,
        "shop_id": "shop_001",
        "rating": 5,
        "comment": "Điện thoại tầm trung đáng mua nhất hiện nay. Màn hình 120Hz mượt, camera 50MP chụp đẹp. Pin trâu, sử dụng cả ngày không lo hết. Giá rẻ mà chất lượng cao, recommend!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 15,
        "helpful_voted_user_ids": ["user_customer_002", "user_customer_006", "user_customer_008"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-21T10:00:00Z"),
        "updated_at": new Date("2024-12-21T10:00:00Z")
    },
    // Review 5: MacBook Pro M3
    {
        "_id": "review_005",
        "product_id": "product_laptop_001",
        "user_id": "user_customer_010",
        "user_name": "Phan Thị Kim",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_005",
        "order_item_id": null,
        "shop_id": "shop_003",
        "rating": 5,
        "comment": "MacBook Pro M3 Pro là chiếc laptop tuyệt vời nhất tôi từng dùng. Hiệu năng mạnh mẽ cho công việc đồ họa, render video cực nhanh. Màn hình Liquid Retina XDR đẹp lung linh. Pin dùng cả ngày không cần sạc. Tuy hơi đắt nhưng xứng đáng từng đồng!",
        "images": [],
        "videos": [],
        "vendor_response": {
            "comment": "Cảm ơn chị đã tin tưởng shop! Chúc chị làm việc hiệu quả với MacBook mới.",
            "responded_at": new Date("2025-01-05T10:00:00Z")
        },
        "is_verified_purchase": true,
        "helpful_count": 20,
        "helpful_voted_user_ids": ["user_customer_003", "user_customer_004", "user_customer_006", "user_customer_007"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2025-01-05T09:00:00Z"),
        "updated_at": new Date("2025-01-05T10:00:00Z")
    },
    // Review 6: ASUS ROG
    {
        "_id": "review_006",
        "product_id": "product_laptop_002",
        "user_id": "user_customer_003",
        "user_name": "Đỗ Quang Minh",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_006",
        "order_item_id": null,
        "shop_id": "shop_003",
        "rating": 4,
        "comment": "Laptop gaming ASUS ROG mạnh mẽ, chơi game AAA ở setting cao vẫn mượt. RTX 4060 cho đồ họa đẹp. Tuy nhiên máy hơi nóng khi chơi lâu và quạt hơi ồn. Nhìn chung tốt cho game thủ.",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 7,
        "helpful_voted_user_ids": ["user_customer_005", "user_customer_009"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-31T15:00:00Z"),
        "updated_at": new Date("2024-12-31T15:00:00Z")
    },
    // Review 7: iPad Pro
    {
        "_id": "review_007",
        "product_id": "product_tablet_001",
        "user_id": "user_customer_004",
        "user_name": "Hoàng Thị Lan",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_007",
        "order_item_id": null,
        "shop_id": "shop_002",
        "rating": 5,
        "comment": "iPad Pro 12.9 inch tuyệt vời cho công việc thiết kế. Màn hình Liquid Retina XDR sắc nét, màu sắc chính xác. Chip M2 xử lý mượt mà. Apple Pencil Gen 2 viết rất tự nhiên. Rất hài lòng với sản phẩm!",
        "images": [],
        "videos": [],
        "vendor_response": {
            "comment": "Cảm ơn chị đã ủng hộ! Chúc chị làm việc hiệu quả.",
            "responded_at": new Date("2024-12-27T10:00:00Z")
        },
        "is_verified_purchase": true,
        "helpful_count": 10,
        "helpful_voted_user_ids": ["user_customer_001", "user_customer_006"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-27T09:00:00Z"),
        "updated_at": new Date("2024-12-27T10:00:00Z")
    },
    // Review 8: Galaxy Tab S9 Ultra
    {
        "_id": "review_008",
        "product_id": "product_tablet_002",
        "user_id": "user_customer_006",
        "user_name": "Ngô Thị Hương",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_008",
        "order_item_id": null,
        "shop_id": "shop_002",
        "rating": 5,
        "comment": "Galaxy Tab S9 Ultra màn hình khổng lồ 14.6 inch tuyệt đẹp! Bút S-Pen đi kèm rất tiện. Hiệu năng mạnh mẽ, pin trâu. Dùng để xem phim, làm việc đều tốt. Xứng đáng với giá tiền!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 6,
        "helpful_voted_user_ids": ["user_customer_004"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2024-12-18T16:00:00Z"),
        "updated_at": new Date("2024-12-18T16:00:00Z")
    },
    // Review 9: AirPods Pro 2
    {
        "_id": "review_009",
        "product_id": "product_headphone_001",
        "user_id": "user_customer_002",
        "user_name": "Vũ Thu Hà",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": "order_010",
        "order_item_id": null,
        "shop_id": "shop_002",
        "rating": 5,
        "comment": "AirPods Pro 2 chống ồn tuyệt vời! Âm thanh trong trẻo, bass đầm. Tự động kết nối với iPhone rất tiện. Hộp sạc USB-C tiện lợi hơn. Pin dùng lâu, khoảng 6 tiếng nghe nhạc. Rất đáng mua!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": true,
        "helpful_count": 9,
        "helpful_voted_user_ids": ["user_customer_001", "user_customer_005"],
        "status": "APPROVED",
        "moderation_result": null,
        "admin_decision": null,
        "created_at": new Date("2025-01-03T10:00:00Z"),
        "updated_at": new Date("2025-01-03T10:00:00Z")
    },

    // REVIEWS CÓ VẤN ĐỀ - PENDING_MODERATION (15% = khoảng 2 reviews)
    // Review 10: Review có ngôn ngữ không phù hợp (lóng, toxic nhẹ)
    {
        "_id": "review_010",
        "product_id": "product_phone_005",
        "user_id": "user_customer_008",
        "user_name": "Lý Thị Ngọc",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": null,
        "order_item_id": null,
        "shop_id": "shop_001",
        "rating": 2,
        "comment": "Máy này tệ vãi! Pin kém, dùng được mỗi nửa ngày là hết. Giá thì đắt đỏ nhưng chất lượng như đ*t. Khuyên mọi người đừng mua rác này. Shop bán hàng l*a đ*o khách hàng!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": false,
        "helpful_count": 0,
        "helpful_voted_user_ids": [],
        "status": "PENDING_MODERATION",
        "moderation_result": {
            "is_flagged": true,
            "toxicity_score": 0.85,
            "detected_issues": ["PROFANITY", "TOXIC_LANGUAGE", "NEGATIVE_SENTIMENT"],
            "confidence": 0.92,
            "analyzed_at": new Date("2025-01-04T10:00:00Z")
        },
        "admin_decision": null,
        "created_at": new Date("2025-01-04T09:30:00Z"),
        "updated_at": new Date("2025-01-04T10:00:00Z")
    },
    // Review 11: Review spam quảng cáo
    {
        "_id": "review_011",
        "product_id": "product_laptop_004",
        "user_id": "user_customer_009",
        "user_name": "Trịnh Văn Bình",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": null,
        "order_item_id": null,
        "shop_id": "shop_003",
        "rating": 5,
        "comment": "Sản phẩm tốt nhưng các bạn nên mua tại shop TechMart.vn giá rẻ hơn nhiều! Liên hệ Zalo 0987654321 để được tư vấn và giảm giá thêm 20%. Giao hàng toàn quốc, bảo hành 2 năm!",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": false,
        "helpful_count": 0,
        "helpful_voted_user_ids": [],
        "status": "PENDING_MODERATION",
        "moderation_result": {
            "is_flagged": true,
            "toxicity_score": 0.45,
            "detected_issues": ["SPAM", "COMPETITOR_MENTION", "CONTACT_INFO"],
            "confidence": 0.88,
            "analyzed_at": new Date("2025-01-04T11:00:00Z")
        },
        "admin_decision": null,
        "created_at": new Date("2025-01-04T10:30:00Z"),
        "updated_at": new Date("2025-01-04T11:00:00Z")
    },
    // Review 12: Review off-topic, không liên quan sản phẩm
    {
        "_id": "review_012",
        "product_id": "product_tablet_003",
        "user_id": "user_customer_001",
        "user_name": "Phạm Minh Tuấn",
        "user_avatar": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "order_id": null,
        "order_item_id": null,
        "shop_id": "shop_002",
        "rating": 3,
        "comment": "Chưa mua sản phẩm này nhưng hôm qua tôi đi ăn phở rất ngon. Nhà hàng phục vụ tốt, giá cả hợp lý. Mọi người nên thử! À còn ship hàng của shop hơi lâu nhé.",
        "images": [],
        "videos": [],
        "vendor_response": null,
        "is_verified_purchase": false,
        "helpful_count": 0,
        "helpful_voted_user_ids": [],
        "status": "PENDING_MODERATION",
        "moderation_result": {
            "is_flagged": true,
            "toxicity_score": 0.15,
            "detected_issues": ["OFF_TOPIC", "IRRELEVANT_CONTENT"],
            "confidence": 0.79,
            "analyzed_at": new Date("2025-01-05T09:00:00Z")
        },
        "admin_decision": null,
        "created_at": new Date("2025-01-05T08:30:00Z"),
        "updated_at": new Date("2025-01-05T09:00:00Z")
    }
]);
print("✓ Reviews inserted: 12 (9 approved, 3 pending moderation)");

// Update product review statistics
db.products.updateOne(
    { "_id": "product_phone_001" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_phone_002" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_phone_003" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 4.0 }
    }
);
db.products.updateOne(
    { "_id": "product_phone_006" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_laptop_001" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_laptop_002" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 4.0 }
    }
);
db.products.updateOne(
    { "_id": "product_tablet_001" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_tablet_002" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
db.products.updateOne(
    { "_id": "product_headphone_001" },
    { 
        $inc: { "review_count": 1 },
        $set: { "average_rating": 5.0 }
    }
);
print("✓ Product review statistics updated");

// Update user interaction review_count
db.user_interactions.updateOne(
    { "user_id": "user_customer_006", "product_id": "product_phone_001" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_007", "product_id": "product_phone_002" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_003", "product_id": "product_phone_003" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_005", "product_id": "product_phone_006" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_010", "product_id": "product_laptop_001" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_003", "product_id": "product_laptop_002" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_004", "product_id": "product_tablet_001" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_006", "product_id": "product_tablet_002" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
db.user_interactions.updateOne(
    { "user_id": "user_customer_002", "product_id": "product_headphone_001" },
    { $set: { "review_count": 1 }, $inc: { "total_score": 4 } }
);
print("✓ User interaction review_count updated");

print("\n==============================================");
print("✅ SEED DATA IMPORT COMPLETED SUCCESSFULLY!");
print("==============================================");
print("Summary:");
print("- Customer Segments: 3");
print("- Categories: 4");
print("- Category Attributes: 18");
print("- Users: 14 (1 admin, 3 vendors, 10 customers)");
print("- Shops: 3");
print("- Products: 22 (8 phones, 5 laptops, 4 tablets, 2 chargers, 3 headphones)");
print("- Orders: 10");
print("- User Interactions: 50");
print("- Reviews: 12 (9 approved, 3 pending moderation)");
print("==============================================");
