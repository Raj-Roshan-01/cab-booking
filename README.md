# 🚖 CabGo – Cab Booking Application (Java / Spring Boot)

Java 17 · Spring Boot 3 · Spring Security (JWT + Google OAuth2) · JPA/Hibernate · PostgreSQL (H2 for quick start) · STOMP WebSockets · Stripe Java SDK · iText 7 · JavaMailSender · Leaflet · Docker · GitHub Actions

## Run
```bash
mvn spring-boot:run            # http://localhost:8080  (in-memory H2, Stripe in demo mode)
mvn verify                     # unit + end-to-end tests
docker compose up --build      # PostgreSQL + app + Nginx on http://localhost
```
Admin login: `admin@cab.com / Admin@123` (change via `ADMIN_EMAIL`, `ADMIN_PASSWORD`). Promo `WELCOME10` is seeded.

### Optional configuration (env vars)
| Feature | Variables |
|---|---|
| PostgreSQL / Supabase | `DB_URL=jdbc:postgresql://host:5432/db`, `DB_USER`, `DB_PASSWORD` |
| JWT | `JWT_SECRET` (≥ 32 chars) |
| Stripe (real) | `STRIPE_SECRET_KEY`, `STRIPE_PUBLISHABLE_KEY`, `STRIPE_WEBHOOK_SECRET` – point the Stripe webhook at `/api/payments/webhook` (events `payment_intent.succeeded`, `payment_intent.payment_failed`). Without a key the app runs in **demo payment mode**. |
| Google login | `SPRING_PROFILES_ACTIVE=oauth` + `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` (redirect URI `/login/oauth2/code/google`) |
| E-mail receipts | `SPRING_PROFILES_ACTIVE=mail` + `MAIL_HOST`, `MAIL_USER`, `MAIL_PASSWORD` |

## Try it (3 browser tabs)
1. Tab A: register a **Driver**, click the map to set location, press *Go online*.
2. Tab B: register a **Rider**, click pickup then drop, *Book now*.
3. Tab A: *Accept* → *I've arrived* → *Start* → *Complete*. Tab B updates live; driver marker moves when the driver re-clicks the map.
4. Tab B: *Pay* → rate the driver → download PDF receipt. SOS button alerts the admin tab live.

## Feature → code map
| Feature in spec | Where |
|---|---|
| Rider/driver registration, roles, BCrypt, JWT | `AuthService`, `JwtService`, `JwtFilter`, `SecurityConfig` |
| Google OAuth2 social login | `SecurityConfig` (oauth2Login) + `AuthService.oauthLogin` |
| Ride booking + fare calc + promo codes | `RideService.book`, `FareService` |
| Nearby-driver matching, accept/reject | `RideService.matchDrivers/accept/reject` (optimistic lock → one winner) |
| Driver dashboard (requests, earnings, history, ratings) | `DriverController`, `index.html` |
| Maps: location pick, route, distance, ETA, live tracking | Leaflet in `index.html`, `FareService`, `/topic/ride/{id}/location` |
| WebSockets: status, arrival, start/end, SOS | `WebSocketConfig`, `LiveController` (`@MessageMapping`/`@SendTo`), `Notifier` |
| Stripe intent → confirm → webhook → receipt | `PaymentService`, `PaymentController` |
| Ratings, average, driver ranking | `RatingService` (`/api/drivers/ranking`) |
| Ride history, iText PDF invoice, e-mail receipt | `RideController`, `ReceiptService` |
| Tables users, drivers, rides, payments, ratings, promo_codes, support_tickets | `model/*` |
| Admin monitoring, promos, tickets | `SupportAdminController` |

## Two-week plan → what was delivered
| Day | Plan | Delivered |
|---|---|---|
| 1–2 | Spring Boot setup, PostgreSQL, authentication | `pom.xml`, `application.yml`, security package, `AuthService` |
| 3 | Entities + JPA repositories | `model/`, `repo/` |
| 4 | Booking APIs, fare logic | `RideController`, `FareService` |
| 5 | Driver matching | `matchDrivers`, `pendingFor`, accept/reject |
| 6 | WebSocket real-time | `ws/` package |
| 7 | Testing & debugging | `FareServiceTest`, `CabFlowTest` |
| 8 | Stripe | `PaymentService` |
| 9 | History & receipts | `ReceiptService` (iText + mail) |
| 10 | Ratings | `RatingService` |
| 11 | Security hardening | JWT-checked STOMP, admin-only SOS topic, rate-limit filter, validation, security headers, role-based routes, ownership checks |
| 12 | Performance | DB indexes, `@Cacheable` ranking, pagination, Hikari pool, JDBC batching (add `spring-boot-starter-data-redis` + `spring.cache.type=redis` to move the cache to Redis) |
| 13 | Final testing | end-to-end test of the entire lifecycle |
| 14 | Dockerize & deploy | `Dockerfile`, `docker-compose.yml`, `nginx.conf`, `.github/workflows/ci.yml` (deploy image to Render/Railway/AWS) |

## Known limitations
- Distance is Haversine × 1.3 (no Google key needed); swap `FareService.distanceKm` for Google Distance Matrix/OSRM if desired.
- Per-ride STOMP topics are not ownership-checked (only CONNECT auth and the admin-only SOS topic are).
- Kafka and Redis are optional in the spec and not wired in.
- Frontend is a single static page (Leaflet + Tailwind CDN + vanilla JS), not a React build.

## React frontend (`frontend/`)
Vite + React 18 + Tailwind + react-router (HashRouter) + react-leaflet + STOMP/SockJS + Stripe Elements.
```
src/api.js            fetch wrapper, receipt download, useWs() STOMP hook
src/AuthContext.jsx   login/register/logout, Google-OAuth token pickup
src/components/       MapView, PayBox (Stripe Elements / demo), Toast, ui
src/pages/            Login, Rider, Driver, Admin (role-guarded routes)
```
- **Dev (hot reload):** run the backend, then `cd frontend && npm install && npm run dev` → http://localhost:5173 (proxies `/api`, `/ws`, `/oauth2` to :8080).
- **Production:** `cd frontend && npm run build` writes into `src/main/resources/static`, replacing the old single-page UI; then `mvn package` serves everything from one jar. The Dockerfile and CI do this automatically.
