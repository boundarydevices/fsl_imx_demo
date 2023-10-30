package com.example.android.camera2.basic;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.navigation.Navigation;

import javax.annotation.Nullable;

public class SwipeGestureDetector extends GestureDetector.SimpleOnGestureListener {
    private static final int MIN_SWIPE_DISTANCE_X = 100;
    private static final String TAG = SwipeGestureDetector.class.getSimpleName();
    private View view;

    @Override
    public boolean onFling(
            @Nullable MotionEvent e1, @Nullable MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) {
            Log.e(TAG, "e1 or e2 is null!");
            return super.onFling(e1, e2, velocityX, velocityY);
        }
        float deltaX = e1.getX() - e2.getX();
        float deltaXAbs = Math.abs(deltaX);
        if (deltaXAbs >= MIN_SWIPE_DISTANCE_X && deltaX < 0) {
            if (view.getTag().equals("right"))
                Navigation.findNavController(view).navigate(R.id.action_camera_to_video);
        }

        return true;
    }

    public SwipeGestureDetector(View view) {
        this.view = view;
    }
}
