package com.example.arduinoledmemory;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.arduinoledmemory.dialogs.CustomProgressDialog;
import com.example.arduinoledmemory.objects.DiscoveredBluetoothDevice;
import com.example.arduinoledmemory.profile.callback.LedMemCmdCallback;
import com.example.arduinoledmemory.profile.data.LedMemory;
import com.example.arduinoledmemory.viewmodels.GameViewModel;

import static com.example.arduinoledmemory.MainMenu.EXTRA_DEVICE;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class InGame extends AppCompatActivity {

    private static final String TAG = "LedMem.InGame";

    private static final int INDEX_LED_R = 0, INDEX_LED_G = 1, INDEX_LED_B = 2;

    private boolean bConnectedToDevice = false, bConnectionInProgress = false;
    private GameViewModel viewModel;
    private DiscoveredBluetoothDevice device;
    private CustomProgressDialog customProgressDialog = CustomProgressDialog.getInstance();
    private boolean bWaitingForAnswerResponse = false;
    private long prev = 0;
    private boolean bGodOn = false;

    private TextView tv_level, tv_gameover;
    private View ansBtnHolder;
    private ImageButton btn_r, btn_g, btn_b;
    //private LedMemory.Game_State currentGameState = LedMemory.Game_State.None;

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

    @Override @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_in_game);

        mVisible = true;
        //mControlsView = findViewById(R.id.activity_main_menu_fullscreen_content_controls);
        mContentView = findViewById(R.id.activity_in_game_fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        CustomProgressDialog.registerActivity(this);
        customProgressDialog = CustomProgressDialog.getInstance();

        btn_r = findViewById(R.id.activity_in_game_btn_led_red);
        btn_g = findViewById(R.id.activity_in_game_btn_led_green);
        btn_b = findViewById(R.id.activity_in_game_btn_led_blue);
        Button btn_quit = findViewById(R.id.activity_in_game_btn_home);
        Button btn_restart = findViewById(R.id.activity_in_game_btn_restart);
        Button btn_god = findViewById(R.id.activity_in_game_btn_godmode);
        tv_level = findViewById(R.id.activity_in_game_text_view_level);
        tv_gameover = findViewById(R.id.activity_in_game_text_view_gameover);
        ansBtnHolder = findViewById(R.id.activity_in_game_btn_holder);

        btn_r.setOnClickListener(buttonClickListener);
        btn_g.setOnClickListener(buttonClickListener);
        btn_b.setOnClickListener(buttonClickListener);
        btn_quit.setOnClickListener(buttonClickListener);
        btn_restart.setOnClickListener(buttonClickListener);

        btn_god.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getActionMasked();

                if (action == MotionEvent.ACTION_DOWN)
                    prev = System.currentTimeMillis();
                else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    if(prev > 0) {
                        long curr = System.currentTimeMillis();
                        if (curr - prev >= 1500) {
                            if(bConnectedToDevice) {
                                bGodOn = !bGodOn;
                                viewModel.sendCmd_God(bGodOn);
                            }
                        }
                        prev = 0;
                    }
                }
                return false;
            }
        });

        final Intent intent = getIntent();
        device = intent.getParcelableExtra(EXTRA_DEVICE);
        if(device != null) {
            bConnectedToDevice = false;
            bConnectionInProgress = true;

            // setup view model
            viewModel = new ViewModelProvider(this).get(GameViewModel.class);
            viewModel.setCallback(ledMemDataCallback);
            setupViewModel(device);
            viewModel.connect(device);
        }
        else {
            viewModel = null;
            bConnectedToDevice = false;
            bConnectionInProgress = false;
            quitGame();
        }
    }

    private View.OnClickListener buttonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(!bConnectedToDevice)
                return;

            switch (v.getId()){
                case R.id.activity_in_game_btn_led_red:
                    if(bWaitingForAnswerResponse) {
                        customProgressDialog.setMessage("please wait...");
                        customProgressDialog.show();
                        viewModel.sendCmd_InputAnswer(INDEX_LED_R);
                        toggleAnswerInputButtons(false);
                    }
                    break;
                case R.id.activity_in_game_btn_led_green:
                    if(bWaitingForAnswerResponse) {
                        customProgressDialog.setMessage("please wait...");
                        customProgressDialog.show();
                        viewModel.sendCmd_InputAnswer(INDEX_LED_G);
                        toggleAnswerInputButtons(false);
                    }
                    break;
                case R.id.activity_in_game_btn_led_blue:
                    if(bWaitingForAnswerResponse) {
                        customProgressDialog.setMessage("please wait...");
                        customProgressDialog.show();
                        viewModel.sendCmd_InputAnswer(INDEX_LED_B);
                        toggleAnswerInputButtons(false);
                    }
                    break;
                case R.id.activity_in_game_btn_home:
                    customProgressDialog.setMessage("please wait...");
                    customProgressDialog.show();
                    viewModel.sendCmd_EndGame();

                    bGodOn = false;
                    viewModel.sendCmd_God(false);

                    break;
                case R.id.activity_in_game_btn_restart:
                    customProgressDialog.setMessage("please wait...");
                    customProgressDialog.show();
                    viewModel.sendCmd_StartGame();

                    bGodOn = false;
                    viewModel.sendCmd_God(false);

                    break;
                default:
                    break;
            }
        }
    };

    private void setupViewModel(DiscoveredBluetoothDevice device) {
        viewModel.getConnectionState().observe(this, state -> {
            switch (state.getState()) {
                case CONNECTING:
                case DISCONNECTING:
                case INITIALIZING:
                    customProgressDialog.setMessage("connecting...");
                    customProgressDialog.show();
                    bConnectedToDevice = false;
                    bConnectionInProgress = true;
                    break;
                case READY:
                    bConnectedToDevice = true;
                    bConnectionInProgress = false;
                    customProgressDialog.dismiss();
                    break;
                case DISCONNECTED:
                    bConnectedToDevice = false;
                    bConnectionInProgress = false;
                    // TODO: this should be handled
                    break;
            }
        });
    }

    private	final LedMemCmdCallback ledMemDataCallback = new LedMemCmdCallback() {
        @Override
        public void onCommandResponse(boolean successful) {
            //Toast.makeText(InGame.this, "cmd response: " + successful, Toast.LENGTH_SHORT).show();
            //if(!bWaitingForAnswerResponse)
                //customProgressDialog.dismiss();
        }

        @Override
        public void onError(String msg) {
            //Toast.makeText(InGame.this, "error: " + msg, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onGameStateUpdate(String gameState) {

            bWaitingForAnswerResponse = false;

            if (gameState.equals(LedMemory.Game_State.Just_Started.name())) {
                customProgressDialog.dismiss();
                startGame();
            }
            if (gameState.equals(LedMemory.Game_State.Alert_Leds.name())) {
                // disable controls and ask user to watch the board
                customProgressDialog.setMessage("stay alert!");
                customProgressDialog.show();
            }
            else if (gameState.equals(LedMemory.Game_State.Show_Problem.name())) {
                // ask user to keep looking at the board
                customProgressDialog.setMessage("pattern showing...");
                customProgressDialog.show();
            }
            else if (gameState.equals(LedMemory.Game_State.Wait_For_Answer_Input.name())) {
                // allow user to input answer
                customProgressDialog.dismiss();
                bWaitingForAnswerResponse = true;
                toggleAnswerInputButtons(true);
            }
            else if (gameState.equals(LedMemory.Game_State.Game_Over.name())) {
                // game over, hide answer input buttons, show game over title and keep showing level
                customProgressDialog.dismiss();
                tv_gameover.setVisibility(View.VISIBLE);
            }
            else if (gameState.equals(LedMemory.Game_State.None.name())) {
                // game quit, return to main menu
                customProgressDialog.dismiss();
                quitGame();
            }

            // enable answer buttons if game is in state of 'waiting for user response'
            if(bWaitingForAnswerResponse)
                toggleAnswerInputView(true);
            else
                toggleAnswerInputView(false);
        }

        @Override
        public void onLevelUpdate(int level) {
            tv_level.setText("Level: " + level);
        }

        @Override
        public void onAnswerResponse(boolean correct) {
            if(bWaitingForAnswerResponse) {
                customProgressDialog.dismiss();
                toggleAnswerInputButtons(true);
            }
        }

        @Override
        public void onInvalidData(@NonNull String strData) {
            // parsing failed if reaches here
            Log.d(TAG, "invalid data: " + strData);
            Toast.makeText(InGame.this, "data parse error!", Toast.LENGTH_SHORT).show();
        }
    };

    private void startGame() {
        tv_gameover.setVisibility(View.GONE);
        toggleAnswerInputView(false);
        bWaitingForAnswerResponse = false;
    }

    private void quitGame() {
        final Intent intent = new Intent(this, MainMenu.class);
        if(device != null)
            intent.putExtra(MainMenu.EXTRA_DEVICE, device);
        startActivity(intent);
        finish();
    }

    private void toggleAnswerInputView(boolean enable) {
        ansBtnHolder.setVisibility(enable ? View.VISIBLE : View.GONE);
    }

    private void toggleAnswerInputButtons(boolean enabled) {
        btn_r.setEnabled(enabled);
        btn_g.setEnabled(enabled);
        btn_b.setEnabled(enabled);
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
        CustomProgressDialog.registerActivity(this);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed(){
        if(viewModel != null && device != null && bConnectedToDevice)
            viewModel.sendCmd_EndGame();
        quitGame();
        this.finish();
        super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        CustomProgressDialog.unregisterActivity(this);
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
    }

}