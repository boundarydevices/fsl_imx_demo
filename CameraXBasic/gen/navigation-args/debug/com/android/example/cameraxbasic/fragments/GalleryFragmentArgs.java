package com.android.example.cameraxbasic.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.navigation.NavArgs;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.HashMap;

public class GalleryFragmentArgs implements NavArgs {
  private final HashMap arguments = new HashMap();

  private GalleryFragmentArgs() {
  }

  private GalleryFragmentArgs(HashMap argumentsMap) {
    this.arguments.putAll(argumentsMap);
  }

  @NonNull
  @SuppressWarnings("unchecked")
  public static GalleryFragmentArgs fromBundle(@NonNull Bundle bundle) {
    GalleryFragmentArgs __result = new GalleryFragmentArgs();
    bundle.setClassLoader(GalleryFragmentArgs.class.getClassLoader());
    if (bundle.containsKey("root_directory")) {
      String rootDirectory;
      rootDirectory = bundle.getString("root_directory");
      if (rootDirectory == null) {
        throw new IllegalArgumentException("Argument \"root_directory\" is marked as non-null but was passed a null value.");
      }
      __result.arguments.put("root_directory", rootDirectory);
    } else {
      throw new IllegalArgumentException("Required argument \"root_directory\" is missing and does not have an android:defaultValue");
    }
    return __result;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public String getRootDirectory() {
    return (String) arguments.get("root_directory");
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public Bundle toBundle() {
    Bundle __result = new Bundle();
    if (arguments.containsKey("root_directory")) {
      String rootDirectory = (String) arguments.get("root_directory");
      __result.putString("root_directory", rootDirectory);
    }
    return __result;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
        return true;
    }
    if (object == null || getClass() != object.getClass()) {
        return false;
    }
    GalleryFragmentArgs that = (GalleryFragmentArgs) object;
    if (arguments.containsKey("root_directory") != that.arguments.containsKey("root_directory")) {
      return false;
    }
    if (getRootDirectory() != null ? !getRootDirectory().equals(that.getRootDirectory()) : that.getRootDirectory() != null) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + (getRootDirectory() != null ? getRootDirectory().hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "GalleryFragmentArgs{"
        + "rootDirectory=" + getRootDirectory()
        + "}";
  }

  public static class Builder {
    private final HashMap arguments = new HashMap();

    public Builder(GalleryFragmentArgs original) {
      this.arguments.putAll(original.arguments);
    }

    public Builder(@NonNull String rootDirectory) {
      if (rootDirectory == null) {
        throw new IllegalArgumentException("Argument \"root_directory\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("root_directory", rootDirectory);
    }

    @NonNull
    public GalleryFragmentArgs build() {
      GalleryFragmentArgs result = new GalleryFragmentArgs(arguments);
      return result;
    }

    @NonNull
    public Builder setRootDirectory(@NonNull String rootDirectory) {
      if (rootDirectory == null) {
        throw new IllegalArgumentException("Argument \"root_directory\" is marked as non-null but was passed a null value.");
      }
      this.arguments.put("root_directory", rootDirectory);
      return this;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    public String getRootDirectory() {
      return (String) arguments.get("root_directory");
    }
  }
}
