"""
Scrapy Items Definition
-----------------------
Dinh nghia cau truc du lieu cho san pham duoc crawl.
Theo Medallion Architecture: day la Bronze layer data structure.
"""

import scrapy


class ProductItem(scrapy.Item):
    """
    Item class cho san pham duoc crawl tu cac nguon khac nhau.
    Cac truong nay se duoc luu vao MongoDB raw_products collection.
    """

    # ID duy nhat tu nguon goc (vd: SKU cua trang web)
    external_id = scrapy.Field()

    # Ten san pham
    title = scrapy.Field()

    # Gia san pham (string, se duoc clean thanh float sau)
    price = scrapy.Field()

    # Gia goc (truoc khi giam gia)
    original_price = scrapy.Field()

    # Thuong hieu
    brand = scrapy.Field()

    # Danh muc san pham
    category = scrapy.Field()

    # Mo ta san pham
    description = scrapy.Field()

    # Danh sach URL hinh anh
    image_urls = scrapy.Field()

    # Diem danh gia trung binh
    rating = scrapy.Field()

    # So luong danh gia
    review_count = scrapy.Field()

    # Tinh trang ton kho
    stock = scrapy.Field()

    # Cac thong so ky thuat (dict)
    specifications = scrapy.Field()

    # Nguon crawl (ten trang web)
    source = scrapy.Field()

    # URL goc cua san pham
    source_url = scrapy.Field()

    # Thoi gian crawl (ISO format)
    crawl_time = scrapy.Field()
