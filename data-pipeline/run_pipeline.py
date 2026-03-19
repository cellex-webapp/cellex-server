#!/usr/bin/env python
"""
Data Pipeline Orchestrator
--------------------------
Chay toan bo data pipeline theo thu tu:
1. Crawl du lieu (Scrapy spider)
2. Lam sach du lieu (Bronze -> Silver)
3. Load vao PostgreSQL (Silver -> Gold)
4. Build features
5. Quality check

Medallion Architecture:
- Bronze: raw_products (MongoDB)
- Silver: products_clean (MongoDB)
- Gold: products table (PostgreSQL)

Usage:
    python run_pipeline.py                    # Chay full pipeline
    python run_pipeline.py --step crawl       # Chi chay crawler
    python run_pipeline.py --step clean       # Chi chay clean
    python run_pipeline.py --step load        # Chi chay load to postgres
    python run_pipeline.py --step features    # Chi chay build features
    python run_pipeline.py --step quality     # Chi chay quality check
    python run_pipeline.py --skip-crawl       # Chay full pipeline bo qua crawl
"""

import argparse
import os
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path

from dotenv import load_dotenv
from loguru import logger

# Load environment variables
project_root = Path(__file__).parent
load_dotenv(project_root / ".env")

# Configure loguru
log_dir = project_root / "logs"
log_dir.mkdir(exist_ok=True)
logger.add(
    log_dir / "pipeline_{time:YYYY-MM-DD}.log",
    rotation="1 day",
    retention="30 days",
    level="INFO",
)


