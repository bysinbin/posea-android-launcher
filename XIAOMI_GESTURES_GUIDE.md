# Xiaomi HyperOS & MIUI Tam Ekran Hareketleri Kılavuzu

Bu kılavuz, **Xiaomi (HyperOS / MIUI)** cihazlarda Posea Launcher (veya herhangi bir 3. parti başlatıcı) varsayılan yapıldığında ortaya çıkan **"3 tuş navigasyonuna zorlanma"** ve **"tam ekran hareketlerinin kilitlenmesi"** sorununu kalıcı olarak çözmek için hazırlanmıştır.

---

## 📌 Sorunun Nedeni Nedir?
Xiaomi, tam ekran hareketlerini (gestures / quickstep) kendi sistem başlatıcısı olan `com.miui.home` içine entegre etmiştir. Üçüncü taraf bir başlatıcı varsayılan seçildiğinde, HyperOS güvenlik/kararlılık politikası gereği hareketleri kapatır ve sistemi zorla 3 tuşlu gezinme çubuğuna geçirir.

Posea Launcher'a sistem ayarı düzenleme izni verildiğinde, Posea bu kısıtlamayı aşar ve **WhatsApp, Instagram, Chrome, oyunlar ve masaüstü dahil tüm telefon genelinde** 3 tuşu kaldırıp tam ekran hareketlerini aktif eder.

---

## 🚀 1. Yöntem: Bilgisayar ile ADB Üzerinden Yetkilendirme (Önerilen)

### Adım 1: Telefonda Geliştirici Seçeneklerini Açın
1. **Ayarlar -> Telefon Hakkında** bölümüne gidin.
2. **OS Sürümü / MIUI Sürümü** yazısına art arda 7 kez dokunun ("Artık bir geliştiricisiniz" yazana kadar).
3. **Ayarlar -> Ek Ayarlar -> Geliştirici Seçenekleri** menüsüne girin.
4. Şu iki ayarı açın:
   - **USB Hata Ayıklama** -> `Açık`
   - **USB Hata Ayıklama (Güvenlik Ayarları)** -> `Açık` *(Bu ayar izin atamaları için Xiaomi cihazlarda zorunludur)*

### Adım 2: Telefonu Bilgisayara Bağlayın ve Komutu Çalıştırın
Telefonu USB kablosuyla bilgisayara bağlayın. Bilgisayarınızın terminalinde (Terminal / PowerShell / CMD) şu komutu çalıştırın:

```bash
adb shell pm grant com.bysinbin.posea android.permission.WRITE_SECURE_SETTINGS
```

> **Tebrikler!** Bu komut Posea'ya sistem ayarı yazma yetkisi verir. Artık **Posea Ayarları -> Tam Ekran Hareketleri (Xiaomi)** anahtarını açtığınızda tüm sistemde 3 tuş tamamen kaybolur ve hareketler çalışır. Telefon yeniden başlatılsa dahi Posea bu ayarı korur.

---

## ⚡ 2. Yöntem: Doğrudan ADB ile Hareketleri Açma (Alternatif)

Eğer izni vermek yerine doğrudan sistem düzeyinde hareketleri açmak isterseniz şu iki komutu çalıştırmanız yeterlidir:

#### Tam Ekran Hareketlerini Açma:
```bash
adb shell settings put global force_fsg_nav_bar 1
adb shell cmd overlay enable com.android.internal.systemui.navbar.gestural
```

#### Geri Almak (3 Tuşa Dönmek) İsterseniz:
```bash
adb shell settings put global force_fsg_nav_bar 0
adb shell cmd overlay disable com.android.internal.systemui.navbar.gestural
```

---

## 📱 3. Yöntem: Bilgisayarsız Çözüm (Shizuku & aShell)

Eğer yakınınızda bir bilgisayar yoksa, aynı işlemi doğrudan telefonunuz üzerinden kablosuz olarak yapabilirsiniz:

1. Google Play Store'dan **Shizuku** uygulamasını indirin.
2. **Geliştirici Seçenekleri -> Kablosuz Hata Ayıklama** özelliğini açın.
3. Shizuku'yu eşleştirip başlatın (Wireless Debugging Pair).
4. Google Play Store veya GitHub'dan **aShell** terminal uygulamasını indirin ve Shizuku iznini onaylayın.
5. aShell içine şu komutu yapıştırıp çalıştırın:
   ```text
   pm grant com.bysinbin.posea android.permission.WRITE_SECURE_SETTINGS
   ```
6. Posea Launcher'ı açıp ayarlardan **Tam Ekran Hareketleri**ni aktif edin.

---

## 🔍 Sık Sorulan Sorular

- **Telefonu yeniden başlattığımda bozulur mu?**  
  Hayır, Posea Launcher her açılışta `force_fsg_nav_bar` ayarını otomatik denetleyen bir koruma mekanizmasına sahiptir.

- **Diğer uygulamalarda (WhatsApp, Chrome vb.) tuşlar kayboluyor mu?**  
  Evet, bu ayar sistem genelinde çalıştığı için tüm uygulamalarda tuşlar kaybolur ve ekranın kenarlarından kaydırma hareketleri kusursuz çalışır.
