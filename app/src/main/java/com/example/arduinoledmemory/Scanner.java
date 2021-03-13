package com.example.arduinoledmemory;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.example.arduinoledmemory.adapters.BTDeviceAdapter;
import com.example.arduinoledmemory.objects.DiscoveredBluetoothDevice;
import com.example.arduinoledmemory.utils.Utils;
import com.example.arduinoledmemory.viewmodels.ScannerStateLiveData;
import com.example.arduinoledmemory.viewmodels.ScannerViewModel;

import java.util.ArrayList;

public class Scanner extends AppCompatActivity implements BTDeviceAdapter.BTDevicePickListener {

    private ArrayList<DiscoveredBluetoothDevice> devices = new ArrayList<>();
    private BTDeviceAdapter btDeviceAdapter;
    private ScannerViewModel scannerViewModel;
    private static final int REQUEST_ACCESS_FINE_LOCATION = 1022; // random number

    // additional views for errors and states
    private View mainView;
    //private View scanningProgressBar;
    private View emptyView;
    private View noLocationPermissionView;
    private View noBluetoothView;
    private Button info_grantPermissionButton;
    private Button info_permissionSettingsButton;
    private View info_noLocationView;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_scanner);

        mVisible = true;
        mContentView = findViewById(R.id.activity_scanner_root);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Create view model containing utility methods for scanning
        scannerViewModel = new ViewModelProvider(this).get(ScannerViewModel.class);
        scannerViewModel.getScannerState().observe(this, this::startScan);

        RecyclerView recyclerView = findViewById(R.id.activity_scanner_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        final RecyclerView.ItemAnimator animator = recyclerView.getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }

        btDeviceAdapter = new BTDeviceAdapter(this, devices, scannerViewModel.getDevices());
        btDeviceAdapter.setBTDevicePickListener(this);
        recyclerView.setAdapter(btDeviceAdapter);

        mainView = findViewById(R.id.activity_scanner_mainView);
        //scanningProgressBar = findViewById(R.id.activity_scanner_progressbar);
        emptyView = findViewById(R.id.activity_scanner_include_view_no_devices);
        noLocationPermissionView = findViewById(R.id.activity_scanner_include_view_no_location);
        noBluetoothView = findViewById(R.id.activity_scanner_include_view_bluetooth_off);
        info_grantPermissionButton = findViewById(R.id.info_no_permission_action_grant_location_permission);
        info_permissionSettingsButton = findViewById(R.id.info_no_permission_action_permission_settings);
        info_noLocationView = findViewById(R.id.info_no_devices_no_location);
        Button info_noDevices_enableLocationButton = findViewById(R.id.info_no_devices_action_enable_location);
        Button info_noBluetooth_enableBluetoothButton = findViewById(R.id.info_no_bluetooth_action_enable_bluetooth);

        info_grantPermissionButton.setOnClickListener(onClickListener);
        info_permissionSettingsButton.setOnClickListener(onClickListener);
        info_noDevices_enableLocationButton.setOnClickListener(onClickListener);
        info_noBluetooth_enableBluetoothButton.setOnClickListener(onClickListener);

    }

    @Override
    public void onBTDevicePicked(DiscoveredBluetoothDevice device) {
        if(device.getGameCompatibility() == DiscoveredBluetoothDevice.GameCompatibility.COMPATIBLE) {
            final Intent intent = new Intent(this, MainMenu.class);
            intent.putExtra(MainMenu.EXTRA_DEVICE, device);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, "incompatible device selected! : " + device.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACCESS_FINE_LOCATION) {
            scannerViewModel.refresh();
        }
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent;
            switch (v.getId()){
                case R.id.info_no_permission_action_grant_location_permission:
                    Utils.markLocationPermissionRequested(Scanner.this);
                    ActivityCompat.requestPermissions(Scanner.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ACCESS_FINE_LOCATION);
                    break;
                case R.id.info_no_permission_action_permission_settings:
                    intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.fromParts("package", getPackageName(), null));
                    startActivity(intent);
                    break;
                case R.id.info_no_devices_action_enable_location:
                    intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                    break;
                case R.id.info_no_bluetooth_action_enable_bluetooth:
                    intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivity(intent);
                    break;
            }
        }
    };

    /**
     * Start scanning for Bluetooth devices or displays a message based on the scanner state.
     */
    private void startScan(final ScannerStateLiveData state) {
        // First, check the Location permission. This is required on Marshmallow onwards in order
        // to scan for Bluetooth LE devices.
        if (Utils.isLocationPermissionsGranted(this)) {
            //Toast.makeText(this, "location permission granted!", Toast.LENGTH_SHORT).show();
            noLocationPermissionView.setVisibility(View.GONE);

            // Bluetooth must be enabled.
            if (state.isBluetoothEnabled()) {
                //Toast.makeText(this, "bluetooth permission granted!", Toast.LENGTH_SHORT).show();
                noBluetoothView.setVisibility(View.GONE);

                // We are now OK to start scanning.
                scannerViewModel.startScan();
                mainView.setVisibility(View.VISIBLE);
                //scanningProgressBar.setVisibility(View.VISIBLE);

                if (!state.hasRecords()) {
                    //Toast.makeText(this, "no records!", Toast.LENGTH_SHORT).show();
                    emptyView.setVisibility(View.VISIBLE);

                    if (!Utils.isLocationRequired(this) || Utils.isLocationEnabled(this)) {
                        info_noLocationView.setVisibility(View.INVISIBLE);
                    } else {
                        info_noLocationView.setVisibility(View.VISIBLE);
                    }
                } else {
                    //Toast.makeText(this, "records found!", Toast.LENGTH_SHORT).show();
                    emptyView.setVisibility(View.GONE);
                }
            } else {
                //Toast.makeText(this, "bluetooth permission not granted!", Toast.LENGTH_SHORT).show();
                noBluetoothView.setVisibility(View.VISIBLE);
                mainView.setVisibility(View.GONE);
                //scanningProgressBar.setVisibility(View.INVISIBLE);
                emptyView.setVisibility(View.GONE);
                clear();
            }
        } else {
            //Toast.makeText(this, "location permission not granted!", Toast.LENGTH_SHORT).show();
            noLocationPermissionView.setVisibility(View.VISIBLE);
            noBluetoothView.setVisibility(View.GONE);
            mainView.setVisibility(View.GONE);
            //scanningProgressBar.setVisibility(View.INVISIBLE);
            emptyView.setVisibility(View.GONE);

            final boolean deniedForever = Utils.isLocationPermissionDeniedForever(this);
            info_grantPermissionButton.setVisibility(deniedForever ? View.GONE : View.VISIBLE);
            info_permissionSettingsButton.setVisibility(deniedForever ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * stop scanning for bluetooth devices.
     */
    private void stopScan() {
        scannerViewModel.stopScan();
    }

    /**
     * Clears the list of devices, which will notify the observer.
     */
    private void clear() {
        scannerViewModel.getDevices().clear();
        scannerViewModel.getScannerState().clearRecords();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    // default full screen activity function
    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    // default full screen activity function
    private void hide() {
        mVisible = false;

        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    // default full screen activity function
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        delayedHide(750);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    // default full screen activity function
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        clear();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopScan();
    }

    @Override
    public void onBackPressed(){
        this.finish();
        super.onBackPressed();
        final Intent intent = new Intent(this, MainMenu.class);
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void finish(){
        super.finish();
    }

}