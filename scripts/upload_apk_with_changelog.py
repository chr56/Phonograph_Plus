#!/usr/bin/env python3

import os

TOKEN = os.environ["TOKEN"]
CHAT_ID = os.environ["CHAT_ID"]
ARTIFACTS_DIRECTORY = os.environ["ARTIFACTS"]

VERSION = os.environ["VERSION"]
GIT_COMMIT = os.environ["GIT_COMMIT"]

APK_SUFFIX = "apk"
CHANGE_LOG_FILE = "EscapedReleaseNote.md"
GITHUB_RELEASE_URL = "github.com/chr56/Phonograph_Plus/releases/tag/"

import telebot
from telebot.types import InputMediaDocument
from telebot.types import ReplyParameters
from telebot.formatting import escape_markdown
from datetime import datetime

tb = telebot.TeleBot(TOKEN)


def collect_files(relative_root, files_names: list) -> list[InputMediaDocument]:
    print(f"-+ {relative_root}")

    def file_to_document(name, caption=None, parse_mode="MarkdownV2") -> InputMediaDocument:
        file = open(name, 'rb')
        return InputMediaDocument(file, caption=caption, parse_mode=parse_mode)

    def relative_path(file_name):
        print(f" ++ {file_name}")
        return os.path.join(relative_root, file_name)

    input_media_documents = []
    for i, filename in enumerate(sorted(files_names, reverse=True)):
        if i == len(files_names) - 1:
            current_date = escape_markdown(datetime.now().strftime("%Y.%m.%d"))
            version_name = escape_markdown(VERSION)
            url = escape_markdown(GITHUB_RELEASE_URL + VERSION)
            caption = f"*{version_name} {current_date}* \n\n[GitHub]({url})"
            doc = file_to_document(relative_path(filename), caption=caption)
        else:
            doc = file_to_document(relative_path(filename))
        input_media_documents.append(doc)

    return input_media_documents


# List Files
documents = []
print(f"Collecting files in {ARTIFACTS_DIRECTORY}...")
for root, dirs, files in os.walk(ARTIFACTS_DIRECTORY):
    apks_files = list(filter(lambda name: name.endswith(APK_SUFFIX), files))
    if len(apks_files) > 0:
        documents += collect_files(relative_root=root, files_names=apks_files)
print(f"Collected {len(documents)} files")

if len(documents) <= 0:
    print("No file to send!")
    exit()

# Send Files
print("Sending files...")
media_messages = tb.send_media_group(CHAT_ID, media=documents, disable_notification=True, )
print(f"{len(media_messages)} files have been sent")

# Send Text
print("Sending changelogs...")
changelog_file = open(CHANGE_LOG_FILE, "r", encoding="utf-8")
text = changelog_file.read()
reply_parameters = ReplyParameters(media_messages[0].message_id, CHAT_ID)
text_message = tb.send_message(CHAT_ID,
                               text,
                               parse_mode="MarkdownV2",
                               reply_parameters=reply_parameters,
                               disable_notification=True, )

print("Success!")
