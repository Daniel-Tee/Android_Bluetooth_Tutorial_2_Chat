package com.example.bluetoothtutorial2_chat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionServ";
    private static final String appName = "Tutorial2";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    private final BluetoothAdapter mBluetoothAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    public BluetoothConnectionService(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

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

}
