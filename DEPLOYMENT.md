# Deployment Guide

This project is easiest to deploy to Railway as a Docker-based Java service.

## What is already deployment-ready

- The app reads the port from the `PORT` environment variable.
- The server starts with `com.fooddelivery.app.OnlineFoodDeliveryTracker`.
- Static HTML, CSS, and JavaScript are served directly by the Java app.

## Important limitation

The project stores users and orders in local serialized files under `data/`:

- `data/users-data.ser`
- `data/orders-data.ser`

That means:

- Deploying to hosts with ephemeral storage will reset data when the app restarts or redeploys.
- For real production usage, move persistence to MySQL, PostgreSQL, or another external database.
- For demos, this setup is fine if the host supports persistent disk or volume mounting.

## Option 1: Run with Docker locally

Build the image:

```powershell
docker build -t online-food-delivery-tracker .
```

Run the container:

```powershell
docker run -p 8080:8080 -e PORT=8080 online-food-delivery-tracker
```

Open:

```text
http://localhost:8080
```

## Option 2: Deploy to Railway

Railway will use the root `Dockerfile` automatically, and this repo includes `railway.json` for deploy settings.

### Deploy from GitHub

1. Push this project to GitHub.
2. Sign in to Railway.
3. Create a new project from your GitHub repository.
4. Railway will detect the `Dockerfile` and build the service.
5. After deployment, open the generated Railway domain.

### Railway config included

- `railway.json` sets the builder to `DOCKERFILE`
- `railway.json` configures the healthcheck path to `/health`
- the app already listens on `PORT`

## If you want persistent data

Railway deployments without attached persistent storage can lose the contents of the `data/` folder after restart or redeploy.

For a demo, this is usually acceptable.

For longer-term use, either:

- move storage to MySQL or PostgreSQL
- or attach persistent storage if your Railway setup supports it

## Manual non-Docker startup

If your host gives a Java runtime directly, use:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
java -cp out com.fooddelivery.app.OnlineFoodDeliveryTracker
```
