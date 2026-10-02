# Customer Service

Del av ett mikrotjänstsystem (3 tjänster totalt) för ett bokningssystem. Den här tjänsten äger allt som rör **kunder**: registrering, inloggning och kunduppgifter.

Övriga tjänster i systemet:
- [`pensionat-app`](../pensionat-app) – rum och bokningar (port 8083).
- [`review-service`](../review-service) – recensioner av rum (port 8082).

## Deployad tjänst

| Miljö | URL | Health check |
|---|---|---|
| Production | https://customer-service-production-1ad5.up.railway.app | [/actuator/health](https://customer-service-production-1ad5.up.railway.app/actuator/health) |
| Staging | https://customer-service-staging-3344.up.railway.app | [/actuator/health](https://customer-service-staging-3344.up.railway.app/actuator/health) |

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

## GitHub Flow & Branching Strategy

### Syfte

Projektet använder GitHub Flow som arbetsflöde för versionshantering och samarbete. Målet är att säkerställa att alla ändringar kan granskas och testas innan de integreras i `main`.

Direkta commits till `main` ska undvikas. All utveckling sker i separata branches och integreras genom Pull Requests.

### Motivering till val av branch-strategi

Vi har valt **GitHub Flow** som branch-strategi för detta projekt, framför alternativ som Git Flow. Detta baseras på följande faktorer:

**Redan etablerat arbetssätt i gruppen**
Gruppens medlemmar har sedan tidigare kurser (bl.a. Backend 1 och Backend 2) mer eller mindre redan arbetat enligt principerna i GitHub Flow, utan att uttryckligen ha namngett eller definierat det som en formell strategi. Att fortsätta med samma grundläggande arbetssätt, nu med tydligare struktur och namngivningsstandard, gjorde övergången enkel och naturlig för hela gruppen.

**Liten grupp och kort projekttid**
Vi är tre utvecklare som arbetar tillsammans under en begränsad tidsperiod. GitHub Flow är enkelt att förstå och kräver inte lika mycket administration som Git Flow, som normalt innehåller flera permanenta branches (t.ex. `develop`, `release`, `hotfix`) utöver `main`. För ett projekt av vår storlek skulle den extra komplexiteten inte ge något mervärde, utan bara öka risken för missförstånd i gruppen.

**Kontinuerlig leverans till staging och production**
Vårt deploymentflöde bygger på att samma Docker-image ska kunna verifieras i staging och sedan släppas till production (build once, deploy multiple times). GitHub Flow passar naturligt ihop med detta arbetssätt, eftersom varje merge till `main` automatiskt kan triggra en ny build och deployment till staging, utan att behöva vänta på en separat release-branch.

**Enkel spårbarhet**
Med en enda långlivad branch (`main`) och korta feature-branches blir det tydligt vilken kod som är aktuell. Alla ändringar går via review och Pull Requests, vilket säkerställer kodgranskning och att CI-kontroller körs innan något når `main`.

**Snabb feedback**
Eftersom branches är kortlivade och mergas in ofta, minskar risken för stora och svårlösta merge-konflikter jämfört med om utveckling hade skett i långlivade parallella branches under en längre tid.

Sammanfattningsvis passar GitHub Flow gruppens redan etablerade arbetssätt, projektets storlek och tidsram, samt behovet av kontinuerlig integration och leverans.

### GitHub Projects

Projektets tickets hanteras i en **GitHub Project** som används som Kanban-tavla. Tavlan används för att planera arbetet och följa en ticket från planering till färdig implementation.

En ticket följer normalt detta flöde:

```text
Backlog
   │
   ▼
Ready
   │
   ▼
In Progress
   │
   ▼
In Review
   │
   ▼
Done
```

**Backlog** innehåller tickets som är planerade men ännu inte redo att påbörjas.

**Ready** innehåller tickets som är definierade och redo att tas upp för utveckling.

**In Progress** används när arbetet med ticketen har påbörjats och en branch har skapats.

**In Review** används när implementationen är klar och en Pull Request har skapats mot `main`. Ticketen stannar här under code review, automatiska tester och eventuell komplettering.

**Done** används när Pull Requesten är godkänd, mergad till `main` och arbetet är färdigställt.

Kopplingen mellan Kanban-tavlan och GitHub Flow kan illustreras så här:

```text
Backlog
   │
   ▼
Ready
   │
   │ Create branch
   ▼
In Progress
   │
   │ Develop / Commit / Push
   ▼
In Review
   │
   │ Pull Request
   │ Code review
   │ Automated tests
   ▼
Done
   │
   │ Merge
   ▼
main
```

På så sätt används GitHub Project för att följa statusen på arbetet, medan GitHub Flow hanterar hur ändringen utvecklas och integreras i `main`.

### Utvecklingsflöde

