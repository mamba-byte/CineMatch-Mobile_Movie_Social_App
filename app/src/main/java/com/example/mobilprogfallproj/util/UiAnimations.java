package com.example.mobilprogfallproj.util;

import android.view.View;

public class UiAnimations {

    public static void applyClickScale(View view, Runnable action) {
        if (view == null) return;

        view.setOnClickListener(v -> {
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(80)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .start();
                        if (action != null) {
                            action.run();
                        }
                    })
                    .start();
        });
    }
}


