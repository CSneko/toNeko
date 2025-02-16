# 这事toNeko的AI设置
## 服务设置
当前一共支持以下5种服务
- neko（内置代理google）
- google（可免费使用）
- openai
- siliconflow（有免费额度）
- groq（可免费使用）

如果你是中国大陆用户并且不会代理，那么你可以使用neko或siliconflow哦
## 获取Key
想要获取Key很简单哦，根据你使用模型的不同，需要到不同网站获取key

### google和neko
你只需要一个谷歌帐号就可以直接免费获取到啦

首先打开[Google AI Studio](https://aistudio.google.com)，在这个页面点击`Get API key`按钮后呢，一步步往下操作就好啦

### openai
首先登陆[OpenAI管理面板](https://platform.openai.com/api-keys)，然后在这个页面点击`create new secret key`按钮，然后按照提示操作即可
### siliconflow
首先注册[硅基流动](https://cloud.siliconflow.cn/i/2ZR74wDe)，使用这个链接注册可以获得一些免费额度，然后在[密钥页面](https://cloud.siliconflow.cn/account/ak)创建API密钥即可
### groq
同样的，只需要一个邮箱就能免费获取到（注意：已知不支持outlook邮箱）
请先去[groq官网](https://console.groq.com)注册一个账号，然后去[Keys](https://console.groq.com/keys)页面获取key即可

## 模型设置
不同服务支持的模型有所不同，你可以在以下页面查看所有支持的模型
- [google和neko](https://ai.google.dev/gemini-api/docs/models/gemini?hl=zh-cn)
- [openai](https://platform.openai.com/docs/models)
- [siliconflow](https://cloud.siliconflow.cn/models)
- [groq](https://console.groq.com/docs/models)

### 推荐模型
这里给大家推荐以下几个模型
#### neko或google
- gemini-2.0-flash
- gemini-1.5-pro
#### openai
- gpt-4o-2024-08-06
- o3-mini-2025-01-31
#### siliconflow
- deepseek-ai/DeepSeek-R1
- deepseek-ai/DeepSeek-R1-Distill-Llama-70B
#### groq
- llama3-70b-8192
- deepseek-r1-distill-llama-70b

## 配置
在toNeko配置页面（模组菜单->toNeko->配置）或配置文件（config/toneko.yml）启用AI功能，并填入你的模型和密钥就好啦。

### 显示思考过程
这个功能是可选的，启用后会在游戏内显示思考过程（但并不会影响AI输出）

## 代理
这个功能是可选的，如果你无法访问外网，你可能需要用到。

如果你使用neko或siliconflow，那么在游戏内是不需要代理的。

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