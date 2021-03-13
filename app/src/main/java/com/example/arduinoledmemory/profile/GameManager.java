/*
 * Copyright (c) 2018, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.example.arduinoledmemory.profile;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;

import com.example.arduinoledmemory.profile.callback.LedMemCmdCallback;
import com.example.arduinoledmemory.profile.data.LedMemory;
import com.example.arduinoledmemory.utils.GattAttributes;

import java.util.ArrayList;
import java.util.UUID;

import no.nordicsemi.android.ble.callback.DataReceivedCallback;
import no.nordicsemi.android.ble.callback.DataSentCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.livedata.ObservableBleManager;
import no.nordicsemi.android.log.LogContract;
import no.nordicsemi.android.log.LogSession;
import no.nordicsemi.android.log.Logger;

import static com.example.arduinoledmemory.profile.data.LedMemory.*;

public class GameManager extends ObservableBleManager {

	private final static String TAG_INIT = "GameManager.init";
	private final static String TAG_DATA = "GameManager.data";

	/** Arduino Led Memory Service UUID. */
	public final static UUID LEDMEM_SERVICE_UUID = GattAttributes.Led_Memory_Service;
	/** Arduino Led Memomry Data Characteristic UUID. */
	private final static UUID LEDMEM_DATA_CHAR_UUID = GattAttributes.Led_Memory_Data_Characteristic;

	private final MutableLiveData<Integer> gameLevel = new MutableLiveData<>();

	private BluetoothGattCharacteristic dataCharacteristic;
	private LogSession logSession;
	private boolean supported;
	private LedMemCmdCallback ledMemCmdCallback;

	public GameManager(@NonNull final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BleManagerGattCallback getGattCallback() {
		return new LedMemBleManagerGattCallback();
	}

	/**
	 * Sets the log session to be used for low level logging.
	 * @param session the session, or null, if nRF Logger is not installed.
	 */
	public void setLogger(@Nullable final LogSession session) {
		logSession = session;
	}

	@Override
	public void log(final int priority, @NonNull final String message) {
		// The priority is a Log.X constant, while the Logger accepts it's log levels.
		Logger.log(logSession, LogContract.Log.Level.fromPriority(priority), message);
	}

	@Override
	protected boolean shouldClearCacheWhenDisconnected() {
		return !supported;
	}

	private	final DataReceivedCallback dataReceivedCallback = new DataReceivedCallback() {
		@Override
		public void onDataReceived(@NonNull BluetoothDevice device, @NonNull Data data) {
			String strData = data.getStringValue(0);
			if(strData == null || strData.isEmpty())
				return;

			if(bParseInProgress)
				incomingDataQueue.add(strData);
			else
				processIncomingData(strData,false);
		}
	};

	private final DataSentCallback dataSentCallback = new DataSentCallback() {
		@Override
		public void onDataSent(@NonNull BluetoothDevice device, @NonNull Data data) {
			// do nothing for now ...
			//Log.d(TAG_DATA, "sent: " + data.getStringValue(0));
		}
	};

	private final static int MAX_BUFF = 128;
	private char[] incomingData = new char[MAX_BUFF];
	private int index = 0;
	private boolean bCommandInProgress = false;
	boolean bParseInProgress = false;
	private ArrayList<String> incomingDataQueue = new ArrayList<>();

	private void processIncomingData(@NonNull String strData, boolean queuedData) {
		bParseInProgress = true;

		for(char c : strData.toCharArray()) {
			if(bCommandInProgress){
				if(c == LedMemory.EndMarker) {
					bCommandInProgress = false;
					parseData(new String(incomingData, 0, index));
				}
				else {
					incomingData[index] = c;
					index++;
					if(index >= MAX_BUFF)
						index = MAX_BUFF-1;
				}
			}
			else {
				if(c == LedMemory.StartMarker) {
					index = 0;
					bCommandInProgress = true;
				}
			}
		}

		if(queuedData)
			incomingDataQueue.remove(0);

		bParseInProgress = false;

		if(incomingDataQueue.size() > 0)
			processIncomingData(incomingDataQueue.get(0), true);
	}

	private void parseData(@NonNull String strData) {

		//Log.d(TAG_DATA, "got: " + strData);

		if(ledMemCmdCallback == null)
			return;

		if (strData.contains(R_CMD_RESPONSE)) {
			if (strData.substring(R_CMD_RESPONSE.length()).equals(LedMemory.Result.Success.name())) {
				ledMemCmdCallback.onCommandResponse(true);
				return;
			}
			else if (strData.substring(R_CMD_RESPONSE.length()).equals(LedMemory.Result.Fail.name())) {
				ledMemCmdCallback.onCommandResponse(false);
				return;
			}
		}
		else if (strData.contains(LedMemory.R_CMD_ERROR)) {
			ledMemCmdCallback.onError(strData.substring(R_CMD_ERROR.length()));
			return;
		}
		else if (strData.contains(LedMemory.R_CMD_GAME_STATE_UPDATE)) {
			ledMemCmdCallback.onGameStateUpdate(strData.substring(R_CMD_GAME_STATE_UPDATE.length()));
			return;
		}
		else if (strData.contains(R_CMD_LEVEL_UPDATE)) {
			try {
				int level = Integer.parseInt(strData.substring(R_CMD_LEVEL_UPDATE.length()));
				ledMemCmdCallback.onLevelUpdate(level);
				return;
			}
			catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}
		else if (strData.contains(R_CMD_ANS_RES)) {
			if (strData.substring(R_CMD_ANS_RES.length()).equals(LedMemory.Result.Success.name())) {
				ledMemCmdCallback.onAnswerResponse(true);
				return;
			}
			else if (strData.substring(R_CMD_ANS_RES.length()).equals(LedMemory.Result.Fail.name())) {
				ledMemCmdCallback.onAnswerResponse(false);
				return;
			}
		}

		ledMemCmdCallback.onInvalidData(strData);
	}

	/**
	 * BluetoothGatt callbacks object.
	 */
	private class LedMemBleManagerGattCallback extends BleManagerGattCallback {
		@Override
		protected void initialize() {
			setNotificationCallback(dataCharacteristic).with(dataReceivedCallback);
			readCharacteristic(dataCharacteristic).with(dataReceivedCallback).enqueue();
			enableNotifications(dataCharacteristic).enqueue();
			Log.d(TAG_INIT, "Initialized");
		}

		@Override
		public boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(LEDMEM_SERVICE_UUID);
			if (service != null) {
				dataCharacteristic = service.getCharacteristic(LEDMEM_DATA_CHAR_UUID);
			}

			boolean writeRequest = false;
			if (dataCharacteristic != null) {
				final int rxProperties = dataCharacteristic.getProperties();
				writeRequest = (rxProperties & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0;
			}

			if (dataCharacteristic != null && writeRequest)
				dataCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

			supported = dataCharacteristic != null && writeRequest;
			Log.d(TAG_INIT, "Supported: " + supported);
			return supported;
		}

		@Override
		protected void onDeviceDisconnected() {
			dataCharacteristic = null;
			Log.d(TAG_INIT, "Disconnected");
		}
	}

	// --- SEND COMMANDS BELOW --- //

	public void sendCmd_StartGame() {
		// Are we connected?
		if (dataCharacteristic == null)
			return;

		Data command = LedMemory.cmd_StartGame();
		writeCharacteristic(dataCharacteristic, LedMemory.cmd_StartGame()).with(dataSentCallback).enqueue();

		Log.d(TAG_DATA, "Command Sent: " + command.getStringValue(0));
	}

	public void sendCmd_EndGame() {
		// Are we connected?
		if (dataCharacteristic == null)
			return;

		Data command = LedMemory.cmd_EndGame();
		writeCharacteristic(dataCharacteristic, command).with(dataSentCallback).enqueue();

		StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
		for(int i = 0; i < stackTraceElements.length; i++){
			Log.d(TAG_DATA, i + " : trace: " + stackTraceElements[i].getMethodName());
			Log.d(TAG_DATA, i + " : is native: " + stackTraceElements[i].isNativeMethod());
		}

		Log.d(TAG_DATA, "Command Sent: " + command.getStringValue(0));
	}

	public void sendCmd_InputAnswer(int ledIndex) {
		// Are we connected?
		if (dataCharacteristic == null)
			return;

		Data command = LedMemory.cmd_InputAnswer(ledIndex);
		writeCharacteristic(dataCharacteristic, command).with(dataSentCallback).enqueue();

		Log.d(TAG_DATA, "Command Sent: " + command.getStringValue(0));
	}

	public void sendCmd_SetDifficulty(LedMemory.Game_Difficulty difficulty) {
		// Are we connected?
		if (dataCharacteristic == null)
			return;

		Data command = LedMemory.cmd_SetDifficulty(difficulty);
		writeCharacteristic(dataCharacteristic, command).with(dataSentCallback).enqueue();

		Log.d(TAG_DATA, "Command Sent: " + command.getStringValue(0));
	}

	public void sendCmd_God(boolean on) {
		// Are we connected?
		if (dataCharacteristic == null)
			return;

		Data command = LedMemory.cmd_God(on);
		writeCharacteristic(dataCharacteristic, command).with(dataSentCallback).enqueue();

		Log.d(TAG_DATA, "Command Sent: " + command.getStringValue(0));
	}

	public void updateCallback(LedMemCmdCallback ledMemCmdCallback) {
		this.ledMemCmdCallback = ledMemCmdCallback;
	}

	public void removeCallback() {
		this.ledMemCmdCallback = null;
	}

}
