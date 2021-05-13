// Generated by view binder compiler. Do not edit!
package com.example.android.camera2.basic.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import com.example.android.camera.utils.AutoFitSurfaceView;
import com.example.android.camera2.basic.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class FragmentVideoBinding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final ImageButton captureButton;

  @NonNull
  public final View overlay;

  @NonNull
  public final AutoFitSurfaceView viewFinder;

  private FragmentVideoBinding(@NonNull ConstraintLayout rootView,
      @NonNull ImageButton captureButton, @NonNull View overlay,
      @NonNull AutoFitSurfaceView viewFinder) {
    this.rootView = rootView;
    this.captureButton = captureButton;
    this.overlay = overlay;
    this.viewFinder = viewFinder;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static FragmentVideoBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static FragmentVideoBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.fragment_video, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static FragmentVideoBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    String missingId;
    missingId: {
      ImageButton captureButton = rootView.findViewById(R.id.capture_button);
      if (captureButton == null) {
        missingId = "captureButton";
        break missingId;
      }
      View overlay = rootView.findViewById(R.id.overlay);
      if (overlay == null) {
        missingId = "overlay";
        break missingId;
      }
      AutoFitSurfaceView viewFinder = rootView.findViewById(R.id.view_finder);
      if (viewFinder == null) {
        missingId = "viewFinder";
        break missingId;
      }
      return new FragmentVideoBinding((ConstraintLayout) rootView, captureButton, overlay,
          viewFinder);
    }
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
