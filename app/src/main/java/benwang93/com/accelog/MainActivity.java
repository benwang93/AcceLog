/* The following licence applies to parts of this source code. The original
 * source can be found at: https://github.com/jeppsson/Arduino-Communicator
 *
 * Copyright (C) 2015 Benjamin Wang, benwang@umich.edu
 */

/*
 * Copyright (C) 2012 Mathias Jeppsson
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

package benwang93.com.accelog;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    private static final int ARDUINO_USB_VENDOR_ID = 0x2341;
    private static final int ARDUINO_UNO_USB_PRODUCT_ID = 0x01;
    private static final int ARDUINO_MEGA_2560_USB_PRODUCT_ID = 0x10;
    private static final int ARDUINO_MEGA_2560_R3_USB_PRODUCT_ID = 0x42;
    private static final int ARDUINO_UNO_R3_USB_PRODUCT_ID = 0x43;
    private static final int ARDUINO_MEGA_2560_ADK_R3_USB_PRODUCT_ID = 0x44;
    private static final int ARDUINO_MEGA_2560_ADK_USB_PRODUCT_ID = 0x3F;

    private final static String TAG = "MainActivity";
    private final static boolean DEBUG = false;

    final static String DATA_SAVE = "benwang93.com.accelog.intent.extra.SAVE_DATA";

    private Boolean mIsReceiving;
    private ArrayList<ByteArray> mTransferredDataList = new ArrayList<ByteArray>();
    private String receivedData = "";
//    private ArrayAdapter<ByteArray> mDataAdapter;

    // UI Elements
    TextView TV_console;

    private void findDevice() {
//DEBUG
displayMessage(TV_console, "findDevice()\n");

        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = null;
        HashMap<String, UsbDevice> usbDeviceList = usbManager.getDeviceList();
        if (DEBUG) Log.d(TAG, "length: " + usbDeviceList.size());
        Iterator<UsbDevice> deviceIterator = usbDeviceList.values().iterator();
        if (deviceIterator.hasNext()) {
            UsbDevice tempUsbDevice = deviceIterator.next();

            // Print device information. If you think your device should be able
            // to communicate with this app, add it to accepted products below.
            if (DEBUG) Log.d(TAG, "VendorId: " + tempUsbDevice.getVendorId());
            if (DEBUG) Log.d(TAG, "ProductId: " + tempUsbDevice.getProductId());
            if (DEBUG) Log.d(TAG, "DeviceName: " + tempUsbDevice.getDeviceName());
            if (DEBUG) Log.d(TAG, "DeviceId: " + tempUsbDevice.getDeviceId());
            if (DEBUG) Log.d(TAG, "DeviceClass: " + tempUsbDevice.getDeviceClass());
            if (DEBUG) Log.d(TAG, "DeviceSubclass: " + tempUsbDevice.getDeviceSubclass());
            if (DEBUG) Log.d(TAG, "InterfaceCount: " + tempUsbDevice.getInterfaceCount());
            if (DEBUG) Log.d(TAG, "DeviceProtocol: " + tempUsbDevice.getDeviceProtocol());

            if (tempUsbDevice.getVendorId() == ARDUINO_USB_VENDOR_ID) {
                if (DEBUG) Log.i(TAG, "Arduino device found!");

                switch (tempUsbDevice.getProductId()) {
                    case ARDUINO_UNO_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Uno " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                    case ARDUINO_MEGA_2560_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Mega 2560 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                    case ARDUINO_MEGA_2560_R3_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Mega 2560 R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                    case ARDUINO_UNO_R3_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Uno R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                    case ARDUINO_MEGA_2560_ADK_R3_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Mega 2560 ADK R3 " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                    case ARDUINO_MEGA_2560_ADK_USB_PRODUCT_ID:
                        Toast.makeText(getBaseContext(), "Arduino Mega 2560 ADK " + getString(R.string.found), Toast.LENGTH_SHORT).show();
                        usbDevice = tempUsbDevice;
                        break;
                }
            }
        }

        if (usbDevice == null) {
            if (DEBUG) Log.i(TAG, "No device found!");
displayMessage(TV_console, "No device found\n");
            Toast.makeText(getBaseContext(), getString(R.string.no_device_found), Toast.LENGTH_LONG).show();
        } else {
            if (DEBUG) Log.i(TAG, "Device found!");

displayMessage(TV_console, "Device found!\n");
            Intent startIntent = new Intent(getApplicationContext(), ArduinoCommunicatorService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(), 0, startIntent, 0);
            usbManager.requestPermission(usbDevice, pendingIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (DEBUG) Log.d(TAG, "onCreate()");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ArduinoCommunicatorService.DATA_RECEIVED_INTENT);
        filter.addAction(ArduinoCommunicatorService.DATA_SENT_INTERNAL_INTENT);
        registerReceiver(mReceiver, filter);

//        mDataAdapter = new ArrayAdapter<ByteArray>(this, android.R.layout.simple_list_item_1, mTransferredDataList);
//        setListAdapter(mDataAdapter);

        // Grab UI elements
        TV_console = (TextView) findViewById(R.id.MainActivity_TextView_Console);
        TV_console.setMovementMethod(new ScrollingMovementMethod());
//DEBUG
displayMessage(TV_console, "onCreate() has found textView\n");

        final EditText ET_send = (EditText) findViewById(R.id.MainActivity_EditText_Send);

        // Send Button
        findViewById(R.id.MainActivity_Button_Send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArduinoCommunicatorService.SEND_DATA_INTENT);
                intent.putExtra(ArduinoCommunicatorService.DATA_EXTRA, ET_send.getText().toString().getBytes());
//displayMessage(TV_console, "Sending: " + ET_send.getText().toString().getBytes() + "\n");
                sendBroadcast(intent);
                ET_send.setText("");
            }
        });

        // Save Button
        findViewById(R.id.MainActivity_Button_Save).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent saveIntent = new Intent(getApplicationContext(), SaveCSVActivity.class);
                saveIntent.putExtra(DATA_SAVE, receivedData.getBytes());
                startActivity(saveIntent);

                // Clear received data buffer
                receivedData = "";
            }
        });

        findDevice();
//DEBUG
displayMessage(TV_console, "onCreate() finished!\n");

    }


    @Override
    protected void onNewIntent(Intent intent) {
//DEBUG
displayMessage(TV_console, "onNewIntent() called\n");

        if (DEBUG) Log.d(TAG, "onNewIntent() " + intent);
        super.onNewIntent(intent);

        if (UsbManager.ACTION_USB_DEVICE_ATTACHED.contains(intent.getAction())) {
            if (DEBUG) Log.d(TAG, "onNewIntent() " + intent);
            findDevice();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        // Save data


        // Clear string
//        receivedData = "";
    }

    @Override
    protected void onDestroy() {
        if (DEBUG) Log.d(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
displayMessage(TV_console, "Opetion selected: " + item.getItemId() + "\n");

        switch (item.getItemId()) {
            case R.id.help:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://ron.bems.se/arducom/usage.html")));
                return true;
            case R.id.about:
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://ron.bems.se/arducom/primaindex.php")));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        private void handleTransferredData(Intent intent, boolean receiving) {
            if (mIsReceiving == null || mIsReceiving != receiving) {
                mIsReceiving = receiving;
                mTransferredDataList.add(new ByteArray());
            }

            final byte[] newTransferredData = intent.getByteArrayExtra(ArduinoCommunicatorService.DATA_EXTRA);
            if (DEBUG) Log.i(TAG, "data: " + newTransferredData.length + " \"" + new String(newTransferredData) + "\"");

//            ByteArray transferredData = mTransferredDataList.get(mTransferredDataList.size() - 1);
//            transferredData.add(newTransferredData);
//            mTransferredDataList.set(mTransferredDataList.size() - 1, transferredData);

//Toast.makeText(getApplicationContext(), "mTransferredDataList.size(): " + mTransferredDataList.size(), Toast.LENGTH_SHORT).show();
//            mDataAdapter.notifyDataSetChanged();

            // Convert transferred data into string
            String newTransferredDataString = new String(newTransferredData);
            receivedData += newTransferredDataString;

            // Display new text
            displayMessage(TV_console, receiving ? "R: " : "S: ");
            displayMessage(TV_console, /*"New message: " +*/ newTransferredDataString + "\n");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive() " + action);

//DEBUG
//displayMessage(TV_console, "Broadcast Receiver has received" + action + "\n");


            if (ArduinoCommunicatorService.DATA_RECEIVED_INTENT.equals(action)) {
                handleTransferredData(intent, true);
            } else if (ArduinoCommunicatorService.DATA_SENT_INTERNAL_INTENT.equals(action)) {
                handleTransferredData(intent, false);
            }
        }
    };

    private void displayMessage(TextView textView, String message){
        // Append text
        textView.append(message);

        // find the amount we need to scroll.  This works by
        // asking the TextView's internal layout for the position
        // of the final line and then subtracting the TextView's height

        int scrollAmount;

        // Calculate scroll amount
        if (textView.getLineCount() == 0)
            scrollAmount = 0;
        else
            scrollAmount = textView.getLayout().getLineTop(textView.getLineCount()) - textView.getHeight();

        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            textView.scrollTo(0, scrollAmount);
        else
            textView.scrollTo(0, 0);
    }
}
