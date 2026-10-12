# AI Studio GitHub Hesabını Değiştirme Kılavuzu

Google AI Studio Build platformunda mevcut GitHub hesabınızın depolama alanı (kota) dolduğunda, projeyi yeni bir GitHub hesabına aktarmak veya AI Studio'ya bağlı GitHub hesabını değiştirmek için izlenecek adımlar aşağıda açıklanmıştır.

### User Review & Critical Decisions

> [!IMPORTANT]
> AI Studio doğrudan GitHub OAuth yetkilendirmesi üzerinden çalışır. Tarayıcınızda ve GitHub ayarlarınızda mevcut oturumu yenileyerek yeni hesaba geçiş yapabilirsiniz.
>
> Projenizin mevcut kaynak kodunu kaybetmemek için her ihtimale karşı önce **Export as ZIP** yapmanız önerilir.

- **Seçilen Yöntem**: AI Studio profilindeki GitHub bağlantısını sıfırlayıp yeni hesaba bağlama
- **Öncelikli Eylem**: Proje kodlarının yedeğini ZIP olarak indirmek (Güvenlik önlemi)

---

### 1. Overview & Core Concept

- **Amaç**: Depolama alanı dolan GitHub hesabı yerine yeni/boş bir GitHub hesabını Google AI Studio'ya entegre etmek ve depolama sorununu çözmek.
- **Kapsam**: AI Studio arayüzü, GitHub OAuth izin yönetimi ve proje aktarım adımları.
- **Kazanım**: Depolama kotasına takılmadan projenin yeni bir GitHub reposuna aktarılması ve CI/CD (GitHub Actions) yapısının kesintisiz çalışması.

---

### 2. Kullanıcı Akışı ve Adım Adım Yapılışı

Mevcut GitHub bağlantısını değiştirip yeni hesaba geçmek için şu 4 aşamayı uygulayabilirsiniz:

#### Aşama 1: Projenin Güvenli Yedeğini Alın (Opsiyonel ama Tavsiye Edilir)
1. AI Studio ekranının sağ üst köşesindeki menü simgesine (veya proje ayarları / export butonuna) tıklayın.
2. **"Export as ZIP"** seçeneğini kullanarak projenin son halini bilgisayarınıza indirin.

#### Aşama 2: Mevcut GitHub Hesabındaki AI Studio Yetkisini Kaldırın
AI Studio'nun eski GitHub hesabınızı unutması için:
1. Tarayıcınızda **[github.com](https://github.com)** adresine gidin ve şu anki (dolu olan) hesabınızla giriş yapın.
2. Sağ üstteki profil resminize tıklayıp **Settings (Ayarlar)** seçeneğine gidin.
3. Sol menüden **Applications** (veya **Integrations -> Authorized OAuth Apps** / **GitHub Apps**) sekmesini açın.
4. Listede yer alan **"Google AI Studio"** veya **"AI Studio Build"** uygulamasını bulun.
5. Yanındaki üç nokta (`...`) veya Revoke butonuna tıklayarak **Revoke access (Erişimi Kaldır)** seçeneğini onaylayın.

#### Aşama 3: Tarayıcıda Yeni GitHub Hesabına Geçiş Yapın
1. `github.com` üzerinde mevcut hesabınızdan **Sign out (Çıkış yap)** deyin.
2. Kullanmak istediğiniz **yeni GitHub hesabınızla giriş yapın**.

#### Aşama 4: AI Studio Üzerinden Yeni Hesabı Bağlayın
1. AI Studio sekmesine geri dönün ve sayfayı yenileyin (F5).
2. Sağ üst menüden **"Push to GitHub"** veya **"Export to GitHub"** seçeneğine tıklayın.
3. Karşınıza GitHub yetkilendirme ekranı gelecektir.
4. Yeni oturum açtığınız GitHub hesabı görünecektir; **Authorize (Yetkilendir)** butonuna tıklayarak izin verin.
5. Yeni bir repo adı belirleyin ve projenizi yeni hesabınıza aktarın.

---

### 3. Key Product Decisions & Trade-Offs

- **Karar 1: GitHub OAuth İznini Revoke Etme vs. Gizli Sekme Kullanımı**
  - *Seçilen Yaklaşım*: GitHub Ayarlarından OAuth iznini kaldırmak.
  - *Neden*: AI Studio token'ı eski hesaba bağlı kaldığı sürece her aktarma denemesinde otomatik olarak eski hesabı seçer. İzni iptal etmek temiz bir yeniden bağlanma sağlar.
  - *Alternatif*: Gizli sekmede (Incognito) yeni GitHub hesabı ve AI Studio'yu açıp aktarmak (Hızlı alternatif yöntem).

- **Karar 2: Git LFS ve Repository Temizliği**
  - *Not*: Eğer eski hesabınızda sadece bu repo yer kaplıyorsa, eski repodaki gereksiz build artifact'larını veya release dosyalarını silerek de yer açabilirsiniz. Ancak yeni bir hesaba geçmek en temiz çözümdür.

---

### 4. Entegrasyon Akışı

```
┌─────────────────────────┐       Revoke Access       ┌──────────────────────────┐
│ Eski GitHub Hesabı      │ ◄──────────────────────── │ GitHub Settings          │
│ (Depolama Dolu)         │                           │ Applications / OAuth     │
└─────────────────────────┘                           └──────────────────────────┘
                                                                    │
                                                                    ▼
┌─────────────────────────┐       Authorize           ┌──────────────────────────┐
│ AI Studio Arayüzü       │ ────────────────────────► │ Yeni GitHub Hesabı       │
│ "Export / Push"         │                           │ (Yeni Repo Oluşturma)    │
└─────────────────────────┘                           └──────────────────────────┘
```

Adımları inceleyip onayladığınızda veya takıldığınız bir adım olursa hemen bildirebilirsiniz.
