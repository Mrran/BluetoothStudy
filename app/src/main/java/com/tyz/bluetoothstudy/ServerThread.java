package com.tyz.bluetoothstudy;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
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

public class ServerThread implements Runnable {

    final String TAG = "ServerThread";

    BluetoothAdapter mBtAdapter;
    BluetoothServerSocket mBtServerSocket =null;
    BluetoothSocket mConnSocket = null;
    Handler mUiHandler;
    Handler writeHandler;

    OutputStream mConnOs;
    InputStream mConnIs;
    BufferedReader reader;

    boolean mAcceptFlag = true;

    public ServerThread(BluetoothAdapter bluetoothAdapter, Handler handler) {
        this.mBtAdapter = bluetoothAdapter;
        this.mUiHandler = handler;
        BluetoothServerSocket tmpSocket = null;
        try {
            tmpSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(Params.NAME, UUID.fromString(Params.UUID));
        } catch (IOException e) {
            e.printStackTrace();
        }
        mBtServerSocket = tmpSocket;
        Log.e(TAG, "-------------- do new()");
    }

    @Override
    public void run() {
        Log.e(TAG, "-------------- do run()");
        try {
            while (mAcceptFlag) {
                mConnSocket = mBtServerSocket.accept();
                // 阻塞，直到有客户端连接
                if (mConnSocket != null) {
                    Log.e(TAG, "-------------- mBtClientSocket not null, get a client");

                    mConnOs = mConnSocket.getOutputStream();
                    mConnIs = mConnSocket.getInputStream();
                    //reader=new BufferedReader(new InputStreamReader(mBtClientSocket.getInputStream(),"utf-8"));

                    BluetoothDevice remoteDevice = mConnSocket.getRemoteDevice();
                    Message message = new Message();
                    message.what = Params.MSG_REV_A_CLIENT;
                    message.obj = remoteDevice;
                    mUiHandler.sendMessage(message);

                    // 读取服务器 mBtClientSocket 数据
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG, "-----------do server read run()");

                            byte[] buffer = new byte[1024];
                            int len;
                            String content;
                            try {
                                while ((len = mConnIs.read(buffer)) != -1) {
                                    content = new String(buffer, 0, len);
                                    Message message = new Message();
                                    message.what = Params.MSG_CLIENT_REV_NEW;
                                    message.obj = content;
                                    mUiHandler.sendMessage(message);
                                    Log.e(TAG, "------------- server read data mClientInputStream while ,send msg ui" + content);
                                }

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
//                    Looper.prepare();
//                    writeHandler = new Handler() {
//                        @Override
//                        public void handleMessage(Message msg) {
//                            switch (msg.what) {
//                                case Params.MSG_SERVER_WRITE_NEW:
//                                    String data = msg.obj.toString() + "\n";
//                                    try {
//                                        mClientOutputStream.write(data.getBytes("utf-8"));
//                                        Log.e(TAG, "-------------server write data " + data);
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                    break;
//                            }
//                        }
//                    };
//                    Looper.loop();
                    break;
                }
            }// end while(true)
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void write(String data){
//        data = data+"\r\n";
        try {
            mConnOs.write(data.getBytes("utf-8"));
            Log.e(TAG, "---------- write data ok "+data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cancel() {
        try {
            mAcceptFlag = false;
            mBtServerSocket.close();
            Log.e(TAG, "-------------- do cancel ,flag is "+ mAcceptFlag);

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "----------------- cancel " + TAG + " error");
        }
    }
}
