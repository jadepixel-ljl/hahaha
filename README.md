# Personal Site

This repo contains a Vue + Spring Boot personal-space app with:

- admin and normal user roles
- unique account registration, password login, simple captcha, nickname, avatar upload
- profile editing
- text and image posts
- one like per user per post, with unlike support
- comments and nested replies
- normal-user delete limits for owned posts and comments
- admin moderation for accounts, bans, posts, and comments
- MySQL tables for users, posts, likes, and comments

## Local Frontend

```powershell
cd D:\personal-site\frontend
npm.cmd install
npm.cmd run dev
```

The Vite dev server proxies `/api` and `/uploads` to `http://127.0.0.1:8080`.

## Local Backend

Install JDK 17+ and Maven, then configure MySQL:

```powershell
cd D:\personal-site\backend
mvn spring-boot:run
```

Useful environment variables:

```env
DB_URL=jdbc:mysql://localhost:3306/personal_site?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=
APP_TOKEN_SECRET=change-this-secret
APP_ADMIN_USERNAME=admin
APP_ADMIN_PASSWORD=change-this-admin-password
APP_ADMIN_NICKNAME=admin
APP_UPLOAD_DIR=uploads
```

## Database

The schema is in `backend/schema.sql`. Spring JPA is also set to `ddl-auto: update` for development.

Core tables:

- `users`
- `posts`
- `post_likes`
- `comments`

## Production Notes

The provided Nginx config serves the frontend on `8080`, proxies `/api` and `/uploads` to Spring Boot on `127.0.0.1:8081`, and keeps Open WebUI on `127.0.0.1:3000`.

Run the backend with:

```env
SERVER_PORT=8081
```

The AI entry remains a separate Open WebUI service.