class PipelineOrchestrator:
    """
    Orchestrator chay toan bo data pipeline.
    """

    def __init__(self, project_root: Path):
        """
        Khoi tao orchestrator.

        Args:
            project_root: Duong dan den thu muc goc cua project
        """
        self.project_root = project_root
        self.results = {}

    def run_crawler(self) -> bool:
        """
        Chay Scrapy spider de crawl du lieu.
        Luu du lieu vao MongoDB raw_products collection.

        Returns:
            True neu thanh cong, False neu that bai
        """
        logger.info("=" * 60)
        logger.info("STEP 1: Running Scrapy Crawler")
        logger.info("=" * 60)

        start_time = time.time()

        try:
            # Chay scrapy voi CrawlerProcess
            from scrapy.crawler import CrawlerProcess
            from scrapy.utils.project import get_project_settings

            # Add project root to path for imports
            sys.path.insert(0, str(self.project_root))

            # Set SCRAPY_SETTINGS_MODULE environment variable
            os.environ["SCRAPY_SETTINGS_MODULE"] = "scraper.crawler.settings"

            # Import spider
            from scraper.crawler.spiders.product_spider import ProductSpider

            # Get settings from the module
            settings = get_project_settings()

            # Override settings neu can
            settings.set("LOG_LEVEL", "INFO")

            # Create process with settings
            process = CrawlerProcess(settings)

            # Chay spider
            process.crawl(ProductSpider, mode="mock")
            process.start()

            duration = time.time() - start_time
            logger.info(f"Crawler completed in {duration:.2f} seconds")

            self.results["crawl"] = {
                "status": "success",
                "duration": duration,
            }
            return True

        except Exception as e:
            logger.error(f"Crawler failed: {e}")
            self.results["crawl"] = {
                "status": "failed",
                "error": str(e),
            }
            return False

    def run_clean(self) -> bool:
        """
        Chay clean_products.py de lam sach du lieu.
        Bronze -> Silver transformation.

        Returns:
            True neu thanh cong, False neu that bai
        """
        logger.info("=" * 60)
        logger.info("STEP 2: Running Clean Products (Bronze -> Silver)")
        logger.info("=" * 60)

        start_time = time.time()

        try:
            # Import va chay cleaner
            sys.path.insert(0, str(self.project_root))
            from etl.clean_products import ProductCleaner

            cleaner = ProductCleaner()
            stats = cleaner.run()

            duration = time.time() - start_time
            logger.info(f"Clean completed in {duration:.2f} seconds")

            self.results["clean"] = {
                "status": "success",
                "duration": duration,
                "stats": stats,
            }
            return stats["final_count"] > 0

        except Exception as e:
            logger.error(f"Clean failed: {e}")
            self.results["clean"] = {
                "status": "failed",
                "error": str(e),
            }
            return False

    def run_load(self) -> bool:
        """
        Chay load_postgres.py de load du lieu vao PostgreSQL.
        Silver -> Gold transformation.

        Returns:
            True neu thanh cong, False neu that bai
        """
        logger.info("=" * 60)
        logger.info("STEP 3: Running Load to PostgreSQL (Silver -> Gold)")
        logger.info("=" * 60)

        start_time = time.time()

        try:
            sys.path.insert(0, str(self.project_root))
            from etl.load_postgres import PostgresLoader

            loader = PostgresLoader()
            stats = loader.run()

            duration = time.time() - start_time
            logger.info(f"Load completed in {duration:.2f} seconds")

            self.results["load"] = {
                "status": "success",
                "duration": duration,
                "stats": stats,
            }
            return stats["success"] > 0

        except Exception as e:
            logger.error(f"Load failed: {e}")
            self.results["load"] = {
                "status": "failed",
                "error": str(e),
            }
            return False

    def run_features(self) -> bool:
        """
        Chay build_features.py de tao features.

        Returns:
            True neu thanh cong, False neu that bai
        """
        logger.info("=" * 60)
        logger.info("STEP 4: Running Build Features")
        logger.info("=" * 60)

        start_time = time.time()

        try:
            sys.path.insert(0, str(self.project_root))
            from etl.build_features import FeatureBuilder

            builder = FeatureBuilder()
            stats = builder.run(generate_mock_metrics=True)

            duration = time.time() - start_time
            logger.info(f"Features completed in {duration:.2f} seconds")

            self.results["features"] = {
                "status": "success",
                "duration": duration,
                "stats": stats,
            }
            return stats["product_features_saved"] > 0

        except Exception as e:
            logger.error(f"Features failed: {e}")
            self.results["features"] = {
                "status": "failed",
                "error": str(e),
            }
            return False

    def run_quality(self) -> bool:
        """
        Chay quality_check.py de kiem tra chat luong.

        Returns:
            True neu thanh cong, False neu that bai
        """
        logger.info("=" * 60)
        logger.info("STEP 5: Running Quality Check")
        logger.info("=" * 60)

        start_time = time.time()

        try:
            sys.path.insert(0, str(self.project_root))
            from etl.quality_check import QualityChecker, QualityThresholds

            thresholds = QualityThresholds(
                max_null_percentage=10.0,
                max_duplicate_percentage=5.0,
                max_invalid_price_percentage=5.0,
                min_record_count=1,
            )

            checker = QualityChecker(thresholds=thresholds)
            results = checker.run(raise_on_failure=False)

            duration = time.time() - start_time
            logger.info(f"Quality check completed in {duration:.2f} seconds")

            self.results["quality"] = {
                "status": "success" if results["overall_passed"] else "warning",
                "duration": duration,
                "passed": results["overall_passed"],
                "issues": results["all_issues"],
            }
            return True  # Khong fail pipeline neu quality check co warnings

        except Exception as e:
            logger.error(f"Quality check failed: {e}")
            self.results["quality"] = {
                "status": "failed",
                "error": str(e),
            }
            return False

    def run_full_pipeline(self, skip_crawl: bool = False) -> bool:
        """
        Chay toan bo pipeline theo thu tu.

        Args:
            skip_crawl: Bo qua buoc crawl

        Returns:
            True neu toan bo pipeline thanh cong
        """
        logger.info("*" * 60)
        logger.info("STARTING FULL DATA PIPELINE")
        logger.info(f"Timestamp: {datetime.now().isoformat()}")
        logger.info("*" * 60)

        pipeline_start = time.time()
        all_success = True

        # Step 1: Crawl
        if not skip_crawl:
            if not self.run_crawler():
                logger.error("Pipeline stopped at CRAWL step")
                all_success = False
        else:
            logger.info("Skipping CRAWL step as requested")
            self.results["crawl"] = {"status": "skipped"}

        # Step 2: Clean (tiep tuc ngay ca khi crawl fail de clean du lieu cu)
        if not self.run_clean():
            logger.error("Pipeline stopped at CLEAN step")
            all_success = False

        # Step 3: Load to PostgreSQL
        if all_success or self.results.get("clean", {}).get("status") == "success":
            if not self.run_load():
                logger.error("Pipeline stopped at LOAD step")
                all_success = False

        # Step 4: Build Features
        if all_success or self.results.get("load", {}).get("status") == "success":
            if not self.run_features():
                logger.error("Pipeline stopped at FEATURES step")
                all_success = False

        # Step 5: Quality Check (luon chay)
        self.run_quality()

        # Summary
        total_duration = time.time() - pipeline_start
        self._print_summary(total_duration, all_success)

        return all_success

    def run_single_step(self, step: str) -> bool:
        """
        Chay mot buoc cu the.

        Args:
            step: Ten buoc can chay ('crawl', 'clean', 'load', 'features', 'quality')

        Returns:
            True neu thanh cong
        """
        step_map = {
            "crawl": self.run_crawler,
            "clean": self.run_clean,
            "load": self.run_load,
            "features": self.run_features,
            "quality": self.run_quality,
        }

        if step not in step_map:
            logger.error(f"Unknown step: {step}")
            logger.info(f"Available steps: {list(step_map.keys())}")
            return False

        start_time = time.time()
        result = step_map[step]()
        duration = time.time() - start_time

        self._print_summary(duration, result)
        return result

    def _print_summary(self, total_duration: float, success: bool) -> None:
        """In tong ket pipeline."""
        logger.info("*" * 60)
        logger.info("PIPELINE SUMMARY")
        logger.info("*" * 60)

        for step, result in self.results.items():
            status = result.get("status", "unknown")
            duration = result.get("duration", 0)

            status_icon = "✓" if status == "success" else "✗" if status == "failed" else "⚠" if status == "warning" else "○"
            logger.info(f"  {status_icon} {step.upper()}: {status} ({duration:.2f}s)")

            if result.get("stats"):
                for key, value in result["stats"].items():
                    logger.info(f"      - {key}: {value}")

            if result.get("issues"):
                for issue in result["issues"]:
                    logger.warning(f"      ! {issue}")

        logger.info("-" * 60)
        logger.info(f"Total Duration: {total_duration:.2f} seconds")
        logger.info(f"Overall Status: {'SUCCESS' if success else 'FAILED'}")
        logger.info("*" * 60)


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="Data Pipeline Orchestrator",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  python run_pipeline.py                    # Run full pipeline
  python run_pipeline.py --step crawl       # Run only crawler
  python run_pipeline.py --step clean       # Run only clean
  python run_pipeline.py --step load        # Run only load to postgres
  python run_pipeline.py --step features    # Run only build features
  python run_pipeline.py --step quality     # Run only quality check
  python run_pipeline.py --skip-crawl       # Run full pipeline, skip crawl
        """,
    )

    parser.add_argument(
        "--step",
        type=str,
        choices=["crawl", "clean", "load", "features", "quality"],
        help="Run a specific step only",
    )

    parser.add_argument(
        "--skip-crawl",
        action="store_true",
        help="Skip the crawler step when running full pipeline",
    )

    args = parser.parse_args()

    # Initialize orchestrator
    project_root = Path(__file__).parent
    orchestrator = PipelineOrchestrator(project_root)

    # Run pipeline
    if args.step:
        success = orchestrator.run_single_step(args.step)
    else:
        success = orchestrator.run_full_pipeline(skip_crawl=args.skip_crawl)

    # Exit with appropriate code
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
