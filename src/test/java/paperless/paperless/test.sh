#!/usr/bin/env bash
set -euo pipefail
API="http://localhost:8081/api"
echo "Uploading..."
ID="1"
echo "Created id: ${ID}"
echo "Fetching..."
curl -s "$API/documents/${ID}" | jq .
