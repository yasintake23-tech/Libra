# Libra R2 Worker

Libra'nın profil fotoğrafı ve ileride diğer medya dosyaları için Cloudflare R2 katmanıdır.

## Mimari

Android uygulaması Firebase Authentication ile kullanıcı kimliğini doğrular ve Firebase ID tokenını Worker'a gönderir.

Worker:
- Firebase ID tokenını doğrular.
- Kullanıcının sadece `users/{uid}/` altında nesne yazmasına/silmesine izin verir.
- Dosyayı R2'de `libra-media` bucket'ına yazar.
- `/media/*` üzerinden dosyayı yayınlar.
- `/health` üzerinden sağlık kontrolü verir.

R2 erişimi Worker'ın R2 binding'i üzerinden yapılır. Bu nedenle R2 Access Key ve Secret Key'i Android APK'ya koymak gerekmez. Cloudflare'ın güncel R2 Worker API'si, bucket binding ile `put/get/delete` işlemlerini doğrudan Worker içinde destekler. citeturn891706search0turn891706search5

## Kurulum

1. Cloudflare R2 içinde `libra-media` bucket'ını oluştur.
2. Bu klasörde:

```bash
npm install
```

3. Firebase Web API Key'i Worker secret olarak ekle:

```bash
npx wrangler secret put FIREBASE_WEB_API_KEY --name libra-r2-worker
```

Cloudflare, Worker secret'larının kaynak koda veya `vars` içine yazılmamasını ve `wrangler secret put` ile saklanmasını öneriyor. citeturn430417search3turn430417search10

4. Worker'ı deploy et:

```bash
npm run deploy
```

5. Deploy edilen Worker adresinin sağlık kontrolünü aç:

```text
https://<worker-adresi>/health
```

Beklenen cevap:

```json
{"service":"Libra R2 Worker","ok":true,"storage":"r2"}
```

6. Android tarafında `app/src/main/res/values/strings.xml` içindeki `r2_upload_endpoint` değerini Worker adresiyle doldur.

## GitHub Actions

`.github/workflows/cloudflare-r2-worker.yml` dosyası Worker kodunu her değişiklikte doğrular.

Deploy'un otomatik olması için repository secrets:
- `CLOUDFLARE_API_TOKEN`
- `CLOUDFLARE_ACCOUNT_ID`
- `GOOGLE_SERVICES_JSON_B64`

tanımlanmalıdır.

Workflow, mevcut Firebase yapılandırmasından Web API Key'i çıkarıp Worker'a secret olarak yükler ve ardından Worker'ı deploy eder.

R2 bucket binding'i `wrangler.toml` içinde `libra-media` olarak sabittir. citeturn891706search8
