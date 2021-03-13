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

package com.example.arduinoledmemory.profile.callback;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.data.Data;

public interface LedMemCmdCallback {

    // called after sending any command to indicate whether the command was successful or not
    void onCommandResponse(final boolean successful);

    // called received after invalid command is sent and failed response is received, contains reason of failure
    void onError(final String msg);

    // called whenever game state is changed, eg: showing problem, waiting for answer input, alert leds, game over
    void onGameStateUpdate(final String gameState);

    // called whenever level is changed
    void onLevelUpdate(final int level);

    // called whenever answer is input, stating correct or incorrect
    void onAnswerResponse(final boolean correct);

    // called whenever invalid data is received and fails to be parsed
    void onInvalidData(@NonNull String strData);

}