Utveckling följer normalt detta flöde:

```text
main
 │
 ├── Create branch
 │
 ▼
feature/105-user-service
 │
 ├── Develop
 ├── Commit
 └── Push
 │
 ▼
Pull Request
 │
 ├── Code review
 ├── Automated tests
 └── Validation
 │
 ▼
main
 │
 └── Merge
```

Direkta commits till `main` är inte tillåtna. All förändring ska gå via en Pull Request.

### Skyddad huvudbranch

Huvudbranchen (`master` i detta repo, motsvarar `main` i texten ovan) är skyddad med branch protection:

* Pull Request krävs, ingen kan pusha direkt.
* Minst 1 godkännande från en annan gruppmedlem.
* CI måste vara grön innan merge.

**1. Synkronisera med `main`**

Hämta den senaste versionen innan en ny branch skapas.

```bash
git switch main
git pull origin main
```

**2. Skapa en branch**

Skapa en branch från den senaste versionen av `main` med ett namn som följer projektets namngivningsstandard.

```bash
git switch -c docs/112-update-readme-with-workflow-description
```

**3. Implementera och committa**

Gör ändringarna i den nya branchen. Commits ska vara tydliga och beskriva vad som har ändrats.

```bash
git add .
git commit -m "Update README with team workflow"
```

**4. Pusha branchen**

Publicera branchen på GitHub.

```bash
git push -u origin docs/112-update-readme-with-workflow-description
```

**5. Skapa en Pull Request**

Skapa en Pull Request från branchen mot `main`.

Pull Requesten ska normalt innehålla:

* En tydlig beskrivning med syftet av förändringen
* Referens till relevant ticket/issue
* Tester eller annan verifiering
* Eventuella övriga ändringar som reviewers behöver känna till

För att koppla Pull Requesten till rätt ticket kan ticket-numret anges i PR-beskrivningen:

```text
Ticket: #112
```

Där `#112` ersätts med numret på den aktuella ticketen. Pull Requesten får ett eget nummer av GitHub, vilket innebär att PR-numret och ticket-numret kan vara olika.

**6. Granskning och tester**

Innan mergning ska ändringarna granskas av en annan utvecklare. Relevanta tester och automatiska kontroller ska passera enligt projektets krav.

**7. Merge och städning**

När Pull Requesten är godkänd mergas den till `main`. Branchen kan därefter tas bort.

### Branch-namngivning

Samtliga branches ska följa formatet:

```text
<kategori>/<ticket-id>-<beskrivning>
```

Exempel:

```text
docs/112-update-readme-with-workflow-description
```

Branch-namnet består av tre delar:

* **Kategori:** anger typen av arbete.
* **Ticket-ID:** identifierar den issue eller uppgift som arbetet tillhör.
* **Beskrivning:** sammanfattar ändringen med korta, beskrivande ord.

### Kategorier

| Kategori    | Beskrivning                                               | Exempel                                            |
| ----------- | --------------------------------------------------------- | -------------------------------------------------- |
| `feature/`  | Ny funktionalitet eller nya komponenter i Java/Spring     | `feature/105-user-service`                         |
| `fix/`      | Rättning av buggar i befintlig funktionalitet             | `fix/108-null-pointer-error`                       |
| `refactor/` | Förbättrad kodstruktur utan avsedd funktionell förändring | `refactor/110-update-repository`                   |
| `chore/`    | Underhåll, beroenden och konfiguration                    | `chore/111-upgrade-spring-boot`                    |
| `docs/`     | Dokumentation, README och Javadoc                         | `docs/112-update-readme-with-workflow-description` |
| `hotfix/`   | Akuta korrigeringar av produktionsproblem                 | `hotfix/115-security-token-leak`                   |

### Namngivningsregler

1. Branch-namn ska vara beskrivande och relevanta för uppgiften.
2. Kategorin ska skrivas med gemener och avslutas med `/`.
3. Ticket-numret ska placeras direkt efter kategorin.
4. Beskrivningen ska skrivas på engelska med gemener och bindestreck mellan orden.
5. Mellanslag och onödiga specialtecken ska undvikas.
6. En branch ska i normalfallet avse en avgränsad uppgift.
7. Branch-namnet ska följa formatet även när ticketen saknar ett separat projektnamn eller prefix.


## Hantering av merge-konflikt

Under arbetet skapades en medveten merge-konflikt i `.github/workflows/ci.yaml` (rad 83) för att öva på konflikthantering i Git, som ett samarbete mellan alla gruppmedlemmar.

**Orsak:**
En gruppmedlem skapade och mergade sin branch till `master` först. En annan gruppmedlems branch hade under tiden ändrat samma rad i samma fil, vilket gjorde att en konflikt uppstod när den branchen skulle mergas in, eftersom `master` redan innehöll den första ändringen.

