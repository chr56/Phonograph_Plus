# **Development Road Map** & **TO-DO list**

_Last Update: 2025.12.04_

------

## Standalone Music Library Database

This part is currently work-in-progress, as next major feature.

See branch `room-database`

#### Stage 1 MediaStore Cache/Mirror

_(Completed!)_

#### Stage 2 Database Playlist

_(Completed!)_

#### Stage 3 Database Favorites

_(Completed!)_

#### Stage 4 Solving Complex Relationship for Artists/Albums

_(Completed! But it may have issues.)_

#### Stage 5 Enhanced Genres

- [x] Table for Genres

- [ ] Sync logic implementation, with spited field (parse ';', '&', '/', '\', ',')

- [ ] Update loader to enable new implementation 

#### Stage 6 Artwork Cache (Tentative)

Cache artwork information in database, for quicker lookup.

- [ ] Table for artwork locations for Artists/Albums

- [ ] Sync logic implementation

- [ ] Update relative coil components to enable new implementation

#### Stage 7 Independent from MediaStore & Relative Enhancement

Scan media files manually without any existed metadata from Mediastore; use JAudioTagger to read tags and parse more data.

- [ ] Multiple tag source (Android MediaStore & JAudioTagger Parse) and manual scan implementation.

- [ ] Metadata enhancement: united Genre and Style from tag fields

- [ ] Metadata enhancement: read and store Replay-Gain tag

- [ ] Player: use parsed Replay-Gain tag

#### Stage 8 More Enhancement

- [ ] Enhanced Search

------

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

#### Native Player Decoder (Tentative, possibly canceled)

Integrate with native decoder like `ffmpeg` or `Symphonia`.

- [x] Test for NDK Cross Compile

##### Exoplayer with `ffmpeg` backend

- [ ] Migrate to Exoplayer first

- [ ] Build its `ffmpeg` decoder library

- [ ] Integrate native decoder

- [ ] Reproducible Builds

##### Full native route (possibly canceled)

- [ ] Build the native decoder

- [ ] Player support in JVM side.

- [ ] Glue and logical code in Native side.

- [ ] Reproducible Builds

#### Cloud Player (Tentative, possibly canceled)

- [ ] (Tentative and Planing) Extend player, support Samba/NFS/SFTP

- [ ] (Tentative and Planing) Refactor File relative UI

- [ ] ...

------

## Enhance Main Player UI

- [ ] better tablet support and landscape layout enhancement

- [ ] more Now Playing Screen styles (Tentative)

- [ ] enhance SlidingMusicBar (Tentative, possibly canceled)

------

## Modularize

Disassemble project into multiple small Gradle modules. It's in staging.

- [ ] extract base module `api`

- [ ] split ui and mechanism logic as modules

- [ ] further split mechanism logic by aspect/facet

- [ ] further split ui

- [ ] create light weight variant by removing non-essential modules (Tentative)

------

## Migrate Settings to Protobuf Datastore

Migrate current settings backend from Preference Datastore to Protobuf Datastore:

- [ ] prepare infrastructure

- [ ] partial migrate all Json based settings with custom datatype

- [ ] earlier stage backup and backward compatibility support

- [ ] migrate path filter, and remove its legacy database implementation

- [ ] full migrate

- [ ] full backup and backward compatibility support

------

## Unit Tests

Write unit tests for core mechanism / logic.

(Waiting for detailed road map)

------

## Redesign _Album Detail_  and _Artist Detail_ (Tentative, possibly canceled)

Redesign them for a more appealing appearance, and enhance their efficiency and maintainability.

(Waiting for detailed road map)

------

## Playlist Detail Enhancement (Tentative, possibly canceled)

- [ ] Handling intent of open (playlist) file

- [ ] Enhance search support

- [ ] Better way to modify playlist

------

## Misc Development Plan

- [ ] Improve App Intro (Tentative, possibly canceled)

- [ ] Support some Android's StatusBar lyrics, such as FlyMe / EvolutionX
