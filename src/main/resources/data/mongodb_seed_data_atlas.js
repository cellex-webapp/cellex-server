// ============================================
// CELLEX E-COMMERCE MONGODB SEED DATA
// ============================================
// Dữ liệu mẫu chất lượng cao để huấn luyện AI
// - Recommendation System
// - Content Moderation AI
// ============================================

// Kết nối database
use('cellex_prod');

// ============================================
// 1. CUSTOMER SEGMENTS (Phân khúc khách hàng)
// ============================================
print("Inserting Customer Segments...");
db.customer_segments.deleteMany({});
db.customer_segments.insertMany([
    {
        "_id": "segment_bronze_001",
        "name": "Đồng",
        "min_spend": 0,
        "max_spend": 5000000,
        "level": 1,
        "description": "Khách hàng mới hoặc chi tiêu thấp",
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-01T00:00:00Z")
    },
    {
        "_id": "segment_gold_001",
        "name": "Vàng",
        "min_spend": 5000000,
        "max_spend": 20000000,
        "level": 2,
        "description": "Khách hàng trung thành với chi tiêu ổn định",
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-01T00:00:00Z")
    },
    {
        "_id": "segment_diamond_001",
        "name": "Kim cương",
        "min_spend": 20000000,
        "max_spend": null,
        "level": 3,
        "description": "Khách hàng VIP với chi tiêu cao",
        "created_at": new Date("2025-01-01T00:00:00Z"),
        "updated_at": new Date("2025-01-01T00:00:00Z")
    }
]);
print("✓ Customer Segments inserted: 3");

// ============================================
// 2. CATEGORIES (Danh mục sản phẩm)
// ============================================
print("\nInserting Categories...");
db.categories.deleteMany({});
db.categories.insertMany([
    {
        "_id": "cat_phone_001",
        "name": "Điện thoại",
        "slug": "dien-thoai",
        "parent_id": null,
        "image_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917097/smartphone_yvztzo.png",
        "description": "Điện thoại thông minh các loại, từ phổ thông đến cao cấp",
        "is_active": true
    },
    {
        "_id": "cat_laptop_001",
        "name": "Laptop",
        "slug": "laptop",
        "parent_id": null,
        "image_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917097/smartphone_yvztzo.png",
        "description": "Laptop văn phòng, gaming và đồ họa chuyên nghiệp",
        "is_active": true
    },
    {
        "_id": "cat_tablet_001",
        "name": "Máy tính bảng",
        "slug": "may-tinh-bang",
        "parent_id": null,
        "image_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917097/smartphone_yvztzo.png",
        "description": "Máy tính bảng cho học tập, giải trí và công việc",
        "is_active": true
    },
    {
        "_id": "cat_accessories_001",
        "name": "Phụ kiện",
        "slug": "phu-kien",
        "parent_id": null,
        "image_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917097/smartphone_yvztzo.png",
        "description": "Phụ kiện công nghệ: sạc, tai nghe, ốp lưng...",
        "is_active": true
    }
]);
print("✓ Categories inserted: 4");

