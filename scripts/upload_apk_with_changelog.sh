#!/usr/bin/env bash

# This bash script is to upload apk

# Required Environment Variables:
#   $REPO_ROOT      : root path of repository
#   $GIT_REF_NAME   : tag name
#   $GIT_COMMIT_SHA : commit sha
#   $TG_BOT_TOKEN   : [secret] bot token
#   $TG_CHAT_ID     : [secret] target chat id


# init variables

if [ -z "$REPO_ROOT" ]; then
  export REPO_ROOT=$PWD
fi

if [ -z "$GIT_REF_NAME" ]; then
  export GIT_REF_NAME="NA"
fi

if [ -z "$GIT_COMMIT_SHA" ]; then
  export GIT_COMMIT_SHA="NA"
fi


if [ -z "$TG_BOT_TOKEN" ]; then
  echo "Error: No Token"
  exit 255
fi

if [ -z "$TG_CHAT_ID" ]; then
  echo "Error: No Chat ID"
  exit 255
fi

# search APK
APK_FILE=$(find products/PreviewRelease -name "Phonograph*.apk" | tr -d '[:blank:]')

# upload APK
curl -v \
-X POST \
-H "Content-Type:multipart/form-data"  \
-F "document=@${REPO_ROOT}/${APK_FILE}"  \
"https://api.telegram.org/bot${TG_BOT_TOKEN}/sendDocument?chat_id=${TG_CHAT_ID}&disable_notification=true&disable_web_page_preview=true&parse_mode=HTML&caption=%3Cb%3ENew%20Preview%20Version%3C%2Fb%3E%3A%20%3Ca%20href%3D%22https%3A%2F%2Fgithub.com%2Fchr56%2FPhonograph_Plus%2Freleases%2Ftag%2F${GIT_REF_NAME}%22%3E%3Cb%3E${GIT_REF_NAME}%3C%2Fb%3E%3C%2Fa%3E%20%0D%0APreview%20versions%20might%20have%20potential%20bugs.%20%0D%0APreview%20%E7%89%88%E6%9C%AC%E5%8F%AF%E8%83%BD%E5%AD%98%E5%9C%A8%E6%BD%9C%E5%9C%A8%E9%97%AE%E9%A2%98!%20%0D%0A(%3Cpre%3E${GIT_COMMIT_SHA}%3C%2Fpre%3E)%20"

# upload changelog
RELEASE_NOTE=$(cat "${REPO_ROOT}/GitHubReleaseNote.url.txt")
curl -v "https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage?chat_id=${TG_CHAT_ID}&disable_notification=true&disable_web_page_preview=true&parse_mode=Markdown&text=$RELEASE_NOTE"
