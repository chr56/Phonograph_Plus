#!/bin/bash
set -euo pipefail

usage() {
    local exit_code="${1:-0}"
    cat <<EOF
Sign an APK with the given key.
Supports key rotation via apksigner.

Usage: $(basename "$0") [options] <apk>

Options:
  -o, --output <path>          Output path (default: overwrite input)
  -k, --keystore <path>        New keystore path (default: \$KEYSTORE_FILE)
  -p, --password <pass>        New keystore password (default: \$KEYSTORE_PASSWORD)
  -a, --alias <alias>          New key alias (default: \$KEYSTORE_ALIAS)
      --old-keystore <path>    Old keystore for key rotation (default: \$OLD_KEYSTORE_FILE)
      --old-password <pass>    Old keystore password (default: \$OLD_KEYSTORE_PASSWORD)
      --old-alias <alias>      Old key alias (default: \$OLD_KEYSTORE_ALIAS)
      --lineage <path>         Lineage file for key rotation (default: \$LINEAGE_FILE)

When any --old-* option is provided, the script runs in rotation mode:
the old key is used to generate a lineage file (if missing), and the APK
is signed with the new key + lineage.

Missing credentials prompt interactively. Set CI=true to disable prompts,
then passwords are passed to apksigner through environment variables.
EOF
    exit "$exit_code"
}


# CI Mode
IS_CI=false
case "${CI:-}" in
    1|true|TRUE|yes|YES)
        IS_CI=true
        ;;
esac

# defaults from env
APK=""
OUTPUT=""

KEYSTORE="${KEYSTORE_FILE:-}"
KEYSTORE_PASSWORD="${KEYSTORE_PASSWORD:-}"
ALIAS="${KEYSTORE_ALIAS:-}"

OLD_KEYSTORE="${OLD_KEYSTORE_FILE:-}"
OLD_KEYSTORE_PASSWORD="${OLD_KEYSTORE_PASSWORD:-}"
OLD_ALIAS="${OLD_KEYSTORE_ALIAS:-}"
LINEAGE="${LINEAGE_FILE:-}"

while [[ $# -gt 0 ]]; do
    case "$1" in
        -o|--output)       OUTPUT="$2";                shift 2 ;;
        -k|--keystore)     KEYSTORE="$2";              shift 2 ;;
        -p|--password)     KEYSTORE_PASSWORD="$2";     shift 2 ;;
        -a|--alias)        ALIAS="$2";                 shift 2 ;;
        --old-keystore)    OLD_KEYSTORE="$2";          shift 2 ;;
        --old-password)    OLD_KEYSTORE_PASSWORD="$2"; shift 2 ;;
        --old-alias)       OLD_ALIAS="$2";             shift 2 ;;
        --lineage)         LINEAGE="$2";               shift 2 ;;
        -h|--help)         usage 0                  ;;
        -*)
            echo "Unknown: $1" >&2; usage 1 ;;
        *)
            APK="$1"; shift ;;
    esac
done

[[ -z "$APK" ]] && { echo "Missing APK argument." >&2; usage 1; }

# find apksigner and zipalign
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -z "$SDK" ]] && SDK="$HOME/Android/Sdk"
APKSIGNER=$(find "$SDK"/build-tools/*/apksigner* -type f 2>/dev/null | sort -V | tail -1)
[[ -z "$APKSIGNER" ]] && { echo "apksigner not found in \$ANDROID_HOME/build-tools/" >&2; exit 1; }
ZIPALIGN="$(dirname "$APKSIGNER")/zipalign"
[[ -z "$ZIPALIGN" ]] && { echo "zipalign not found in \$ANDROID_HOME/build-tools/" >&2; exit 1; }

check-status(){
    local APK_FILE="${1}"
    local TITLE="${2:-APK}"
    echo "$TITLE: $APK_FILE"
    "$ZIPALIGN" -c -P 16 -v 4 "$APK_FILE" > "${APK_FILE}.zipalign-report.txt" || true
    "$APKSIGNER" verify --verbose "$APK_FILE" || true
    echo "-----------"
}

require_value() {
    local var_name="$1" prompt="$2" silent="${3:-0}"

    eval "value=\"\${$var_name-}\""
    [[ -n "$value" ]] && return

    $IS_CI && { echo "Missing required credential in CI." >&2; exit 1; }

    if [[ "$silent" -eq 1 ]]; then
        read -r -s -p "$prompt: " "$var_name" && echo >&2
    else
        read -r -p "$prompt: " "$var_name"
    fi
}

# detect rotation mode from old-key inputs
ROTATE=false
if [[ -n "$OLD_KEYSTORE" || -n "$OLD_KEYSTORE_PASSWORD" || -n "$OLD_ALIAS" ]]; then
    ROTATE=true
fi

# prompt for missing credentials
require_value KEYSTORE "Keystore path" 0
require_value KEYSTORE_PASSWORD "Keystore password" 1
require_value ALIAS "Key alias" 0

if $ROTATE; then
    export OLD_KEYSTORE_PASSWORD KEYSTORE_PASSWORD
    require_value OLD_KEYSTORE "Old keystore path" 0
    require_value OLD_KEYSTORE_PASSWORD "Old keystore password" 1
    require_value OLD_ALIAS "Old key alias" 0

    [[ -z "$LINEAGE" ]] && LINEAGE="${APK}.lineage"
fi

check-status "$APK" 'Original APK'

[[ -z "$OUTPUT" ]] && cp "$APK" "${APK}.backup" && OUTPUT="$APK"

# zipalign
APK_ALIGNED="${OUTPUT}.aligned"
"$ZIPALIGN" -f -P 16 4 "$APK" "$APK_ALIGNED"

if ! [[ -e "$APK_ALIGNED" ]]; then
    echo "failed execute zipalign" >&2; exit 1;
fi


# generate lineage if needed (rotation mode, file doesn't exist yet)
if $ROTATE && [[ ! -f "$LINEAGE" ]]; then
    echo "Generating lineage: $LINEAGE"
    "$APKSIGNER" rotate \
        --out "$LINEAGE" \
        --old-signer --ks "$OLD_KEYSTORE" --ks-pass "env:OLD_KEYSTORE_PASSWORD" --ks-key-alias "$OLD_ALIAS" \
        --new-signer --ks "$KEYSTORE" --ks-pass "env:KEYSTORE_PASSWORD" --ks-key-alias "$ALIAS"
    echo "-----------"
fi

if $ROTATE; then
    "$APKSIGNER" sign \
        --v1-signing-enabled false \
        --v2-signing-enabled true \
        --v3-signing-enabled true \
        --min-sdk-version 28 \
        --ks "$OLD_KEYSTORE" \
        --ks-pass "env:OLD_KEYSTORE_PASSWORD" \
        --ks-key-alias "$OLD_ALIAS" \
        --next-signer \
        --ks "$KEYSTORE" \
        --ks-pass "env:KEYSTORE_PASSWORD" \
        --ks-key-alias "$ALIAS" \
        --lineage "$LINEAGE" \
        --out "$OUTPUT" \
        "$APK_ALIGNED"
else
    export KEYSTORE_PASSWORD
    "$APKSIGNER" sign \
        --v1-signing-enabled false \
        --v2-signing-enabled true \
        --v3-signing-enabled false \
        --min-sdk-version 24 \
        --ks "$KEYSTORE" \
        --ks-pass "env:KEYSTORE_PASSWORD" \
        --ks-key-alias "$ALIAS" \
        --out "$OUTPUT" \
        "$APK_ALIGNED"
fi


check-status "$OUTPUT" 'Signed APK'
