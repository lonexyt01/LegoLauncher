#!/usr/bin/env python3
"""Pojav layoutlarini LegoLauncher uslubiga o'tkazadi (id'lar saqlanadi).
Ishlatish: python3 tools/gen_ui.py <upstream-papka>  -> lego-res/layout ga yozadi."""
import re, sys, os
up = sys.argv[1]
src = os.path.join(up, 'app_pojavlauncher/src/main/res/layout')
dst = os.path.join(os.path.dirname(__file__), '..', 'lego-res', 'layout')

TRANSPARENT_BG = ['fragment_fabric_install','fragment_mod_search','fragment_mod_version_list',
                  'fragment_file_selector','fragment_controller_remapper']
BTN = ['fragment_fabric_install','fragment_mod_version_list','dialog_mod_filters']
ALL = TRANSPARENT_BG + ['view_mod','view_mod_extended','dialog_mod_filters','dialog_side_dialog',
       'view_logger','item_multirt_runtime','item_version_profile_layout','spinner_mc_version',
       'dialog_expendable_list_view','dialog_color_selector','item_minecraft_account']

def add_style(m):
    tag = m.group(0)
    return tag if 'style=' in tag else tag.replace('<Button', '<Button style="@style/LegoFieldButton"', 1)

for n in ALL:
    t = open(os.path.join(src, n + '.xml'), encoding='utf8').read()
    if n in TRANSPARENT_BG:
        t = t.replace('android:background="@color/background_app"', 'android:background="@android:color/transparent"')
    t = t.replace('style="@style/TextAppearance.AppCompat.Title"', 'style="@style/LegoTitle"')
    t = t.replace('@style/Widget.AppCompat.Button.Borderless', '@style/LegoGhostButton')
    if n in BTN:
        t = re.sub(r'<Button\b[^>]*>', add_style, t, flags=re.S)
    if n == 'view_mod':
        t = t.replace('android:background="@drawable/background_line"', 'android:background="@drawable/lego_mod_item_bg"')
        t = t.replace('android:paddingHorizontal="@dimen/_2sdp"', 'android:padding="@dimen/_6sdp"')
        t = t.replace('android:paddingVertical="@dimen/_2sdp"', '')
        t = t.replace('style="@style/TextAppearance.AppCompat.Body2"', 'style="@style/LegoItemTitle"')
        t = t.replace('style="@style/TextAppearance.AppCompat.Body1"', 'style="@style/LegoItemBody"')
    if n == 'view_logger':
        t = t.replace('#555555', '@color/lego_card').replace('android:background="#000000"', 'android:background="#0B0D10"')
    if n == 'spinner_mc_version':
        t = t.replace('@color/background_bottom_bar', '@drawable/lego_mod_item_bg')
    if n == 'item_multirt_runtime':
        t = t.replace('style="@style/TextAppearance.AppCompat.Body1"', 'style="@style/LegoItemTitle"')
    open(os.path.join(dst, n + '.xml'), 'w', encoding='utf8').write(t)
    print('ok', n)


# ---------------------------------------------------------------- shakllar / dialoglar (to'liq qayta uslublash)
def add_attr(tag, name, value):
    return tag if name in tag else re.sub(r'^(<[\w.]+)', lambda m: m.group(1) + f'\n        {name}="{value}"', tag, count=1)

def restyle_form(t):
    # EditText -> yumaloq maydon (transparent fon berilmagan bo'lsa)
    def edit(m):
        tag = m.group(0)
        if 'android:background' in tag: return tag
        tag = add_attr(tag, 'android:background', '@drawable/background_line')
        return add_attr(tag, 'android:paddingHorizontal', '@dimen/padding_heavy')
    t = re.sub(r'<EditText\b[^>]*>', edit, t, flags=re.S)
    # Spinner (shaffof fon) -> maydon
    t = re.sub(r'(<Spinner\b[^>]*?)android:background="@android:color/transparent"', r'\1android:background="@drawable/background_line"', t, flags=re.S)
    # *_textView sarlavhalari -> sariq, qalin
    def lab(m):
        tag = m.group(0)
        if 'style=' in tag or 'ExtendedTextView' in tag or 'tools:text' in tag and 'textView_percent' in tag: return tag
        if '_textView' not in tag or '_percent' in tag: return tag
        return add_attr(tag, 'style', '@style/LegoLabelInline')
    t = re.sub(r'<TextView\b[^>]*>', lab, t, flags=re.S)
    # Button -> Lego
    t = re.sub(r'<Button\b[^>]*>', add_style, t, flags=re.S)
    return t

