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
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.w3c.dom.Text;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {

    // Debug mode?
    public static boolean DEBUG = false;

//    // Packet specification
//    private static final char ACCEL_SOP = '{';
////    private static final char ACCEL_DELIM = '\t';
////    private static final char ACCEL_EOP = '}';

    // Arduino communicator variables
    private static final int ARDUINO_USB_VENDOR_ID = 0x2341;
    private static final int ARDUINO_UNO_USB_PRODUCT_ID = 0x01;
    private static final int ARDUINO_MEGA_2560_USB_PRODUCT_ID = 0x10;
    private static final int ARDUINO_MEGA_2560_R3_USB_PRODUCT_ID = 0x42;
    private static final int ARDUINO_UNO_R3_USB_PRODUCT_ID = 0x43;
    private static final int ARDUINO_MEGA_2560_ADK_R3_USB_PRODUCT_ID = 0x44;
    private static final int ARDUINO_MEGA_2560_ADK_USB_PRODUCT_ID = 0x3F;

    private final static String TAG = "MainActivity";

    final static String DATA_SAVE = "benwang93.com.accelog.intent.extra.SAVE_DATA";

    private Boolean mIsReceiving;
    private ArrayList<ByteArray> mTransferredDataList = new ArrayList<ByteArray>();

//    // Receive buffer definitions
//    final static int PACKET_NUM_LONGS = 1;                              // Number of longs (timestamp)
//    final static int PACKET_BYTES_LONG = 4;                             // Number of bytes in a long
//    final static int PACKET_BUFF_NUM_FLOATS = 3;						// Number of floats in packet
//    final static int PACKET_BYTES_FLOAT = 4;                            // Number of bytes in a float
//    final static int PACKET_BUFF_LENGTH =                               // Length of packet (3x float = 12 B)
//            PACKET_BUFF_NUM_FLOATS * PACKET_BYTES_FLOAT +
//            PACKET_NUM_LONGS * PACKET_BYTES_LONG;
//    private static byte[] packetBuff = new byte[PACKET_BUFF_LENGTH];	// Buffer for packet
//    private static int currBuffPos = 0;									// Current position in buffer

    // Arraylist for saving all readings
    public static ArrayList<AccelSample> accelSamples = new ArrayList<>(0);

    // Simple date formatter for X-axis values on chart
    public static Date startTime = Calendar.getInstance().getTime();
    public static SimpleDateFormat sdf_graph = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
    public static long startTimeOffset;

    // UI Elements
    TextView TV_console;
    LineChart LC_oscope;
    private static final int LC_MAX_ELTS = 120;

    Button Btn_startStop;
    LinearLayout LL_debugConsole;

    // Start/Stop button functionality (true == recording)
    public static boolean recordingIsStarted = false;

//    // Frame skip for drawing graph
//    public static final int LC_OSCOPE_FRAMESKIP = 10;       // Number of frames to skip
//    public static int LC_oscope_currentFrameskip = 0;      // Counter for current frame

    // Chart specifications
    private static final float AXIS_MIN = -2f;
    private static final float AXIS_MAX = 2f;

    String[] CHART_NAMES = {"X Axis", "Y Axis", "Z Axis"};
    int[] CHART_COLORS = {Color.BLUE, Color.GREEN, Color.RED};

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
            // Save current time
            startTimeOffset = Calendar.getInstance().getTimeInMillis();

            // Display message
            if (DEBUG) Log.i(TAG, "Device found!");
            displayMessage(TV_console, "Device found!\n");

            // Save new start time
            startTime = Calendar.getInstance().getTime();

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
        filter.addAction(ArduinoCommunicatorService.UPDATE_CHART_INTENT);
        registerReceiver(mReceiver, filter);

        // Grab UI elements
        TV_console = (TextView) findViewById(R.id.MainActivity_TextView_Console);
        TV_console.setMovementMethod(new ScrollingMovementMethod());

        final EditText ET_frameskip = (EditText) findViewById(R.id.MainActivity_EditText_Frameskip);
        ET_frameskip.setText("" + ArduinoCommunicatorService.LC_oscope_frameskip);
        Button B_setFrameskip = (Button) findViewById(R.id.MainActivity_Button_SetFrameskip);
        B_setFrameskip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    ArduinoCommunicatorService.LC_oscope_frameskip = Integer.parseInt(ET_frameskip.getText().toString());
                    Toast.makeText(getApplicationContext(), "Frameskip set to " + ArduinoCommunicatorService.LC_oscope_frameskip, Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    Toast.makeText(getApplicationContext(), "Error setting frameskip", Toast.LENGTH_SHORT).show();
                    ET_frameskip.setText("" + ArduinoCommunicatorService.LC_oscope_frameskip);
                }
            }
        });

        LL_debugConsole = (LinearLayout) findViewById(R.id.MainActivity_LinearLayout_DebugConsole);
        LL_debugConsole.setVisibility(View.GONE);

        final EditText ET_send = (EditText) findViewById(R.id.MainActivity_EditText_Send);

        // Send Button
        findViewById(R.id.MainActivity_Button_Send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ArduinoCommunicatorService.SEND_DATA_INTENT);
                intent.putExtra(ArduinoCommunicatorService.DATA_EXTRA, ET_send.getText().toString().getBytes());
                sendBroadcast(intent);
                ET_send.setText("");
            }
        });

        // Save Button
        findViewById(R.id.MainActivity_Button_Save).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                saveDataActivity();
            }
        });

        // Start recording button
        Btn_startStop = (Button) findViewById(R.id.MainActivity_Button_StartStop);
        Btn_startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Toggle recording status
                recordingIsStarted = !recordingIsStarted;

                // Set new button text
                Btn_startStop.setText(recordingIsStarted ?
                        R.string.MainActivity_Button_StartStop_Stop :
                        R.string.MainActivity_Button_StartStop_Start);
            }
        });

        // DEBUG button
        findViewById(R.id.MainActivity_Button_Debug).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDebug();
                displayMessage(TV_console, "Debug mode active: " + DEBUG + "\n");
            }
        });

        // Set focus off of EditText
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // Configure chart
        LC_oscope = (LineChart) findViewById(R.id.MainActivity_LineChart_Oscope);
        
        // no description text
        LC_oscope.setDescription("");
        LC_oscope.setNoDataTextDescription("Please connect accelerometer sensor to log data");

        // enable value highlighting
        LC_oscope.setHighlightEnabled(true);

        // enable touch gestures
        LC_oscope.setTouchEnabled(true);

        // enable scaling and dragging
        LC_oscope.setDragEnabled(true);
        LC_oscope.setScaleEnabled(true);
        LC_oscope.setDrawGridBackground(false);

        // if disabled, scaling can be done on x- and y-axis separately
        LC_oscope.setPinchZoom(true);

        // set an alternative background color
        LC_oscope.setBackgroundColor(Color.DKGRAY);

        // Set axes
        YAxis leftAxis = LC_oscope.getAxisLeft();
        leftAxis.setAxisMaxValue(AXIS_MAX);
        leftAxis.setAxisMinValue(AXIS_MIN);
        leftAxis.setStartAtZero(false);

        YAxis rightAxis = LC_oscope.getAxisRight();
        rightAxis.setAxisMaxValue(AXIS_MAX);
        rightAxis.setAxisMinValue(AXIS_MIN);
        rightAxis.setStartAtZero(false);

        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        for (int j = 0; j < 3; j++) dataSets.add(new LineDataSet(new ArrayList<Entry>(), "DataSet " + (j + 1)));
        LineData data = new LineData(xVals, dataSets);
        data.setValueTextColor(Color.WHITE);

        // add empty data
        initData(3);

        // Search for connected USB device
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

        // TODO: Save data
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
            case R.id.MainActivity_action_save:
                displayMessage(TV_console, "Save option selected!");
                saveDataActivity();
                return true;
            case R.id.MainActivity_action_load:
                displayMessage(TV_console, "Load option selected");
                return true;
            case R.id.MainActivity_action_quit:
                finish();       // finish activity
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

            // Debug print
            if (!receiving) displayMessage(TV_console, "S: " + new String(newTransferredData) + "\n");

            // Receive and parse packet
