#!/usr/bin/env bash

# This bash script is to generate signing key and signing config

# Required Environment Variables:
#   $SECRETS_KEY             : [secret] signing key store file encoded via BASE64
#   $SECRETS_STORE_PASSWORD  : [secret] signing key store file password
#   $SECRETS_KEY_PASSWORD    : [secret] signing key password
#   $SECRETS_KEY_ALIAS       : [secret] signing key key alias
# Optional Environment Variables:
#   $KEYSTORE_FILE           : recovered key store file (default:`.././key.jdk`)
#   $CONFIG_FILE             : generated `signing.properties` file (default:`.././signing.properties`)

echo "Generate signing key and config..."

if [ -z "$KEYSTORE_FILE" ]; then
  export KEYSTORE_FILE=.././key.jdk
  echo "WARNING: KEYSTORE_FILE not specified! use default: $KEYSTORE_FILE"
else
  echo "KEYSTORE_FILE path: $KEYSTORE_FILE"
fi

if [ -z "$CONFIG_FILE" ]; then
  export CONFIG_FILE=.././signing.properties
  echo "WARNING: CONFIG_FILE not specified! use default: $CONFIG_FILE"
else
  echo "CONFIG_FILE   path: $CONFIG_FILE"
fi


if [[ -n "$SECRETS_KEY" ]]
then
  echo "recovering..."
  # recover keystore from base64
  printf "%s" "$SECRETS_KEY"  | base64 -d | tee "$KEYSTORE_FILE" > /dev/null
  # write signing.properties
  printf "storePassword=%s\n" "$SECRETS_STORE_PASSWORD"  | tee -a "$CONFIG_FILE" > /dev/null
  printf "storeFile=%s\n" "$KEYSTORE_FILE"               | tee -a "$CONFIG_FILE" > /dev/null
  printf "keyAlias=%s\n" "$SECRETS_KEY_ALIAS"            | tee -a "$CONFIG_FILE" > /dev/null
  printf "keyPassword=%s\n" "$SECRETS_KEY_PASSWORD"      | tee -a "$CONFIG_FILE" > /dev/null
  # print information
  echo "Keystore file ($KEYSTORE_FILE) sha256sum:"
  sha256sum "$KEYSTORE_FILE"
  echo "Keystore file and signing config are recovered successfully!"
else
  echo 'WARNING: Signing key not found!'
fi

echo "Generate signing key and config completed!"