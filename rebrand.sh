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
rm -f "$APP/src/main/res/drawable/ic_pojav_full.webp"
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
# 8) Java: animatsiya, bosh menyu (Discord'siz, Modlar tugmasi bilan), Modrinth mod ekrani
JAVA="$APP/src/main/java/net/kdt/pojavlaunch"
cp ../lego-src/LegoAnim.java "$JAVA/LegoAnim.java"
cp ../lego-src/MainMenuFragment.java "$JAVA/fragments/MainMenuFragment.java"
cp ../lego-src/LegoModsFragment.java "$JAVA/fragments/LegoModsFragment.java"
# 9) Discord'ni butunlay o'chirish (ikonka va barcha tillardagi matnlar)
rm -f "$APP/src/main/res/drawable/ic_discord.xml"
find "$APP/src/main/res" -name strings.xml | xargs -r sed -i '/mcl_button_discord/d;/name="discord_invite"/d'
if grep -rqi discord "$APP/src/main" ; then echo "OGOHLANTIRISH: 'discord' hali qolgan:"; grep -rli discord "$APP/src/main"; fi
grep -q 'sensorLandscape' "$MF" || echo "OGOHLANTIRISH: landscape qulfi qo'yilmadi"
# 10) Qolgan "Pojav" matnlari va sozlamalar ekrani fonini Lego qilish
find "$APP/src/main/res" -name strings.xml | xargs -r perl -pi -e 's/(?<!name=")\b[Pp]ojav\w*/LegoLauncher/g; s#pojavlauncherteam\.github\.io#modrinth.com#g'
find "$APP/src/main/res/layout" -name '*.xml' | xargs -r sed -i 's/PojavLauncher version/LegoLauncher version/'
sed -i 's/LegoLauncherLauncher/LegoLauncher/g' $(find "$APP/src/main/res" -name strings.xml)
PF="$APP/src/main/java/net/kdt/pojavlaunch/prefs/screens/LauncherPreferenceFragment.java"
[ -f "$PF" ] && sed -i 's/getResources().getColor(R.color.background_app)/0x00000000/' "$PF"
echo "Rebrand tayyor"
