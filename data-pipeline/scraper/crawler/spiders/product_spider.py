"""
Product Spider
--------------
Spider crawl san pham tu nhieu nguon khac nhau.
Day la Bronze layer - thu thap raw data.

Su dung:
    scrapy crawl product_spider

Hoac tu run_pipeline.py
"""

import json
from datetime import datetime, timezone
from typing import Generator, Any

import scrapy
from scrapy.http import Response

from scraper.crawler.items import ProductItem


class ProductSpider(scrapy.Spider):
    """
    Spider crawl san pham cong nghe tu nhieu nguon.
    Hien tai ho tro:
    - Mock data (demo)
    - Co the mo rong them cac nguon khac
    """

    name = "product_spider"
    allowed_domains = []  # Cho phep tat ca domain trong demo mode

    # Custom settings cho spider nay
    custom_settings = {
        "DOWNLOAD_DELAY": 1.0,
        "CONCURRENT_REQUESTS_PER_DOMAIN": 2,
    }

    def __init__(self, mode: str = "mock", *args, **kwargs):
        """
        Khoi tao spider.

        Args:
            mode: Che do crawl ('mock' cho demo data, 'live' cho real crawling)
        """
        super().__init__(*args, **kwargs)
        self.mode = mode
        self.logger.info(f"ProductSpider initialized in '{mode}' mode")

    def start_requests(self) -> Generator[scrapy.Request, None, None]:
        """
        Tao cac request ban dau.
        Trong mock mode, tao request den mot dummy URL de trigger spider.
        """
        if self.mode == "mock":
            # Mock mode: su dung dummy request de trigger callback
            # Callback se tra ve mock data
            yield scrapy.Request(
                url="data:text/html,<html></html>",
                callback=self.parse_mock_data,
                dont_filter=True,
            )
        else:
            # Live mode: co the them real URLs o day
            # Vi du: Crawl tu trang demo hoac test site
            demo_urls = [
                # Them URLs thuc te o day khi can
            ]
            for url in demo_urls:
                yield scrapy.Request(url=url, callback=self.parse)

    def parse_mock_data(self, response: Response) -> Generator[ProductItem, None, None]:
        """
        Generate mock product data cho demo va testing.
        Du lieu mo phong san pham cong nghe thuc te.

        Args:
            response: Scrapy response (khong dung trong mock mode)

        Yields:
            ProductItem: Cac san pham mock
        """
        self.logger.info("Generating mock product data...")

        # Mock data - san pham dien thoai, laptop, phu kien
        mock_products = [
            # === DIEN THOAI ===
            {
                "external_id": "IP15PM-256-BLK",
                "title": "iPhone 15 Pro Max 256GB - Black Titanium",
                "price": 34990000,
                "original_price": 36990000,
                "brand": "Apple",
                "category": "Điện thoại",
                "description": "iPhone 15 Pro Max với chip A17 Pro, camera 48MP, Dynamic Island",
                "image_urls": ["https://example.com/iphone15promax-black.jpg"],
                "rating": 4.8,
                "review_count": 1250,
                "stock": 50,
                "specifications": {
                    "screen": "6.7 inch Super Retina XDR",
                    "chip": "A17 Pro",
                    "ram": "8GB",
                    "storage": "256GB",
                    "battery": "4422mAh",
                },
            },
            {
                "external_id": "IP15PM-512-NAT",
                "title": "iPhone 15 Pro Max 512GB - Natural Titanium",
                "price": 41990000,
                "original_price": 43990000,
                "brand": "Apple",
                "category": "Điện thoại",
                "description": "iPhone 15 Pro Max 512GB Natural Titanium, camera chuyên nghiệp",
                "image_urls": ["https://example.com/iphone15promax-natural.jpg"],
                "rating": 4.9,
                "review_count": 890,
                "stock": 30,
                "specifications": {
                    "screen": "6.7 inch Super Retina XDR",
                    "chip": "A17 Pro",
                    "ram": "8GB",
                    "storage": "512GB",
                },
            },
            {
                "external_id": "SS-S24U-256-BLK",
                "title": "Samsung Galaxy S24 Ultra 256GB - Titanium Black",
                "price": 29990000,
                "original_price": 33990000,
                "brand": "Samsung",
                "category": "Điện thoại",
                "description": "Galaxy S24 Ultra với AI Galaxy, S Pen tích hợp, camera 200MP",
                "image_urls": ["https://example.com/s24ultra-black.jpg"],
                "rating": 4.7,
                "review_count": 2100,
                "stock": 75,
                "specifications": {
                    "screen": "6.8 inch Dynamic AMOLED 2X",
                    "chip": "Snapdragon 8 Gen 3",
                    "ram": "12GB",
                    "storage": "256GB",
                },
            },
            {
                "external_id": "XM-14U-512-BLK",
                "title": "Xiaomi 14 Ultra 512GB - Black",
                "price": 23990000,
                "original_price": 26990000,
                "brand": "Xiaomi",
                "category": "Điện thoại",
                "description": "Xiaomi 14 Ultra với camera Leica, chip Snapdragon 8 Gen 3",
                "image_urls": ["https://example.com/xiaomi14ultra-black.jpg"],
                "rating": 4.5,
                "review_count": 450,
                "stock": 40,
                "specifications": {
                    "screen": "6.73 inch LTPO AMOLED",
                    "chip": "Snapdragon 8 Gen 3",
                    "ram": "16GB",
                    "storage": "512GB",
                },
            },
            # === LAPTOP ===
            {
                "external_id": "MBP-M3P-14-512",
                "title": "MacBook Pro 14 inch M3 Pro 512GB",
                "price": 49990000,
                "original_price": 52990000,
                "brand": "Apple",
                "category": "Laptop",
                "description": "MacBook Pro 14 inch với chip M3 Pro, màn hình Liquid Retina XDR",
                "image_urls": ["https://example.com/macbookpro14-m3pro.jpg"],
                "rating": 4.9,
                "review_count": 680,
                "stock": 25,
                "specifications": {
                    "screen": "14.2 inch Liquid Retina XDR",
                    "chip": "Apple M3 Pro",
                    "ram": "18GB",
                    "storage": "512GB SSD",
                    "battery": "Up to 17 hours",
                },
            },
            {
                "external_id": "DELL-XPS15-I7-16",
                "title": "Dell XPS 15 Intel Core i7, 16GB RAM, 512GB SSD",
                "price": 39990000,
                "original_price": 42990000,
                "brand": "Dell",
                "category": "Laptop",
                "description": "Dell XPS 15 với màn hình OLED 3.5K, thiết kế siêu mỏng",
                "image_urls": ["https://example.com/dell-xps15.jpg"],
                "rating": 4.6,
                "review_count": 320,
                "stock": 15,
                "specifications": {
                    "screen": "15.6 inch OLED 3.5K",
                    "cpu": "Intel Core i7-13700H",
                    "ram": "16GB DDR5",
                    "storage": "512GB NVMe SSD",
                },
            },
            {
                "external_id": "ASUS-ROG-G16-RTX4070",
                "title": "ASUS ROG Strix G16 RTX 4070 Gaming Laptop",
                "price": 45990000,
                "original_price": 48990000,
                "brand": "ASUS",
                "category": "Laptop",
                "description": "Laptop gaming ASUS ROG Strix G16 với RTX 4070, màn hình 240Hz",
                "image_urls": ["https://example.com/asus-rog-g16.jpg"],
                "rating": 4.7,
                "review_count": 185,
                "stock": 20,
                "specifications": {
                    "screen": "16 inch QHD+ 240Hz",
                    "cpu": "Intel Core i9-13980HX",
                    "gpu": "NVIDIA RTX 4070",
                    "ram": "32GB DDR5",
                    "storage": "1TB NVMe SSD",
                },
            },
            # === TAI NGHE ===
            {
                "external_id": "APP-MAX-SLV",
                "title": "AirPods Max - Silver",
                "price": 12990000,
                "original_price": 14990000,
                "brand": "Apple",
                "category": "Tai nghe",
                "description": "Tai nghe over-ear cao cấp với Active Noise Cancellation",
                "image_urls": ["https://example.com/airpods-max-silver.jpg"],
                "rating": 4.6,
                "review_count": 890,
                "stock": 60,
                "specifications": {
                    "type": "Over-ear",
                    "anc": "Yes",
                    "battery": "20 hours",
                    "connectivity": "Bluetooth 5.0",
                },
            },
            {
                "external_id": "SONY-WH1000XM5-BLK",
                "title": "Sony WH-1000XM5 Wireless Noise Cancelling Headphones",
                "price": 7990000,
                "original_price": 8990000,
                "brand": "Sony",
                "category": "Tai nghe",
                "description": "Tai nghe không dây cao cấp với ANC hàng đầu thế giới",
                "image_urls": ["https://example.com/sony-wh1000xm5.jpg"],
                "rating": 4.8,
                "review_count": 2350,
                "stock": 100,
                "specifications": {
                    "type": "Over-ear",
                    "anc": "Industry-leading",
                    "battery": "30 hours",
                    "weight": "250g",
                },
            },
            # === DONG HO THONG MINH ===
            {
                "external_id": "AW-S9-45-GPS-MN",
                "title": "Apple Watch Series 9 45mm GPS - Midnight",
                "price": 11990000,
                "original_price": 12990000,
                "brand": "Apple",
                "category": "Đồng hồ thông minh",
                "description": "Apple Watch Series 9 với chip S9, Double Tap gesture",
                "image_urls": ["https://example.com/apple-watch-s9-midnight.jpg"],
                "rating": 4.7,
                "review_count": 560,
                "stock": 45,
                "specifications": {
                    "size": "45mm",
                    "gps": "Yes",
                    "chip": "S9 SiP",
                    "display": "Always-On Retina",
                },
            },
            {
                "external_id": "SS-GW6-44-BLK",
                "title": "Samsung Galaxy Watch 6 Classic 44mm - Black",
                "price": 8990000,
                "original_price": 10990000,
                "brand": "Samsung",
                "category": "Đồng hồ thông minh",
                "description": "Galaxy Watch 6 Classic với rotating bezel, theo dõi sức khỏe",
                "image_urls": ["https://example.com/galaxy-watch6-classic.jpg"],
                "rating": 4.5,
                "review_count": 420,
                "stock": 35,
                "specifications": {
                    "size": "44mm",
                    "bezel": "Rotating",
                    "os": "Wear OS",
                    "battery": "590mAh",
                },
            },
            # === PHU KIEN ===
            {
                "external_id": "ANKER-735-65W",
                "title": "Anker 735 GaNPrime 65W Charger",
                "price": 1290000,
                "original_price": 1490000,
                "brand": "Anker",
                "category": "Phụ kiện",
                "description": "Sạc nhanh 65W GaN với 3 cổng, hỗ trợ PPS",
                "image_urls": ["https://example.com/anker-735-charger.jpg"],
                "rating": 4.8,
                "review_count": 1850,
                "stock": 200,
                "specifications": {
                    "output": "65W max",
                    "ports": "2x USB-C, 1x USB-A",
                    "technology": "GaN Prime",
                },
            },
            {
                "external_id": "MAG-BAT-5K-WHT",
                "title": "MagSafe Battery Pack 5000mAh - White",
                "price": 2490000,
                "original_price": 2790000,
                "brand": "Apple",
                "category": "Phụ kiện",
                "description": "Pin dự phòng MagSafe chính hãng cho iPhone",
                "image_urls": ["https://example.com/magsafe-battery.jpg"],
                "rating": 4.3,
                "review_count": 620,
                "stock": 80,
                "specifications": {
                    "capacity": "5000mAh",
                    "magsafe": "Yes",
                    "compatible": "iPhone 12 and later",
                },
            },
            # === CAMERA ===
            {
                "external_id": "GOPRO-H12-BLK",
                "title": "GoPro HERO12 Black",
                "price": 12990000,
                "original_price": 13990000,
                "brand": "GoPro",
                "category": "Camera",
                "description": "Action camera 5.3K60, HyperSmooth 6.0, chống nước 10m",
                "image_urls": ["https://example.com/gopro-hero12.jpg"],
                "rating": 4.6,
                "review_count": 780,
                "stock": 55,
                "specifications": {
                    "video": "5.3K60, 4K120",
                    "photo": "27MP",
                    "stabilization": "HyperSmooth 6.0",
                    "waterproof": "10m",
                },
            },
            {
                "external_id": "DJI-OM6-GRY",
                "title": "DJI Osmo Mobile 6 - Slate Gray",
                "price": 3590000,
                "original_price": 3990000,
                "brand": "DJI",
                "category": "Phụ kiện",
                "description": "Gimbal smartphone 3 trục với ActiveTrack 5.0",
                "image_urls": ["https://example.com/dji-osmo-mobile6.jpg"],
                "rating": 4.7,
                "review_count": 340,
                "stock": 40,
                "specifications": {
                    "axis": "3-axis",
                    "tracking": "ActiveTrack 5.0",
                    "battery": "6.4 hours",
                    "weight": "309g",
                },
            },
        ]

        # Yield tung san pham
        crawl_time = datetime.now(timezone.utc).isoformat()

        for product in mock_products:
            item = ProductItem()

            # Set tat ca cac truong
            item["external_id"] = product["external_id"]
            item["title"] = product["title"]
            item["price"] = product["price"]
            item["original_price"] = product.get("original_price")
            item["brand"] = product.get("brand")
            item["category"] = product.get("category")
            item["description"] = product.get("description")
            item["image_urls"] = product.get("image_urls", [])
            item["rating"] = product.get("rating")
            item["review_count"] = product.get("review_count")
            item["stock"] = product.get("stock")
            item["specifications"] = product.get("specifications", {})
            item["source"] = "mock_demo"
            item["source_url"] = f"https://demo.cellex.com/products/{product['external_id']}"
            item["crawl_time"] = crawl_time

            self.logger.info(f"Yielding product: {product['title']}")
            yield item

        self.logger.info(f"Mock data generation complete. Total: {len(mock_products)} products")

    def parse(self, response: Response) -> Generator[ProductItem, None, None]:
        """
        Parse response tu real website.
        Day la template - can customize cho tung nguon cu the.

        Args:
            response: Scrapy response

        Yields:
            ProductItem: San pham da extract
        """
        # Template cho real crawling
        # Can customize selectors cho tung trang web cu the
        self.logger.info(f"Parsing: {response.url}")

        # Vi du: Extract product cards tu trang listing
        # products = response.css('div.product-card')
        # for product in products:
        #     item = ProductItem()
        #     item['title'] = product.css('h2.title::text').get()
        #     item['price'] = product.css('span.price::text').get()
        #     ...
        #     yield item

        pass
