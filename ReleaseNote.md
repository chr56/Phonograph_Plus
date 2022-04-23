**Preview 0.2.4-beta 2022.4.23**

**CN**
修复 专辑与艺术家页面下,歌曲与专辑或艺术家(在同一专辑但艺术家不同的情况下)配对(解析)错误
优化 专辑与艺术家解析机制(加载时间可能会延长)
新增 专辑与艺术家以及流派支持按歌曲(或专辑)数量排序
修复 歌曲在使用"添加日期"的排序依据下,滚动条不提示日期
改进 使用通知提示大多数耗时操作而不是各种对话框
改进 使用通知提示更多内部错误
开发 增强混淆
开发 清理代码, 替换所有AsyncTask
**注意**
1.此次更新涉及存储播放队列数据库的改动,若发现当前播放队列异常,请清除数据
2.此次更新涉及排序方式设置的改动,需要重新设置排序方式



**EN**
Fix: an error in pairing (parsing) between songs and albums or artists (in the case of the same album but different artists) under the album and artist page
Optimize: album and artist parsing mechanism (loading time may be extended)
Add: albums and artists and genres to support sorting by number of songs (or albums)
Fix: that the scroll bar does not prompt the date when the song is sorted by "Add Date"
Improve: notifications for most time-consuming operations instead of various dialogs
Improve: notification for more internal errors
Development: Enhanced Obfuscation
Development: clean up code, replace all AsyncTask
**Note**
This update involves changes to the storage play queue database. If you find that the current play queue is abnormal, please clear the data
This update involves changes to the sorting method settings, and the sorting method needs to be reset