package com.android.example.cameraxbasic.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.android.example.cameraxbasic.R;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class VideoFragmentDirections {
  private VideoFragmentDirections() {
  }

  @NonNull
  public static ActionVideoToPreview actionVideoToPreview(@NonNull String rootDirectory) {
    return new ActionVideoToPreview(rootDirectory);
  }

  @NonNull
  public static NavDirections actionVideoToPermissions() {
    return new ActionOnlyNavDirections(R.id.action_video_to_permissions);
  }

  @NonNull
  public static NavDirections actionVideoToCamera() {
    return new ActionOnlyNavDirections(R.id.action_video_to_camera);
  }

  public static class ActionVideoToPreview implements NavDirections {
    private final HashMap arguments = new HashMap();

    private ActionVideoToPreview(@NonNull String rootDirectory) {
      if (rootDirectory == null) {
        throw new IllegalArgumentException("Argument \"root_directory\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("root_directory", rootDirectory);
    }

    @NonNull
    public ActionVideoToPreview setRootDirectory(@NonNull String rootDirectory) {
      if (rootDirectory == null) {
        throw new IllegalArgumentException("Argument \"root_directory\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("root_directory", rootDirectory);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    @NonNull
    public Bundle getArguments() {
      Bundle __result = new Bundle();
      if (arguments.containsKey("root_directory")) {
        String rootDirectory = (String) arguments.get("root_directory");
        __result.putString("root_directory", rootDirectory);
      }
      return __result;
    }

    @Override
    public int getActionId() {
      return R.id.action_video_to_preview;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getRootDirectory() {
      return (String) arguments.get("root_directory");
    }

    @Override
    public boolean equals(Object object) {
      if (this == object) {
          return true;
      }
      if (object == null || getClass() != object.getClass()) {
          return false;
      }
      ActionVideoToPreview that = (ActionVideoToPreview) object;
      if (arguments.containsKey("root_directory") != that.arguments.containsKey("root_directory")) {
        return false;
      }
      if (getRootDirectory() != null ? !getRootDirectory().equals(that.getRootDirectory()) : that.getRootDirectory() != null) {
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
      result = 31 * result + (getRootDirectory() != null ? getRootDirectory().hashCode() : 0);
      result = 31 * result + getActionId();
      return result;
    }

    @Override
    public String toString() {
      return "ActionVideoToPreview(actionId=" + getActionId() + "){"
          + "rootDirectory=" + getRootDirectory()
          + "}";
    }
  }
}
