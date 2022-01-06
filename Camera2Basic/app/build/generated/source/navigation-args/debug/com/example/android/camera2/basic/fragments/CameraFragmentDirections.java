package com.example.android.camera2.basic.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.example.android.camera2.basic.R;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class CameraFragmentDirections {
  private CameraFragmentDirections() {
  }

  @NonNull
  public static NavDirections actionCameraToPermissions() {
    return new ActionOnlyNavDirections(R.id.action_camera_to_permissions);
  }

  @NonNull
  public static ActionCameraToJpegViewer actionCameraToJpegViewer(@NonNull String filePath) {
    return new ActionCameraToJpegViewer(filePath);
  }

  public static class ActionCameraToJpegViewer implements NavDirections {
    private final HashMap arguments = new HashMap();

    @SuppressWarnings("unchecked")
    private ActionCameraToJpegViewer(@NonNull String filePath) {
      if (filePath == null) {
        throw new IllegalArgumentException("Argument \"file_path\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("file_path", filePath);
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionCameraToJpegViewer setFilePath(@NonNull String filePath) {
      if (filePath == null) {
        throw new IllegalArgumentException("Argument \"file_path\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("file_path", filePath);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionCameraToJpegViewer setOrientation(int orientation) {
      this.arguments.put("orientation", orientation);
      return this;
    }

    @NonNull
    @SuppressWarnings("unchecked")
    public ActionCameraToJpegViewer setDepth(boolean depth) {
      this.arguments.put("depth", depth);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
      Bundle __result = new Bundle();
      if (arguments.containsKey("file_path")) {
        String filePath = (String) arguments.get("file_path");
        __result.putString("file_path", filePath);
      }
      if (arguments.containsKey("orientation")) {
        int orientation = (int) arguments.get("orientation");
        __result.putInt("orientation", orientation);
      } else {
        __result.putInt("orientation", 0);
      }
      if (arguments.containsKey("depth")) {
        boolean depth = (boolean) arguments.get("depth");
        __result.putBoolean("depth", depth);
      } else {
        __result.putBoolean("depth", false);
      }
      return __result;
    }

    @Override
    public int getActionId() {
      return R.id.action_camera_to_jpeg_viewer;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getFilePath() {
      return (String) arguments.get("file_path");
    }

    @SuppressWarnings("unchecked")
    public int getOrientation() {
      return (int) arguments.get("orientation");
    }

    @SuppressWarnings("unchecked")
    public boolean getDepth() {
      return (boolean) arguments.get("depth");
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionCameraToJpegViewer that = (ActionCameraToJpegViewer) object;
      if (arguments.containsKey("file_path") != that.arguments.containsKey("file_path")) {
        return false;
      }
      if (getFilePath() != null ? !getFilePath().equals(that.getFilePath()) : that.getFilePath() != null) {
        return false;
      }
      if (arguments.containsKey("orientation") != that.arguments.containsKey("orientation")) {
        return false;
      }
      if (getOrientation() != that.getOrientation()) {
        return false;
      }
      if (arguments.containsKey("depth") != that.arguments.containsKey("depth")) {
        return false;
      }
      if (getDepth() != that.getDepth()) {
        return false;
      }
      if (getActionId() != that.getActionId()) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      int result = 1;
      result = 31 * result + (getFilePath() != null ? getFilePath().hashCode() : 0);
      result = 31 * result + getOrientation();
      result = 31 * result + (getDepth() ? 1 : 0);
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionCameraToJpegViewer(actionId=" + getActionId() + "){"
          + "filePath=" + getFilePath()
          + ", orientation=" + getOrientation()
          + ", depth=" + getDepth()
          + "}";
    }
  }
}
