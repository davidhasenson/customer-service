# Pensionat App Infrastructure

Denna mapp innehåller Docker Compose-konfigurationen för att köra hela systemet med alla mikrotjänster och databaser lokalt.

## Konfiguration (.env)

Innan du startar systemet måste du flytta `docker-compose.yml` till den mapp där alla andra mikrotjänster ligger.

Du behöver även skapa en `.env`-fil i samma mapp som `docker-compose.yml`.

Skapa filen `.env` och lägg till följande variabler:

```env
DB_USERNAME=root
DB_PASSWORD=ditt_lösenord_här
JWT_SECRET=din_hemliga_jwt_nyckel_här
```

För att bygga och starta alla tjänster och databaser i bakgrunden, kör följande kommando i den mapp där `docker-compose.yml` ligger:

```bash
docker compose up -d --build
```

För att stoppa och ta bort alla körande containrar:

```bash
docker compose down
```
