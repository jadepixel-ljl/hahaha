# Backend

Spring Boot + MySQL API for the personal-space app.

Implemented modules:

- captcha, registration, login, token auth
- admin and normal user roles
- account status and bans
- profile updates
- avatar and post image uploads
- posts
- one-like-per-user likes
- comments and nested replies
- admin user and content moderation

Run with JDK 17+ and Maven:

```powershell
cd D:\personal-site\backend
mvn spring-boot:run
```

Database schema: `schema.sql`
