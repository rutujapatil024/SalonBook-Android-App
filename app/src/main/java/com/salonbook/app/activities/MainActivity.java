package com.salonbook.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.salonbook.app.R;
import com.salonbook.app.databinding.ActivityMainBinding;
import com.salonbook.app.fragments.*;
import com.salonbook.app.receivers.NetworkReceiver;
import com.salonbook.app.utils.Constants;
import com.salonbook.app.utils.LocaleHelper;
import com.salonbook.app.utils.SessionManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private SessionManager sessionManager;
    private ServicesFragment servicesFragment;
    private StylistsFragment stylistsFragment;
    private AppointmentsFragment appointmentsFragment;
    private ProfileFragment profileFragment;
    private DashboardFragment dashboardFragment;
    private Fragment activeFragment;
    private BroadcastReceiver networkReceiver;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.attachBaseContext(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sessionManager = new SessionManager(this);
        setSupportActionBar(binding.toolbar);

        initFragments();
        setupBottomNavigation();
        registerNetworkReceiver();
    }

    private void initFragments() {
        servicesFragment = new ServicesFragment();
        appointmentsFragment = new AppointmentsFragment();
        profileFragment = new ProfileFragment();

        if (sessionManager.isOwner()) {
            dashboardFragment = new DashboardFragment();
            stylistsFragment = new StylistsFragment();
        }
    }

    private void setupBottomNavigation() {
        if (sessionManager.isOwner()) {
            binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_owner);
            loadFragment(dashboardFragment);
        } else {
            binding.bottomNavigation.inflateMenu(R.menu.bottom_nav_customer);
            loadFragment(servicesFragment);
        }

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard) { loadFragment(dashboardFragment); return true; }
            if (id == R.id.nav_services) { loadFragment(servicesFragment); return true; }
            if (id == R.id.nav_stylists) { loadFragment(stylistsFragment); return true; }
            if (id == R.id.nav_appointments) { loadFragment(appointmentsFragment); return true; }
            if (id == R.id.nav_profile) { loadFragment(profileFragment); return true; }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        if (fragment == null) return;
        activeFragment = fragment;
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        // Refresh the options menu based on active fragment
        invalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) { return false; }
                @Override
                public boolean onQueryTextChange(String newText) {
                    if (activeFragment instanceof ServicesFragment) {
                        ((ServicesFragment) activeFragment).filterServices(newText);
                    } else if (activeFragment instanceof StylistsFragment) {
                        ((StylistsFragment) activeFragment).filterStylists(newText);
                    } else if (activeFragment instanceof AppointmentsFragment) {
                        ((AppointmentsFragment) activeFragment).filterByQuery(newText);
                    }
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        MenuItem filterItem = menu.findItem(R.id.action_filter);
        MenuItem settingsItem = menu.findItem(R.id.action_settings);
        MenuItem logoutItem = menu.findItem(R.id.action_logout);

        boolean isProfile = activeFragment instanceof ProfileFragment;
        boolean isDashboard = activeFragment instanceof DashboardFragment;
        boolean isServices = activeFragment instanceof ServicesFragment;
        boolean isStylists = activeFragment instanceof StylistsFragment;
        boolean isAppointments = activeFragment instanceof AppointmentsFragment;

        // Show search only on Services, Stylists, and Appointments
        searchItem.setVisible(isServices || isStylists || isAppointments);

        // Show filter only on Services and Appointments
        filterItem.setVisible(isServices || isAppointments);

        // Always show settings and logout
        settingsItem.setVisible(true);
        logoutItem.setVisible(true);

        // Hide all menu items on Profile (only show settings + logout via overflow)
        if (isProfile) {
            searchItem.setVisible(false);
            filterItem.setVisible(false);
        }

        // Hide search and filter on Dashboard
        if (isDashboard) {
            searchItem.setVisible(false);
            filterItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_filter) {
            showCategoryFilter();
            return true;
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showCategoryFilter() {
        String[] categories = new String[Constants.CATEGORIES.length + 1];
        categories[0] = getString(R.string.all_categories);
        System.arraycopy(Constants.CATEGORIES, 0, categories, 1, Constants.CATEGORIES.length);

        new MaterialAlertDialogBuilder(this, R.style.SalonBook_Dialog)
                .setTitle(R.string.filter_category)
                .setItems(categories, (dialog, which) -> {
                    String cat = which == 0 ? null : categories[which];
                    if (activeFragment instanceof ServicesFragment) {
                        ((ServicesFragment) activeFragment).filterByCategory(cat);
                    } else if (activeFragment instanceof AppointmentsFragment) {
                        ((AppointmentsFragment) activeFragment).filterByCategory(cat);
                    }
                }).show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this, R.style.SalonBook_Dialog)
                .setTitle(R.string.logout)
                .setMessage(R.string.confirm_logout)
                .setPositiveButton(R.string.yes, (d, w) -> {
                    FirebaseAuth.getInstance().signOut();
                    sessionManager.logout();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }).setNegativeButton(R.string.no, null).show();
    }

    private void registerNetworkReceiver() {
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isConnected = intent.getBooleanExtra(NetworkReceiver.EXTRA_IS_CONNECTED, true);
                if (!isConnected) {
                    Snackbar.make(binding.getRoot(), R.string.no_internet, Snackbar.LENGTH_LONG).show();
                }
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(networkReceiver,
                new IntentFilter(NetworkReceiver.NETWORK_STATUS_ACTION));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (networkReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(networkReceiver);
        }
    }
}
