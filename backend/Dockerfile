FROM python:3.11-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 

WORKDIR /src

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential curl && \
    rm -rf /var/lib/apt/lists/*

RUN pip install --upgrade pip && pip install poetry

COPY pyproject.toml poetry.lock ./
RUN poetry install --no-root

COPY . .