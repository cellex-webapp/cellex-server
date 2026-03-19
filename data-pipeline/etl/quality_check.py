"""
Quality Check ETL
-----------------
Kiem tra chat luong du lieu trong pipeline.

Module nay thuc hien:
- Validate du lieu trong MongoDB (Silver layer)
- Validate du lieu trong PostgreSQL (Gold layer)
- Kiem tra:
  - Ty le null
  - Ty le duplicate
  - Gia tri khong hop le (price <= 0)
- Raise exception neu vuot nguong cho phep

Usage:
    python -m etl.quality_check
"""

import os
import sys
from dataclasses import dataclass, field
from datetime import datetime
from pathlib import Path
from typing import Any

import psycopg
from psycopg.rows import dict_row
from dotenv import load_dotenv
from loguru import logger
from pymongo import MongoClient
from pymongo.errors import ConnectionFailure

# Load environment variables
project_root = Path(__file__).parent.parent
load_dotenv(project_root / ".env")

# Configure loguru
log_dir = project_root / "logs"
log_dir.mkdir(exist_ok=True)
logger.add(
    log_dir / "quality_check_{time:YYYY-MM-DD}.log",
    rotation="1 day",
    retention="7 days",
    level="INFO",
)


@dataclass
class QualityThresholds:
    """Nguong chat luong cho phep."""

    # Ty le null toi da (%)
    max_null_percentage: float = 10.0

    # Ty le duplicate toi da (%)
    max_duplicate_percentage: float = 5.0

    # Ty le gia khong hop le toi da (%)
    max_invalid_price_percentage: float = 5.0

    # So luong record toi thieu
    min_record_count: int = 1


@dataclass
class QualityReport:
    """Bao cao ket qua kiem tra chat luong."""

    source: str
    timestamp: str = field(default_factory=lambda: datetime.now().isoformat())
    total_records: int = 0
    null_checks: dict = field(default_factory=dict)
    duplicate_count: int = 0
    duplicate_percentage: float = 0.0
    invalid_price_count: int = 0
    invalid_price_percentage: float = 0.0
    issues: list = field(default_factory=list)
    passed: bool = True

    def add_issue(self, issue: str) -> None:
        """Them issue vao report."""
        self.issues.append(issue)
        self.passed = False

    def to_dict(self) -> dict:
        """Chuyen report thanh dict."""
        return {
            "source": self.source,
            "timestamp": self.timestamp,
            "total_records": self.total_records,
            "null_checks": self.null_checks,
            "duplicate_count": self.duplicate_count,
            "duplicate_percentage": self.duplicate_percentage,
            "invalid_price_count": self.invalid_price_count,
            "invalid_price_percentage": self.invalid_price_percentage,
            "issues": self.issues,
            "passed": self.passed,
        }


