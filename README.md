# Personal Site

这是一个清甜、软萌的小羊主题个人主站。主页作为正式个人网站使用，AI 对话不再嵌入主页，而是跳转到独立的 Open WebUI 服务。

## 线上地址

- 主站：`http://110.42.218.171:8080`
- AI 对话：`http://110.42.218.171:8010`
- AI 备用入口：`http://ai.110.42.218.171.sslip.io:8010/auth`
- 本地开发：`http://127.0.0.1:5174`

当前服务器通过 Nginx 监听 `8080` 和 `8010`：

- 默认 Host/IP 访问主站静态文件：`/var/www/personal-site`
- `ai.110.42.218.171.sslip.io` 访问 Open WebUI，反向代理到 `127.0.0.1:3000`
- `110.42.218.171:8010` 直接访问 Open WebUI，反向代理到 `127.0.0.1:3000`
- `/auth` 等 Open WebUI 必要路径也会代理到 `127.0.0.1:3000`，作为同 IP 备用入口。

## 目录结构

- `frontend/` - Vue 3 + TypeScript + Vite 主站页面。
- `backend/` - Spring Boot + MySQL 后端骨架，后续可扩展内容接口。
- `openwebui/` - 本地 Docker Compose 配置示例。
- `deploy/` - 服务器 Nginx、Open WebUI systemd 部署配置。

## 本地开发

```powershell
cd frontend
npm.cmd install
npm.cmd run dev -- --host 127.0.0.1 --port 5174
```

生产构建使用 `frontend/.env.production`，AI 地址指向服务器 Open WebUI。

```powershell
cd frontend
npm.cmd run build
```

## 服务器部署状态

服务器系统：Ubuntu 24.04，用户：`ubuntu`。

主站文件：

```bash
/var/www/personal-site
```

Open WebUI：

```bash
/opt/personal-site/openwebui
/etc/systemd/system/openwebui.service
```

Nginx 配置：

```bash
/etc/nginx/sites-available/personal-site-8080
/etc/nginx/sites-enabled/personal-site-8080
```

常用检查命令：

```bash
sudo systemctl status openwebui
sudo journalctl -u openwebui -f
sudo nginx -t
sudo systemctl reload nginx
```

## Open WebUI 配置

服务器使用官方 Python 安装方式运行 Open WebUI，因为 Docker 镜像仓库在当前网络下容易超时。Open WebUI 仍然是官方开源项目，只是运行方式不是容器。

敏感配置放在：

```bash
/opt/personal-site/openwebui/.env
```

首次访问 AI 地址后创建管理员账号。创建完成后建议把 `.env` 中的：

```env
ENABLE_SIGNUP=false
```

然后重启：

```bash
sudo systemctl restart openwebui
```

配置模型 Key 时，编辑服务器 `.env`：

```env
ENABLE_OPENAI_API=true
OPENAI_API_BASE_URL=https://api.openai.com/v1
OPENAI_API_BASE_URLS=https://api.openai.com/v1
OPENAI_API_KEY=你的站长统一APIKey
OPENAI_API_KEYS=你的站长统一APIKey
RAG_OPENAI_API_BASE_URL=https://api.openai.com/v1
RAG_OPENAI_API_KEY=你的站长统一APIKey
```

OpenAI-compatible 的 base URL 一定要包含 `/v1`，否则 Open WebUI 可能取不到模型列表。

当前还配置了：

```env
RAG_EMBEDDING_ENGINE=openai
RAG_EMBEDDING_MODEL=text-embedding-3-small
RAG_EMBEDDING_MODEL_AUTO_UPDATE=false
HF_HUB_OFFLINE=1
```

这是为了避免服务器首启时访问 HuggingFace 下载本地 embedding 模型。

## 本地 Docker 方案

如果后续本机 Docker Desktop 可用，也可以使用：

```powershell
cd openwebui
Copy-Item .env.example .env
docker compose up -d
```

本地 Open WebUI 默认地址是 `http://127.0.0.1:3000`。

## 注意

- 不要把真实 API Key 提交到仓库。
- 主站只保存 Open WebUI 入口地址，不保存 API Key。
- AI 页面不再嵌 iframe，而是打开独立 Open WebUI 页面。
- `sslip.io` 是临时解析域名；正式上线建议绑定自己的域名，再把 Nginx 的 AI `server_name` 改成正式子域名。
