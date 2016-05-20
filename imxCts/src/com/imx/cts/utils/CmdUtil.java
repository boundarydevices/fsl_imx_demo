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
package com.imx.cts.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class CmdUtil {

    public static CmdResult execCommand(String command){
        return execCommand(new String[] {command});
    }

    public static CmdResult execCommand(String[] commands) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CmdResult(result, null, null);
        }

        Process process = null;
        BufferedReader successReader = null;
        BufferedReader errorReader = null;
        DataOutputStream os = null;
        StringBuffer messageSuccess = new StringBuffer();
        StringBuffer messageError = new StringBuffer();

        try {
            process = Runtime.getRuntime().exec("sh");
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }
                os.write(command.getBytes());
                os.writeBytes("\n");
                os.flush();
            }
            os.writeBytes("exit\n");
            os.flush();

            result = process.waitFor();



            successReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = successReader.readLine()) != null) {
                messageSuccess.append(line);
            }
            while ((line = errorReader.readLine()) != null) {
                messageError.append(line);
            }
        }  catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successReader != null) {
                    successReader.close();
                }
                if (errorReader != null) {
                    errorReader.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (process != null) {
                process.destroy();
            }
        }
        return new CmdResult(result, messageSuccess == null ? null : messageSuccess.toString(), messageError == null ? null
                : messageError.toString());
    }

    public static class CmdResult {

        public int result;
        public String successMsg;
        public String errorMsg;

        public CmdResult(int result) {
            this.result = result;
        }

        public CmdResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }
}
