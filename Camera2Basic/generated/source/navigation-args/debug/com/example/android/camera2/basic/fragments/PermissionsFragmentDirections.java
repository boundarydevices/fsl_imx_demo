package com.example.android.camera2.basic.fragments;

import androidx.annotation.NonNull;
import androidx.navigation.ActionOnlyNavDirections;
import androidx.navigation.NavDirections;
import com.example.android.camera2.basic.R;

public class PermissionsFragmentDirections {
  private PermissionsFragmentDirections() {
  }

  @NonNull
  public static NavDirections actionPermissionsToSelector() {
    return new ActionOnlyNavDirections(R.id.action_permissions_to_selector);
  }
}
