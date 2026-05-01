# n11-clone

n11 & Patika.dev Java Spring Bootcamp bitirme projesi icin mikroservis tabanli e-ticaret backend uygulamasi.

## Servisler

| Servis | Port | Gorev |
| --- | ---: | --- |
| config-server | 8762 | Merkezi konfigurasyon |
| discovery-server | 8761 | Eureka service discovery |
| api-gateway | 8763 | Gateway, JWT auth, Swagger aggregation |
| user-service | 8766 | Kullanici kayit ve profil islemleri |
| product-service | 8764 | Urun katalog islemleri |
| shopping-cart-service | 8765 | Sepet islemleri |
| order-service | 8767 | Siparis ve saga orchestration |
| payment-service | 8768 | Mock odeme servisi |
| stock-service | 8769 | Stok rezervasyon, release ve commit |

## Calistirma

Servis imajlarini build edip tum sistemi Docker Compose ile kaldir:

```bash
mvn -pl config-server,discovery-server,api-gateway,user-service,product-service,shopping-cart-service,order-service,stock-service,payment-service jib:dockerBuild
docker compose up -d
```

Gateway: `http://localhost:8763`

Swagger UI: `http://localhost:8763/swagger-ui.html`

## Onemli ortam degiskenleri

| Degisken | Aciklama |
| --- | --- |
| `JWT_SECRET` | Gateway JWT imza secret'i |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Postgres kullanici bilgileri |
| `RABBIT_USER` / `RABBIT_PASSWORD` | RabbitMQ kullanici bilgileri |
| `PAYMENT_MOCK_FORCE_FAILURE` | `true` ise payment-service odemeleri reddeder |

## Siparis akisi

1. `order-service` siparisi `CREATED` olarak kaydeder.
2. Transaction commit olduktan sonra RabbitMQ uzerinden `StockReserveRequestedEvent` yayinlar.
3. `stock-service` stogu `reserve` eder ve sonucu order servisine event olarak yollar.
4. Stok ayrildiysa `order-service` payment-service'e odeme istegi atar.
5. Odeme basariliysa stok `commit`, siparis `COMPLETED` olur.
6. Odeme basarisizsa stok `release`, siparis `CANCELLED` olur.

## Auth

Yeni kullanici kaydi: `POST /api/user/signup`

JWT login: `POST /api/auth/login`

Refresh token: `POST /api/auth/login/refresh`
