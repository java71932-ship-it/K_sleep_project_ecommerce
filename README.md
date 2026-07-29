# 🚀 KSleep Ecommerce — Render Deployment Guide (Hinglish)

Yeh project fully restructurre kar diya gaya hai render aur cloud deployment ke liye.

---

## 🛠 Project Mein Kiye Gaye Major Fixes (Summary)

1. **Subfolder Nesting Resolved**: Subfolder `demo/` se saare files root level (`/`) par move kar diye gaye hain taaki GitHub aur Render direct `pom.xml` aur `Dockerfile` detect kar sake.
2. **Hardcoded Secrets Removed**: `application.properties` se Gmail Passwords, Database Passwords aur JWT Secrets ko remove karke Environment Variables (`SPRING_DATASOURCE_URL`, `SPRING_MAIL_PASSWORD`, `JWT_SECRET`) mein convert kar diya gaya hai.
3. **Hardcoded `localhost:1234` Fixed**: `SecurityConfig.java` aur `Controllers.java` se hardcoded URLs ko clean kar diya gaya hai.
4. **Multi-Stage Dockerfile Added**: Optimized Dockerfile ready hai jo Maven build create karke Light-weight Java 17 runtime par app run karta hai.
5. **Render Blueprint Added**: `render.yaml` include kiya gaya hai for 1-click deployment.

---

## 📋 Render Par Deploy Karne Ka Step-by-Step Tarika

### Step 1: Changes Ko GitHub Par Push Karein

Aapne VS Code / Antigravity Terminal mein ye commands chalayein:

```bash
git add .
git commit -m "Fix project structure and prepare for Render deployment"
git push origin main
```

---

### Step 2: Render Par Web Service Create Karein

1. [Render Dashboard](https://dashboard.render.com/) par login karein.
2. **New +** button par click karein aur **Web Service** select karein.
3. Apna GitHub Repository (`Ecommerce`) connect karein.
4. Settings fill karein:
   - **Name**: `ksleep-ecommerce`
   - **Environment**: `Docker`
   - **Region**: `Singapore` (ya nearest)
   - **Branch**: `main`
   - **Dockerfile Path**: `./Dockerfile`

---

### Step 3: Environment Variables Set Karein (Render Dashboard)

Render Web Service ke **Environment** tab mein niche diye gaye Environment Variables add karein:

| Key | Value Example / Details |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://<your-db-host>:3306/<db_name>` (Aap Aiven/CleverCloud/Supabase Database URL use kar sakte hain) |
| `SPRING_DATASOURCE_USERNAME` | `<your-db-username>` |
| `SPRING_DATASOURCE_PASSWORD` | `<your-db-password>` |
| `SPRING_MAIL_USERNAME` | `rockh7189@gmail.com` |
| `SPRING_MAIL_PASSWORD` | `<your-gmail-app-password>` (16 digit Gmail App Password) |
| `JWT_SECRET` | `<your-secret-key-32-chars-minimum>` |
| `PORT` | `8080` |

---

### Step 4: Deploy Button Par Click Karein

**Deploy Web Service** par click karein. Render automatically Docker build start karega aur aapko public URL (e.g. `https://ksleep-ecommerce.onrender.com`) de dega! 🎉
