**0.3.3 2022.9.18**

**CN**
- 修复 打开应用时播放队列被随机刷新
- 改进 将交换队列操作(随机播放、播放播放列表/专辑/艺术家)移动到后台，以修复更改队列时卡死(如果歌曲过多)
- 改进 加载封面图时指定图片大小，尽量避免图片过大导致应用崩溃

**EN**
- Fix: randomly refresh playing queue on opening
- Improve: move swapping-queue operations (e.g. shuffle all, play a playlist, play an album/artist) to background, to
  fix getting frozen when changing queue especially for users having to many song
- Improve: designate a rational images size when loading cover artworks, to try to not crash app if the image is too
  large 