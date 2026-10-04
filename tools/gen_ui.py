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
