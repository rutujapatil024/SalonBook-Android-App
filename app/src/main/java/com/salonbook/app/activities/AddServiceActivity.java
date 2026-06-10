package com.salonbook.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivityAddServiceBinding;
import com.salonbook.app.models.Service;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;

public class AddServiceActivity extends AppCompatActivity {

    private ActivityAddServiceBinding binding;
    private FirebaseFirestore db;
    private boolean editMode = false;
    private String serviceId = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddServiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Category spinner
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Constants.CATEGORIES);
        binding.spinnerCategory.setAdapter(catAdapter);

        // Check edit mode
        editMode = getIntent().getBooleanExtra(Constants.EXTRA_EDIT_MODE, false);
        if (editMode) {
            binding.toolbar.setTitle(R.string.edit_service);
            binding.btnSaveService.setText(R.string.update);
            serviceId = getIntent().getStringExtra(Constants.EXTRA_SERVICE_ID);
            binding.etServiceName.setText(getIntent().getStringExtra(Constants.EXTRA_SERVICE_NAME));
            String category = getIntent().getStringExtra(Constants.EXTRA_SERVICE_CATEGORY);
            if (category != null) {
                int idx = catAdapter.getPosition(category);
                if (idx >= 0) binding.spinnerCategory.setSelection(idx);
            }
            binding.etDuration.setText(String.valueOf(getIntent().getIntExtra(Constants.EXTRA_SERVICE_DURATION, 0)));
            binding.etPrice.setText(String.valueOf(getIntent().getDoubleExtra(Constants.EXTRA_SERVICE_PRICE, 0)));
        }

        binding.btnSaveService.setOnClickListener(v -> saveService());
    }

    private void saveService() {
        String name = binding.etServiceName.getText().toString().trim();
        String category = binding.spinnerCategory.getSelectedItem().toString();
        String durationStr = binding.etDuration.getText().toString().trim();
        String priceStr = binding.etPrice.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { binding.tilServiceName.setError(getString(R.string.field_required)); return; }
        if (TextUtils.isEmpty(durationStr)) { binding.tilDuration.setError(getString(R.string.field_required)); return; }
        if (TextUtils.isEmpty(priceStr)) { binding.tilPrice.setError(getString(R.string.field_required)); return; }

        binding.progressService.setVisibility(View.VISIBLE);
        binding.btnSaveService.setEnabled(false);

        Service service = new Service(name, category, Integer.parseInt(durationStr), Double.parseDouble(priceStr));

        if (editMode && serviceId != null && !serviceId.isEmpty()) {
            db.collection(Constants.COLLECTION_SERVICES).document(serviceId)
                    .set(service.toMap())
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, R.string.service_updated, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressService.setVisibility(View.GONE);
                        binding.btnSaveService.setEnabled(true);
                        Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection(Constants.COLLECTION_SERVICES)
                    .add(service.toMap())
                    .addOnSuccessListener(docRef -> {
                        Toast.makeText(this, R.string.service_saved, Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        binding.progressService.setVisibility(View.GONE);
                        binding.btnSaveService.setEnabled(true);
                        Toast.makeText(this, R.string.error_occurred, Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
