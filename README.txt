LegoLauncher (Pojav asosida) - o'zi ishlaydigan Minecraft Java launcher
Boshqa ilova KERAK EMAS: APK o'zi papka (games/LegoLauncher) va JRE'ni o'rnatadi, o'yinni o'zi ishga tushiradi.

APK olish:
A) GitHub: shu papkadagi hamma narsani repoga yuklang (git add . && git commit && git push),
   Actions -> "Build LegoLauncher" -> Run workflow -> "LegoLauncher-apk" artifact.
B) Windows: build.bat   C) Linux/macOS: build.sh

Dizayn: lego-res/ papkasi Pojav resurslari ustiga yoziladi (rebrand.sh qiladi):
  values/colors.xml, styles.xml  - qizil/sariq Lego ranglar
  layout/ va layout-land/fragment_launcher.xml - yangi bosh ekran (sarlavha, kartali tugmalar, qizil Play)
  drawable/lego_*.xml - karta, sarlavha, Play tugmasi fonlari
Wiki tugmasi -> Modrinth (modrinth.com/mods). Ranglarni o'zgartirish: lego-res/values/colors.xml.

Eslatma: Pojav'ning o'zi (o'yin ishga tushirish) o'zgarmagan. Litsenziya: LGPL-3.0 - kodni ochiq saqlang.
