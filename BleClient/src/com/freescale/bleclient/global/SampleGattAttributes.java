/*
 * Copyright (C) 2016 Freescale Semiconductor, Inc.
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
package com.freescale.bleclient.global;

import java.util.HashMap;

import android.util.Log;

public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    static {
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
        attributes.put(IMXUuid.SERVICE_DEVICE_INFORMATION, "I.MX Device");
        attributes.put(IMXUuid.CHAR_MANUFACTURER_NAME_STRING, "Manufacturer Name");
        attributes.put(IMXUuid.CHAR_MODEL_NUMBER_STRING, "Model");
        attributes.put(IMXUuid.CHAR_SERIAL_NUMBER_STRING, "Serial Number");
        attributes.put(IMXUuid.SERVICE_IMMEDIATE_ALERT, "Immediate Alert");
        attributes.put(IMXUuid.CHAR_ALERT_LEVEL, "Alert Level");
        attributes.put(IMXUuid.SERVICE_BATTERY_SERVICE, "Battery");
        attributes.put(IMXUuid.CHAR_BATTERY_LEVEL, "Battery Level");
        attributes.put(IMXUuid.SERVICE_HEART_RATE, "Heart Rate");
        attributes.put(IMXUuid.CHAR_BATTER, "Batter");
        attributes.put(IMXUuid.SERVICE_CPU_TEMP, "Cpu Temperature");
        attributes.put(IMXUuid.CHAR_CPU_TEMP, "Cpu Temperature");
        attributes.put(IMXUuid.SERVICE_DATE, "Server Date");
        attributes.put(IMXUuid.CHAR_DATE, "Server Date");
        attributes.put(IMXUuid.SERVICE_MESSAGE, "Custom Message");
        attributes.put(IMXUuid.CHAR_MESSAGE, "Custom Message");
    }
    
    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
