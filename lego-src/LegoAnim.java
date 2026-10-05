package net.kdt.pojavlaunch;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Outline;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/** LegoLauncher: bosh ekran animatsiyalari va yumaloq kesish yordamchisi. */
public final class LegoAnim {
    private LegoAnim() {}

    /** View'ni yumaloq burchak bilan kesadi (rasmlar uchun). */
    public static void round(View v, float radiusPx) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
            }
        });
        v.setClipToOutline(true);
    }

    private static ObjectAnimator loop(View v, String prop, float from, float to, long dur, boolean smooth) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(smooth ? new AccelerateDecelerateInterpolator() : new LinearInterpolator());
        return a;
    }

    /** Asosiy rasm sekin siljiydi/kattalashadi, O'YNASH tugmasi "nafas oladi". */
    public static void start(View root) {
        View hero = root.findViewById(R.id.lego_hero_image);
        if (hero != null) {
            hero.setScaleX(1.12f);
            hero.setScaleY(1.12f);
            Animator pan = loop(hero, "translationX", -18f, 18f, 14000, true);
            pan.start();
            Animator zoom = loop(hero, "scaleX", 1.12f, 1.2f, 14000, true);
            zoom.start();
            loop(hero, "scaleY", 1.12f, 1.2f, 14000, true).start();
        }
        View play = root.findViewById(R.id.play_button);
        if (play != null) {
            loop(play, "scaleX", 1f, 1.025f, 1100, true).start();
            loop(play, "scaleY", 1f, 1.025f, 1100, true).start();
        }
    }
}
