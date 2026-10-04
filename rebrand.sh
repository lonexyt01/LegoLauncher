#!/usr/bin/env bash
# Ishlatish: bash rebrand.sh <upstream-papka>
# PojavLauncher kodini LegoLauncher qilib qayta nomlaydi (paket nomi JNI uchun O'ZGARMAYDI).
set -e
cd "$1"
APP=app_pojavlauncher
# 1) Ilova nomi (faqat resurslarda)
grep -rl --include=strings.xml 'PojavLauncher' "$APP/src/main/res" | xargs -r sed -i 's/PojavLauncher/LegoLauncher/g'
# 2) applicationId (o'rnatilgan Pojav bilan to'qnashmasligi uchun)
sed -i 's/applicationId "net\.kdt\.pojavlaunch[^"]*"/applicationId "uz.legolauncher.mobile"/' "$APP/build.gradle"
# 3) O'yin papkasi: .../games/LegoLauncher
grep -rl --include=*.java --include=*.kt 'games/PojavLauncher' "$APP/src" | xargs -r sed -i 's#games/PojavLauncher#games/LegoLauncher#g'
# 4) Ikonka (ImageMagick bo'lsa; Windows'dagi convert.exe ga tegmaydi)
IM=""
if command -v magick >/dev/null 2>&1; then IM="magick"
elif command -v convert >/dev/null 2>&1 && convert -version 2>/dev/null | grep -q ImageMagick; then IM="convert"; fi
if [ -n "$IM" ]; then
  find "$APP/src/main/res" -name 'ic_launcher*.png' | while read f; do
    w=$(file "$f" | sed -n 's/.*PNG image data, \([0-9]*\) x.*/\1/p'); w=${w:-192}
    $IM ../icon.png -resize ${w}x${w} "$f" || true
  done
else echo "ImageMagick yo'q - ikonka almashtirilmadi"; fi
# 5) Lego dizayni: tayyor resurslarni ustiga yozish (ranglar, uslublar, bosh ekran)
cp -r ../lego-res/. "$APP/src/main/res/"
# 6) Matnlar: ilova nomi, Wiki -> Modrinth, tugma havolasi
find "$APP/src/main/res" -name strings.xml | while read f; do
  sed -i 's#\(<string name="mcl_tab_wiki">\)[^<]*\(</string>\)#\1Modrinth\2#' "$f"
done
sed -i 's#\(<string name="app_name">\)[^<]*\(</string>\)#\1LegoLauncher\2#' "$APP/src/main/res/values/strings.xml"
sed -i 's#URL_HOME = "https://pojavlauncherteam.github.io"#URL_HOME = "https://modrinth.com/mods"#' "$APP/src/main/java/net/kdt/pojavlaunch/Tools.java"
# 7) Landscape (yoyilgan) ekran: LauncherActivity'ni gorizontalga qulflash
MF="$APP/src/main/AndroidManifest.xml"
if ! grep -A3 'android:name=".LauncherActivity"' "$MF" | grep -q screenOrientation; then
  sed -i '/android:name="\.LauncherActivity"/a\            android:screenOrientation="sensorLandscape"' "$MF"
fi
# 8) Animatsiya (g'ishtlar + Play pulsi)
mkdir -p "$APP/src/main/java/net/kdt/pojavlaunch"
cp ../lego-src/LegoAnim.java "$APP/src/main/java/net/kdt/pojavlaunch/LegoAnim.java"
MM="$APP/src/main/java/net/kdt/pojavlaunch/fragments/MainMenuFragment.java"
grep -q 'LegoAnim.start' "$MM" || sed -i 's#^\([[:space:]]*\)mNewsButton.setOnLongClickListener(#\1net.kdt.pojavlaunch.LegoAnim.start(view);\n\1mNewsButton.setOnLongClickListener(#' "$MM"
grep -q 'LegoAnim.start' "$MM" || echo "OGOHLANTIRISH: LegoAnim ulanmadi (MainMenuFragment o'zgargan)"
grep -q 'sensorLandscape' "$MF" || echo "OGOHLANTIRISH: landscape qulfi qo'yilmadi"
echo "Rebrand tayyor"
