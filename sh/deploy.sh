#!/bin/bash

MODE=$1

if [ "$MODE" = "dev" ]; then
    echo "Deploying in DEVELOPMENT mode (unified container build)..."
    docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
elif [ "$MODE" = "prod" ]; then
    echo "Deploying in PRODUCTION mode (unified container build)..."
    docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
else
    echo "Usage: $0 [dev|prod]"
    exit 1
fi
