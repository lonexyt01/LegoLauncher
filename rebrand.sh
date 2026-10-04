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
# 5) Lego ranglar temasi (ranglar nomiga bog'liq emas)
PY=$(command -v python3 || command -v python || true)
if [ -n "$PY" ]; then "$PY" ../recolor.py "$APP/src/main/res" || true; else echo "Python yo'q - ranglar almashtirilmadi"; fi
echo "Rebrand tayyor"