// ============================================
// 3. CATEGORY ATTRIBUTES (Thuộc tính danh mục)
// ============================================
print("\nInserting Category Attributes...");
db.category_attributes.deleteMany({});
db.category_attributes.insertMany([
    // Điện thoại
    {
        "_id": "attr_phone_ram_001",
        "category_id": "cat_phone_001",
        "attribute_name": "RAM",
        "attribute_key": "ram",
        "data_type": "SELECT",
        "unit": "GB",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["4GB", "6GB", "8GB", "12GB", "16GB"],
        "sort_order": 1,
        "description": "Dung lượng RAM của điện thoại",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_phone_storage_001",
        "category_id": "cat_phone_001",
        "attribute_name": "Bộ nhớ trong",
        "attribute_key": "storage",
        "data_type": "SELECT",
        "unit": "GB",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["64GB", "128GB", "256GB", "512GB", "1TB"],
        "sort_order": 2,
        "description": "Dung lượng bộ nhớ trong",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_phone_chipset_001",
        "category_id": "cat_phone_001",
        "attribute_name": "Chipset",
        "attribute_key": "chipset",
        "data_type": "TEXT",
        "unit": "",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 3,
        "description": "Vi xử lý của điện thoại",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_phone_battery_001",
        "category_id": "cat_phone_001",
        "attribute_name": "Pin",
        "attribute_key": "battery",
        "data_type": "NUMBER",
        "unit": "mAh",
        "is_required": true,
        "is_highlight": false,
        "sort_order": 4,
        "description": "Dung lượng pin",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_phone_screen_001",
        "category_id": "cat_phone_001",
        "attribute_name": "Màn hình",
        "attribute_key": "screen",
        "data_type": "TEXT",
        "unit": "inch",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 5,
        "description": "Kích thước và công nghệ màn hình",
        "is_active": true,
        "created_at": new Date()
    },
    // Laptop
    {
        "_id": "attr_laptop_ram_001",
        "category_id": "cat_laptop_001",
        "attribute_name": "RAM",
        "attribute_key": "ram",
        "data_type": "SELECT",
        "unit": "GB",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["8GB", "16GB", "32GB", "64GB"],
        "sort_order": 1,
        "description": "Dung lượng RAM của laptop",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_laptop_cpu_001",
        "category_id": "cat_laptop_001",
        "attribute_name": "CPU",
        "attribute_key": "cpu",
        "data_type": "TEXT",
        "unit": "",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 2,
        "description": "Bộ vi xử lý",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_laptop_storage_001",
        "category_id": "cat_laptop_001",
        "attribute_name": "Ổ cứng",
        "attribute_key": "storage",
        "data_type": "TEXT",
        "unit": "",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 3,
        "description": "Loại và dung lượng ổ cứng",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_laptop_gpu_001",
        "category_id": "cat_laptop_001",
        "attribute_name": "Card đồ họa",
        "attribute_key": "gpu",
        "data_type": "TEXT",
        "unit": "",
        "is_required": false,
        "is_highlight": true,
        "sort_order": 4,
        "description": "Card đồ họa rời (nếu có)",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_laptop_screen_001",
        "category_id": "cat_laptop_001",
        "attribute_name": "Màn hình",
        "attribute_key": "screen",
        "data_type": "TEXT",
        "unit": "inch",
        "is_required": true,
        "is_highlight": false,
        "sort_order": 5,
        "description": "Kích thước màn hình",
        "is_active": true,
        "created_at": new Date()
    },
    // Máy tính bảng
    {
        "_id": "attr_tablet_ram_001",
        "category_id": "cat_tablet_001",
        "attribute_name": "RAM",
        "attribute_key": "ram",
        "data_type": "SELECT",
        "unit": "GB",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["3GB", "4GB", "6GB", "8GB", "16GB"],
        "sort_order": 1,
        "description": "Dung lượng RAM",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_tablet_storage_001",
        "category_id": "cat_tablet_001",
        "attribute_name": "Bộ nhớ",
        "attribute_key": "storage",
        "data_type": "SELECT",
        "unit": "GB",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["64GB", "128GB", "256GB", "512GB"],
        "sort_order": 2,
        "description": "Dung lượng bộ nhớ",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_tablet_screen_001",
        "category_id": "cat_tablet_001",
        "attribute_name": "Màn hình",
        "attribute_key": "screen",
        "data_type": "TEXT",
        "unit": "inch",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 3,
        "description": "Kích thước màn hình",
        "is_active": true,
        "created_at": new Date()
    },
    // Phụ kiện - Sạc
    {
        "_id": "attr_charger_type_001",
        "category_id": "cat_accessories_001",
        "attribute_name": "Loại cổng",
        "attribute_key": "port_type",
        "data_type": "SELECT",
        "unit": "",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["USB-C", "Lightning", "Micro USB", "USB-A"],
        "sort_order": 1,
        "description": "Loại cổng sạc",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_charger_wattage_001",
        "category_id": "cat_accessories_001",
        "attribute_name": "Công suất",
        "attribute_key": "wattage",
        "data_type": "NUMBER",
        "unit": "W",
        "is_required": true,
        "is_highlight": true,
        "sort_order": 2,
        "description": "Công suất sạc",
        "is_active": true,
        "created_at": new Date()
    },
    // Phụ kiện - Tai nghe
    {
        "_id": "attr_headphone_type_001",
        "category_id": "cat_accessories_001",
        "attribute_name": "Loại tai nghe",
        "attribute_key": "headphone_type",
        "data_type": "SELECT",
        "unit": "",
        "is_required": true,
        "is_highlight": true,
        "select_options": ["Bluetooth", "Có dây", "True Wireless"],
        "sort_order": 3,
        "description": "Loại kết nối",
        "is_active": true,
        "created_at": new Date()
    },
    {
        "_id": "attr_headphone_anc_001",
        "category_id": "cat_accessories_001",
        "attribute_name": "Chống ồn",
        "attribute_key": "active_noise_canceling",
        "data_type": "BOOLEAN",
        "unit": "",
        "is_required": false,
        "is_highlight": true,
        "sort_order": 4,
        "description": "Hỗ trợ chống ồn chủ động (ANC)",
        "is_active": true,
        "created_at": new Date()
    }
]);
print("✓ Category Attributes inserted: 18");

