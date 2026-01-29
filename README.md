# GALiveNotification

Bir Android uygulaması için gerçek zamanlı bildirim sistemi sağlayan kütüphane ve örnek proje.

## 📋 İçindekiler

- [Live Update Notification Nedir?](#live-update-notification-nedir)
- [Proje Yapısı](#proje-yapısı)
- [Nasıl Kullanılır?](#nasıl-kullanılır)
- [Özellikler](#özellikler)
- [Gereksinimler](#gereksinimler)
- [Lisans](#lisans)

## 🔔 Live Update Notification Nedir?

Live Update Notification, kullanıcılara sürekli olarak güncellenebilen bildirimler göndermek için kullanılan bir sistem bileşenidir. Geleneksel statik bildirimlerin aksine, bu sistem:

- **Dinamik İçerik**: Bildirimin içeriği, başlığı, ilerlemesi ve görselleri anlık olarak güncellenebilir
- **Adımsal İzleme**: Sipariş takibi, yükleme durumu vb. gibi çok adımlı işlemlerin ilerleme durumunu gösterebilir
- **Gerçek Zamanlı Güncelleme**: Her adım tamamlandığında bildirimi anında güncelleyerek kullanıcıya en son bilgiyi sunar
- **Foreground Service**: Sürekli bildirim gösterebilmek için Android'in Foreground Service mekanizmasını kullanır

### Kullanım Alanları

- 🛵 Kargo/Sipariş Takibi
- ⬇️ Dosya İndirme İlerleme Gösterimi
- 📱 Uygulama Yükleme/Güncelleme Durumu
- 🎵 Medya Oynatım Kontrolü
- 🔄 Senkronizasyon Durumu

## 🏗️ Proje Yapısı

### Ana Bileşenler

```
com.gkhnakbs.galivenotification/
├── LiveNotificationManager.kt     # Bildirim yönetimi ve oluşturma
├── LiveNotificationService.kt      # Foreground Service implementasyonu
├── MainActivity.kt                 # Demo uygulaması
└── ui/                            # Compose UI bileşenleri
```

### LiveNotificationManager

Bildirimlerin oluşturulması ve yönetilmesinden sorumlu merkezi bileşendir:

- Bildirim kanalı oluşturma ve yapılandırma
- Farklı durum için özelleştirilmiş bildirimler
- İlerleme bar ve görsel öğeler
- Adım bazlı gösterim

### LiveNotificationService

Android Foreground Service olarak çalışan servis:

- Bildirimlerin arka planda gösterilmesini sağlar
- İlişkili runnables'ları yönetir
- Servis yaşam döngüsüyle bildirim gösterimi senkronizasyon
- Handler üzerinden zamanlı güncellemeler

## 🚀 Nasıl Kullanılır?

### 1. Temel Kurulum

Uygulamanızın Activity'sinde veya Service'inde:

```kotlin
// NotificationManager'ı alın
val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

// LiveNotificationManager'ı initialize edin
LiveNotificationManager.initialize(applicationContext, notificationManager)
```

### 2. Servisi Başlatma

```kotlin
val intent = Intent(context, LiveNotificationService::class.java)
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}
```

### 3. Adımsal Bildirimleri Kontrol Etme

LiveNotificationService, `LiveNotificationManager.startWithService()` içinde otomatik olarak:

- Belirli zaman aralıklarında bildirimleri günceller
- Her adım tamamlandığında yeni bir bildirim gösterir
- Tamamlandığında servisi durdurur

```kotlin
LiveNotificationManager.startWithService(
    onScheduleNotification = { notification, delay ->
        // Bildirim gösterilecek
    },
    onComplete = {
        // Tüm adımlar tamamlandı
    }
)
```

### 4. AndroidManifest.xml İzinleri

```xml
<!-- Foreground Service izni -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<!-- Bildirim izni (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<!-- Service tanımlama -->
<service
    android:name="com.gkhnakbs.galivenotification.LiveNotificationService"
    android:foregroundServiceType="specialUse"
    android:exported="false" />
```

## ✨ Özellikler

- ✅ **Compose UI Desteği**: Modern Android Compose ile UI bileşenleri
- ✅ **Dinamik İçerik**: Bildirimleri gerçek zamanda güncelleyebilme
- ✅ **İlerleme Gösterimi**: Segmentli ilerleme barı ve yüzde gösterimi
- ✅ **Zengin Görseller**: Büyük simgeler ve renkli tasarım
- ✅ **Zaman Gösterimi**: Kronometreyle sayaç gösterimi
- ✅ **Otomatik Zamanlama**: Handler üzerinden otomatik adım ilerlemesi
- ✅ **Esneklik**: Farklı durum ve senaryolara özelleştirilebilir

## 📦 Gereksinimler

- **Android SDK**: API Level 36 (Android 15.0 / Baklava) ve üstü
- **Kotlin**: 1.9 ve üstü
- **Jetpack Compose**: Latest
- **AndroidX Core**: Latest

## 📄 Lisans

Bu proje MIT Lisansı altında lisanslanmıştır. Detaylı bilgi için [LICENSE](LICENSE) dosyasını inceleyiniz.

---

**Katkılar**: Bu proje için yapılacak katkılar memnuniyetle kabul edilir. Lütfen değişikliklerinizle bir pull request gönderin.
