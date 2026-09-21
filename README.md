# Libra

Libra, kullanıcıların kitap keşfedebildiği, okuyabildiği, kendi kitaplarını yazabildiği ve ilerleyen sürümlerde sosyal olarak etkileşime girebildiği uzun ömürlü bir Android platformudur.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- MVVM / katmanlı Clean Architecture yaklaşımı
- Coroutines + Flow / StateFlow
- Navigation Compose
- Firebase Authentication
- Firebase Realtime Database
- Cloudflare R2 abstraction
- Coil
- Android Credential Manager + Google ID

## Project structure

```text
app/src/main/java/com/libra/app/
├── core/
│   ├── di/
│   ├── result/
│   └── state/
├── data/
│   ├── ai/
│   ├── auth/
│   ├── book/
│   ├── storage/
│   └── user/
├── domain/
│   ├── model/
│   └── repository/
├── feature/
│   ├── auth/
│   ├── home/
│   ├── library/
│   ├── write/
│   ├── friends/
│   └── profile/
└── ui/
    ├── components/
    ├── navigation/
    └── theme/
```

## Firebase setup

1. Firebase Console'da bir Android uygulaması oluşturun.
2. Android package name olarak `com.libra.app` kullanın.
3. SHA-1 / SHA-256 değerlerini Firebase projesine ekleyin.
4. Google Authentication sağlayıcısını etkinleştirin.
5. Realtime Database oluşturun.
6. Bu proje klasörüne gerçek `app/google-services.json` dosyanızı koyun. Örnek şablon `app/google-services.json.example` dosyasında bulunur.
7. Realtime Database kurallarını gerektiğinde `database.rules.json` içeriğine göre yayınlayın.
8. `app/src/main/res/values/strings.xml` içindeki `google_web_client_id` değerini Firebase projenizin OAuth 2.0 Web Client ID değeriyle değiştirin.

`google-services.json` ve diğer yerel secret/config dosyaları `.gitignore` ile dışarıda bırakılmıştır.

## Realtime Database schema

```text
/users/{uid}
/books/{bookId}
/chapters/{bookId}/{chapterId}
/libraries/{uid}/{shelfType}/{bookId}
/friendships/{uid}/...
/notifications/{uid}/...
```

Kütüphane kayıtları kitap objesini kopyalamak yerine kitap ID'si, okuma ilerlemesi ve eklenme zamanı referansı tutar.

## Cloudflare R2

R2, istemciye uzun ömürlü erişim anahtarı gömmek yerine abstraction/presigned upload yaklaşımı için ayrılmıştır. `StorageRepository` UI ve domain katmanlarını R2 ayrıntılarından korur.

Gerçek upload için bir backend/presigned URL endpoint'i bağlandığında `CloudflareR2StorageRepositoryImpl` bu akışa adapte edilecektir.

## AI

`AiAssistantRepository`, yazma asistanı özelliklerinin UI'dan ayrık kalması için oluşturulmuştur. Henüz gerçek AI sağlayıcısı bağlı değildir; repository başarı numarası uydurmaz ve yapılandırma yoksa açık bir hata döndürür.


## GitHub Actions — APK

Libra artık her `main` push'unda ve pull request'te Android build kontrolü yapar. Ayrıca GitHub Actions arayüzünden elle çalıştırılabilir.

Workflow dosyası:

`.github/workflows/build-apk.yml`

Başarılı bir çalıştırmanın sonunda:

- `Libra-APK-<run-number>` artifact'ı içinde kurulabilir debug APK bulunur.
- `Libra-Build-Info-<run-number>` artifact'ı APK boyutunu ve SHA-256 değerini içerir.
- Build/test başarısız olursa mevcut test raporları ayrı artifact olarak yüklenir.

### APK alma

GitHub → **Actions** → **Libra Android APK** → bir workflow run → **Artifacts** bölümünden `Libra-APK-...` dosyasını indirin. ZIP'i açtığınızda APK hazır olacaktır.

### Firebase ile CI build

Gerçek Firebase `google-services.json` dosyasını repository'ye koymak yerine GitHub Actions Secret olarak `GOOGLE_SERVICES_JSON_B64` tanımlayabilirsiniz.

Yerelde Base64 üretmek için:

```bash
base64 -w 0 app/google-services.json
```

Windows/PowerShell'de:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("app/google-services.json"))
```

Secret tanımlı değilse workflow yine APK derlemeyi dener; Firebase runtime özellikleri için gerçek Firebase yapılandırması gerekir.

## Build

Android Studio ile projeyi açın ve Gradle sync tamamlandıktan sonra `app` modülünü çalıştırın.

Google/Firebase yapılandırması olmadan proje derlenebilir, ancak Authentication ve Realtime Database çalışma zamanında yapılandırma hatası gösterecektir.

## Current scope

İlk aşamada gerçek Authentication + kullanıcı profili altyapısı, Firebase tabanlı kitap/kütüphane/chapter repository'leri, temel ekranlar ve genişletilebilir navigation mevcut.

Henüz tamamlanmayan ana modüller:

- Tam kitap okuyucu
- Gelişmiş kitap editörü ve bölüm yönetimi ekranları
- Gerçek arkadaşlık/takip işlemleri ve bildirimler
- Arama/keşif sıralama sistemi
- Cloudflare R2 presigned upload backend'i
- Gerçek AI provider entegrasyonu

## Architecture decisions

- UI hiçbir zaman Firebase/R2 SDK çağrısı yapmaz.
- Repository sonuçları `AppResult` ile modellenir.
- UI yükleniyor/başarı/hata durumlarını `UiState` ile yönetir.
- Demo authentication ve sahte backend başarıları kaldırılmıştır.
- Ürün adı ve package adı Libra standardına taşınmıştır.
- İlk sürümde gereksiz Room, Retrofit, Moshi, Camera ve Location dependency'leri tutulmamıştır.
