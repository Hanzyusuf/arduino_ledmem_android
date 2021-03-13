package com.example.arduinoledmemory;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arduinoledmemory.dialogs.CustomProgressDialog;
import com.example.arduinoledmemory.objects.DiscoveredBluetoothDevice;
import com.example.arduinoledmemory.profile.callback.LedMemCmdCallback;
import com.example.arduinoledmemory.profile.data.LedMemory;
import com.example.arduinoledmemory.viewmodels.GameViewModel;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainMenu extends AppCompatActivity {

    //private static int REQUEST_ENABLE_BT = 1011;

    private static final String TAG = "LedMem.MainMenu";
    public static final String EXTRA_DEVICE = "com.example.arduinoledmemory.EXTRA_DEVICE";

    private boolean bConnectedToDevice = false, bConnectionInProgress = false;
    private GameViewModel viewModel;
    private TextView textView_connectionStatus;
    private Button btn_connect;
    private DiscoveredBluetoothDevice device;

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
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    /*private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            switch (motionEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (AUTO_HIDE) {
                        delayedHide(AUTO_HIDE_DELAY_MILLIS);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    view.performClick();
                    break;
                default:
                    break;
            }
            return false;
        }
    };*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main_menu);

        mVisible = true;
        //mControlsView = findViewById(R.id.activity_main_menu_fullscreen_content_controls);
        mContentView = findViewById(R.id.activity_main_menu_fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        CustomProgressDialog.registerActivity(this);
        CustomProgressDialog customProgressDialog = CustomProgressDialog.getInstance();

        Button btn_easy = findViewById(R.id.activity_main_menu_btn_easy);
        Button btn_medium = findViewById(R.id.activity_main_menu_btn_medium);
        Button btn_hard = findViewById(R.id.activity_main_menu_btn_hard);
        textView_connectionStatus = findViewById(R.id.activity_main_menu_tv_connection_status);
        btn_connect = findViewById(R.id.activity_main_menu_btn_connect);

        btn_easy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewModel != null && device != null && bConnectedToDevice) {
                    viewModel.sendCmd_SetDifficulty(LedMemory.Game_Difficulty.EASY);
                    viewModel.sendCmd_StartGame();
                }
                else
                    Toast.makeText(MainMenu.this, "device not ready!", Toast.LENGTH_SHORT).show();
            }
        });
        btn_medium.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewModel != null && device != null && bConnectedToDevice) {
                    viewModel.sendCmd_SetDifficulty(LedMemory.Game_Difficulty.MEDIUM);
                    viewModel.sendCmd_StartGame();
                }
                else
                    Toast.makeText(MainMenu.this, "device not ready!", Toast.LENGTH_SHORT).show();
            }
        });
        btn_hard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(viewModel != null && device != null && bConnectedToDevice) {
                    viewModel.sendCmd_SetDifficulty(LedMemory.Game_Difficulty.HARD);
                    viewModel.sendCmd_StartGame();
                }
                else
                    Toast.makeText(MainMenu.this, "device not ready!", Toast.LENGTH_SHORT).show();
            }
        });

        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        if(device != null) {
            textView_connectionStatus.setText("CONNECTING: " + device.getName());
            textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_CONNECTING));
            btn_connect.setText("DISCONNECT");
            btn_connect.setEnabled(false);

            bConnectedToDevice = false;
            bConnectionInProgress = true;

            // setup view model
            viewModel = new ViewModelProvider(this).get(GameViewModel.class);
            viewModel.setCallback(ledMemDataCallback);
            setupViewModel(device);
            viewModel.connect(device);
        }
        else{
            textView_connectionStatus.setText("DISCONNECTED");
            textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_DISCONNECTED));
            btn_connect.setText("FIND DEVICES");

            viewModel = null;
            bConnectedToDevice = false;
            bConnectionInProgress = false;
        }

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if connection is not already in progress, else don't do anything
                if(!bConnectionInProgress) {
                    if (!bConnectedToDevice) {
                        final Intent intent = new Intent(MainMenu.this, Scanner.class);
                        startActivity(intent);
                        finish();
                    } else {
                        textView_connectionStatus.setText("DISCONNECTED");
                        textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_DISCONNECTED));
                        btn_connect.setText("FIND DEVICES");

                        viewModel.disconnect();
                        bConnectedToDevice = false;
                    }
                }
            }
        });
    }

    private void setupViewModel(DiscoveredBluetoothDevice device) {
        final String deviceName = device.getName();

        viewModel.getConnectionState().observe(this, state -> {
            switch (state.getState()) {
                case CONNECTING:
                    textView_connectionStatus.setText("CONNECTING: " + deviceName);
                    textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_CONNECTING));
                    btn_connect.setText("DISCONNECT");

                    bConnectedToDevice = false;
                    bConnectionInProgress = true;
                    btn_connect.setEnabled(false);

                    break;
                case INITIALIZING:
                    textView_connectionStatus.setText("INITIALIZING: " + deviceName);
                    textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_INITIALIZING));
                    btn_connect.setText("DISCONNECT");

                    bConnectedToDevice = false;
                    bConnectionInProgress = true;
                    btn_connect.setEnabled(false);

                    break;
                case READY:
                    textView_connectionStatus.setText("CONNECTED: " + deviceName);
                    textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_CONNECTED));
                    btn_connect.setText("DISCONNECT");

                    bConnectedToDevice = true;
                    bConnectionInProgress = false;
                    btn_connect.setEnabled(true);

                    break;
                case DISCONNECTING:
                    textView_connectionStatus.setText("DISCONNECTING");
                    textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_DISCONNECTED));
                    btn_connect.setText("FIND DEVICES");

                    bConnectedToDevice = false;
                    bConnectionInProgress = true;
                    btn_connect.setEnabled(false);

                    break;
                case DISCONNECTED:
                    textView_connectionStatus.setText("DISCONNECTED");
                    textView_connectionStatus.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.STATUS_DISCONNECTED));
                    btn_connect.setText("FIND DEVICES");

                    bConnectedToDevice = false;
                    bConnectionInProgress = false;
                    btn_connect.setEnabled(true);

                    break;
            }
        });
    }

    private	final LedMemCmdCallback ledMemDataCallback = new LedMemCmdCallback() {
        @Override
        public void onCommandResponse(boolean successful) {
            //Toast.makeText(MainMenu.this, "cmd response: " + successful, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String msg) {
            //Toast.makeText(MainMenu.this, "error: " + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onGameStateUpdate(String gameState) {
            if (gameState.equals(LedMemory.Game_State.Just_Started.name())) {
                // game just started
                if(device != null) {
                    viewModel.removeCallback();
                    final Intent intent = new Intent(MainMenu.this, InGame.class);
                    intent.putExtra(MainMenu.EXTRA_DEVICE, device);
                    startActivity(intent);
                    finish();
                }
            }
            else if (!gameState.equals(LedMemory.Game_State.None.name())) {
                // send cmd to stop game since game seems to be running already while client is on main menu
                viewModel.sendCmd_EndGame();
            }
        }

        @Override
        public void onLevelUpdate(int level) {

        }

        @Override
        public void onAnswerResponse(boolean correct) {

        }

        @Override
        public void onInvalidData(@NonNull String strData) {
            // parsing failed if reaches here
            Log.d(TAG, "invalid data: " + strData);
            Toast.makeText(MainMenu.this, "data parse error!", Toast.LENGTH_SHORT).show();
        }
    };

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
        CustomProgressDialog.registerActivity(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed(){
        this.finish();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        CustomProgressDialog.unregisterActivity(this);
        super.onDestroy();
    }

    @Override
    public void finish(){
        super.finish();
    }
}