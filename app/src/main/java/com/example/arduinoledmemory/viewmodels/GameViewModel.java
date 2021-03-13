package com.example.arduinoledmemory.viewmodels;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.arduinoledmemory.objects.DiscoveredBluetoothDevice;
import com.example.arduinoledmemory.profile.GameManager;
import com.example.arduinoledmemory.profile.callback.LedMemCmdCallback;
import com.example.arduinoledmemory.profile.data.LedMemory;

import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.state.ConnectionState;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

public class GameViewModel extends AndroidViewModel {

	private final GameManager gameManager;
	private BluetoothDevice device;

	public GameViewModel(@NonNull final Application application) {
		super(application);

		// Initialize the manager.
		gameManager = new GameManager(application);
	}

	public void setCallback(LedMemCmdCallback ledMemCmdCallback) {
		gameManager.updateCallback(ledMemCmdCallback);
	}

	public void removeCallback() {
		gameManager.removeCallback();
	}

	public LiveData<ConnectionState> getConnectionState() {
		return gameManager.getState();
	}

	/**
	 * Connect to the given peripheral.
	 *
	 * @param target the target device.
	 */
	public void connect(@NonNull final DiscoveredBluetoothDevice target) {
		// Prevent from calling again when called again (screen orientation changed).
		if (device == null) {
			device = target.getDevice();
			final LogSession logSession = Logger.newSession(getApplication(), null, target.getAddress(), target.getName());
			gameManager.setLogger(logSession);
			reconnect();
		}
	}

	/**
	 * Reconnects to previously connected device.
	 * If this device was not supported, its services were cleared on disconnection, so
	 * reconnection may help.
	 */
	public void reconnect() {
		if (device != null) {
			gameManager.connect(device)
					.retry(3, 100)
					.useAutoConnect(false)
					.enqueue();
		}
	}

	/**
	 * Disconnect from peripheral.
	 */
	public void disconnect() {
		device = null;
		gameManager.disconnect().enqueue();
	}

	/**
	 * Sends a command to turn ON or OFF the LED on the nRF5 DK.
	 *
	 * @param -LOL-on true to turn the LED on, false to turn it OFF.
	 */
	/*public void setLedState(final boolean on) {
		blinkyManager.turnLed(on);
	}*/

	@Override
	protected void onCleared() {
		super.onCleared();
		if (gameManager.isConnected())
			disconnect();
		gameManager.removeCallback();
	}

	// --- SEND COMMANDS BELOW --- //

	public void sendCmd_StartGame() {
		gameManager.sendCmd_StartGame();
	}

	public void sendCmd_EndGame() {
		gameManager.sendCmd_EndGame();
	}

	public void sendCmd_InputAnswer(int ledIndex) {
		gameManager.sendCmd_InputAnswer(ledIndex);
	}

	public void sendCmd_SetDifficulty(LedMemory.Game_Difficulty difficulty) {
		gameManager.sendCmd_SetDifficulty(difficulty);
	}

	public void sendCmd_God(boolean on) {
		gameManager.sendCmd_God(on);
	}
}