FORMS = ['dialog_control_button_setting','dialog_quick_setting','dialog_live_mouse_speed_editor',
         'dialog_per_version_control','activity_import_control','dialog_cropper']
for n in FORMS:
    f = os.path.join(src, n + '.xml')
    if not os.path.exists(f): print('yo\'q:', n); continue
    t = open(f, encoding='utf8').read()
    t = t.replace('style="@style/TextAppearance.AppCompat.Title"', 'style="@style/LegoTitle"')
    t = restyle_form(t)
    open(os.path.join(dst, n + '.xml'), 'w', encoding='utf8').write(t)
    print('forma', n)

# yon dialog sarlavhasi
f = os.path.join(dst, 'dialog_side_dialog.xml')
t = open(f, encoding='utf8').read().replace('style="@style/TextAppearance.AppCompat.Large"', 'style="@style/LegoTitle"')
open(f, 'w', encoding='utf8').write(t)

# ---------------------------------------------------------------- Sozlamalar (androidx.preference 1.2.0) kartalari
PREF_ITEM = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginHorizontal="@dimen/_8sdp"
    android:layout_marginVertical="@dimen/_2sdp"
    android:background="@drawable/lego_pref_item"
    android:clipToPadding="false"
    android:gravity="center_vertical"
    android:minHeight="@dimen/_36sdp"
    android:paddingStart="@dimen/_10sdp"
    android:paddingEnd="@dimen/_10sdp">

    <FrameLayout
        android:id="@+id/icon_frame"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:gravity="start|center_vertical"
        android:minWidth="0dp"
        android:paddingEnd="@dimen/_6sdp">
        <ImageView
            android:id="@android:id/icon"
            android:layout_width="@dimen/_20sdp"
            android:layout_height="@dimen/_20sdp"
            android:contentDescription="@null" />
    </FrameLayout>

    <RelativeLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:paddingTop="@dimen/_8sdp"
        android:paddingBottom="@dimen/_8sdp">

        <TextView
            android:id="@android:id/title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:ellipsize="marquee"
            android:fadingEdge="horizontal"
            android:singleLine="true"
            android:textColor="@color/primary_text"
            android:textSize="@dimen/_11ssp"
            android:textStyle="bold" />

        <TextView
            android:id="@android:id/summary"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_alignStart="@android:id/title"
            android:layout_below="@android:id/title"
            android:maxLines="4"
            android:textColor="@color/secondary_text"
            android:textSize="@dimen/_9ssp" />
    </RelativeLayout>

    <LinearLayout
        android:id="@android:id/widget_frame"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:gravity="end|center_vertical"
        android:orientation="vertical"
        android:paddingStart="@dimen/_6sdp" />
</LinearLayout>
"""
PREF_CAT = """<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="@dimen/_8sdp"
    android:gravity="center_vertical"
    android:orientation="vertical"
    android:paddingHorizontal="@dimen/_14sdp"
    android:paddingTop="@dimen/_8sdp"
    android:paddingBottom="@dimen/_3sdp">

    <TextView
        android:id="@android:id/title"
        style="@style/LegoSection"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />

    <TextView
        android:id="@android:id/summary"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/secondary_text"
        android:textSize="@dimen/_9ssp" />
</LinearLayout>
"""
open(os.path.join(dst, 'preference_material.xml'), 'w', encoding='utf8').write(PREF_ITEM)
open(os.path.join(dst, 'preference_category_material.xml'), 'w', encoding='utf8').write(PREF_CAT)
print('sozlamalar kartalari')
