## **v1.7.1-dev2 2024.06.06**

This is a _Preview Channel_ Release (with package name suffix `preview`), which might have potential bugs.
此为预览通道版本 (包名后缀`preview`), 可能存在潜在问题!

### EN

1. Fix: wrong cover if scrolling too fast
2. Fix: missing album cover in Artist Detail after going back from Artist Album Detail
3. Improve: accelerate palette color generation for performance
4. Modify: reimplement image preload mechanism
5. Development: avoid unexpectedly blocking main thread when loading images
6. Development: disable emoji compat to improve RecyclerView performance
7. Development: do not use plurals strings to improve RecyclerView performance
8. Development: cleanup code


### ZH

1. 修复 滚动过快时封面错误
2. 修复 艺术家详情中，从专辑详情返回后，专辑封面丢失
3. 改进 改进主色调生成以提升性能
4. 修改 重新实现图像预加载机制
5. 开发 避免在加载图像时意外阻塞主线程
6. 开发 禁用 emoji compat 以提升 RecyclerView 性能
7. 开发 不使用 plurals 字符串以提升 RecyclerView 性能
8. 开发 清理代码



**Commit log**: 