**Lösning:**
En tredje gruppmedlem hjälpte till att lösa konflikten genom att merga in `master` i den konfliktande branchen och manuellt välja rätt version av den ändrade raden. Efter att ändringen pushats kördes CI-kontrollerna (GitHub Actions) igen för att säkerställa att allt fortfarande fungerade. När checkarna gått igenom godkändes PR:n av en gruppmedlem, och mergen in i `master` genomfördes sedan av en annan gruppmedlem — ingen mergade sin egen PR.
Detta visade praktiskt hur man löser en konflikt i samarbete, verifierar lösningen med CI, och säkerställer att fler än en person är involverad i granskning och merge innan en konfliktlösning går in i `master`.

---

## Deploymentflöde (Railway & GitHub Actions)

Tjänsten deployas till **Railway** i två separata miljöer:

- **Staging** – automatisk miljö för verifiering och integrationstestning vid varje godkänd ändring i `master`.
- **Production** – den skarpa miljön där tjänsten körs för användare.

Pipeline-strukturen bygger på principen **Build once, deploy multiple times**. En Docker-image byggs och publiceras en gång till **GitHub Container Registry (GHCR)**. Samma exakta image (identifierad via sitt **image digest**) deployas sedan till både Staging och Production på Railway. Miljöerna skiljer sig enbart åt genom sina miljövariabler (`RAILWAY_STAGING_TOKEN` vs `RAILWAY_PRODUCTION_TOKEN`, databaslänkar osv.).

### Pipeline-jobb (Separation of Concerns)

Workflowet i GitHub Actions är uppdelat i fyra fristående och modulariserade jobb:

- `build-and-test`: Sätter upp Java, startar en MySQL-servicecontainer, bygger applikationen med Maven och kör alla testerna.
- `push-to-container-registry`: Körs endast vid push/merge (ej vid PR). Bygger Docker-imagen, taggar den via `docker/metadata-action` och pushar den till GHCR. Jobbet genererar och skickar vidare ett unikt `image_digest.
- `deploy-staging`: Körs automatiskt vid push till `master`. Installerar Railway CLI och deployar imagen via `image_digest` till Staging-miljön på Railway.
- `deploy-production`: Körs manuellt via `workflow_dispatch` eller automatiskt vid skapande av en Git-tagg (`v*.*.*`). Deployar samma `image_digest` till Production-miljön på Railway.

### Översiktsdiagram över deploymentflödet

```text

┌─────────────────┐
│  Feature branch │
└────────┬────────┘
         │
         │ Pull Request
         ▼
┌─────────────────┐
│     master      │
└────────┬────────┘
         │
         │ Merge / Push
         ▼
┌─────────────────────────────────┐
│ GitHub Actions (build-and-test) │
│  - Maven build & JUnit tests    │
│  - MySQL service container      │
└────────┬────────────────────────┘
         │
         │ Success
         ▼
┌──────────────────────────────────┐
│  push-to-container-registry      │
│  - Build Docker image            │
│  - Push to GHCR                  │
│  - Output: image_digest          │
└────────┬─────────────────────────┘
         │
         │ Automatic deployment
         ▼
┌──────────────────────────────────┐
│ deploy-staging (Railway)         │
│  - Image: GHCR @ image_digest    │
│  - Env: Staging config           │
└────────┬─────────────────────────┘
         │
         │ Verify / Test
         ▼
┌──────────────────────────────────┐
│ Release trigger                  │
│  - workflow_dispatch (manuell)   │
│    eller                         │
│  - Git-tag (v*.*.*)              │
└────────┬─────────────────────────┘
         │
         ▼
┌──────────────────────────────────┐
│ deploy-production (Railway)      │
│  - Same image: GHCR @ digest     │
│  - Env: Production config        │
└──────────────────────────────────┘
```

### Steg för steg

#### 1. Integrationstest i PR & Merge till `master`

När en Pull Request skapas eller uppdateras triggas GitHub Actions och kör jobbet `build-and-test` för att säkerställa att koden kompilerar och att alla tester passerar mot MySQL. När PR:en godkänns och mergas till master startar hela CI/CD-flödet.

#### 2. Bygg och publicering till GHCR

Jobbet `push-to-container-registry` loggar in i GHCR med `GITHUB_TOKEN`, extraherar metadata och bygger en Docker-image. 
Bilden publiceras till GHCR och dess unika `image_digest` skickas som output till efterföljande deploy-jobb.

#### 3. Automatisk deployment till Staging

Så fort bilden finns i GHCR startar jobbet `deploy-staging`:

- Railway CLI installeras på runnern.
- Ett **Service Token** (`RAILWAY_STAGING_TOKEN`) används för autentisering.
- Kommandot `railway deploy` pekar ut tjänsten `customer-service` och laddar in bilden direkt via dess digest:

```bash
railway deploy \
  --service customer-service \
  --image ghcr.io//@sha256:...
