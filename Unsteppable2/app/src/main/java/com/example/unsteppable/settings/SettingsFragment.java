package com.example.unsteppable.settings;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.unsteppable.R;

public class SettingsFragment extends PreferenceFragmentCompat {
    //https://developer.android.com/guide/topics/ui/settings/customize-your-settings
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {


        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        EditTextPreference numberPreference = findPreference(getResources().getString(R.string.base_goal));

        if (numberPreference != null) {
            numberPreference.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                        }
                    });
        }
    }
}