## **Development Plan (or Road Map?)** & **TO-DO list**


### Major Development Plan

#### Independent Database by Jetpack Room

Use AndroidX Room to build a audio database, which:

- [x] has proper Song-Album-Artist Relationship (one-many & many-many) with `artistName` spited (parse ';', '&', '/', '\', ',').

- [ ] has independent Genre store, which united Genre and Style fields with spited (parse ';', '&', '/', '\', ',').

- [ ] has multiple data source (Android MediaStore & File System JAudioTagger Parse).

- [x] can be refresh manually.

- [ ] can be update automatically.

- [ ] all queries are in background.

- [ ] search enhanced.


This part is currently work-in-progress. 
See branch `room` or `database`


#### Refactor Main Player UI

This is for simplifying player fragments and related fragments, including:


- Simplifying layout

- reduce fragments complexity

- fully MVVM architecture or MVI architecture



#### Redesign `AlbumDetail`  and `ArtistDetail` activities

Redesign these to have a better appearance and to make maintenance easier.



### Minor Development Plan

Small development plans.


- [ ] Improve App Intro (WIP🚧)

- [ ] Enhance Playlist Detail: support search, Better way to
  modify , handle intent of open (playlist) file

- [ ] improve SlidingMusicBar

- [ ] Support some Android's StatusBar lyrics, such as FlyMe / EvolutionX


### Canceled Development Plan

They may not be implemented as planned.

- [x] <del>Validate audio files</del>

- [ ] <del>Correctly Handling Android 11+ File Permission</del>

- [ ] <del>Refactor so-called Theme Engine</del>