// ============================================
// 4. USERS (Người dùng)
// ============================================
print("\nInserting Users...");
db.users.deleteMany({});
db.users.insertMany([
    {
        "_id": "user_admin_001",
        "full_name": "Admin System",
        "email": "admin@cellex.vn",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2", // password: admin123
        "phone_number": "0901234567",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "ADMIN",
        "address": null,
        "customer_segment_id": null,
        "total_spend": 0.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-01T00:00:00Z"),
        "updated_at": new Date("2024-12-01T00:00:00Z")
    },
    {
        "_id": "user_vendor_001",
        "full_name": "Nguyễn Văn Hùng",
        "email": "hung.vendor@cellex.vn",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0912345678",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "VENDOR",
        "address": null,
        "customer_segment_id": null,
        "total_spend": 0.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-05T00:00:00Z"),
        "updated_at": new Date("2024-12-05T00:00:00Z")
    },
    {
        "_id": "user_vendor_002",
        "full_name": "Trần Thị Mai",
        "email": "mai.vendor@cellex.vn",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0923456789",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "VENDOR",
        "address": null,
        "customer_segment_id": null,
        "total_spend": 0.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-06T00:00:00Z"),
        "updated_at": new Date("2024-12-06T00:00:00Z")
    },
    {
        "_id": "user_vendor_003",
        "full_name": "Lê Hoàng Nam",
        "email": "nam.vendor@cellex.vn",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0934567890",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "VENDOR",
        "address": null,
        "customer_segment_id": null,
        "total_spend": 0.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-07T00:00:00Z"),
        "updated_at": new Date("2024-12-07T00:00:00Z")
    },
    // Khách hàng Đồng
    {
        "_id": "user_customer_001",
        "full_name": "Phạm Minh Tuấn",
        "email": "tuan.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0945678901",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00001",
            "commune_name": "Quận Ba Đình",
            "detail_address": "123 Đường Láng"
        },
        "customer_segment_id": "segment_bronze_001",
        "total_spend": 2500000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-11-15T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },
    {
        "_id": "user_customer_002",
        "full_name": "Vũ Thu Hà",
        "email": "ha.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0956789012",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79001",
            "commune_name": "Quận 1",
            "detail_address": "456 Nguyễn Huệ"
        },
        "customer_segment_id": "segment_bronze_001",
        "total_spend": 1800000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-01T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    // Khách hàng Vàng
    {
        "_id": "user_customer_003",
        "full_name": "Đỗ Quang Minh",
        "email": "minh.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0967890123",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "48",
            "province_name": "Đà Nẵng",
            "commune_code": "48001",
            "commune_name": "Quận Hải Châu",
            "detail_address": "789 Trần Phú"
        },
        "customer_segment_id": "segment_gold_001",
        "total_spend": 12000000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-10-10T00:00:00Z"),
        "updated_at": new Date("2025-01-04T00:00:00Z")
    },
    {
        "_id": "user_customer_004",
        "full_name": "Hoàng Thị Lan",
        "email": "lan.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0978901234",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00002",
            "commune_name": "Quận Hoàn Kiếm",
            "detail_address": "15 Hàng Bài"
        },
        "customer_segment_id": "segment_gold_001",
        "total_spend": 8500000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-09-20T00:00:00Z"),
        "updated_at": new Date("2025-01-03T00:00:00Z")
    },
    {
        "_id": "user_customer_005",
        "full_name": "Bùi Văn Đức",
        "email": "duc.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0989012345",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79002",
            "commune_name": "Quận 3",
            "detail_address": "88 Võ Văn Tần"
        },
        "customer_segment_id": "segment_gold_001",
        "total_spend": 15500000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-08-15T00:00:00Z"),
        "updated_at": new Date("2025-01-05T00:00:00Z")
    },
    // Khách hàng Kim cương
    {
        "_id": "user_customer_006",
        "full_name": "Ngô Thị Hương",
        "email": "huong.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0990123456",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00003",
            "commune_name": "Quận Cầu Giấy",
            "detail_address": "102 Xuân Thủy"
        },
        "customer_segment_id": "segment_diamond_001",
        "total_spend": 35000000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-07-01T00:00:00Z"),
        "updated_at": new Date("2025-01-05T00:00:00Z")
    },
    {
        "_id": "user_customer_007",
        "full_name": "Đinh Quốc Anh",
        "email": "anh.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0901234568",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79003",
            "commune_name": "Quận 7",
            "detail_address": "250 Nguyễn Văn Linh"
        },
        "customer_segment_id": "segment_diamond_001",
        "total_spend": 42000000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-06-15T00:00:00Z"),
        "updated_at": new Date("2025-01-04T00:00:00Z")
    },
    // Khách hàng bổ sung
    {
        "_id": "user_customer_008",
        "full_name": "Lý Thị Ngọc",
        "email": "ngoc.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0912345679",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "48",
            "province_name": "Đà Nẵng",
            "commune_code": "48002",
            "commune_name": "Quận Thanh Khê",
            "detail_address": "55 Nguyễn Hữu Thọ"
        },
        "customer_segment_id": "segment_bronze_001",
        "total_spend": 3200000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-12-10T00:00:00Z"),
        "updated_at": new Date("2025-01-02T00:00:00Z")
    },
    {
        "_id": "user_customer_009",
        "full_name": "Trịnh Văn Bình",
        "email": "binh.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0923456780",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00004",
            "commune_name": "Quận Đống Đa",
            "detail_address": "77 Xã Đàn"
        },
        "customer_segment_id": "segment_gold_001",
        "total_spend": 9800000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-11-01T00:00:00Z"),
        "updated_at": new Date("2025-01-04T00:00:00Z")
    },
    {
        "_id": "user_customer_010",
        "full_name": "Phan Thị Kim",
        "email": "kim.customer@gmail.com",
        "password": "$2a$10$xNH0Y2yPSJOBPXzF4Jls/OPqQvEt8FsHZ1R3LxGYx4.xkY5FQ0kv2",
        "phone_number": "0934567891",
        "avatar_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "role": "CUSTOMER",
        "address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79004",
            "commune_name": "Quận Bình Thạnh",
            "detail_address": "199 Điện Biên Phủ"
        },
        "customer_segment_id": "segment_diamond_001",
        "total_spend": 28000000.0,
        "is_active": true,
        "is_banned": false,
        "created_at": new Date("2024-08-20T00:00:00Z"),
        "updated_at": new Date("2025-01-05T00:00:00Z")
    }
]);
print("✓ Users inserted: 14");

