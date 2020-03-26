package com.tyz.bluetoothstudy;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

/**
 * Created by Administrator on 2017/4/4.
 */
public class DataTransFragment extends Fragment {

    TextView mDeviceNameTv;
    ListView mShowDataLv;
    EditText mInputEt;
    Button mSendBtn;
    ArrayAdapter<String> mDataAdapter;

    Handler uiHandler;

    BluetoothDevice remoteDevice;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_data_trans, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mDeviceNameTv = (TextView) view.findViewById(R.id.device_name_tv);
        mShowDataLv = (ListView) view.findViewById(R.id.show_data_lv);
        mInputEt = (EditText) view.findViewById(R.id.input_et);
        mSendBtn = (Button) view.findViewById(R.id.send_bt);
        mSendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msgSend = mInputEt.getText().toString();
                Message message = new Message();
                message.what = Params.MSG_WRITE_DATA;
                message.obj = msgSend;
                uiHandler.sendMessage(message);
                mInputEt.setText("");
            }
        });

        mDataAdapter = new ArrayAdapter<String>(getContext(), R.layout.layout_item_new_data);
        mShowDataLv.setAdapter(mDataAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        MainActivity act = (MainActivity) getActivity();
        if (act != null) {
            uiHandler = act.getUiHandler();
        }
    }

    /**
     * 显示连接远端(客户端)设备
     */
    public void showRemoteDevice(BluetoothDevice clientDevice) {
        this.remoteDevice = clientDevice;
        mDeviceNameTv.setText("连接设备: " + remoteDevice.getName());
    }

    /**
     * 显示新消息
     *
     * @param newMsg
     */
    public void updateDataView(String newMsg,int role) {
        if (role == Params.REMOTE) {
            String remoteName = remoteDevice.getName()==null ? "未命名设备":remoteDevice.getName();
            newMsg = remoteName + " : " + newMsg;
        } else if (role == Params.LOCAL){
            newMsg = "我 : " + newMsg;
        }
        mDataAdapter.add(newMsg);
        mShowDataLv.setSelection(mDataAdapter.getCount()-1);
    }

    /**
     * 客户端连接服务器端设备后，显示
     *
     * @param serverDevice
     */
    public void showConnectToServer(BluetoothDevice serverDevice) {
        this.remoteDevice = serverDevice;
        mDeviceNameTv.setText("连接设备: " + remoteDevice.getName());
    }
}
