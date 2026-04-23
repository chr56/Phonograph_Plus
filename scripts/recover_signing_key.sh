#!/usr/bin/env bash

# Recover keystore file from BASE64 content.
# Args:
#   --secrets-key <BASE64>
#   --keystore-file <path>
# Env fallback:
#   SECRETS_KEY, KEYSTORE_FILE (default: .././key.jdk)

set -euo pipefail

while [[ $# -gt 0 ]]; do
  case "$1" in
    --secrets-key)
      SECRETS_KEY="${2:-}"
      shift 2
      ;;
    --keystore-file)
      KEYSTORE_FILE="${2:-}"
      shift 2
      ;;
    -h|--help)
      echo "Usage: recover_signing_key.sh [--secrets-key <BASE64>] [--keystore-file <path>]"
      exit 0
      ;;
    *)
      echo "ERROR: Unknown argument: $1"
      echo "Usage: recover_signing_key.sh [--secrets-key <BASE64>] [--keystore-file <path>]"
      exit 1
      ;;
  esac
done

SECRETS_KEY="${SECRETS_KEY:-}"
KEYSTORE_FILE="${KEYSTORE_FILE:-.././key.jdk}"

if [[ -z "$SECRETS_KEY" ]]; then
  echo "WARNING: Signing key not found, skip generating keystore."
  exit 255
fi

echo "KEYSTORE_FILE path: $KEYSTORE_FILE"
echo "Recovering signing key..."

# recover keystore from base64
printf "%s" "$SECRETS_KEY" | base64 -d > "$KEYSTORE_FILE"

echo "Keystore file ($KEYSTORE_FILE) sha256sum:"
sha256sum "$KEYSTORE_FILE"
echo "Keystore file recovered successfully."
