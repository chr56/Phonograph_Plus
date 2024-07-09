#!/usr/bin/env python3

import os

TOKEN = os.environ["TOKEN"]
CHAT_ID = os.environ["CHAT_ID"]
ARTIFACTS = os.environ["ARTIFACTS"]

VERSION = os.environ["VERSION"]
GIT_COMMIT = os.environ["GIT_COMMIT"]

SUFFIX = "apk"
CHANGE_LOG_FILE = "EscapedReleaseNote.md"

import telebot
from telebot.types import InputMediaDocument
from telebot.types import ReplyParameters
from telebot.formatting import escape_markdown

tb = telebot.TeleBot(TOKEN)


def pack_file(name, caption, parse_mode="MarkdownV2") -> InputMediaDocument:
    file = open(name, 'rb')
    return InputMediaDocument(file, caption=caption, parse_mode=parse_mode)


def collect_file(relative_root, files_names: list) -> list[InputMediaDocument]:
    print(f"-+ {relative_root}")

    def relative_path(file_name):
        print(f" ++ {file_name}")
        return os.path.join(relative_root, file_name)

    def caption_text(name) -> str:
        return f"`{escape_markdown(name)}` \n{escape_markdown(VERSION)}"

    input_media_documents = map(
        lambda files_name: pack_file(relative_path(files_name), caption_text(files_name)),
        files_names
    )
    return list(input_media_documents)


# List Files
documents = []
print(f"Collecting files in {ARTIFACTS}...")
for root, dirs, files in os.walk(ARTIFACTS):
    apks_files = list(filter(lambda name: name.endswith(SUFFIX), files))
    if len(apks_files) > 0:
        documents += collect_file(relative_root=root, files_names=apks_files)
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
reply_parameters = ReplyParameters(media_messages[-1].message_id, CHAT_ID)
text_message = tb.send_message(CHAT_ID,
                               text,
                               parse_mode="MarkdownV2",
                               reply_parameters=reply_parameters,
                               disable_notification=True, )

print("Success!")
