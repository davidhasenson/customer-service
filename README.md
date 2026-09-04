# Customer Service

Del av ett mikrotjänstsystem (3 tjänster totalt) för ett bokningssystem. Den här tjänsten äger allt som rör **kunder**: registrering, inloggning och kunduppgifter.

Övriga tjänster i systemet:
- [`pensionat-app`](../pensionat-app) – rum och bokningar (port 8083).
- [`review-service`](../review-service) – recensioner av rum (port 8082).

## Vad tjänsten gör

- **Kundregister** – skapa, hämta, uppdatera och ta bort kunder (`/api/customers`).
- **Autentisering** – inloggning via JWT (`/auth/login`). Tjänsten hashar lösenord med BCrypt och validerar inloggning mot databasen.
- **Behörighetskontroll** – en inloggad kund kan bara se/ändra/ta bort sin *egen* kundpost (jämförs mot username i JWT-token).
- **Datalagring** – kunddata sparas i en egen MySQL-databas (`customer_db`).
- **Seed-data** – vid tom databas seedas 10 testkunder automatiskt (Härskarringen-tema) med lösenordet `password123`.

### Endpoints i korthet

| Metod | Path | Beskrivning | Kräver JWT |
|---|---|---|---|
| POST | `/auth/login` | Loggar in och returnerar en JWT-token | Nej |
| POST | `/api/customers` | Registrerar en ny kund | Nej |
| GET | `/api/customers` | Hämtar alla kunder | Ja |
| GET | `/api/customers/{id}` | Hämtar en kund via ID | Ja (måste vara egen kund) |
| GET | `/api/customers/by-email?email=` | Hämtar en kund via e-post | Ja (måste vara egen kund) |
| PUT | `/api/customers/{id}` | Uppdaterar en kund via ID | Ja (måste vara egen kund) |
| PUT | `/api/customers/email/{email}` | Uppdaterar en kund via e-post | Ja (måste vara egen kund) |
| DELETE | `/api/customers/{id}` | Tar bort en kund via ID | Ja (måste vara egen kund) |
| DELETE | `/api/customers/email/{email}` | Tar bort en kund via e-post | Ja (måste vara egen kund) |

## Hur tjänsterna pratar med varandra

- **JWT-baserad autentisering**: `customer-service` genererar JWT-tokens vid inloggning. Andra tjänster (t.ex. `booking-service`) kan verifiera samma token eftersom den signeras med en delad `JWT_SECRET`. Klienten skickar token i `Authorization: Bearer <token>`-headern på alla efterföljande anrop.
- **Anrop till booking-service**: Innan en kund tas bort måste `customer-service` kontrollera och koppla bort ev. bokningar hos `booking-service`, via REST-anrop (`RestTemplate`):
  - `GET {BOOKING_SERVICE_URL}/api/bookings/active-bookings/{customerId}` – kollar om kunden har aktiva bokningar. Finns det aktiva bokningar avbryts borttagningen (`400 Bad Request`).
  - `POST {BOOKING_SERVICE_URL}/api/bookings/unlink-bookings/{customerId}` – kopplar loss ev. historiska bokningar från kunden innan den tas bort.
  - Den inkommande JWT-token vidarebefordras till `pensionat-app` i dessa anrop, så att bokningstjänsten kan lita på anropet.
  - Om `pensionat-app` inte går att nå returneras `503 Service Unavailable`.
- **Anrop från pensionat-app**: `pensionat-app` anropar i sin tur tillbaka till `customer-service` (`GET /api/customers/by-email`, `GET /api/customers/{id}`) för att slå upp kunduppgifter när en bokning skapas eller visas, med samma JWT-token vidarebefordrad.
- Tjänsten är alltså både **konsument** och **producent** gentemot `pensionat-app`: den anropar bokningstjänsten vid kundborttagning, och blir själv anropad av bokningstjänsten vid uppslag av kunduppgifter. Utöver detta är den **producent** av inloggning/identitet (JWT) som alla tjänster litar på.

## Konfiguration (miljövariabler)

Sätts via `.env`-fil i repo-roten (används av `docker-compose.yml`):

```
DB_URL=jdbc:mysql://localhost:3306/customer_db
DB_USERNAME=root
DB_PASSWORD=<ditt-db-lösenord>
JWT_SECRET=<delad hemlighet, samma i alla tjänster som ska verifiera tokens>
BOOKING_SERVICE_URL=http://pensionat-app:8083   # url till booking-service
```

> **Obs:** `JWT_SECRET` måste vara identisk i alla tjänster som ska kunna verifiera inloggade användare, annars misslyckas token-valideringen.

## Starta tjänsten

### Med Docker Compose (rekommenderas)

1. Skapa en `.env`-fil i repo-roten enligt konfigurationen ovan.
2. Bygg och starta:

   ```bash
   docker compose up --build
   ```

3. Tjänsten startar på **http://localhost:8081** och väntar på att `customer-db` (MySQL) blir healthy innan den startar.

För att stoppa:

```bash
docker compose down
```

Lägg till `-v` om du även vill rensa databasvolymen och seed-datan skapas på nytt vid nästa uppstart:

```bash
docker compose down -v
```

### Lokalt utan Docker

Kräver Java 17, Maven och en lokal MySQL-instans.

```bash
./mvnw spring-boot:run
```

Se till att motsvarande miljövariabler (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `BOOKING_SERVICE_URL`) finns tillgängliga, t.ex. via en `.env`-fil (läses automatiskt med `dotenv-java`).

## Starta hela systemet

Det här repot innehåller endast `customer-service` + sin egen databas. För att köra hela systemet tillsammans med `pensionat-app` behövs en gemensam `docker-compose.yml` på systemnivå som:

- Startar båda tjänsterna och deras respektive MySQL-databaser.
- Sätter `BOOKING_SERVICE_URL=http://pensionat-app:8083` för `customer-service`.
- Sätter `CUSTOMER_SERVICE_BASE_URL=http://customer-service:8081` för `pensionat-app`.
- Använder samma `JWT_SECRET` i båda tjänsterna, så att tokens kan verifieras oavsett vilken tjänst som tog emot dem.

Systemet innehåller även `review-service` (recensioner, port 8082), som bara behöver samma `JWT_SECRET` och en egen databas — den gör inga anrop till vare sig `customer-service` eller `pensionat-app`.

## Teknisk stack

- Java 17, Spring Boot (Web, Data JPA, Security, Validation)
- MySQL 8
- JWT (jjwt) för autentisering
- Docker multi-stage build (Alpine + Eclipse Temurin)
- Kubernetes-manifest finns också inkluderade (`Deployment`/`Service` för både tjänsten och databasen)
