# toNeko AI Settings
[Simplified Chinese](https://github.com/CSneko/toNeko/blob/main/docs/AI.md) | [English](https://github.com/CSneko/toNeko/blob/main/docs/AI_en.md)

## Service Settings
The following services are currently supported:
- **elefant** (Simplest, recommended for beginners)
- **neko** (Built-in Google proxy)
- **google** (Free usage available)
- **openai**
- **siliconflow** (Includes free tier)
- **groq** (Free usage available)
- **Other Services**

If you are in Mainland China and unable to set up a proxy, you can use **elefant**, **neko**, or **siliconflow**.

### Other Services
If the service uses the OpenAI-compatible API format (typically with endpoints like `https://api.xxx.com/v1/chat/completions`), simply enter the endpoint URL.

---

## How to Use elefant
**Elefant** is the easiest option for beginners.

1. Visit the [elefant Website](https://elefant.gg/), download and launch the client.
2. In-game, enable AI in the toNeko configuration menu and set the service to `elefant`.  
   Alternatively, run these commands:  
```
/tonekoadmin config set ai.enable true
/tonekoadmin config set ai.service elefant
```

*If the command not work, try running `/tonekoadmin config set ai.service "http://localhost:4315/v1/chat/completions"` instead or running `/tonekoadmin config reload` to reload*

---

## Obtaining API Keys
### **google** and **neko**
1. Sign in with a Google account at [Google AI Studio](https://aistudio.google.com).
2. Click `Get API Key` and follow the instructions.

### **openai**
1. Log in to the [OpenAI Dashboard](https://platform.openai.com/api-keys).
2. Click `Create new secret key` and follow the prompts.

### **siliconflow**
1. Register via [SiliconFlow](https://cloud.siliconflow.cn/i/2ZR74wDe) (this link provides free credits).
2. Create an API key on the [API Keys Page](https://cloud.siliconflow.cn/account/ak).

### **groq**
1. Sign up at [Groq Console](https://console.groq.com) (Note: Outlook emails are unsupported).
2. Obtain your key from the [Keys Page](https://console.groq.com/keys).

### **Other Services**
Refer to the respective service’s documentation.

---

## Model Settings
Supported models vary by service. Check the links below:
- **elefant**: Leave blank.
- **google/neko**: [Gemini Models](https://ai.google.dev/gemini-api/docs/models/gemini?hl=zh-cn)
- **openai**: [OpenAI Models](https://platform.openai.com/docs/models)
- **siliconflow**: [SiliconFlow Models](https://cloud.siliconflow.cn/models)
- **groq**: [Groq Models](https://console.groq.com/docs/models)

### Recommended Models
#### elefant
Leave blank.
#### neko/google
- `gemini-2.0-flash`
- `gemini-1.5-pro`
#### openai
- `gpt-4o-2024-08-06`
- `o3-mini-2025-01-31`
#### siliconflow
- `deepseek-ai/DeepSeek-R1`
- `deepseek-ai/DeepSeek-R1-Distill-Llama-70B`
#### groq
- `llama3-70b-8192`
- `deepseek-r1-distill-llama-70b`

---

## Configuration
Enable AI in the toNeko configuration menu (`Mod Menu > toNeko > Config`) or `config/toneko.yml`, then fill in your model and API key.

### Display Thinking Process
Optional. When enabled, the AI’s intermediate reasoning will be shown in-game (does not affect output).

---

## TTS
This feature is optional. AI will read text when enabled

### Enable
Open the toNeko configuration page in the game and set Enable TTS to true, or execute the following command
```
/tonekoadmin config set ai.tts.enable true
```
### How to use
Currently only supports elefant, so you can use it directly after opening the elefant client.
### Modify voice
You can use a browser to open `http://127.0.0.1:4315/v1/tts/voices`, then select one you like and copy the id, then modify the voice in the game.
### Notes
If you use multiplayer games, please also set tts on the client

---
## Proxy Setup (Optional)
Required if you cannot access external networks directly.

- **elefant**, **neko**, and **siliconflow** do not require a proxy in-game.
- For other services in Mainland China, configure a proxy.

### Proxy Clients
Use tools like [NekoRay](https://github.com/MatsuriDayo/nekoray) or [Clash Meta](https://github.com/MetaCubeX/mihomo/tree/Meta) to set up a proxy, then enter the proxy IP and port in the configuration.

#### Notes:
- Only **HTTP proxies** are supported.
- Proxies requiring authentication are currently unsupported.
- If using **Tun mode** in your proxy client, leave the proxy settings empty in-game.

---

## Prompt Customization
Customize prompts using placeholders:

| Placeholder         | Description              | Example Value         |  
|---------------------|--------------------------|-----------------------|  
| `%neko_name%`       | Neko's name              | `Ayame`, `cinamono`   |  
| `%neko_type%`       | Neko's type              | `Adventurer Neko`     |  
| `%neko_des%`        | Neko's description       | `An adventurous neko` |  
| `%neko_height%`     | Neko's height            | `1.50`                |  
| `%neko_moe_tags%`   | Neko's moe traits        | `Tsundere`, `Yuri`    |  
| `%player_name%`     | Player's name            | `Crystal_Neko`        |  
| `%player_is_owner%` | Is the player the owner? | `Yes`, `No`           |  
| `%player_is_neko%`  | Is the player a neko?    | `Yes`, `No`           |  
| `%world_time%`      | In-game time             | `Day`, `Night`        |  
| `%world_weather%`   | In-game weather          | `Sunny`, `Rainy`      |  