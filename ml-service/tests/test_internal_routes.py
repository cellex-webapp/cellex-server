"""Tests for internal retraining endpoints."""

from unittest.mock import AsyncMock, patch

import pytest
from fastapi.testclient import TestClient


@pytest.fixture
def client():
    from app.main import app

    return TestClient(app)


class TestInternalTrainAllEndpoint:
    @patch("app.api.internal_routes.settings")
    def test_train_all_requires_bearer_token(self, mock_settings, client):
        mock_settings.internal_train_token = "secret-token"

        response = client.post("/api/v1/internal/train-all")

        assert response.status_code == 401

    @patch("app.api.internal_routes.chatbot_routes.chatbot_agent")
    @patch("app.api.internal_routes.recommender")
    @patch("app.api.internal_routes.settings")
    def test_train_all_queues_background_job(self, mock_settings, mock_recommender, mock_chatbot_agent, client):
        mock_settings.internal_train_token = "secret-token"
        mock_recommender.train.return_value = {
            "trained_at": "2024-01-01T00:00:00Z",
            "rmse": 0.1234,
        }
        mock_chatbot_agent.index_products = AsyncMock(return_value=17)

        response = client.post(
            "/api/v1/internal/train-all",
            headers={"Authorization": "Bearer secret-token"},
        )

        assert response.status_code == 202
        data = response.json()
        assert data["success"] is True
        assert data["queued"] is True

        status_response = client.get("/api/v1/internal/train-all/status")
        assert status_response.status_code == 200
        status_data = status_response.json()
        assert status_data["last_result"]["success"] is True
        assert status_data["last_result"]["rmse"] == 0.1234
        assert status_data["last_result"]["indexed_count"] == 17