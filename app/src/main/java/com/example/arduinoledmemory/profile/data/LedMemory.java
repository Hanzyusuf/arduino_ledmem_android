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

package com.example.arduinoledmemory.profile.data;

import androidx.annotation.NonNull;

import no.nordicsemi.android.ble.data.Data;

public final class LedMemory {

    // command markers
    public static final char StartMarker = '<';
    public static final char EndMarker = '>';

    // commands to send
    private static final String S_CMD_REQ_STATE = "Request_State:";
    private static final String S_CMD_IN_ANS = "Answer_Input:";
    private static final String S_CMD_GAME_START = "Start_Game";
    private static final String S_CMD_GAME_END = "End_Game";
    private static final String S_CMD_SET_DIFF = "Set_Difficulty:";
    private static final String S_CMD_GOD_ON = "IAmALoser";
    private static final String S_CMD_GOD_OFF = "IAmNotALoser";

    // commands to be received
    public static final String R_CMD_RESPONSE = "CMD_Response:"; // Result after :
    public static final String R_CMD_ERROR = "Error:"; // String after :
    public static final String R_CMD_GAME_STATE_UPDATE = "GameState_Update:"; // Game_State after :
    public static final String R_CMD_LEVEL_UPDATE = "Level_Update:"; // Integer after :
    public static final String R_CMD_ANS_RES = "Answer_Response:"; // Result after :

    public enum Game_Difficulty {
        EASY,
        MEDIUM,
        HARD
    }

    public enum Game_State {
        None,
        Just_Started,
        Alert_Leds,
        Show_Problem,
        Wait_For_Answer_Input,
        Game_Over
    }

    public enum Result {
        Success,
        Fail
    }

    @NonNull
    public static Data cmd_StartGame() {
        String CMD = StartMarker + S_CMD_REQ_STATE + S_CMD_GAME_START + EndMarker;
        return Data.from(CMD);
    }

    @NonNull
    public static Data cmd_EndGame() {
        String CMD = StartMarker + S_CMD_REQ_STATE + S_CMD_GAME_END + EndMarker;
        return Data.from(CMD);
    }

    @NonNull
    public static Data cmd_InputAnswer(int ledIndex) {
        String CMD = StartMarker + S_CMD_IN_ANS + ledIndex + EndMarker;
        return Data.from(CMD);
    }

    @NonNull
    public static Data cmd_SetDifficulty(Game_Difficulty difficulty) {
        String CMD = StartMarker + S_CMD_SET_DIFF + difficulty.name() + EndMarker;
        return Data.from(CMD);
    }

    @NonNull
    public static Data cmd_God(boolean on) {
        String CMD = StartMarker + (on ? S_CMD_GOD_ON : S_CMD_GOD_OFF) + EndMarker;
        return Data.from(CMD);
    }

    //private static final byte STATE_OFF = 0x00;
    //private static final byte STATE_ON = 0x01;

    /*@NonNull
    public static Data turnOn() {
        return Data.opCode(STATE_ON);
    }

    @NonNull
    public static Data turnOff() {
        return Data.opCode(STATE_OFF);
    }*/

}
