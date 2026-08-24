# Docker Deployment Guide: GoDaddy VPS

This guide deploys the Playville CRM Spring Boot API and its MySQL database with
Docker Compose. The application listens on port `8080` and uses the `/api/v1`
context path, so the API base URL is `http://SERVER_IP:8080/api/v1` until a
domain and reverse proxy are configured.

The included Compose limits are intentionally sized for a small testing VPS:
the application is limited to `600 MB` and `0.5 CPU`, while MySQL is limited to
`500 MB` and `0.5 CPU`. Java uses a `384 MB` maximum heap. These limits leave
headroom for the three existing WordPress sites, the web server, and the host.

## 1. Prerequisites

- An Ubuntu VPS with a non-root sudo user.
- A DNS record pointing the intended domain to the VPS IP address.
- The repository available on the server, either by Git or a private artifact.
- At least 2 GB RAM recommended for the Java build and application.
- At least 1 GB swap recommended when sharing the VPS with other sites.

Install Docker using GoDaddy's supported Ubuntu image instructions or Docker's
official convenience script, then verify:

```bash
docker --version
docker compose version
```

Allow SSH and HTTP/HTTPS in the VPS firewall. Do not expose MySQL port `3306`.

Check the VPS before starting:

```bash
free -h
df -h
```

If swap is absent, create a 1 GB swap file before deployment:

```bash
sudo fallocate -l 1G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
```

## 2. Configure production secrets

From the repository directory on the VPS:

```bash
cp .env.example .env
nano .env
```

Replace every `replace-with-...` value. `PLAYVILLE_JWT_SECRET` must be a unique
random secret of at least 32 characters. Keep `.env` private; it is ignored by
Docker and must never be committed.

The Compose file connects the application to the database service as `db`, not
`127.0.0.1`. Flyway runs automatically when the application starts and applies
pending migrations.

## 3. Build and start

```bash
# Prefer building on a local machine or CI because this VPS has one CPU core.
docker compose build --pull
docker compose up -d
docker compose ps
docker compose logs -f app
```

Wait for a log message showing that Spring Boot has started. Verify the public
API documentation endpoint:

```bash
curl -i http://127.0.0.1:8080/api/v1/swagger-ui.html
```

The endpoint may redirect to the Swagger UI. The login endpoint is at
`/api/v1/auth/login`.

## 4. Updates and rollback

Pull the new code and recreate the application:

```bash
git pull
docker compose build --pull app
docker compose up -d app
docker compose logs --tail=200 app
```

Create a database backup before migrations or significant releases:

```bash
docker compose exec -T db mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" > backup-$(date +%F-%H%M).sql
```

The database is stored in the `mysql_data` Docker volume. Do not run
`docker compose down -v` on a live installation because it deletes that data.

Keep the previous image available for an application-only rollback:

```bash
docker image ls playville-crm
docker compose up -d app
```

If a migration has already changed the schema, restore the database backup and
follow the migration's documented rollback procedure before reverting code.

## 5. Reverse proxy and HTTPS

For production traffic, put Nginx or Caddy in front of the container and expose
only ports `80` and `443`. Proxy requests to `127.0.0.1:8080`, issue a Let's
Encrypt certificate, and set:

```dotenv
PLAYVILLE_PUBLIC_DISCLAIMER_URL=https://your-domain.example/public/disclaimer
```

The current backend CORS configuration permits the local Angular development
origins only. Before connecting a production browser frontend, add its exact
HTTPS origin to `SecurityConfig.corsConfigurationSource()` and redeploy. Do not
use a wildcard origin together with credentials.

## 6. Operations checklist

- Confirm `.env` permissions: `chmod 600 .env`.
- Confirm `docker compose ps` reports both services as running.
- Check logs after every deployment: `docker compose logs --tail=200 app`.
- Test login and one authenticated API request over HTTPS.
- Confirm Flyway completed without validation errors.
- Schedule off-server backups of the MySQL dump or volume contents.
- Monitor disk usage, especially Docker images and database backups.
- Monitor `docker stats`; if the app is repeatedly near `600 MB` or the host
	starts swapping heavily, stop the test deployment and move it to a larger VPS.