//            parseForPacket(newTransferredData);
        }

        private void extractAndUpdateChart(Intent intent){
            // Extract current sample from intent
            int index = intent.getIntExtra(ArduinoCommunicatorService.DATA_EXTRA, 0);

            if (DEBUG) displayMessage(TV_console, "Updating chart, index " + index + "\n");

            // Update chart
            updateChart(accelSamples.get(index));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive() " + action);

            if (ArduinoCommunicatorService.UPDATE_CHART_INTENT.equals(action)) {
                extractAndUpdateChart(intent);
            } else if (ArduinoCommunicatorService.DATA_RECEIVED_INTENT.equals(action)) {
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


//    // Returns index of next SOP starting from startIndex, or -1 if no SOP found
//    private static int indexOfSOP(byte[] receiveBuff, int startIndex){
//        for (int i = startIndex; i < receiveBuff.length; i++){
//            if (receiveBuff[i] == ACCEL_SOP){
//                return i;
//            }
//        }
//
//        // Default value
//        return -1;
//    }
//
//    // Returns true if buffer contains a SOP after startIndex
//    private static boolean bufferHasPacket(byte[] receiveBuff, int startIndex){
//        return indexOfSOP(receiveBuff, startIndex) != -1;
//    }
//
//
//    // Call upon data receive to parse for floats to populate the x, y, and z axis acceleration
//    private void  parseForPacket(byte[] receiveBuff){
//        boolean SOPFound = false;		// Check for if buffer contains a SOP
//        int remainingBuffLen = -1;		// Number of bytes left in receive buffer before next SOP or end of buffer
//        int currReceivePos = 0;			// Current position in receive buffer
//
//        // Exit if not recording
//        if (!recordingIsStarted){
//            return;
//        }
//
//        // Repeat while data available
//        do {
//            try {
//                // Initialize
//                SOPFound = bufferHasPacket(receiveBuff, currReceivePos);	// Bool if SOP is found
//                remainingBuffLen =											// Remaining characters until first SOP (or end of buffer)
//                        SOPFound ?
//                                indexOfSOP(receiveBuff, currReceivePos) - currReceivePos:
//                                receiveBuff.length - currReceivePos;
//
//                // Error check length. Discard this data if wrong. Grab next
//                if ((SOPFound && currBuffPos + remainingBuffLen != PACKET_BUFF_LENGTH) ||		// SOP found and incorrect length
//                        (!SOPFound && currBuffPos + remainingBuffLen > PACKET_BUFF_LENGTH)){	// or buffer length appended is too long
//
//                    // Debug
//                    if (DEBUG) displayMessage(TV_console, "Receiving error: Invalid packet length: " + (currBuffPos + remainingBuffLen) + "\n");
//
//                    // Discard this data
//                    currReceivePos = indexOfSOP(receiveBuff, currReceivePos) + 1;
//
//                    // Reset lengths/buffer
//                    currBuffPos = 0;
//
//                    // Parse for next packet
//                    continue;
//                }
//
//                // Compute bytes to copy and copy buffer over
//                int numBytesCopied = Math.min(PACKET_BUFF_LENGTH - currBuffPos, remainingBuffLen);
//                System.arraycopy(receiveBuff, currReceivePos, packetBuff, currBuffPos, numBytesCopied);
//
//                // Recalculate buffer position
//                currReceivePos += numBytesCopied;
//                currBuffPos += numBytesCopied;
//
//                // Full & valid length buffer check
//                // if full buffer, parse for floats and add to packet
//                if (SOPFound && currBuffPos == PACKET_BUFF_LENGTH){
//                    // Create new packet
//                    AccelSample currSample = new AccelSample();
////                    currSample.time = Calendar.getInstance().getTimeInMillis();
//                    currSample.time = ByteBuffer.wrap(packetBuff, 0, PACKET_BYTES_LONG).order(ByteOrder.LITTLE_ENDIAN) + startTimeOffset;
//                    currSample.aX = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 0 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                    currSample.aY = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 1 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//                    currSample.aZ = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 2 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
//
//                    // TODO: Check for data within bounds
//
//                    // Add current sample to array
//                    accelSamples.add(currSample);
//
//                    // Frame skip
//                    if (LC_oscope_currentFrameskip > LC_OSCOPE_FRAMESKIP) {
//                        LC_oscope_currentFrameskip = 0;
//
//                        // Update graph
//                        updateChart(currSample);
//                    } else {
//                        LC_oscope_currentFrameskip++;
//                    }
//
//                    // Reset lengths/buffer
//                    currReceivePos++;		// Skip over SOP character
//                    currBuffPos = 0;
//                }
//            } catch (Exception e){
//                displayMessage(TV_console, "Recieve exception: " + e.getMessage() + "\n");
//
//                // On exception, discard this packet and reset buffer
//                currReceivePos = indexOfSOP(receiveBuff, currReceivePos) + 1;
//                currBuffPos = 0;
//            }
//        } while (currReceivePos < receiveBuff.length);
//    }

    private static void stringMod(String s){
        s.concat(" world");
    }

    private void updateChart(AccelSample sample){
        LineData data = LC_oscope.getData();

        // Add new x value
        data.addXValue(sdf_graph.format(new Date(sample.time)));

        // Number of entries
        int numEntries = data.getXValCount();

        // Set new data
        data.addEntry(new Entry((float)sample.aX, numEntries), 0);
        data.addEntry(new Entry((float)sample.aY, numEntries), 1);
        data.addEntry(new Entry((float)sample.aZ, numEntries), 2);

        // Remove old data
//        displayMessage(TV_console, "numElts: " + data.getDataSetByIndex(0).getEntryCount() + "  numX: " + data.getXValCount() + "\n");
        if (numEntries > LC_MAX_ELTS) {
            data.removeEntry(numEntries + 1 - LC_MAX_ELTS, 0);
            data.removeEntry(numEntries + 1 - LC_MAX_ELTS, 1);
            data.removeEntry(numEntries + 1 - LC_MAX_ELTS, 2);
//            data.removeXValue(0); // Don't remove X value bc graph doesn't scroll right
        }

        LC_oscope.notifyDataSetChanged();

        // limit the number of visible entries
        LC_oscope.setVisibleXRangeMaximum(120);

        // move to the latest entry
//        LC_oscope.invalidate();   // Don't invalidate bc moveViewToX() does it
        LC_oscope.moveViewToX(data.getXValCount() - 121);
    }

    void initData(int numSets){
        ArrayList<String> xVals = new ArrayList<String>();
        ArrayList<LineDataSet> dataSets = new ArrayList<>();
        for (int setNum = 0; setNum < numSets; setNum++){
            LineDataSet set = new LineDataSet(new ArrayList<Entry>(), CHART_NAMES[setNum]);
            set.setAxisDependency(YAxis.AxisDependency.LEFT);
            set.setColor(CHART_COLORS[setNum]);
            set.setCircleColor(CHART_COLORS[setNum]);
//            set.setLineWidth(2f);
            set.setDrawValues(false);
            set.setCircleSize(0);
//            set.setFillAlpha(65);
//            set.setFillColor(Color.GREEN);
//            set.setDrawCircleHole(false);
//            set.setHighLightColor(Color.rgb(244, 117, 117));
            dataSets.add(set);
        }

        LineData data = new LineData(xVals, dataSets);
        data.setValueTextColor(Color.WHITE);
        data.setValueTextSize(9f);

        LC_oscope.setData(data);
    }

    void saveDataActivity(){
        Intent saveIntent = new Intent(getApplicationContext(), SaveCSVActivity.class);
        startActivity(saveIntent);
    }

    void toggleDebug(){
        DEBUG = !DEBUG;

        if (DEBUG){
            // Enable console and debug
            LL_debugConsole.setVisibility(View.VISIBLE);
        } else {
            // Disable console and debug
            LL_debugConsole.setVisibility(View.GONE);
        }

    }
}