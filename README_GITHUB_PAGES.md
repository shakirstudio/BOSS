# 🚀 BOSS Website - Launch & GitHub Pages Deployment Guide

Your official **BOSS (Business Operations Studio System)** launch website is 100% prepared for GitHub Pages, Netlify, Vercel, or custom domain deployment.

---

## Method 1: New Dedicated Repository for Website (Fastest - 2 Minutes)

1. Open [github.com](https://github.com) and click **"New repository"**.
2. Name it (for example: `boss-studio` or `boss-website`).
3. Make it **Public** and click **"Create repository"**.
4. Click on **"uploading an existing file"**.
5. Drag and drop these files from your computer:
   - `index.html` (from the `website` or `docs` folder)
   - `BOSS-app-debug.apk`
6. Click **"Commit changes"**.
7. Go to **Settings** (tab at the top) -> click **Pages** on the left menu.
8. Under **"Build and deployment"**:
   - Source: **Deploy from a branch**
   - Branch: **main** (or master), select folder **/ (root)**
   - Click **Save**.
9. In 1 minute, your website will be live at:
   `https://<your-username>.github.io/boss-website/`

---

## Method 2: Auto-Deploy via GitHub Actions (Pre-Configured)

We have configured:
- The `/docs` folder containing `index.html` and `BOSS-app-debug.apk`.
- The `.github/workflows/pages.yml` automated deployment workflow.

1. Push your project to GitHub using the **Export / Push to GitHub** option in AI Studio settings.
2. In your GitHub repository, click on **Settings** -> **Pages**.
3. Under **Build and deployment**:
   - Select **GitHub Actions** (it will auto-deploy the `/docs` folder on every push!).
   - Or select **Deploy from a branch**, choose **main**, and select folder **/docs**.
4. Click **Save**. Your site will be published live with free SSL.
