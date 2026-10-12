# EcoNet - Step-by-Step Cloud Deployment Guide

This guide walks you through deploying **EcoNet** to the cloud so you have a **live public URL** and a **live cloud PostgreSQL database** for your MCA viva.

---

## Step 1: Set up Free Cloud PostgreSQL (Neon.tech)

1. Go to [https://neon.tech](https://neon.tech) and Sign up (free with GitHub or Google).
2. Click **Create Project**:
   - Name: `econet-cloud-db`
   - Region: Choose nearest (e.g., `Asia Pacific (Singapore)` or `US East`).
3. Once created, Neon will show your connection string:
   ```
   postgres://[user]:[password]@[host]/[dbname]?sslmode=require
   ```
4. Note your credentials:
   - **JDBC URL:** `jdbc:postgresql://[host]/[dbname]?sslmode=require`
   - **Username:** `[user]`
   - **Password:** `[password]`

---

## Step 2: Push your Code to GitHub

1. In your terminal on your laptop, initialize Git in the `api` folder:
   ```powershell
   cd "C:\Users\KIIT0001\OneDrive\Desktop\api"
   git init
   git add .
   git commit -m "feat: EcoNet cloud-native hyper-local telemetry backend"
   ```
2. Create a new repository on GitHub (e.g., `econet-backend`).
3. Link and push:
   ```powershell
   git remote add origin https://github.com/[YOUR_GITHUB_USERNAME]/econet-backend.git
   git branch -M main
   git push -u origin main
   ```

---

## Step 3: Deploy Free Web Service on Render.com

1. Go to [https://render.com](https://render.com) and log in.
2. Click **New +** $\rightarrow$ **Web Service**.
3. Select **Connect a repository** and choose your `econet-backend` repository.
4. Configure the service:
   - **Name:** `econet-cloud`
   - **Language / Environment:** `Docker` (Render will automatically detect your `Dockerfile`)
   - **Instance Type:** `Free`
5. Scroll down to **Environment Variables** and add:
   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://[your-neon-host]/[dbname]?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME`: `[your-neon-user]`
   - `SPRING_DATASOURCE_PASSWORD`: `[your-neon-password]`
   - `SPRING_JPA_HIBERNATE_DDL_AUTO`: `update`
6. Click **Deploy Web Service**!

Render will build your Docker container in ~2 minutes and provide you with a live URL:
👉 `https://econet-cloud.onrender.com`

---

## Step 4: Verify Your Live Cloud Endpoints

You can test these endpoints from your browser or mobile phone:

- **Cloud Health Probe:** `https://econet-cloud.onrender.com/api/environment/health`
- **Live Environmental Dashboard:** `https://econet-cloud.onrender.com/api/environment/dashboard`
- **Live Latest Readings:** `https://econet-cloud.onrender.com/api/environment/readings/live`
- **Active Alerts & Actions:** `https://econet-cloud.onrender.com/api/environment/alerts/active`
- **24-Hour Statistical Audit:** `https://econet-cloud.onrender.com/api/environment/report/daily`

---

## How to Explain This to Your Teachers

### To Cloud Ma'am:
> *"EcoNet is deployed as a cloud-native microservice on Render using multi-stage Docker containerization. It uses a cloud-managed PostgreSQL database on Neon. It features an automated background worker (`@Scheduled`) that continuously ingests hyper-local telemetry for Patia Eco-Zone across Air, Water, Soil, and Pollution domains, and automatically triggers event-driven advisories when critical thresholds are breached."*

### To DBMS Sir:
> *"The database follows 3NF normalization. It separates location masters, configurable threshold rules, indexed time-series sensor telemetry, and automated incident logs. We have database views (`v_live_zone_health`) for real-time aggregation, database triggers (`trg_check_threshold`) for automated violation capture, and composite indexes on `(zone_id, recorded_at)` for sub-millisecond query performance."*
