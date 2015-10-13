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

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ArduinoCommunicatorService extends Service {

    private final static String TAG = "ArduinoCommunicatorService";
    private final static boolean DEBUG = false;

    private boolean mIsRunning = false;
    private SenderThread mSenderThread;

    private volatile UsbDevice mUsbDevice = null;
    private volatile UsbDeviceConnection mUsbConnection = null;
    private volatile UsbEndpoint mInUsbEndpoint = null;
    private volatile UsbEndpoint mOutUsbEndpoint = null;

    final static String DATA_RECEIVED_INTENT = "benwang93.com.accelog.intent.action.DATA_RECEIVED";
    final static String SEND_DATA_INTENT = "benwang93.com.accelog.intent.action.SEND_DATA";
    final static String DATA_SENT_INTERNAL_INTENT = "benwang93.com.accelog.internal.intent.action.DATA_SENT";
    final static String DATA_EXTRA = "benwang93.com.accelog.intent.extra.DATA";
    final static String UPDATE_CHART_INTENT = "benwang93.com.accelog.intent.action.UPDATE_CHART";

    private final static int BAUD_RATE = 57600;






    // Packet specification
    private static final char ACCEL_SOP = '{';
//    private static final char ACCEL_DELIM = '\t';
//    private static final char ACCEL_EOP = '}';

    // Receive buffer definitions
    final static int PACKET_NUM_LONGS = 1;                              // Number of longs (timestamp)
    final static int PACKET_BYTES_LONG = 4;                             // Number of bytes in a long
    final static int PACKET_BUFF_NUM_FLOATS = 3;						// Number of floats in packet
    final static int PACKET_BYTES_FLOAT = 4;                            // Number of bytes in a float
    final static int PACKET_BUFF_LENGTH =                               // Length of packet (3x float = 12 B)
            PACKET_BUFF_NUM_FLOATS * PACKET_BYTES_FLOAT +
                    PACKET_NUM_LONGS * PACKET_BYTES_LONG;
    private static byte[] packetBuff = new byte[PACKET_BUFF_LENGTH];	// Buffer for packet
    private static int currBuffPos = 0;									// Current position in buffer

    // Frame skip for drawing graph
    public static final int LC_OSCOPE_FRAMESKIP = 10;       // Number of frames to skip
    public static int LC_oscope_currentFrameskip = 0;      // Counter for current frame





    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "onCreate()");
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction(SEND_DATA_INTENT);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "onStartCommand() " + intent + " " + flags + " " + startId);

        if (mIsRunning) {
            if (DEBUG) Log.i(TAG, "Service already running.");
            return Service.START_REDELIVER_INTENT;
        }

        mIsRunning = true;

        if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            if (DEBUG) Log.i(TAG, "Permission denied");
            Toast.makeText(getBaseContext(), getString(R.string.permission_denied), Toast.LENGTH_LONG).show();
            stopSelf();
            return Service.START_REDELIVER_INTENT;
        }

        if (DEBUG) Log.d(TAG, "Permission granted");
        mUsbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (!initDevice()) {
            if (DEBUG) Log.e(TAG, "Init of device failed!");
            stopSelf();
            return Service.START_REDELIVER_INTENT;
        }

        if (DEBUG) Log.i(TAG, "Receiving!");
        Toast.makeText(getBaseContext(), getString(R.string.receiving), Toast.LENGTH_SHORT).show();
        startReceiverThread();
        startSenderThread();

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy()");
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mUsbDevice = null;
        if (mUsbConnection != null) {
            mUsbConnection.close();
        }
    }

    private byte[] getLineEncoding(int baudRate) {
        final byte[] lineEncodingRequest = { (byte) 0x80, 0x25, 0x00, 0x00, 0x00, 0x00, 0x08 };
        switch (baudRate) {
        case 14400:
            lineEncodingRequest[0] = 0x40;
            lineEncodingRequest[1] = 0x38;
            break;

        case 19200:
            lineEncodingRequest[0] = 0x00;
            lineEncodingRequest[1] = 0x4B;
            break;

        case 57600:
            lineEncodingRequest[0] = (byte) 0x00;
            lineEncodingRequest[1] = (byte) 0xE1;
            break;

        case 115200:
            lineEncodingRequest[0] = (byte) 0x00;
            lineEncodingRequest[1] = (byte) 0xC2;
            lineEncodingRequest[2] = (byte) 0x01;

            break;
        }

        return lineEncodingRequest;
    }

    private boolean initDevice() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        mUsbConnection = usbManager.openDevice(mUsbDevice);
        if (mUsbConnection == null) {
            if (DEBUG) Log.e(TAG, "Opening USB device failed!");
            Toast.makeText(getBaseContext(), getString(R.string.opening_device_failed), Toast.LENGTH_LONG).show();
            return false;
        }
        UsbInterface usbInterface = mUsbDevice.getInterface(1);
        if (!mUsbConnection.claimInterface(usbInterface, true)) {
            if (DEBUG) Log.e(TAG, "Claiming interface failed!");
            Toast.makeText(getBaseContext(), getString(R.string.claimning_interface_failed), Toast.LENGTH_LONG).show();
            mUsbConnection.close();
            return false;
        }

        // Arduino USB serial converter setup
        // Set control line state
        mUsbConnection.controlTransfer(0x21, 0x22, 0, 0, null, 0, 0);
        // Set line encoding.
        mUsbConnection.controlTransfer(0x21, 0x20, 0, 0, getLineEncoding(BAUD_RATE), 7, 0);

        for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
            if (usbInterface.getEndpoint(i).getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (usbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_IN) {
                    mInUsbEndpoint = usbInterface.getEndpoint(i);
                } else if (usbInterface.getEndpoint(i).getDirection() == UsbConstants.USB_DIR_OUT) {
                    mOutUsbEndpoint = usbInterface.getEndpoint(i);
                }
            }
        }

        if (mInUsbEndpoint == null) {
            if (DEBUG) Log.e(TAG, "No in endpoint found!");
            Toast.makeText(getBaseContext(), getString(R.string.no_in_endpoint_found), Toast.LENGTH_LONG).show();
            mUsbConnection.close();
            return false;
        }

        if (mOutUsbEndpoint == null) {
            if (DEBUG) Log.e(TAG, "No out endpoint found!");
            Toast.makeText(getBaseContext(), getString(R.string.no_out_endpoint_found), Toast.LENGTH_LONG).show();
            mUsbConnection.close();
            return false;
        }

        return true;
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (DEBUG) Log.d(TAG, "onReceive() " + action);

            if (SEND_DATA_INTENT.equals(action)) {
                final byte[] dataToSend = intent.getByteArrayExtra(DATA_EXTRA);
                if (dataToSend == null) {
                    if (DEBUG) Log.i(TAG, "No " + DATA_EXTRA + " extra in intent!");
                    String text = String.format(getResources().getString(R.string.no_extra_in_intent), DATA_EXTRA);
                    Toast.makeText(context, text, Toast.LENGTH_LONG).show();
                    return;
                }

                mSenderThread.mHandler.obtainMessage(10, dataToSend).sendToTarget();
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                Toast.makeText(context, getString(R.string.device_detaches), Toast.LENGTH_LONG).show();
                mSenderThread.mHandler.sendEmptyMessage(11);
                stopSelf();
            }
        }
    };

    private void startReceiverThread() {
        new Thread("arduino_receiver") {
            public void run() {
                byte[] inBuffer = new byte[4096];
                while(mUsbDevice != null ) {
                    if (DEBUG) Log.d(TAG, "calling bulkTransfer() in");
                    final int len = mUsbConnection.bulkTransfer(mInUsbEndpoint, inBuffer, inBuffer.length, 0);
                    if (len > 0) {
                        parseForPacket(inBuffer, len);
                    } else {
                        if (DEBUG) Log.i(TAG, "zero data read!");
                    }
                }

                if (DEBUG) Log.d(TAG, "receiver thread stopped.");
            }
        }.start();
    }

    private void startSenderThread() {
        mSenderThread = new SenderThread("arduino_sender");
        mSenderThread.start();
    }

    private class SenderThread extends Thread {
        public Handler mHandler;

        public SenderThread(String string) {
            super(string);
        }

        public void run() {

            Looper.prepare();

            mHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (DEBUG) Log.i(TAG, "handleMessage() " + msg.what);
                    if (msg.what == 10) {
                        final byte[] dataToSend = (byte[]) msg.obj;

                        if (DEBUG) Log.d(TAG, "calling bulkTransfer() out");
                        final int len = mUsbConnection.bulkTransfer(mOutUsbEndpoint, dataToSend, dataToSend.length, 0);
                        if (DEBUG) Log.d(TAG, len + " of " + dataToSend.length + " sent.");
                        Intent sendIntent = new Intent(DATA_SENT_INTERNAL_INTENT);
                        sendIntent.putExtra(DATA_EXTRA, dataToSend);
                        sendBroadcast(sendIntent);
                    } else if (msg.what == 11) {
                        Looper.myLooper().quit();
                    }
                }
            };

            Looper.loop();
            if (DEBUG) Log.i(TAG, "sender thread stopped");
        }
    }



    // Call upon data receive to parse for floats to populate the x, y, and z axis acceleration
    private void  parseForPacket(byte[] receiveBuff, int receiveBuffLen){
        boolean SOPFound = false;		// Check for if buffer contains a SOP
        int remainingBuffLen = -1;		// Number of bytes left in receive buffer before next SOP or end of buffer
        int currReceivePos = 0;			// Current position in receive buffer

        try {
            Log.d("AcceLog", "Parsing buffer of len: " + receiveBuffLen + " str:\'" + new String(receiveBuff, 0, receiveBuffLen, "US-ASCII") + "\'");
        } catch (UnsupportedEncodingException e){
            Log.d("AcceLog", "Parsing buffer of len: " + receiveBuffLen);
        }
        // Exit if not recording
        if (!MainActivity.recordingIsStarted){
            Log.d("AcceLog", "Recording not started");
            return;
        }

        // Repeat while data available
        do {
            try {
                // Initialize
                SOPFound = bufferHasPacket(receiveBuff, receiveBuffLen, currReceivePos);	// Bool if SOP is found
                remainingBuffLen =											// Remaining characters until first SOP (or end of buffer)
                        SOPFound ?
                                indexOfSOP(receiveBuff, receiveBuffLen, currReceivePos) - currReceivePos:
                                receiveBuffLen - currReceivePos;
                Log.d("AcceLog", "Parsed: SOP?" + SOPFound + " Len:" + remainingBuffLen + " str:\'" + new String(receiveBuff, currReceivePos, remainingBuffLen, "US-ASCII") + "\'");

                // Error check length. Discard this data if wrong. Grab next
                if ((SOPFound && currBuffPos + remainingBuffLen != PACKET_BUFF_LENGTH) ||		// SOP found and incorrect length
                        (!SOPFound && currBuffPos + remainingBuffLen > PACKET_BUFF_LENGTH)){	// or buffer length appended is too long

                    // Debug
//                    if (DEBUG) displayMessage(TV_console, "Receiving error: Invalid packet length: " + (currBuffPos + remainingBuffLen) + "\n");
                    Log.d("AcceLog", "Receiving error: Invalid packet length: " + (currBuffPos + remainingBuffLen));

                    // Discard this data
                    if (SOPFound)
                        currReceivePos = indexOfSOP(receiveBuff, receiveBuffLen, currReceivePos) + 1;
                    else
                        currReceivePos = receiveBuffLen;

                    // Reset lengths/buffer
                    currBuffPos = 0;

                    // Parse for next packet
                    continue;
                }

                // Compute bytes to copy and copy buffer over
                int numBytesCopied = Math.min(PACKET_BUFF_LENGTH - currBuffPos, remainingBuffLen);
                System.arraycopy(receiveBuff, currReceivePos, packetBuff, currBuffPos, numBytesCopied);

                // Recalculate buffer position
                currReceivePos += numBytesCopied;
                currBuffPos += numBytesCopied;

                // Full & valid length buffer check
                // if full buffer, parse for floats and add to packet
                if (SOPFound && currBuffPos == PACKET_BUFF_LENGTH){
                    Log.d("AcceLog", "Packet found!");

                    // Create new packet
                    AccelSample currSample = new AccelSample();
//                    currSample.time = Calendar.getInstance().getTimeInMillis();
                    currSample.time = ByteBuffer.wrap(packetBuff, 0, PACKET_BYTES_LONG).order(ByteOrder.LITTLE_ENDIAN).getInt() + MainActivity.startTimeOffset;
                    currSample.aX = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 0 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    currSample.aY = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 1 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    currSample.aZ = ByteBuffer.wrap(packetBuff, PACKET_BYTES_FLOAT * 2 + PACKET_BYTES_LONG, PACKET_BYTES_FLOAT).order(ByteOrder.LITTLE_ENDIAN).getFloat();

                    // TODO: Check for data within bounds

                    // Add current sample to array
                    MainActivity.accelSamples.add(currSample);

                    Log.d("AcceLog", "Added to accelSamples at pos " + (MainActivity.accelSamples.size()-1));

                    // Frame skip
                    if (LC_oscope_currentFrameskip > LC_OSCOPE_FRAMESKIP) {
                        LC_oscope_currentFrameskip = 0;

                        Log.d("AcceLog", "Graphing at pos " + (MainActivity.accelSamples.size() - 1));

                        Intent intent = new Intent(UPDATE_CHART_INTENT);
                        intent.putExtra(DATA_EXTRA, MainActivity.accelSamples.size() - 1);
                        sendBroadcast(intent);
                    } else {
                        LC_oscope_currentFrameskip++;
                    }

                    // Reset lengths/buffer
                    currReceivePos++;		// Skip over SOP character
                    currBuffPos = 0;
                }
            } catch (Exception e){
//                displayMessage(TV_console, "Recieve exception: " + e.getMessage() + "\n");

                // On exception, discard this packet and reset buffer
                currReceivePos = indexOfSOP(receiveBuff, receiveBuffLen, currReceivePos) + 1;
                currBuffPos = 0;
            }
        } while (currReceivePos < receiveBuffLen);
    }

    // Returns index of next SOP starting from startIndex, or -1 if no SOP found
    private static int indexOfSOP(byte[] receiveBuff, int receiveBuffLen, int startIndex){
        for (int i = startIndex; i < receiveBuffLen; i++){
            if (receiveBuff[i] == ACCEL_SOP){
                return i;
            }
        }

        // Default value
        return -1;
    }

    // Returns true if buffer contains a SOP after startIndex
    private static boolean bufferHasPacket(byte[] receiveBuff, int receiveBuffLen, int startIndex){
        return indexOfSOP(receiveBuff, receiveBuffLen, startIndex) != -1;
    }
}
