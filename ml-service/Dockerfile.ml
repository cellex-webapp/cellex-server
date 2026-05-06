# ── Build stage ──────────────────────────────────────────────
FROM python:3.11-slim AS builder

WORKDIR /build
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

# ── Runtime stage ────────────────────────────────────────────
FROM python:3.11-slim

RUN groupadd -r mluser && useradd -r -g mluser mluser

WORKDIR /app

COPY --from=builder /install /usr/local
COPY . .

RUN mkdir -p /app/trained_models && chown -R mluser:mluser /app

USER mluser

ENV PORT=8000

EXPOSE ${PORT}

CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]
