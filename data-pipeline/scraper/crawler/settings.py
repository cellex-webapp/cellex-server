"""
Scrapy Settings
---------------
Cau hinh cho Scrapy crawler.
Tat ca cac thong so ket noi database duoc doc tu .env file.
"""

import os
import sys
from pathlib import Path

from dotenv import load_dotenv

# Load environment variables tu .env file
# Tim .env file o thu muc root cua project
project_root = Path(__file__).parent.parent.parent
env_path = project_root / ".env"
load_dotenv(env_path)

# Ten cua Scrapy project
BOT_NAME = "crawler"

# Module chua spiders
SPIDER_MODULES = ["scraper.crawler.spiders"]
NEWSPIDER_MODULE = "scraper.crawler.spiders"

# Crawl responsibly - xac dinh user agent
USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

# Tuan thu robots.txt
ROBOTSTXT_OBEY = True

# Cau hinh so luong concurrent requests
CONCURRENT_REQUESTS = 8

# Delay giua cac request (tranh bi block)
DOWNLOAD_DELAY = 1.5

# So request toi da cho moi domain
CONCURRENT_REQUESTS_PER_DOMAIN = 4

# Disable cookies (giam load)
COOKIES_ENABLED = False

# Cau hinh retry
RETRY_ENABLED = True
RETRY_TIMES = 3
RETRY_HTTP_CODES = [500, 502, 503, 504, 408, 429]

# Timeout cho download
DOWNLOAD_TIMEOUT = 30

# Bat pipeline de xu ly items
ITEM_PIPELINES = {
    "scraper.crawler.pipelines.ValidatePipeline": 100,
    "scraper.crawler.pipelines.MongoPipeline": 300,
}

# MongoDB Configuration - doc tu environment variables
MONGO_URI = os.getenv("MONGO_URI")
MONGO_DATABASE = os.getenv("MONGO_DB_NAME", "cellex_prod")
MONGO_COLLECTION = os.getenv("MONGO_RAW_COLLECTION", "raw_products")

# Logging configuration
LOG_LEVEL = "INFO"
LOG_FORMAT = "%(asctime)s [%(name)s] %(levelname)s: %(message)s"

# Cau hinh output format
FEED_EXPORT_ENCODING = "utf-8"

# Request fingerprinting (Scrapy 2.7+)
REQUEST_FINGERPRINTER_IMPLEMENTATION = "2.7"

# Twisted reactor (Windows compatible)
TWISTED_REACTOR = "twisted.internet.asyncioreactor.AsyncioSelectorReactor"

# Feed exports backup (optional)
# FEEDS = {
#     'output/products_%(time)s.json': {
#         'format': 'json',
#         'encoding': 'utf8',
#     },
# }
