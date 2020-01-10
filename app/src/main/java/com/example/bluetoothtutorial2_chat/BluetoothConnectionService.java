package com.example.bluetoothtutorial2_chat;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "Tutorial2";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;
    
    private ConnectThread mConnectThread;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    // ====================================================================
    // AcceptThread and ConnectThread are basically the same, except Accept is
    // for the server side, attempting to connect to a client and Connect is
    // the client side for a server attempting to connect to it.
    // =====================================================================

    // This thread runs while listening for incoming connections
    private class AcceptThread extends Thread {
        // Local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket that other devices connect to
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread: Setting up server using UUID: " + MY_UUID_INSECURE);
            } catch(IOException e) {
                Log.e(TAG, "AcceptThread: IOException while setting up server socket: ", e);
            }

            mmServerSocket = tmp;
        }

        // The run method automatically runs when Thread is created
        public void run() {
            Log.d(TAG, "run: AcceptThread running.");
            BluetoothSocket socket = null;

            try {
                Log.d(TAG, "run: RFCOMM server socket start...");

                // Blocking call, so will only return on a successful connection
                // or unsuccessful connection
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCOMM server socket accepted connection.");
            } catch (IOException e) {
                Log.e(TAG, "run: IOException while accepting connection: ", e);
            }

            // TODO: Third part
            if (socket != null) {
                connected(socket, mmDevice);
            }

            Log.i(TAG, "run: END mAcceptThread.");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Cancelling AcceptThread.");

            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: IOException while cancelling: ", e);
            }
        }
    }
    
    // This thread runs while attempting to make an outgoing connection
    // with a device. Runs until the connection succeeds or fails
    private class ConnectThread extends Thread {
        private BluetoothSocket mmSocket;
        
        public ConnectThread(BluetoothDevice device, UUID uuid) {
            Log.d(TAG, "ConnectThread: started.");
            mmDevice = device;
            deviceUUID = uuid;
        }
        
        // The run method automatically runs when the Thread is created
        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "run: mConnectThread.");
            
            // Get a BluetoothSocket for a connection with the 
            // given BluetoothDevice
            try {
                Log.d(TAG, "run: Trying to create socket using UUID: " + MY_UUID_INSECURE);
                tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
            } catch(IOException e) {
                Log.e(TAG, "run: Could not create insecure RFCOMM socket: ", e);
            }
            
            mmSocket = tmp;
            
            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();
            
            // Make a connection to BluetoothSocket
            // (Blocking call)
            try {
                mmSocket.connect();
                Log.d(TAG, "run: ConnectThread connected.");
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "run: IOException while handling IOException. Attempted to close socket", e);
                }
                Log.e(TAG, "run: IOException. Closed socket :", e);
            }

            // TODO: part 3
            connected(mmSocket, mmDevice)
        }

        public void cancel() {
            Log.d(TAG, "cancel: Cancelling ConnectThread.");

            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: IOException while cancelling: ", e);
            }
        }
    }

    // This method starts the chat service (specifically AcceptThread).
    // Called by activity onResume().
    public synchronized void start() {
        Log.d(TAG, "start: Begin.");

        // If any thread is attempting to connect, cancel it
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Start AcceptThread (as it's a thread we can use .start()
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    // AcceptThread starts and sits waiting for connection, the ConnectThread starts and
    // attempts to make a connection with another devices AcceptThread
    public void startClient(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Begin.");

        // initprogress dialog
        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                , "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    // ConnectedThread manages the connection once connected
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Begin.");

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Close progress dialog box when connection is established
            mProgressDialog.dismiss();

            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "ConnectedThread: Error getting in/out streams", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        // Run continually reads from input stream.
        // buffer stores incoming data from stream, and bytes is used to read from the buffer.
        // If connection fails, IOException is caught and loop breaks, ending connection
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while(true) {
                try {
                    bytes = mmInStream.read(buffer);

                    // This code is specific to application, used to change inocming data to the
                    // string of text for chat, but can be changed to anything to fit purpose
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "run: IncomingMessage :" + incomingMessage);
                } catch (IOException e) {
                    Log.e(TAG, "run: error reading inStream, ending comms: ", e);
                    break;
                }
            }
        }

        // Called in MainActivity to send data to remote device
        public void write (byte[] bytes) {
            // Convert string for logging
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to outputStream: " + text);

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "write: Error writing to outputstream: ", e);
            }
        }

        // Called in MainActivity to cancel connection
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Error closing connection: ", e);
            }
        }
    }

    // Called in MainActivity to start managing transmission
    private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
        Log.d(TAG, "connected: Begin.");

        // Start thread to manage connection and perform transmissions
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    // Write to ConnectedThread in un-synchronised manner
    public void write(byte[] out) {
        // Create temp object
        ConnectedThread r;

        Log.d(TAG, "write: Called.");

        mConnectedThread.write(out);
    }

}
