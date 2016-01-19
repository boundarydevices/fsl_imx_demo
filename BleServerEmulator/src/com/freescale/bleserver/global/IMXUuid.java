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
package com.freescale.bleserver.global;

/** BLE UUID Strings */
public class IMXUuid {
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	// 180A Device Information
	public static final String SERVICE_DEVICE_INFORMATION = "aaaabbbb-0000-1000-8000-cccccccccccc";
	public static final String CHAR_MANUFACTURER_NAME_STRING = "00002a29-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_MODEL_NUMBER_STRING = "00002a24-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_SERIAL_NUMBER_STRING = "00002a25-0000-1000-8000-00805f9b34fb";

	// 1802 Immediate Alert
	public static final String SERVICE_IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_ALERT_LEVEL = "00002a06-0000-1000-8000-00805f9b34fb";

	// 180F Battery
	public static final String SERVICE_BATTERY_SERVICE = "22222222-0000-1000-8000-eeeeeeeeeeee";
	public static final String CHAR_BATTERY_LEVEL = "22222222-0200-1300-8000-00805f9b34fb";

	// 180D Heart Rate
	public static final String SERVICE_HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_BATTER = "00002a19-0000-1000-8000-00805f9b34fb";

	//CPU Temperature
	public static final String SERVICE_CPU_TEMP = "0000180c-0000-1000-8000-00805f9b34fb";
	public static final String CHAR_CPU_TEMP = "33333333-0000-1000-8000-cccccccccccc";

	//Date
	public static final String SERVICE_DATE = "0cc0180c-0000-1000-8000-eeeeeeeeeeee";
	public static final String CHAR_DATE = "44444444-0000-1000-8000-cccccccccccc";

	//Message
	public static final String SERVICE_MESSAGE = "0cd0180c-0000-1000-8000-eeeeeeeeeeee";
	public static final String CHAR_MESSAGE = "55555555-0000-1000-8000-cccccccccccc";
}
