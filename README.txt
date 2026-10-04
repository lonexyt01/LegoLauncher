LegoLauncher (Pojav asosida) - o'zi ishlaydigan Minecraft Java launcher
Boshqa ilova KERAK EMAS: APK o'zi papka (games/LegoLauncher) va JRE'ni o'rnatadi, o'yinni o'zi ishga tushiradi.

APK olish:
A) GitHub: shu papkadagi hamma narsani repoga yuklang (git add . && git commit && git push),
   Actions -> "Build LegoLauncher" -> Run workflow -> "LegoLauncher-apk" artifact.
B) Windows: build.bat   C) Linux/macOS: build.sh

Dizayn (desktop LegoLauncher uslubida, GORIZONTAL/landscape):
  lego-res/    - Pojav resurslari ustiga yoziladi: ranglar, uslublar, chap panel (sidebar) + bosh sahifa
                 (O'YIN NUSXASI kartasi, sariq O'YNASH tugmasi, animatsiyali g'isht sahnasi, 3x2 tugmalar)
  lego-src/LegoAnim.java - g'ishtlar va O'YNASH tugmasi animatsiyasi
  rebrand.sh   - hammasini avtomatik qo'llaydi, LauncherActivity'ni sensorLandscape qiladi
Ranglar: lego-res/values/colors.xml. Wiki tugmasi -> Modrinth.

Eslatma: o'yinni ishga tushiruvchi qism Pojav'niki. Litsenziya: LGPL-3.0 - kodni ochiq saqlang.
