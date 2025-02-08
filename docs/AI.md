# 这事toNeko的AI设置
## 模型设置
AI支持多种模型，你可以在[Gemini模型页面](https://ai.google.dev/gemini-api/docs/models/gemini?hl=zh-cn)或[groq模型页面](https://console.groq.com/docs/models)查看所有支持的模型 （注意：你需要能够访问外网的能力）

这里给大家推荐以下几个模型
- gemini-2.0-flash
- deepseek-r1-distill-llama-70b
- llama3-70b-8192
## 获取Key
想要获取Key很简单哦，根据你使用模型的不同，需要到不同网站获取key

### gemini模型
你只需要一个谷歌帐号就可以直接免费获取到啦

首先打开[Google AI Studio](https://aistudio.google.com)，在这个页面点击`Get API key`按钮后呢，一步步往下操作就好啦

### 其它模型(deepseek, llama3等)
同样的，只需要一个邮箱就能免费获取到（注意：已知不支持outlook邮箱）
请先去[groq官网](https://console.groq.com)注册一个账号，然后去[Keys](https://console.groq.com/keys)页面获取key即可

## 配置
在toNeko配置页面（模组菜单->toNeko->配置）或配置文件（config/toneko.yml）启用AI功能，并填入你的模型和密钥就好啦。

## 代理
这个功能是可选的，如果你无法访问外网，你可能需要用到。

如果你使用gemini模型，那么在游戏内是不需要代理的，因为模组已经帮你自动代理好了。

而中国大陆环境无法使用其它模型，这时候你可能需要设置代理。

### 代理客户端的使用
你可以使用[NekoRay](https://github.com/MatsuriDayo/nekoray)或[Clash Meta](https://github.com/MetaCubeX/mihomo/tree/Meta)来创建代理，然后在配置中设置对应的代理ip及端口就好

这里就不详细讲解如何使用了，有需要的可以自行探究
### 注意事项
- 目前仅支持http代理
- 目前暂不支持需要验证的代理
- 如果你使用了代理客户端的 Tun 模式，请在游戏内不要设置代理
## 提示词
你可以自由设置提示词，并且可以使用占位符，以下是所有的占位符

| 占位符               | 描述      | 示例值                |
|-------------------|---------|--------------------|
| %neko_name%       | 猫娘的名称   | `Ayame`,`cinamono` |
| %neko_type%       | 猫娘的种类   | `冒险家猫娘`            |
| %neko_des%        | 猫娘的描述   | `热爱冒险的猫娘`          |
| %neko_height%     | 猫娘的身高   | `1.50`             |
| %neko_moe_tags%   | 猫猫的萌属性  | `傲娇`,`百合`          |
| %player_name%     | 玩家名称    | `Crystal_Neko`     |
| %player_is_owner% | 玩家是否为主人 | `是`,`不是`           |
| %player_is_neko%  | 玩家是否为猫娘 | `是`,`不是`           |
| %world_time%      | 世界时间    | `白天`,`夜晚`          |
| %world_weather%   | 世界天气    | `晴天`,`雨天`          |