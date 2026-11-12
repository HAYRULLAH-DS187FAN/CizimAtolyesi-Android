# Çizim Atölyesi — Android başlangıç projesi

Bu depo ilk çalışan çizim tuvali için hazırlanmış bir Kotlin + Jetpack Compose Android projesidir.

## Şu anki kapsam (v0.1)
- Şeffaflığı gösteren dama desenli tuval zemini. Dama deseni çizim içeriğine dahil değildir.
- Parmakla serbest çizim, renk paleti ve fırça kalınlığı.
- Silgi: seçili katmanın kendi çizgilerini şeffaflaştırır; alttaki katmanları doğrudan silmez.
- Çoklu katman oluşturma, katman seçme ve görünürlüğünü açıp kapatma.
- Geri al / yinele.
- İki parmakla yakınlaştırma ve kaydırma.

## Henüz yapılmayanlar
- Katman birleştirme, yeniden adlandırma, kilitleme ve opaklık arayüzü.
- Akıllı sınır algılama: fırça ve silgi hareketini referans çizgi katmanına göre kısıtlama.
- Alansal boyama ve alansal silme.
- Proje kaydetme/yükleme ve gerçek şeffaf PNG dışa aktarma.
- Basınç hassasiyeti, düzgünleştirme, seçim/dönüştürme ve şekil araçları.

## Açma
Android Studio'da bu klasörü `Open` ile aç ve Gradle eşitlemesini tamamla. JDK 17, Android SDK 37 ve Gradle 9.6 gerekir. APK almak için `:app:assembleDebug` görevini çalıştır.

Uygulama adı ve paket kimliği şimdilik geçici: `Çizim Atölyesi` / `com.example.cizimatolyesi`.