```

#### 4. Deployment till production

När ändringen är verifierad i Staging deployas bilden till Production via antingen en Git-tagg eller en manuell körning i GitHub Actions:

Deployment till production sker via en explicit release-trigger:

* **Manuell trigger** med `workflow_dispatch`. Välj workflowet i GitHub Actions och klicka på **Run workflow**.
* **Git-tag** som representerar en release.

Exempel:

```bash
git tag v1.2.0
git push origin v1.2.0
```

Production-miljön på Railway uppdateras med exakt samma image_digest som staging via RAILWAY_PRODUCTION_TOKEN.

## Docker image-taggar

Docker images publiceras till **GitHub Container Registry (GHCR)** och identifieras med följande tags och identifierare:

```text
ghcr.io/<organisation>/<repository>:build142
ghcr.io/<organisation>/<repository>:sha-8f3a21c
ghcr.io/<organisation>/<repository>:v1.1.1
ghcr.io/<organisation>/<repository>:latest
ghcr.io/<organisation>/<repository>@sha256:abc123...
```

| Tag / Identifierare | Betydelse                                 |
| ------------------- | ----------------------------------------- |
| `sha-8f3a21c`       | Commit SHA som imagen byggdes från        |
| `latest`            | Huvudgrenen som imagen har byggts ifrån   |
| `v1.1.1`            | Semantisk releaseversion                  |
| `latest`            | Senaste publicerade image                 |
| `@sha256:...`       | Image digest som identifierar exakt image |

### Commit SHA

`${{ github.sha }}` används för att koppla Docker-imagen till den exakta Git commit som användes vid bygget.

Exempel:

```text
sha-8f3a21c
```

Det gör det möjligt att spåra exakt vilken kodversion som finns i en Docker-image.

### Release version

En semantisk releaseversion, exempelvis `v1.1.1`, används för att identifiera en officiell release av applikationen.

Exempel:

```text
v1.2.0
```

En releaseversion bör behandlas som **immutable**, vilket innebär att samma release-tag inte ska flyttas till en annanimage efter att releasen har skapats.

En releaseversion kan läggas till på en redan skapad image utan att imagen behöver byggas om.

### Latest & Branch-tags (`master`, `latest`)

`master` och `latest` används som enkla referenser till de senast publicerade imagen från huvudgrenen.

```text
ghcr.io/<organisation>/<repository>:master
ghcr.io/<organisation>/<repository>:latest
```

Dessa taggar ska **inte användas som deployment-referens** för staging eller production eftersom taggarna är rörliga och kan flyttas till en nyare image över tid.

### Image digest

Image digest används för att identifiera det exakta innehållet i en Docker-image.

Exempel:

```text
sha256:abc123...
```

En image kan refereras med sitt digest direkt i GHCR:

```text
ghcr.io/<organisation>/<repository>@sha256:abc123...
```

Det gör det möjligt att säkerställa att exakt samma Docker-image används vid deployment till Staging och Production. 
Till skillnad från en tagg som `latest` och `maste` pekar ett digest **alltid** på det specifika image-innehåll som identifierats av digestet.

### Deployment

Vid deployment används alltid **image digest** för att identifiera vilken Docker-image som ska deployas:

```text
ghcr.io/<organisation>/<repository>@sha256:abc123...
```

Samma image används sedan i både staging och production.

```text
                    GHCR
                      │
         ┌────────────┼────────────┼────────────┐
         │            │            │            │
         ▼            ▼            ▼            ▼
 @sha256:abc123... :sha-8f3a21c  :master      :latest
         │            │            │            │
         │            │            │            └── Latest image
         │            │            │
         │            │            └── Branch tag
         │            │
         │            └── Commit SHA
         │
         └── Image digest (Immutable)
         │
         ▼
      Staging
         │
         │ Verification
         ▼
    Release trigger
         │
         └── Tag: v1.2.0
         │
         ▼
     Production
         │
         └── Deployment: @sha256:abc123...

          
```

På så sätt kan samma Docker-image verifieras i staging och därefter deployas till production utan att imagen behöver byggas om. Image digest används alltid som deployment-referens.

Docker-tags kan fortfarande användas för att söka och identifiera images i GHCR. Följande identifierare kan alltså peka på en och samma image:

```text
:sha-8f3a21c
:master
:v1.2.0
:latest
@sha256:abc123...
```

Där sha-8f3a21c identifierar committen, master är källgrenen, v1.2.0 är releaseversionen, latest pekar på den senast publicerade imagen och @sha256:abc123... identifierar exakt image-innehåll och används vid faktiska deployments.