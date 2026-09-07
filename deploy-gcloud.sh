#!/bin/sh
# Builds this repo's Dockerfile via Cloud Build and deploys it to Cloud Run, wiring the same env
# vars the app already knows how to consume (see RenderDatabaseUrlEnvironmentPostProcessor and
# application.properties) - no code or Dockerfile changes needed for this vs. any other host.
#
# Prerequisites (one-time, per GCP project):
#   gcloud auth login
#   gcloud config set project "$PROJECT_ID"
#   gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
#
# Usage:
#   PROJECT_ID=my-gcp-project \
#   DATABASE_URL='postgresql://user:pass@host/db?sslmode=require' \
#   JWT_SECRET='...' \
#   ./deploy-gcloud.sh
#
# Secrets are read from your shell environment at deploy time, never hardcoded here or
# committed - pass them inline as above, or export them in a shell you don't share/commit.
set -e

: "${PROJECT_ID:?Set PROJECT_ID to your GCP project id}"
: "${DATABASE_URL:?Set DATABASE_URL to the Postgres connection string (postgres:// or jdbc:)}"
: "${JWT_SECRET:?Set JWT_SECRET to a real random secret - generate one with: openssl rand -base64 48}"

REGION="${REGION:-europe-west1}"
SERVICE_NAME="${SERVICE_NAME:-karata}"

# 1Gi: Cloud Run's 512Mi default is tight for a JVM plus the bundled web-ui static assets and
# regularly OOMs on startup - bump it rather than let that surprise you.
gcloud run deploy "$SERVICE_NAME" \
  --source . \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --memory 1Gi \
  --allow-unauthenticated \
  --set-env-vars "DATABASE_URL=$DATABASE_URL,JWT_SECRET=$JWT_SECRET"
