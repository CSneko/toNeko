# 在线API
toneko提供了一个在线统计API，你可以通过这个API来获取在线统计数据。
## 被撅统计
你只需要向`https://api.toneko.cneko.org/stick/get?name=你的名字`发送一个GET请求，会返回一串JSON数据如下所示:
```json
{"attack":撅的次数,"attacked":被撅次数}
```

当然，你也可以获取排行榜数据，向`https://api.toneko.cneko.org/stick/get/top?type=neko`发送一个GET请求，会返回一串JSON数据如下所示:
```json
[{"name":"Crystal_Neko","time":{"attack":41,"attacked":560}},{"name":"MuX_Yang","time":{"attack":129,"attacked":369}},{"name":"LoneStar_MS","time":{"attack":0,"attacked":185}}]
```
你可以把type参数改为player以获取撅的次数排行榜，改为neko以获取被撅次数排行榜。
## 喵呜统计
喵呜统计与被撅统计类似，请求url为`https://api.toneko.cneko.org/meow/get?name=你的名字`，返回
```json
{"name":"Crystal_Neko","meow":喵的次数}
```
同样的，它也支持排行榜，url为`https://api.toneko.cneko.org/meow/get/top`，返回
```json
[{"name":"Crystal_Neko","meow":37},{"name":"CrystalNeko","meow":20},{"name":"neko","meow":2}]
```
## 可以在网站上使用它吗
当然可以，它默认支持跨域请求，你可以在网页中通过`fetch`函数来获取数据。