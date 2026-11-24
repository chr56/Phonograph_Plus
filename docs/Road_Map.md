# **Development Road Map** & **TO-DO list**

_Last Update: 2026.11.06_

------

## Primary Milestones

### Playlist Enhancement

- [ ] Redesign Playlist Details.

- [ ] Support display options in Playlist Details.

- [ ] Parsing and writing M3U8 files directly.

- [ ] Switch Mediastore Playlist Backend between Standalone Backend.


### Music Service Migration

#### Persistence Database

- [ ] New PlayingQueue modeling with Room database.

- [ ] Support playing other source other than MediaStore, such as content Uris.

- [ ] Integrate PlayingQueue, History, Song Play Count into one database.

#### Migrate MusicService to Media3 (Exoplayer)

- [ ] Replace implementation with Exoplayer, as an independent PlayerController.

- [ ] Integrate Media3 framework.


### Search Enhancement

- [ ] Rewrite Search user-interface with Jetpack Compose.
   - [x] Prepare Compose functions for various items.
   - [ ] Display systems for Compose.
   - [ ] Implement new Search user-interface.

- [ ] FullTextSearch support.
   - [ ] FTS in music database.
   - [ ] Update Search user-interface.

------

## Minor Milestones

## Enhance Main Player User-Interface

- [ ] Better Edge-to-Edge support.

- [ ] Adaptive screen-size support: tablet, watch and desktop support.

- [ ] More Now Playing Screen styles. (_Tentative_)

- [ ] Customizable SlidingMusicBar. (_Tentative_)


## Enhance Desktop User Experience

- [ ] Adaptive screen-size support.

- [ ] Keyboard shortcut. (_Tentative_)

- [ ] Drag-and-drop. (_Tentative_)

------

## Long-term Goals

### Metadata Enhancement

- [ ] Database: united Genre and Style from tag fields.

- [ ] Database: read and store Replay-Gain tag.

- [ ] Player: respect parsed Replay-Gain tag from databases.

- [ ] Database: cache artwork information in database, for quicker lookup. (_Tentative_)


### Native Decoder for Music Service (_Tentative_)

- [ ] Migrate to Exoplayer first.

- [ ] Configure and build `ffmpeg` with NDK.

- [ ] Implement native decoder for Exoplayer.

- [ ] Integrate native decoder.

- [ ] Ensure reproducible Builds.

### Streaming Player (_Tentative_)

- [ ] Player streaming support.

- [ ] Remote source items support for Queue Database.

- [ ] Support streaming via Samba/NFS/SFTP.

- [ ] Enhance File user-interface and infrastructures for remote source.


------

## Miscellaneous Staging Goals


### Modularization & Plugin Systems

- [ ] Disassemble project into multiple small Gradle modules.
  - [ ] base modules `api` or `core`.
  - [ ] bottom modules `ui` or `app`.
  - [ ] remaining module.
  - [ ] build flavors.

- [ ] Plugin systems. (_Tentative_)


### Migrate Settings to Protobuf Datastore (_Tentative_)

### Redesign _Album Detail_ and _Artist Detail_ (_Tentative_)

### Improve App Intro (_Tentative_)

### Enhance Lyrics System (_Tentative_)

------