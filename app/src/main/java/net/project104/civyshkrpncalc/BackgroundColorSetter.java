package net.project104.civyshkrpncalc;

import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.view.View;

@TargetApi(11)
public class BackgroundColorSetter implements ValueAnimator.AnimatorUpdateListener {
    private View view;

    public BackgroundColorSetter(View view) {
        this.view = view;
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        view.setBackgroundColor((Integer) animation.getAnimatedValue());
    }
}
