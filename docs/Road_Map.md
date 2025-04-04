# **Development Plan (or Road Map?)** & **TO-DO list**

_Last Update: 2025.04.04_

## Independent Database by Jetpack Room

Use AndroidX Room to build an audio database:


This part is currently work-in-progress.
See branch `room` or `database`

- [x] Prepare Jetpack Room

#### Stage 1 MediaStore Cache/Mirror

- [x] Table same as MediaStore

- [x] Manually refreshing

- [ ] Automatically sync with MediaStore


#### Stage 2 Database Playlist

- [x] Table for Playlists and Playlist Songs

- [x] Playlist operations

- [x] Database Playlist UI


#### Stage 3 Database Favorites

- [x] Tables for Favorites Songs and Favorites/Pined Playlists
  
- [ ] Favorites operations

- [ ] Migrate from old implementation

- [ ] UI adaptions

- [ ] Backup support / Backward compatibility

#### Stage 4 Solving Complex Relationship for Artists/Albums

- [x] Tables for Songs/Artists/Albums

- [x] Operations

- [x] Solving Relationship (one-many & many-many)

- [x] Spited `artistName` (parse ';', '&', '/', '\', ',').

- [ ] Automatically sync with MediaStore

- [ ] Update loader

#### Stage 5 Enhanced Genres

- [ ] Table for Genres

- [ ] United Genre and Style from tag fields with spited (parse ';', '&', '/', '\', ',')

- [ ] Update loader

#### Stage 6 Artwork Cache

- [ ] Table for artwork locations for Artists/Albums

- [ ] Operations

- [ ] Update relative coil components

#### Stage 7 More Enhancement

- [ ] Enhanced Search

- [ ] Multiple tag source (Android MediaStore & File System JAudioTagger Parse).

- [ ] Replay-gain tag parse

## Modularize

Disassemble project into small Gradle modules. This part is currently preparing.

- [ ] Reorganize current package hierarchy

- [ ] extract base module `api`

- [ ] split ui and mechanism logic as modules

- [ ] further split mechanism logic by aspect

- [ ] further split ui

- [ ] create light weight variant by removing non-essential modules (Tentative)

## Migrate Settings to Protobuf Datastore

Migrate current settings backend from Preference Datastore to Protobuf Datastore:

- [ ] prepare infrastructure

- [ ] partial migrate all Json based settings with custom datatype

- [ ] earlier stage backup and backward compatibility support

- [ ] migrate path filter, and remove its legacy database implementation

- [ ] full migrate

- [ ] full backup and backward compatibility support

## Player Refactor

This part is currently work-in-progress.

#### Migrate to Exoplayer (Media3)

Migrate player backend to `Exoplayer`/ `Jetpack Media3`

- [x] Implementation as an independent `PlayerController`

- [ ] Integrate as an opt-in experimental feature

- [ ] Drop legacy Implementation

- [ ] Refactor architecture for `Exoplayer`

- [ ] support state of "buffing"

#### Refactor Playings Queue

- [ ] integrate PlayingQueue, History, Song Play Count into one Database

- [ ] enhance PlayingQueue with player, support playing other source beside MediaStore, like content uri

- [ ] update UI etc

#### Cloud Player (Tentative)

- [ ] (Tentative and Planing) Extend player, support Samba/NFS/SFTP

- [ ] (Tentative and Planing) Refactor File relative UI

- [ ] ...

#### Native Player Decoder (Tentative, possibly canceled)

Integrate with native decoder like `ffmpeg` or `Symphonia`.

- [x] Test for NDK Cross Compile

##### Exoplayer with `ffmpeg` route 

- [ ] Migrate to Exoplayer first

- [ ] Build its `ffmpeg` decoder library

- [ ] Integrate native decoder

- [ ] Reproducible Builds

##### Full native route (possibly canceled)

- [ ] Build the native decoder

- [ ] Player support in JVM side.

- [ ] Glue and logical code in Native side.

- [ ] Reproducible Builds

## Refactor Main Player UI

This is for simplifying player fragments and related fragments, including:

#### Refactoring

- [ ] simplify layout

- [ ] reduce fragments complexity

- [ ] fully MVVM architecture or MVI architecture

#### Enhancements

- [ ] flexible Now Playing Screen

- [ ] more Now Playing Screen

- [ ] support for fast-forward/fast-rewind by seconds

- [ ] enhance SlidingMusicBar (Tentative, possibly canceled)

## Unit Tests

Write unit tests for core mechanism / logic.

## Redesign `AlbumDetail`  and `ArtistDetail` activities (Tentative, possibly canceled)

Redesign these to have a better appearance and to make maintenance easier.

(Waiting for plans)


## Playlist Detail Enhancement (Tentative, possibly canceled)

- [ ] Better search support

- [ ] Handling intent of open (playlist) file

- [ ] Better way to modify playlist

## Misc Development Plan

- [ ] Improve App Intro (Tentative, possibly canceled)

- [ ] Support some Android's StatusBar lyrics, such as FlyMe / EvolutionX
