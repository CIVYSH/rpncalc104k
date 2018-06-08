package net.project104.civyshkrpncalc;

import android.widget.HorizontalScrollView;

import static android.view.View.FOCUS_RIGHT;

class HorizontalViewScroller implements Runnable {
    private HorizontalScrollView view;

    HorizontalViewScroller(HorizontalScrollView view) {
        this.view = view;
    }

    @Override
    public void run() {
        view.fullScroll(FOCUS_RIGHT);
    }
}
