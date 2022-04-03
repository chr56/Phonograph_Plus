**Preview 0.2.0 2022.4.?**

**CN**
开发 重构大量歌词相关代码
开发 清理主播放器UI代码, 改进性能
变更 重写歌词对话框
变更 增大歌词对话框大小
新增 歌词对话框支持选择歌词源(内嵌或外部)
新增 歌词对话框支持歌词随歌曲播放滚动(实验性)
修复 通知栏通知偶现的刷新不及时或信息错误(存疑)
新增 错误报告通知,仅记录并通知非紧要的内部错误以便调试
开发 尝试记录卡片式主播放器偶现的卡片错位现象
开发 关闭Glide部分无关紧要的日志输出
开发 清理代码

**EN**
Development: refactor most lyrics related code
Development: Clean up the main player UI code, and improve its performance
Change: rewrite lyrics dialog UI
Change: increase size of the lyrics dialog
New: manually select lyrics source (embedded or external) in lyrics dialog
New: automatically lyrics scrolling along with song playback (experimental support)
Fix: occasionally now-playing notification hasn't been refreshed in time or display incorrect song information in the notification bar (suspecting)
New: error report notifications, only record and notify non-critical internal errors for better debugging
Development: try to record occasionally error layout of playing queue card in the card main player
Development: partially turns off annoying Glide irrelevant log output
Development: cleanup code