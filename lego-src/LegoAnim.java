package net.kdt.pojavlaunch;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/** LegoLauncher: bosh ekrandagi g'ishtlar va Play tugmasi animatsiyasi. */
public final class LegoAnim {
    private LegoAnim() {}

    private static ObjectAnimator loop(View v, String prop, float from, float to, long dur, long delay) {
        ObjectAnimator a = ObjectAnimator.ofFloat(v, prop, from, to);
        a.setDuration(dur);
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        return a;
    }

    public static void start(View root) {
        final List<Animator> running = new ArrayList<>();
        float d = root.getResources().getDisplayMetrics().density;
        int[] ids = {R.id.lego_brick1, R.id.lego_brick2, R.id.lego_brick3, R.id.lego_brick4};
        for (int i = 0; i < ids.length; i++) {
            View v = root.findViewById(ids[i]);
            if (v == null) continue;
            running.add(loop(v, "translationY", 0f, -(6 + i * 2) * d, 1600 + i * 350L, -i * 400L));
            running.add(loop(v, "rotation", -2f, 2f, 1800 + i * 300L, 0));
        }
        View play = root.findViewById(R.id.play_button);
        if (play != null) {
            running.add(loop(play, "scaleX", 1f, 1.025f, 1100, 0));
            running.add(loop(play, "scaleY", 1f, 1.025f, 1100, 0));
        }
        for (Animator a : running) a.start();
        root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override public void onViewAttachedToWindow(View v) {}
            @Override public void onViewDetachedFromWindow(View v) {
                for (Animator a : running) a.cancel();
            }
        });
    }
}
