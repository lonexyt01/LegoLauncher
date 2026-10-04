LegoLauncher (Pojav asosida) - o'zi ishlaydigan Minecraft Java launcher
Boshqa ilova KERAK EMAS: APK o'zi papka (games/LegoLauncher) va JRE'ni o'rnatadi, o'yinni o'zi ishga tushiradi.

APK olish:
A) GitHub: shu papkadagi hamma narsani repoga yuklang (git add . && git commit && git push),
   Actions -> "Build LegoLauncher" -> Run workflow -> "LegoLauncher-apk" artifact.
B) Windows: build.bat   C) Linux/macOS: build.sh

YANGILIKLAR (v2)
  * Discord butunlay o'chirildi: tugma, ikonka, barcha tillardagi matnlar va havola.
  * MODLAR (Modrinth API v2): Bosh sahifa -> "Modlar" tugmasi.
      - Qidirish: nom bo'yicha, Minecraft versiyasi va loader (Fabric/Forge/NeoForge/Quilt) filtri bilan.
        Versiya va loader joriy profildan avtomatik aniqlanadi.
      - O'rnatish: mos versiya tanlanadi, SHA-1 tekshiriladi, majburiy bog'liqliklar
        (masalan Fabric API) o'zi yuklanadi. Fayllar joriy profilning mods/ papkasiga tushadi.
      - "O'rnatilgan" tab: mods/ papkadagi modlar ro'yxati, qidirish va o'chirish.
  * Yangi dizayn: g'isht uslubidagi tugmalar (MineButton), yumaloq maydonlar, qorong'u palitra,
    profil tahrirlash ekrani 3 ta kartaga bo'lingan (Umumiy / O'yin / Texnik).

YANGILIKLAR (v3) - Pojav ko'rinishi qolmadi
  * Barcha launcher ekranlari Lego uslubida: kirish tanlash, lokal kirish, profil turi, Fabric/Forge o'rnatish,
    modpack qidirish va versiya ro'yxati, fayl tanlash, dialoglar, progress paneli, logger, sozlamalar fon.
  * Dialog oynalari (AlertDialog), spinner ro'yxatlari, tugmalar, ajratgichlar - yagona tema.
  * Pojav ikonkalari (standart profil rasmi, bildirishnoma ikonkasi) g'isht bilan almashtirildi.
  * Barcha tillardagi "Pojav..." matnlari LegoLauncher ga o'zgartirildi.
  * tools/gen_ui.py - qolgan Pojav layoutlarini Lego uslubiga avtomatik o'tkazadi (id'lar saqlanadi).

Fayllar:
  lego-res/    - Pojav resurslari ustiga yoziladi (ranglar, uslublar, tugmalar, bosh sahifa, profil, modlar ekrani)
  lego-src/    - LegoAnim.java (animatsiya), MainMenuFragment.java (Discord'siz menyu),
                 LegoModsFragment.java (Modrinth mod ekrani)
  rebrand.sh   - hammasini avtomatik qo'llaydi (nom, applicationId, ikonka, dizayn, Discord'ni o'chirish)
Ranglar: lego-res/values/colors.xml.

Eslatma: o'yinni ishga tushiruvchi qism Pojav'niki. Litsenziya: LGPL-3.0 - kodni ochiq saqlang.