class QualityChecker:
    """
    Class kiem tra chat luong du lieu.
    Validate ca MongoDB (Silver) va PostgreSQL (Gold).
    """

    def __init__(self, thresholds: QualityThresholds | None = None):
        """
        Khoi tao QualityChecker.

        Args:
            thresholds: Nguong chat luong tuy chinh (optional)
        """
        # MongoDB config
        self.mongo_uri = os.getenv("MONGO_URI")
        self.mongo_db = os.getenv("MONGO_DB_NAME", "cellex_prod")
        self.clean_collection = os.getenv("MONGO_CLEAN_COLLECTION", "products_clean")

        # PostgreSQL config
        self.postgres_uri = os.getenv("POSTGRES_URI")

        # Thresholds
        self.thresholds = thresholds or QualityThresholds()

        self.mongo_client = None
        self.pg_conn = None

    def connect_mongo(self) -> None:
        """Ket noi den MongoDB."""
        logger.info("Connecting to MongoDB...")

        try:
            self.mongo_client = MongoClient(
                self.mongo_uri,
                serverSelectionTimeoutMS=10000,
            )
            self.mongo_client.admin.command("ping")
            logger.info("MongoDB connection successful")
        except ConnectionFailure as e:
            logger.error(f"Failed to connect to MongoDB: {e}")
            raise

    def connect_postgres(self) -> None:
        """Ket noi den PostgreSQL."""
        logger.info("Connecting to PostgreSQL...")

        try:
            self.pg_conn = psycopg.connect(
                self.postgres_uri,
                autocommit=True,
                row_factory=dict_row,
            )
            logger.info("PostgreSQL connection successful")
        except psycopg.Error as e:
            logger.error(f"Failed to connect to PostgreSQL: {e}")
            raise

    def close(self) -> None:
        """Dong tat ca ket noi."""
        if self.mongo_client:
            self.mongo_client.close()
        if self.pg_conn:
            self.pg_conn.close()
        logger.info("All connections closed")

    def check_mongo_quality(self) -> QualityReport:
        """
        Kiem tra chat luong du lieu trong MongoDB (Silver layer).

        Returns:
            QualityReport cho MongoDB
        """
        logger.info("=" * 50)
        logger.info("Checking MongoDB Silver layer quality")
        logger.info("=" * 50)

        report = QualityReport(source="MongoDB - products_clean")

        db = self.mongo_client[self.mongo_db]
        collection = db[self.clean_collection]

        # Total records
        total = collection.count_documents({})
        report.total_records = total

        if total == 0:
            report.add_issue("No records found in MongoDB Silver layer")
            return report

        if total < self.thresholds.min_record_count:
            report.add_issue(
                f"Record count ({total}) below minimum ({self.thresholds.min_record_count})"
            )

        # Check null percentages for key fields
        key_fields = ["title", "price", "external_id", "source", "category"]

        for field_name in key_fields:
            null_count = collection.count_documents(
                {"$or": [{field_name: None}, {field_name: {"$exists": False}}, {field_name: ""}]}
            )
            null_pct = (null_count / total * 100) if total > 0 else 0

            report.null_checks[field_name] = {
                "null_count": null_count,
                "null_percentage": round(null_pct, 2),
            }

            if null_pct > self.thresholds.max_null_percentage:
                report.add_issue(
                    f"Field '{field_name}' has {null_pct:.2f}% null values "
                    f"(threshold: {self.thresholds.max_null_percentage}%)"
                )

        # Check duplicates (external_id + source)
        pipeline = [
            {"$group": {"_id": {"external_id": "$external_id", "source": "$source"}, "count": {"$sum": 1}}},
            {"$match": {"count": {"$gt": 1}}},
            {"$count": "duplicate_groups"},
        ]
        dup_result = list(collection.aggregate(pipeline))
        dup_groups = dup_result[0]["duplicate_groups"] if dup_result else 0

        # Count total duplicate records
        dup_pipeline = [
            {"$group": {"_id": {"external_id": "$external_id", "source": "$source"}, "count": {"$sum": 1}}},
            {"$match": {"count": {"$gt": 1}}},
            {"$group": {"_id": None, "total_dups": {"$sum": {"$subtract": ["$count", 1]}}}},
        ]
        dup_count_result = list(collection.aggregate(dup_pipeline))
        dup_count = dup_count_result[0]["total_dups"] if dup_count_result else 0

        report.duplicate_count = dup_count
        report.duplicate_percentage = round((dup_count / total * 100) if total > 0 else 0, 2)

        if report.duplicate_percentage > self.thresholds.max_duplicate_percentage:
            report.add_issue(
                f"Duplicate percentage ({report.duplicate_percentage}%) exceeds "
                f"threshold ({self.thresholds.max_duplicate_percentage}%)"
            )

        # Check invalid prices
        invalid_price_count = collection.count_documents(
            {"$or": [{"price": {"$lte": 0}}, {"price": None}]}
        )
        report.invalid_price_count = invalid_price_count
        report.invalid_price_percentage = round(
            (invalid_price_count / total * 100) if total > 0 else 0, 2
        )

        if report.invalid_price_percentage > self.thresholds.max_invalid_price_percentage:
            report.add_issue(
                f"Invalid price percentage ({report.invalid_price_percentage}%) exceeds "
                f"threshold ({self.thresholds.max_invalid_price_percentage}%)"
            )

        self._log_report(report)
        return report

    def check_postgres_quality(self) -> QualityReport:
        """
        Kiem tra chat luong du lieu trong PostgreSQL (Gold layer).

        Returns:
            QualityReport cho PostgreSQL
        """
        logger.info("=" * 50)
        logger.info("Checking PostgreSQL Gold layer quality")
        logger.info("=" * 50)

        report = QualityReport(source="PostgreSQL - products")

        with self.pg_conn.cursor() as cur:
            # Total records
            cur.execute("SELECT COUNT(*) as count FROM products")
            result = cur.fetchone()
            total = result["count"] if result else 0
            report.total_records = total

            if total == 0:
                report.add_issue("No records found in PostgreSQL Gold layer")
                return report

            if total < self.thresholds.min_record_count:
                report.add_issue(
                    f"Record count ({total}) below minimum ({self.thresholds.min_record_count})"
                )

            # Check null percentages
            key_fields = ["title", "price", "external_id", "source", "category"]

            for field_name in key_fields:
                cur.execute(
                    f"""
                    SELECT COUNT(*) as count
                    FROM products
                    WHERE {field_name} IS NULL OR {field_name}::text = ''
                    """
                )
                result = cur.fetchone()
                null_count = result["count"] if result else 0
                null_pct = (null_count / total * 100) if total > 0 else 0

                report.null_checks[field_name] = {
                    "null_count": null_count,
                    "null_percentage": round(null_pct, 2),
                }

                if null_pct > self.thresholds.max_null_percentage:
                    report.add_issue(
                        f"Field '{field_name}' has {null_pct:.2f}% null values "
                        f"(threshold: {self.thresholds.max_null_percentage}%)"
                    )

            # Check duplicates
            cur.execute(
                """
                SELECT COUNT(*) as dup_count
                FROM (
                    SELECT external_id, source, COUNT(*) as cnt
                    FROM products
                    GROUP BY external_id, source
                    HAVING COUNT(*) > 1
                ) as dups
                """
            )
            result = cur.fetchone()
            dup_groups = result["dup_count"] if result else 0

            # Count duplicate records
            cur.execute(
                """
                SELECT COALESCE(SUM(cnt - 1), 0) as total_dups
                FROM (
                    SELECT external_id, source, COUNT(*) as cnt
                    FROM products
                    GROUP BY external_id, source
                    HAVING COUNT(*) > 1
                ) as dups
                """
            )
            result = cur.fetchone()
            dup_count = int(result["total_dups"]) if result else 0

            report.duplicate_count = dup_count
            report.duplicate_percentage = round(
                (dup_count / total * 100) if total > 0 else 0, 2
            )

            if report.duplicate_percentage > self.thresholds.max_duplicate_percentage:
                report.add_issue(
                    f"Duplicate percentage ({report.duplicate_percentage}%) exceeds "
                    f"threshold ({self.thresholds.max_duplicate_percentage}%)"
                )

            # Check invalid prices
            cur.execute(
                """
                SELECT COUNT(*) as count
                FROM products
                WHERE price IS NULL OR price <= 0
                """
            )
            result = cur.fetchone()
            invalid_price_count = result["count"] if result else 0

            report.invalid_price_count = invalid_price_count
            report.invalid_price_percentage = round(
                (invalid_price_count / total * 100) if total > 0 else 0, 2
            )

            if report.invalid_price_percentage > self.thresholds.max_invalid_price_percentage:
                report.add_issue(
                    f"Invalid price percentage ({report.invalid_price_percentage}%) exceeds "
                    f"threshold ({self.thresholds.max_invalid_price_percentage}%)"
                )

        self._log_report(report)
        return report

    def check_data_consistency(self) -> dict[str, Any]:
        """
        Kiem tra tinh nhat quan giua MongoDB va PostgreSQL.

        Returns:
            Dict chua ket qua kiem tra consistency
        """
        logger.info("=" * 50)
        logger.info("Checking data consistency between MongoDB and PostgreSQL")
        logger.info("=" * 50)

        result = {
            "mongo_count": 0,
            "postgres_count": 0,
            "difference": 0,
            "consistency_percentage": 0,
            "issues": [],
        }

        # MongoDB count
        db = self.mongo_client[self.mongo_db]
        collection = db[self.clean_collection]
        result["mongo_count"] = collection.count_documents({})

        # PostgreSQL count
        with self.pg_conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) as count FROM products")
            pg_result = cur.fetchone()
            result["postgres_count"] = pg_result["count"] if pg_result else 0

        # Calculate difference
        result["difference"] = abs(result["mongo_count"] - result["postgres_count"])

        if result["mongo_count"] > 0:
            result["consistency_percentage"] = round(
                (1 - result["difference"] / result["mongo_count"]) * 100, 2
            )
        else:
            result["consistency_percentage"] = 100.0 if result["postgres_count"] == 0 else 0

        # Log results
        logger.info(f"MongoDB (Silver) count: {result['mongo_count']}")
        logger.info(f"PostgreSQL (Gold) count: {result['postgres_count']}")
        logger.info(f"Difference: {result['difference']}")
        logger.info(f"Consistency: {result['consistency_percentage']}%")

        if result["consistency_percentage"] < 95:
            issue = f"Data consistency below 95% (actual: {result['consistency_percentage']}%)"
            result["issues"].append(issue)
            logger.warning(issue)

        return result

    def _log_report(self, report: QualityReport) -> None:
        """Log report chi tiet."""
        logger.info(f"Source: {report.source}")
        logger.info(f"Total records: {report.total_records}")
        logger.info(f"Duplicate count: {report.duplicate_count} ({report.duplicate_percentage}%)")
        logger.info(
            f"Invalid price count: {report.invalid_price_count} ({report.invalid_price_percentage}%)"
        )

        for field_name, stats in report.null_checks.items():
            logger.info(f"Null check - {field_name}: {stats['null_percentage']}%")

        if report.issues:
            logger.warning(f"Issues found: {len(report.issues)}")
            for issue in report.issues:
                logger.warning(f"  - {issue}")
        else:
            logger.info("No quality issues found")

        logger.info(f"Quality check {'PASSED' if report.passed else 'FAILED'}")

    def run(self, raise_on_failure: bool = True) -> dict[str, Any]:
        """
        Chay toan bo quy trinh kiem tra chat luong.

        Args:
            raise_on_failure: Raise exception neu check that bai

        Returns:
            Dict chua tat ca reports

        Raises:
            ValueError: Neu raise_on_failure=True va co issue
        """
        logger.info("=" * 60)
        logger.info("Starting quality check pipeline")
        logger.info("=" * 60)

        results = {
            "mongo_report": None,
            "postgres_report": None,
            "consistency": None,
            "overall_passed": True,
            "all_issues": [],
        }

        try:
            self.connect_mongo()
            self.connect_postgres()

            # Check MongoDB quality
            mongo_report = self.check_mongo_quality()
            results["mongo_report"] = mongo_report.to_dict()
            if not mongo_report.passed:
                results["overall_passed"] = False
                results["all_issues"].extend(
                    [f"[MongoDB] {i}" for i in mongo_report.issues]
                )

            # Check PostgreSQL quality
            postgres_report = self.check_postgres_quality()
            results["postgres_report"] = postgres_report.to_dict()
            if not postgres_report.passed:
                results["overall_passed"] = False
                results["all_issues"].extend(
                    [f"[PostgreSQL] {i}" for i in postgres_report.issues]
                )

            # Check consistency
            consistency = self.check_data_consistency()
            results["consistency"] = consistency
            if consistency["issues"]:
                results["overall_passed"] = False
                results["all_issues"].extend(
                    [f"[Consistency] {i}" for i in consistency["issues"]]
                )

            # Final summary
            logger.info("=" * 60)
            logger.info("Quality check pipeline completed")
            logger.info(f"Overall result: {'PASSED' if results['overall_passed'] else 'FAILED'}")

            if results["all_issues"]:
                logger.warning(f"Total issues: {len(results['all_issues'])}")
                for issue in results["all_issues"]:
                    logger.warning(f"  {issue}")

                if raise_on_failure:
                    raise ValueError(
                        f"Quality check failed with {len(results['all_issues'])} issues"
                    )

            logger.info("=" * 60)

        finally:
            self.close()

        return results


def main():
    """Entry point cho quality_check module."""
    # Custom thresholds (optional)
    thresholds = QualityThresholds(
        max_null_percentage=10.0,
        max_duplicate_percentage=5.0,
        max_invalid_price_percentage=5.0,
        min_record_count=1,
    )

    checker = QualityChecker(thresholds=thresholds)

    try:
        results = checker.run(raise_on_failure=True)
        if results["overall_passed"]:
            logger.info("All quality checks passed!")
            sys.exit(0)
    except ValueError as e:
        logger.error(f"Quality check failed: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
