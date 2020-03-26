package com.tyz.bluetoothstudy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Administrator on 2017/4/4.
 */

public class ClientThread implements Runnable {

    final String TAG = "ClientThread";

    BluetoothAdapter mBtAdapter;
    BluetoothDevice mDevice;

    Handler mUiHandler;
    Handler writeHandler;

    BluetoothSocket mBtClientSocket;
    OutputStream mClientOutputStream;
    InputStream mClientInputStream;
    BufferedReader reader;

    BtConnnectStatusListener mBtConnnectStatusListener;

    public void SetConnectBack(BtConnnectStatusListener BtConnnectStatusListener) {
        this.mBtConnnectStatusListener = BtConnnectStatusListener;
    }

    public ClientThread(BluetoothAdapter mBtAdapter, BluetoothDevice device, Handler handler) {
        this.mBtAdapter = mBtAdapter;
        this.mDevice = device;
        this.mUiHandler = handler;
        BluetoothSocket tmpSocket = null;
        try {
            tmpSocket = device.createRfcommSocketToServiceRecord(UUID.fromString(Params.UUID));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBtClientSocket = tmpSocket;
    }

    @Override
    public void run() {

        Log.e(TAG, "----------------- do client thread run()");
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        try {
            try {
                // Connect the mDevice through the mBtClientSocket. This will block
                // until it succeeds or throws an exception
                mBtClientSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the mBtClientSocket and get mClientOutputStream
                try {
                    mBtClientSocket.close();
                    mBtConnnectStatusListener.onConnectFailed(mDevice);
                    return;
                } catch (IOException closeException) {
                }
                mBtConnnectStatusListener.onConnectFailed(mDevice);
                return;
            }
            mBtConnnectStatusListener.onConnectSuccess(mDevice);
            connectReader();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------- exception");
        }
    }

    private void connectReader() throws IOException {
        mClientOutputStream = mBtClientSocket.getOutputStream();
        mClientInputStream = mBtClientSocket.getInputStream();
        //reader = new BufferedReader(new InputStreamReader(mBtClientSocket.getInputStream(), "utf-8"));

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "-----------do client read run()");

                byte[] buffer = new byte[1024];
                int len;
                String content;
                try {
                    while ((len = mClientInputStream.read(buffer)) != -1) {
                        content = new String(buffer, 0, len);
                        Message message = new Message();
                        message.what = Params.MSG_CLIENT_REV_NEW;
                        message.obj = content;
                        mUiHandler.sendMessage(message);
                        Log.e(TAG, "------------- client read data mClientInputStream while ,send msg ui" + content);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void write(String data) {
//        data = data+"\r\n";
        try {
            mClientOutputStream.write(data.getBytes("utf-8"));
            Log.e(TAG, "---------- write data ok " + data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface BtConnnectStatusListener {
        void onConnectSuccess(BluetoothDevice device);

        void onConnectFailed(BluetoothDevice device);

        void onConnecting(BluetoothDevice device);
    }


}
