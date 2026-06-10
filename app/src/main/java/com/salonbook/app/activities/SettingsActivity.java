package com.salonbook.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivitySettingsBinding;
import com.salonbook.app.utils.LocaleHelper;
import com.salonbook.app.utils.SessionManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private SessionManager sessionManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Set current language
        String currentLang = sessionManager.getLanguage();
        switch (currentLang) {
            case "hi": binding.rbHindi.setChecked(true); break;
            case "mr": binding.rbMarathi.setChecked(true); break;
            case "gu": binding.rbGujarati.setChecked(true); break;
            case "ta": binding.rbTamil.setChecked(true); break;
            case "pa": binding.rbPunjabi.setChecked(true); break;
            default: binding.rbEnglish.setChecked(true); break;
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            String langCode;
            if (checkedId == R.id.rbHindi) langCode = "hi";
            else if (checkedId == R.id.rbMarathi) langCode = "mr";
            else if (checkedId == R.id.rbGujarati) langCode = "gu";
            else if (checkedId == R.id.rbTamil) langCode = "ta";
            else if (checkedId == R.id.rbPunjabi) langCode = "pa";
            else langCode = "en";

            if (!langCode.equals(currentLang)) {
                sessionManager.setLanguage(langCode);
                Toast.makeText(this, R.string.language_changed, Toast.LENGTH_SHORT).show();

                // Restart the entire app so all activities pick up the new locale
                Intent intent = new Intent(this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
