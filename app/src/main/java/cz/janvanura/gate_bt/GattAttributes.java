/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.janvanura.gate_bt;

import java.util.UUID;

/**
 * Arduino side
 *
 * Commands:
 *  m:secure_key:1                   -> open gate
 *  m:secure_key:0                   -> close gate
 *  c:master_key:new_secure_key      -> change secure key
 *
 * Answers:
 *  ok:m:1                           -> open gate ok
 *  ok:m:0                           -> close gate ok
 *  ok:c:secure_key                  -> secure key was change
 *  err:secure                       -> wrong secure key
 *  err:master                       -> wrong master key
 *  err:length                       -> length of secure key is equal to 0 or greater than 10
 */
public class GattAttributes {

    public static final String GATT_CHAR = "0000ffe1-0000-1000-8000-00805f9b34fb";
    public static final String GATT_SERVICE = "0000ffe0-0000-1000-8000-00805f9b34fb";
    public final static String MAC_ADDRESS = "00:15:83:00:C3:F7";
    public final static String NAME = "Gate BT";

    public static final UUID UUID_CHAR = UUID.fromString(GATT_CHAR);
    public static final UUID UUID_SERVICE = UUID.fromString(GATT_SERVICE);

    public static final String SECURE_KEY = "0000";

    public static final String CMD_MOTION = "m";
    public static final String CMD_CHANGE = "c";
    public final static String VALUE_OPEN = "1";
    public final static String VALUE_CLOSE = "0";
}