// ============================================
// 5. SHOPS (Cửa hàng)
// ============================================
print("\nInserting Shops...");
db.shops.deleteMany({});
db.shops.insertMany([
    {
        "_id": "shop_001",
        "vendor_id": "user_vendor_001",
        "shop_name": "TechZone Store",
        "description": "Chuyên cung cấp điện thoại, laptop chính hãng với giá tốt nhất thị trường. Cam kết bảo hành 12 tháng, đổi trả trong 7 ngày.",
        "logo_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "address": {
            "province_code": "01",
            "province_name": "Hà Nội",
            "commune_code": "00001",
            "commune_name": "Quận Ba Đình",
            "detail_address": "45 Nguyễn Thái Học"
        },
        "phone_number": "0912345678",
        "email": "hung.vendor@cellex.vn",
        "status": "APPROVED",
        "rating": 4.5,
        "created_at": new Date("2024-12-05T00:00:00Z"),
        "updated_at": new Date("2024-12-05T00:00:00Z")
    },
    {
        "_id": "shop_002",
        "vendor_id": "user_vendor_002",
        "shop_name": "Gadget Paradise",
        "description": "Thiên đường công nghệ - Máy tính bảng, phụ kiện cao cấp. Miễn phí vận chuyển toàn quốc cho đơn hàng trên 500k.",
        "logo_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "address": {
            "province_code": "79",
            "province_name": "TP. Hồ Chí Minh",
            "commune_code": "79001",
            "commune_name": "Quận 1",
            "detail_address": "123 Lê Lợi"
        },
        "phone_number": "0923456789",
        "email": "mai.vendor@cellex.vn",
        "status": "APPROVED",
        "rating": 4.7,
        "created_at": new Date("2024-12-06T00:00:00Z"),
        "updated_at": new Date("2024-12-06T00:00:00Z")
    },
    {
        "_id": "shop_003",
        "vendor_id": "user_vendor_003",
        "shop_name": "Digital World",
        "description": "Thế giới số - Chuyên laptop gaming, workstation và phụ kiện chuyên nghiệp. Hỗ trợ trả góp 0% lãi suất.",
        "logo_url": "https://res.cloudinary.com/dr8ez6ua8/image/upload/v1761917096/avatar_pz0phg.avif",
        "address": {
            "province_code": "48",
            "province_name": "Đà Nẵng",
            "commune_code": "48001",
            "commune_name": "Quận Hải Châu",
            "detail_address": "88 Hùng Vương"
        },
        "phone_number": "0934567890",
        "email": "nam.vendor@cellex.vn",
        "status": "APPROVED",
        "rating": 4.8,
        "created_at": new Date("2024-12-07T00:00:00Z"),
        "updated_at": new Date("2024-12-07T00:00:00Z")
    }
]);
print("✓ Shops inserted: 3");

