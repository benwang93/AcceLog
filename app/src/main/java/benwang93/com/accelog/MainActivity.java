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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

    // Packet specification
    private static final char ACCEL_SOP = '{';
    private static final char ACCEL_DELIM = '\t';
    private static final char ACCEL_EOP = '}';

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
    private String receivedData = "";

    // Arraylist for saving all readings
    public static ArrayList<AccelSample> accelSamples = new ArrayList<>(0);

    // Simple date formatter for X-axis values on chart
    public static Date startTime = Calendar.getInstance().getTime();
    public static SimpleDateFormat sdf_graph = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

    // UI Elements
    TextView TV_console;
    LineChart LC_oscope;
    Button Btn_startStop;
    LinearLayout LL_debugConsole;

    // Start/Stop button functionality (true == recording)
    boolean recordingIsStarted = false;

    // Frame skip for drawing graph
    private static final int LC_OSCOPE_FRAMESKIP = 0;
    private static int LC_oscope_currentFrameskip = 0;

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
        registerReceiver(mReceiver, filter);

        // Grab UI elements
        TV_console = (TextView) findViewById(R.id.MainActivity_TextView_Console);
        TV_console.setMovementMethod(new ScrollingMovementMethod());

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
            case R.id.MainActivity_action_save:
                displayMessage(TV_console, "Save option selected!");
                saveDataActivity();
                return true;
            case R.id.MainActivity_action_load:
                displayMessage(TV_console, "Load option selected");
//                startActivity(new Intent(Intent.ACTION_VIEW,
//                        Uri.parse("http://ron.bems.se/arducom/primaindex.php")));
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

            // Convert transferred data into string
            String newTransferredDataString = new String(newTransferredData);
            receivedData += newTransferredDataString;

            // Receive and parse packet
            parseForPacket();


            // Display new text
//            displayMessage(TV_console, receiving ? "R: " : "S: "); displayMessage(TV_console, /*"New message: " +*/ newTransferredDataString + "\n");
            if (!receiving) displayMessage(TV_console, "S: " + newTransferredDataString + "\n");
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive() " + action);

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

    private void parseForPacket(){
        // Parse for complete packet
        int startIndex = receivedData.indexOf(ACCEL_SOP);
        int endIndex = receivedData.indexOf(ACCEL_EOP, startIndex);
		String currPacket;

        // Exit if not recording
        if (!recordingIsStarted){
            // Delete data
            receivedData = "";

            // Exit
            return;
        }

		// Exit if EOP not present
		if (endIndex < 0 || startIndex < 0) return;

        // Debug receive print
        if (DEBUG) displayMessage(TV_console, "Packet found: [" + receivedData.substring(startIndex + 1, endIndex) + "]\n");

		// Grab packet
		StringTokenizer packetTokens = new StringTokenizer(receivedData.substring(startIndex + 1, endIndex));

		// Parse packet type
		String packetType = packetTokens.nextToken();
		switch (packetType.charAt(0)){
			case 'G':					// Acceleration in Gs
				// Check for correct number of elements (# tokens == 3)
				if (packetTokens.countTokens() == 3){
					AccelSample currSample = new AccelSample();
					currSample.time = Calendar.getInstance().getTimeInMillis();
					currSample.aX = Double.parseDouble(packetTokens.nextToken());
					currSample.aY = Double.parseDouble(packetTokens.nextToken());
					currSample.aZ = Double.parseDouble(packetTokens.nextToken());
					
					// TODO: Check for data within bounds
					
					// Add current sample to array
					accelSamples.add(currSample);
					
					// Update graph
                    if (LC_oscope_currentFrameskip > LC_OSCOPE_FRAMESKIP){
                        LC_oscope_currentFrameskip = 0;
                        updateChart(currSample);
                    } else {
                        LC_oscope_currentFrameskip++;
                    }

                    break;
				} else {
                    displayMessage(TV_console, "Incorrect number of packets!\n");
				}
			default:					// Invalid packet type
                displayMessage(TV_console, "Invalid packet type! (" + packetType + ")\n");
		}
		
		// Remove parsed data from buffer
		receivedData = receivedData.substring(endIndex + 1);
    }

    private void updateChart(AccelSample sample){
        LineData data = LC_oscope.getData();

        // Add new x value
        data.addXValue(sdf_graph.format(new Date(sample.time)));

        // Set new data
        data.addEntry(new Entry((float)sample.aX, data.getDataSetByIndex(0).getEntryCount()), 0);
        data.addEntry(new Entry((float)sample.aY, data.getDataSetByIndex(1).getEntryCount()), 1);
        data.addEntry(new Entry((float) sample.aZ, data.getDataSetByIndex(2).getEntryCount()), 2);

        LC_oscope.notifyDataSetChanged();

        // limit the number of visible entries
        LC_oscope.setVisibleXRangeMaximum(120);

        // move to the latest entry
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
        saveIntent.putExtra(DATA_SAVE, receivedData.getBytes());
        startActivity(saveIntent);

        // Clear received data buffer
        receivedData = "";
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