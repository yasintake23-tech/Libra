# Libra R2 Worker

Bu Worker, Libra Android uygulamasinin profil fotografi ve ileride video gibi kullanici medyalarini Cloudflare R2'ye yuklemek icin kullanilir.

## Kurulum

1. Cloudflare R2'de libra-media adinda bucket olusturun.
2. Bu klasorde npm install calistirin.
3. Firebase Web API Key'i Worker secret olarak ekleyin:
   npx wrangler secret put FIREBASE_WEB_API_KEY
4. Worker'i deploy edin:
   npm run deploy
5. Deploy edilen Worker adresini Android projesindeki
   app/src/main/res/values/strings.xml
   icindeki r2_upload_endpoint alanina yazin.

## Guvenlik

R2 Access Key ve Secret Key APK icine konmaz. Android, Firebase ID tokenini Worker'a gonderir. Worker Firebase Auth REST API ile tokeni dogrular ve sadece kullanicinin users/{uid}/ altina yazmasina izin verir.

Cloudflare R2 presigned URL'leri de destekler, ancak Libra'nin ilk medya katmani Worker + R2 binding olarak tasarlanmistir.

DM ve ozel medya daha sonra farkli yetkilendirme ve sureli erisim kurallariyla ayrilabilir.
