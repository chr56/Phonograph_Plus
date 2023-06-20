#!/usr/bin/env bash

# This bash script is to generate signing key and signing config

# Required Environment Variables:
#   $SECRETS_KEY             : [secret] signing key store file encoded via BASE64
#   $SECRETS_STORE_PASSWORD  : [secret] signing key store file password
#   $SECRETS_KEY_PASSWORD    : [secret] signing key password
#   $SECRETS_KEY_ALIAS       : [secret] signing key key alias
#   $KEYSTORE_FILE           : recovered key store file
#   $CONFIG_FILE             : generated `signing.properties` file

echo "Generate signing key and config..."

if [[ -n "$SECRETS_KEY" ]]
then
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
else
  echo 'Signing key not found!'
fi

echo "Generate signing key and config completed!"