// ============================================
// 6. PRODUCTS (Sản phẩm)
// ============================================
print("\nInserting Products...");
db.products.deleteMany({});
db.products.insertMany([
    // ===== ĐIỆN THOẠI (8 sản phẩm) =====
    {
        "_id": "product_phone_001",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "iPhone 15 Pro Max 256GB",
        "description": "iPhone 15 Pro Max với chip A17 Pro mạnh mẽ, camera 48MP chuyên nghiệp, màn hình Super Retina XDR 6.7 inch. Thiết kế titan cao cấp, pin trâu, hỗ trợ 5G.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555584/dienthoai1_bkz3yn.jpg"],
        "price": 29990000,
        "sale_off": 5.0,
        "final_price": 28490500,
        "stock_quantity": 50,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Apple A17 Pro", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "4422", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.7 inch Super Retina XDR", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-10T00:00:00Z"),
        "updated_at": new Date("2024-12-10T00:00:00Z")
    },
    {
        "_id": "product_phone_002",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "Samsung Galaxy S24 Ultra 512GB",
        "description": "Galaxy S24 Ultra với bút S-Pen tích hợp, camera 200MP siêu nét, chip Snapdragon 8 Gen 3. Màn hình Dynamic AMOLED 2X 6.8 inch, pin 5000mAh.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555582/dienthoai2_wrid4u.jpg"],
        "price": 33990000,
        "sale_off": 7.0,
        "final_price": 31610700,
        "stock_quantity": 30,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "12GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "512GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Snapdragon 8 Gen 3", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "5000", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.8 inch Dynamic AMOLED 2X", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-10T00:00:00Z"),
        "updated_at": new Date("2024-12-10T00:00:00Z")
    },
    {
        "_id": "product_phone_003",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "Xiaomi 14 Pro 256GB",
        "description": "Xiaomi 14 Pro với camera Leica đỉnh cao, chip Snapdragon 8 Gen 3, sạc nhanh 120W. Màn hình AMOLED 6.73 inch 120Hz, thiết kế sang trọng.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555581/dienthoai3_d8rgnj.jpg"],
        "price": 19990000,
        "sale_off": 10.0,
        "final_price": 17991000,
        "stock_quantity": 45,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "12GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Snapdragon 8 Gen 3", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "4880", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.73 inch AMOLED", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-11T00:00:00Z"),
        "updated_at": new Date("2024-12-11T00:00:00Z")
    },
    {
        "_id": "product_phone_004",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "OPPO Find X7 Ultra 256GB",
        "description": "OPPO Find X7 Ultra với hệ thống 4 camera Hasselblad, chip Snapdragon 8 Gen 3. Màn hình AMOLED 2K 6.82 inch, sạc nhanh 100W.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555580/dienthoai4_oh4kya.jpg"],
        "price": 22990000,
        "sale_off": 8.0,
        "final_price": 21151200,
        "stock_quantity": 25,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "16GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Snapdragon 8 Gen 3", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "5000", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.82 inch AMOLED 2K", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-11T00:00:00Z"),
        "updated_at": new Date("2024-12-11T00:00:00Z")
    },
    {
        "_id": "product_phone_005",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "iPhone 14 Plus 128GB",
        "description": "iPhone 14 Plus với màn hình lớn 6.7 inch, camera kép 12MP, chip A15 Bionic. Pin trâu sử dụng cả ngày, hỗ trợ 5G.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555580/dienthoai5_wvs9wg.jpg"],
        "price": 21990000,
        "sale_off": 12.0,
        "final_price": 19351200,
        "stock_quantity": 60,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "6GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "128GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Apple A15 Bionic", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "4325", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.7 inch Super Retina XDR", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-12T00:00:00Z"),
        "updated_at": new Date("2024-12-12T00:00:00Z")
    },
    {
        "_id": "product_phone_006",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "Samsung Galaxy A54 5G 128GB",
        "description": "Galaxy A54 5G với camera 50MP OIS, chip Exynos 1380, màn hình Super AMOLED 6.4 inch 120Hz. Thiết kế cao cấp, giá tầm trung hợp lý.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555579/dienthoai6_q2u7zb.jpg"],
        "price": 9990000,
        "sale_off": 15.0,
        "final_price": 8491500,
        "stock_quantity": 80,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "128GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Exynos 1380", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "5000", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.4 inch Super AMOLED", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-12T00:00:00Z"),
        "updated_at": new Date("2024-12-12T00:00:00Z")
    },
    {
        "_id": "product_phone_007",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "Xiaomi Redmi Note 13 Pro 256GB",
        "description": "Redmi Note 13 Pro với camera 200MP siêu nét, chip Snapdragon 7s Gen 2, sạc nhanh 67W. Màn hình AMOLED 6.67 inch, pin 5100mAh.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555578/dienthoai7_nqgfp9.jpg"],
        "price": 7990000,
        "sale_off": 10.0,
        "final_price": 7191000,
        "stock_quantity": 100,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "Snapdragon 7s Gen 2", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "5100", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.67 inch AMOLED", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-13T00:00:00Z"),
        "updated_at": new Date("2024-12-13T00:00:00Z")
    },
    {
        "_id": "product_phone_008",
        "shop_id": "shop_001",
        "category_id": "cat_phone_001",
        "name": "OPPO Reno11 F 5G 256GB",
        "description": "OPPO Reno11 F 5G với camera 64MP, chip MediaTek Dimensity 7050, màn hình AMOLED 6.7 inch. Thiết kế mỏng nhẹ, sạc nhanh 67W.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/dienthoai8_tmkjuy.jpg"],
        "price": 8990000,
        "sale_off": 8.0,
        "final_price": 8270800,
        "stock_quantity": 70,
        "attribute_values": [
            { "attribute_id": "attr_phone_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ trong", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_phone_chipset_001", "attribute_key": "chipset", "attribute_name": "Chipset", "value": "MediaTek Dimensity 7050", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_phone_battery_001", "attribute_key": "battery", "attribute_name": "Pin", "value": "5000", "unit": "mAh", "data_type": "NUMBER" },
            { "attribute_id": "attr_phone_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "6.7 inch AMOLED", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-13T00:00:00Z"),
        "updated_at": new Date("2024-12-13T00:00:00Z")
    },

    // ===== LAPTOP (5 sản phẩm) =====
    {
        "_id": "product_laptop_001",
        "shop_id": "shop_003",
        "category_id": "cat_laptop_001",
        "name": "MacBook Pro 14 inch M3 Pro 512GB",
        "description": "MacBook Pro 14 inch với chip M3 Pro mạnh mẽ, màn hình Liquid Retina XDR, RAM 18GB. Hiệu năng đỉnh cao cho công việc đồ họa, lập trình.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555587/laptop1_jkjynm.jpg"],
        "price": 52990000,
        "sale_off": 5.0,
        "final_price": 50340500,
        "stock_quantity": 20,
        "attribute_values": [
            { "attribute_id": "attr_laptop_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "18GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_laptop_cpu_001", "attribute_key": "cpu", "attribute_name": "CPU", "value": "Apple M3 Pro 11-core", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_storage_001", "attribute_key": "storage", "attribute_name": "Ổ cứng", "value": "SSD 512GB", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_gpu_001", "attribute_key": "gpu", "attribute_name": "Card đồ họa", "value": "Apple M3 Pro GPU 14-core", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "14.2 inch Liquid Retina XDR", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-14T00:00:00Z"),
        "updated_at": new Date("2024-12-14T00:00:00Z")
    },
    {
        "_id": "product_laptop_002",
        "shop_id": "shop_003",
        "category_id": "cat_laptop_001",
        "name": "ASUS ROG Strix G16 G614JV i7-13650HX",
        "description": "Laptop gaming ASUS ROG Strix với CPU Intel Core i7-13650HX, RTX 4060 8GB, RAM 16GB DDR5. Màn hình 16 inch 165Hz, tản nhiệt mạnh mẽ.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555586/laptop2_n6ysje.jpg"],
        "price": 35990000,
        "sale_off": 10.0,
        "final_price": 32391000,
        "stock_quantity": 15,
        "attribute_values": [
            { "attribute_id": "attr_laptop_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "16GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_laptop_cpu_001", "attribute_key": "cpu", "attribute_name": "CPU", "value": "Intel Core i7-13650HX", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_storage_001", "attribute_key": "storage", "attribute_name": "Ổ cứng", "value": "SSD 512GB NVMe", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_gpu_001", "attribute_key": "gpu", "attribute_name": "Card đồ họa", "value": "NVIDIA RTX 4060 8GB", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "16 inch FHD 165Hz", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-14T00:00:00Z"),
        "updated_at": new Date("2024-12-14T00:00:00Z")
    },
    {
        "_id": "product_laptop_003",
        "shop_id": "shop_003",
        "category_id": "cat_laptop_001",
        "name": "Dell XPS 13 Plus i7-1360P 1TB",
        "description": "Dell XPS 13 Plus thiết kế siêu mỏng cao cấp, CPU Intel Core i7-1360P, RAM 16GB LPDDR5. Màn hình 13.4 inch FHD+, pin trâu 12 giờ.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555585/laptop3_byvi2s.jpg"],
        "price": 42990000,
        "sale_off": 7.0,
        "final_price": 39980700,
        "stock_quantity": 25,
        "attribute_values": [
            { "attribute_id": "attr_laptop_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "16GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_laptop_cpu_001", "attribute_key": "cpu", "attribute_name": "CPU", "value": "Intel Core i7-1360P", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_storage_001", "attribute_key": "storage", "attribute_name": "Ổ cứng", "value": "SSD 1TB NVMe", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_gpu_001", "attribute_key": "gpu", "attribute_name": "Card đồ họa", "value": "Intel Iris Xe Graphics", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "13.4 inch FHD+", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-15T00:00:00Z"),
        "updated_at": new Date("2024-12-15T00:00:00Z")
    },
    {
        "_id": "product_laptop_004",
        "shop_id": "shop_003",
        "category_id": "cat_laptop_001",
        "name": "HP Pavilion 15 Ryzen 7 7730U 512GB",
        "description": "HP Pavilion 15 với AMD Ryzen 7 7730U, RAM 16GB DDR4, màn hình 15.6 inch FHD. Laptop đa năng cho học tập và làm việc.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555585/laptop4_ix8gfr.avif"],
        "price": 16990000,
        "sale_off": 12.0,
        "final_price": 14951200,
        "stock_quantity": 40,
        "attribute_values": [
            { "attribute_id": "attr_laptop_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "16GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_laptop_cpu_001", "attribute_key": "cpu", "attribute_name": "CPU", "value": "AMD Ryzen 7 7730U", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_storage_001", "attribute_key": "storage", "attribute_name": "Ổ cứng", "value": "SSD 512GB NVMe", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_gpu_001", "attribute_key": "gpu", "attribute_name": "Card đồ họa", "value": "AMD Radeon Graphics", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "15.6 inch FHD", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-15T00:00:00Z"),
        "updated_at": new Date("2024-12-15T00:00:00Z")
    },
    {
        "_id": "product_laptop_005",
        "shop_id": "shop_003",
        "category_id": "cat_laptop_001",
        "name": "Lenovo ThinkPad X1 Carbon Gen 11 i7",
        "description": "ThinkPad X1 Carbon Gen 11 với Intel Core i7-1355U, RAM 32GB, SSD 1TB. Laptop doanh nhân cao cấp, bàn phím tốt nhất, bền bỉ.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555585/laptop5_nrfnwr.jpg"],
        "price": 48990000,
        "sale_off": 8.0,
        "final_price": 45070800,
        "stock_quantity": 18,
        "attribute_values": [
            { "attribute_id": "attr_laptop_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "32GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_laptop_cpu_001", "attribute_key": "cpu", "attribute_name": "CPU", "value": "Intel Core i7-1355U", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_storage_001", "attribute_key": "storage", "attribute_name": "Ổ cứng", "value": "SSD 1TB NVMe", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_gpu_001", "attribute_key": "gpu", "attribute_name": "Card đồ họa", "value": "Intel Iris Xe Graphics", "unit": "", "data_type": "TEXT" },
            { "attribute_id": "attr_laptop_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "14 inch WUXGA", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-16T00:00:00Z"),
        "updated_at": new Date("2024-12-16T00:00:00Z")
    },

    // ===== MÁY TÍNH BẢNG (4 sản phẩm) =====
    {
        "_id": "product_tablet_001",
        "shop_id": "shop_002",
        "category_id": "cat_tablet_001",
        "name": "iPad Pro 12.9 inch M2 WiFi 256GB",
        "description": "iPad Pro 12.9 inch với chip M2 mạnh mẽ, màn hình Liquid Retina XDR. Hỗ trợ Apple Pencil Gen 2 và Magic Keyboard, lý tưởng cho sáng tạo.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/maytinhbang1_a2b9sv.jpg"],
        "price": 32990000,
        "sale_off": 5.0,
        "final_price": 31340500,
        "stock_quantity": 30,
        "attribute_values": [
            { "attribute_id": "attr_tablet_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "12.9 inch Liquid Retina XDR", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-16T00:00:00Z"),
        "updated_at": new Date("2024-12-16T00:00:00Z")
    },
    {
        "_id": "product_tablet_002",
        "shop_id": "shop_002",
        "category_id": "cat_tablet_001",
        "name": "Samsung Galaxy Tab S9 Ultra 5G 512GB",
        "description": "Galaxy Tab S9 Ultra với màn hình AMOLED 14.6 inch khổng lồ, bút S-Pen đi kèm, Snapdragon 8 Gen 2. Hoàn hảo cho công việc và giải trí.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/maytinhbang2_nnclqx.jpg"],
        "price": 29990000,
        "sale_off": 10.0,
        "final_price": 26991000,
        "stock_quantity": 20,
        "attribute_values": [
            { "attribute_id": "attr_tablet_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "16GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ", "value": "512GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "14.6 inch Dynamic AMOLED 2X", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-17T00:00:00Z"),
        "updated_at": new Date("2024-12-17T00:00:00Z")
    },
    {
        "_id": "product_tablet_003",
        "shop_id": "shop_002",
        "category_id": "cat_tablet_001",
        "name": "iPad Air 11 inch M2 WiFi 128GB",
        "description": "iPad Air 11 inch với chip M2, hỗ trợ Apple Pencil Pro. Thiết kế mỏng nhẹ, màn hình Liquid Retina 11 inch sống động.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555570/maytinhbang3_tyagyx.jpg"],
        "price": 18990000,
        "sale_off": 8.0,
        "final_price": 17470800,
        "stock_quantity": 45,
        "attribute_values": [
            { "attribute_id": "attr_tablet_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ", "value": "128GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "11 inch Liquid Retina", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-17T00:00:00Z"),
        "updated_at": new Date("2024-12-17T00:00:00Z")
    },
    {
        "_id": "product_tablet_004",
        "shop_id": "shop_002",
        "category_id": "cat_tablet_001",
        "name": "Xiaomi Pad 6 Pro 256GB",
        "description": "Xiaomi Pad 6 Pro với Snapdragon 8+ Gen 1, màn hình 11 inch 144Hz, 4 loa Dolby Atmos. Giải trí đỉnh cao với giá cạnh tranh.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555569/maytinhbang4_h4an7q.jpg"],
        "price": 11990000,
        "sale_off": 12.0,
        "final_price": 10551200,
        "stock_quantity": 50,
        "attribute_values": [
            { "attribute_id": "attr_tablet_ram_001", "attribute_key": "ram", "attribute_name": "RAM", "value": "8GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_storage_001", "attribute_key": "storage", "attribute_name": "Bộ nhớ", "value": "256GB", "unit": "GB", "data_type": "SELECT" },
            { "attribute_id": "attr_tablet_screen_001", "attribute_key": "screen", "attribute_name": "Màn hình", "value": "11 inch LCD 144Hz", "unit": "inch", "data_type": "TEXT" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-18T00:00:00Z"),
        "updated_at": new Date("2024-12-18T00:00:00Z")
    },

    // ===== PHỤ KIỆN - SẠC (2 sản phẩm) =====
    {
        "_id": "product_charger_001",
        "shop_id": "shop_002",
        "category_id": "cat_accessories_001",
        "name": "Anker 747 GaNPrime Sạc Nhanh 150W",
        "description": "Sạc Anker 747 công nghệ GaN Prime, công suất 150W, 4 cổng (3 USB-C + 1 USB-A). Sạc nhanh đa thiết bị đồng thời.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555569/sac1_sutjpl.jpg"],
        "price": 2990000,
        "sale_off": 15.0,
        "final_price": 2541500,
        "stock_quantity": 80,
        "attribute_values": [
            { "attribute_id": "attr_charger_type_001", "attribute_key": "port_type", "attribute_name": "Loại cổng", "value": "USB-C", "unit": "", "data_type": "SELECT" },
            { "attribute_id": "attr_charger_wattage_001", "attribute_key": "wattage", "attribute_name": "Công suất", "value": "150", "unit": "W", "data_type": "NUMBER" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-18T00:00:00Z"),
        "updated_at": new Date("2024-12-18T00:00:00Z")
    },
    {
        "_id": "product_charger_002",
        "shop_id": "shop_002",
        "category_id": "cat_accessories_001",
        "name": "Apple 35W Dual USB-C Power Adapter",
        "description": "Củ sạc Apple chính hãng 35W, 2 cổng USB-C, thiết kế nhỏ gọn. Sạc nhanh iPhone, iPad, MacBook Air.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555569/sac2_mtzrz7.png"],
        "price": 1590000,
        "sale_off": 5.0,
        "final_price": 1510500,
        "stock_quantity": 100,
        "attribute_values": [
            { "attribute_id": "attr_charger_type_001", "attribute_key": "port_type", "attribute_name": "Loại cổng", "value": "USB-C", "unit": "", "data_type": "SELECT" },
            { "attribute_id": "attr_charger_wattage_001", "attribute_key": "wattage", "attribute_name": "Công suất", "value": "35", "unit": "W", "data_type": "NUMBER" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-19T00:00:00Z"),
        "updated_at": new Date("2024-12-19T00:00:00Z")
    },

    // ===== PHỤ KIỆN - TAI NGHE (3 sản phẩm) =====
    {
        "_id": "product_headphone_001",
        "shop_id": "shop_002",
        "category_id": "cat_accessories_001",
        "name": "AirPods Pro 2 USB-C",
        "description": "AirPods Pro 2 với chip H2, chống ồn chủ động ANC thế hệ mới, âm thanh Adaptive Audio. Hộp sạc USB-C, chống nước IP54.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555568/tainghe1_s3cyo5.jpg"],
        "price": 6490000,
        "sale_off": 10.0,
        "final_price": 5841000,
        "stock_quantity": 60,
        "attribute_values": [
            { "attribute_id": "attr_headphone_type_001", "attribute_key": "headphone_type", "attribute_name": "Loại tai nghe", "value": "True Wireless", "unit": "", "data_type": "SELECT" },
            { "attribute_id": "attr_headphone_anc_001", "attribute_key": "active_noise_canceling", "attribute_name": "Chống ồn", "value": "true", "unit": "", "data_type": "BOOLEAN" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-19T00:00:00Z"),
        "updated_at": new Date("2024-12-19T00:00:00Z")
    },
    {
        "_id": "product_headphone_002",
        "shop_id": "shop_002",
        "category_id": "cat_accessories_001",
        "name": "Sony WH-1000XM5 Wireless",
        "description": "Tai nghe Sony WH-1000XM5 chống ồn hàng đầu, âm thanh LDAC Hi-Res, pin 30 giờ. Thiết kế sang trọng, thoải mái cả ngày.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555568/tainghe2_b4faz9.jpg"],
        "price": 8990000,
        "sale_off": 12.0,
        "final_price": 7911200,
        "stock_quantity": 35,
        "attribute_values": [
            { "attribute_id": "attr_headphone_type_001", "attribute_key": "headphone_type", "attribute_name": "Loại tai nghe", "value": "Bluetooth", "unit": "", "data_type": "SELECT" },
            { "attribute_id": "attr_headphone_anc_001", "attribute_key": "active_noise_canceling", "attribute_name": "Chống ồn", "value": "true", "unit": "", "data_type": "BOOLEAN" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-20T00:00:00Z")
    },
    {
        "_id": "product_headphone_003",
        "shop_id": "shop_002",
        "category_id": "cat_accessories_001",
        "name": "Samsung Galaxy Buds2 Pro",
        "description": "Galaxy Buds2 Pro với ANC thông minh, âm thanh 360 độ, chống nước IPX7. Tích hợp hoàn hảo với thiết bị Samsung.",
        "images": ["https://res.cloudinary.com/dr8ez6ua8/image/upload/v1767555568/tainghe3_uhz5xq.jpg"],
        "price": 3990000,
        "sale_off": 18.0,
        "final_price": 3271800,
        "stock_quantity": 70,
        "attribute_values": [
            { "attribute_id": "attr_headphone_type_001", "attribute_key": "headphone_type", "attribute_name": "Loại tai nghe", "value": "True Wireless", "unit": "", "data_type": "SELECT" },
            { "attribute_id": "attr_headphone_anc_001", "attribute_key": "active_noise_canceling", "attribute_name": "Chống ồn", "value": "true", "unit": "", "data_type": "BOOLEAN" }
        ],
        "average_rating": 0.0,
        "review_count": 0,
        "purchase_count": 0,
        "is_published": true,
        "created_at": new Date("2024-12-20T00:00:00Z"),
        "updated_at": new Date("2024-12-20T00:00:00Z")
    }
]);
print("✓ Products inserted: 22");
