package com.salonbook.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivityAddStylistBinding;
import com.salonbook.app.models.Stylist;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddStylistActivity extends AppCompatActivity {

    private ActivityAddStylistBinding binding;
    private FirebaseFirestore db;
    private boolean editMode = false;
    private String stylistId = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddStylistBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ArrayAdapter<String> specAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Constants.SPECIALIZATIONS);
        binding.spinnerSpecialization.setAdapter(specAdapter);

        editMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false);
        if (editMode) {
            binding.toolbar.setTitle(R.string.edit_stylist);
            binding.btnSaveStylist.setText(R.string.update);
            stylistId = getIntent().getStringExtra(Constants.EXTRA_STYLIST_ID);
            binding.etStylistName.setText(getIntent().getStringExtra(Constants.EXTRA_STYLIST_NAME));
            String spec = getIntent().getStringExtra(Constants.EXTRA_STYLIST_SPEC);
            if (spec != null) {
                int idx = specAdapter.getPosition(spec);
                if (idx >= 0) binding.spinnerSpecialization.setSelection(idx);
            }
            binding.etExperience.setText(String.valueOf(getIntent().getIntExtra(Constants.EXTRA_STYLIST_EXP, 0)));
            binding.switchAvailable.setChecked(getIntent().getBooleanExtra(Constants.EXTRA_STYLIST_AVAILABLE, true));
        }

        binding.btnSaveStylist.setOnClickListener(v -> saveStylist());
    }



    private void saveStylist() {
        String name = binding.etStylistName.getText().toString().trim();
        String spec = binding.spinnerSpecialization.getSelectedItem().toString();
        String expStr = binding.etExperience.getText().toString().trim();
        boolean available = binding.switchAvailable.isChecked();

        if (TextUtils.isEmpty(name)) { binding.tilStylistName.setError(getString(R.string.field_required)); return; }
        if (TextUtils.isEmpty(expStr)) { binding.tilExperience.setError(getString(R.string.field_required)); return; }

        binding.progressStylist.setVisibility(View.VISIBLE);
        binding.btnSaveStylist.setEnabled(false);

        Stylist stylist = new Stylist(name, spec, Integer.parseInt(expStr), available);

        if (editMode && stylistId != null && !stylistId.isEmpty()) {
            db.collection(Constants.COLLECTION_STYLISTS).document(stylistId)
                    .set(stylist.toMap())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.stylist_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressStylist.setVisibility(View.GONE);
                        binding.btnSaveStylist.setEnabled(true);
                        Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection(Constants.COLLECTION_STYLISTS)
                    .add(stylist.toMap())
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, R.string.stylist_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressStylist.setVisibility(View.GONE);
                        binding.btnSaveStylist.setEnabled(true);
                        Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
