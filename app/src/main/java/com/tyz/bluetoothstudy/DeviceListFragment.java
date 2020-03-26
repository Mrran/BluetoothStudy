package com.tyz.bluetoothstudy;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.jakewharton.rxbinding3.view.RxView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.reactivex.functions.Consumer;
import kotlin.Unit;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * Created by Administrator on 2017/4/4.
 */
public class DeviceListFragment extends Fragment {

    final String TAG = "DeviceListFragment";
    String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000

    RecyclerView mDeviceListRv;
    MyListAdapter mDeviceListAdapter;
    List<BluetoothDevice> mDeviceList = new ArrayList<>();

    BluetoothAdapter mBtAdapter;
    MyBtReceiver mBtReceiver;
    IntentFilter intentFilter;

    MainActivity mainActivity;
    Handler mUiHandler;

    ClientThread mBtClientThread;
    ServerThread mBtServerThread;
    Toastinerface mToast;
    boolean is;

    private final String ACTIONFILTER = "android.bluetooth.device.action.PAIRING_REQUEST";

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case Params.MY_PERMISSION_REQUEST_CONSTANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 运行时权限已授权
                }
                return;
            }
        }
    }

    public void setUiToastInerface(Toastinerface toastinerface) {
        this.mToast = toastinerface;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBtAdapter == null) {
            Log.e(TAG, "--------------- 不支持蓝牙");
            getActivity().finish();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bt_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        final ExecutorService threadExecutor = Executors.newFixedThreadPool(1);
        mDeviceListRv = (RecyclerView) view.findViewById(R.id.device_list_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        // 设置布局管理器
        mDeviceListRv.setLayoutManager(layoutManager);
        mDeviceListAdapter = new MyListAdapter();
        mDeviceListRv.setAdapter(mDeviceListAdapter);
        mDeviceListAdapter.notifyDataSetChanged();

        mDeviceListAdapter.setOnItemClick(new OnItemClickListener() {
            @Override
            public void onItemClick(final int position) {
//                 关闭服务器监听
                Log.e(TAG, "accept:点击位置 ：" + position);
                handleItemClick(position, threadExecutor);
            }
        });
        mDeviceListAdapter.setOnBtnclick(new OnBtnClickListener() {
            @Override
            public void onBtnClick(int position) {
                try {
                    //通过工具类ClsUtils,调用createBond方法
                    BluetoothDevice btDevice = mDeviceList.get(position);
                    ClsUtils.createBond(btDevice.getClass(), btDevice);
                } catch (Exception e) {

                    e.printStackTrace();
                }

            }
        });
    }

    private void handleItemClick(int position, ExecutorService threadExecutor) {
        if (mBtServerThread != null) {
            mBtServerThread.cancel();
            mBtServerThread = null;
            Log.e(TAG, "---------------client item click , cancel server thread ," +
                    "server thread is null");
        }
        final BluetoothDevice device = mDeviceList.get(position);
        // 开启客户端线程，连接点击的远程设备
        mBtClientThread = new ClientThread(mBtAdapter, device, mUiHandler);
        threadExecutor.execute(mBtClientThread);
//                new Thread(mBtClientThread).start();
        mBtClientThread.SetConnectBack(new ClientThread.BtConnnectStatusListener() {
            @Override
            public void onConnectSuccess(BluetoothDevice device) {
                // 通知 ui 连接的服务器端设备
                Message message = new Message();
                message.what = Params.MSG_CONNECT_TO_SERVER;
                message.obj = device;
                mUiHandler.sendMessage(message);
            }

            @Override
            public void onConnectFailed(BluetoothDevice device) {
                mToast.uiToast("连接失败,请检查服务端是否打开。");
            }

            @Override
            public void onConnecting(BluetoothDevice device) {
                mToast.uiToast("请稍等，正在连接中。");
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        mUiHandler = mainActivity.getUiHandler();

    }

    @Override
    public void onResume() {
        super.onResume();

        // 蓝牙未打开，询问打开
        if (!mBtAdapter.isEnabled()) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(i, Params.REQUEST_ENABLE_BT);
        }

        intentFilter = new IntentFilter();
        mBtReceiver = new MyBtReceiver();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(ACTIONFILTER);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        getActivity().registerReceiver(mBtReceiver, intentFilter);

        mBtReceiver.SetPairlistener(new MakePariBlueToothListener() {
            @Override
            public void whilePari(BluetoothDevice device) {

            }

            @Override
            public void pairingSuccess(BluetoothDevice device) {
                mDeviceListAdapter.notifyDataSetChanged();
                mToast.uiToast("配对完成");
            }

            @Override
            public void cancelPari(BluetoothDevice device) {

            }
        });

        // 蓝牙已开启
        if (mBtAdapter.isEnabled()) {
            showBondedDevice();
            // 默认开启服务线程监听
            if (mBtServerThread != null) {
                mBtServerThread.cancel();
            }
            Log.e(TAG, "-------------- new server thread");
            mBtServerThread = new ServerThread(mBtAdapter, mUiHandler);
            new Thread(mBtServerThread).start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mBtReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.main, menu);
    }


//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        menu.add(Menu.NONE, R.id.enable_visibility, Menu.NONE, "Menu A");
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_visibility:
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                startActivityForResult(enableIntent, Params.REQUEST_ENABLE_VISIBILITY);
                break;
            case R.id.discovery:
                if (mBtAdapter.isDiscovering()) {
                    mBtAdapter.cancelDiscovery();
                }
                if (Build.VERSION.SDK_INT >= 6.0) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            Params.MY_PERMISSION_REQUEST_CONSTANT);
                }

                mBtAdapter.startDiscovery();
                break;
            case R.id.disconnect:
                mBtAdapter.disable();
                mDeviceList.clear();
                mDeviceListAdapter.notifyDataSetChanged();
                mToast.uiToast("蓝牙已关闭");
                break;
        }
        return super.onOptionsItemSelected(item);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Params.REQUEST_ENABLE_BT: {
                if (resultCode == RESULT_OK) {
                    showBondedDevice();
                }
                break;
            }
            case Params.REQUEST_ENABLE_VISIBILITY: {
                if (resultCode == 600) {
                    mToast.uiToast("蓝牙已设置可见");
                } else if (resultCode == RESULT_CANCELED) {
                    mToast.uiToast("蓝牙设置可见失败,请重试");
                }
                break;
            }
        }
    }

    /**
     * 用户打开蓝牙后，显示已绑定的设备列表
     */
    private void showBondedDevice() {
        mDeviceList.clear();
        Set<BluetoothDevice> deviceSet = mBtAdapter.getBondedDevices();
        mDeviceList.addAll(deviceSet);
        mDeviceListAdapter.notifyDataSetChanged();
    }


    /**
     * 向 mBtClientSocket 写入发送的数据
     *
     * @param dataSend
     */
    public void writeData(String dataSend) {
//        Message message =new Message();
//        message.obj = dataSend;
//        if (mBtServerThread!=null){
//            message.what=Params.MSG_SERVER_WRITE_NEW;
//            mBtServerThread.writeHandler.sendMessage(message);
//        }
//        if (mBtClientThread!=null){
//            message.what=Params.MSG_CLIENT_WRITE_NEW;
//            mBtClientThread.writeHandler.sendMessage(message);
//        }
        if (mBtServerThread != null) {
            mBtServerThread.write(dataSend);
        } else if (mBtClientThread != null) {
            mBtClientThread.write(dataSend);
        }
    }


    /**
     * 设备列表的adapter
     */
    private class MyListAdapter extends RecyclerView.Adapter<ViewHolder> {

        public MyListAdapter() {
        }

        private OnItemClickListener mOnItemClickListener;
        private OnBtnClickListener mbtnOnBtnClickListener;

        public void setOnItemClick(OnItemClickListener listener) {
            this.mOnItemClickListener = listener;
        }

        public void setOnBtnclick(OnBtnClickListener ClickListener) {
            this.mbtnOnBtnClickListener = ClickListener;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // 创建一个View，简单起见直接使用系统提供的布局，就是一个TextView

            View view = View.inflate(parent.getContext(), R.layout.layout_item_bt_device, null);

            // 创建一个ViewHolder

            ViewHolder holder = new ViewHolder(view);

            return holder;

        }

        @SuppressLint("CheckResult")
        @Override
        public void onBindViewHolder(ViewHolder viewHolder, final int position) {
            // 绑定数据到ViewHolder上
            int bondState = mDeviceList.get(position).getBondState();
            String name = mDeviceList.get(position).getName();
            String mac = mDeviceList.get(position).getAddress();
            String state;
            if (name == null || name.length() == 0) {
                name = "未命名设备";
            }
            if (bondState == BluetoothDevice.BOND_BONDED) {
                state = "ready";
                viewHolder.deviceState.setTextColor(getResources().getColor(R.color.green));
            } else {
                state = "new";
                viewHolder.deviceState.setTextColor(getResources().getColor(R.color.red));
            }
            if (mac == null || mac.length() == 0) {
                mac = "未知 mac 地址";
            }
            viewHolder.deviceName.setText(name);
            viewHolder.deviceMac.setText(mac);
            viewHolder.deviceState.setText(state);
            if (mDeviceList.get(position).getBondState() == BluetoothDevice.BOND_BONDED) {
                viewHolder.devicePair.setText(R.string.repaired);
            }
//            viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                }
//            });
            RxView.clicks(viewHolder.itemView).throttleFirst(10, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Unit>() {
                        @Override
                        public void accept(Unit unit) throws Exception {
                            mOnItemClickListener.onItemClick(position);
                        }
                    });
            RxView.clicks(viewHolder.devicePair).debounce(10, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Unit>() {
                        @Override
                        public void accept(Unit unit) throws Exception {
                            mbtnOnBtnClickListener.onBtnClick(position);
                        }
                    });
//            viewHolder.devicePair.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    mbtnOnBtnClickListener.onItemClick(position);
//                }
//            });
        }


        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return mDeviceList.size();
        }


    }

    /**
     * 该item点击事件对应的接口
     */
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    /**
     * 按钮点击事件对应的接口
     */
    public interface OnBtnClickListener {
        void onBtnClick(int position);
    }

    /**
     * 与 adapter 配合的 viewholder
     */

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView deviceName;
        public TextView deviceMac;
        public TextView deviceState;
        public Button devicePair;

        public ViewHolder(View itemView) {

            super(itemView);

//			mTextView = (TextView) itemView;
            deviceName = (TextView) itemView.findViewById(R.id.device_name);
            deviceMac = (TextView) itemView.findViewById(R.id.device_mac);
            deviceState = (TextView) itemView.findViewById(R.id.device_state);
            devicePair = (Button) itemView.findViewById(R.id.device_pair);
        }

    }

    public interface MakePariBlueToothListener {

        public void whilePari(BluetoothDevice device);

        public void pairingSuccess(BluetoothDevice device);

        public void cancelPari(BluetoothDevice device);
    }

    /**
     * 广播接受器
     */
    private class MyBtReceiver extends BroadcastReceiver {
        MakePariBlueToothListener mMakePariListener;

        public void SetPairlistener(MakePariBlueToothListener makePariBlueToothListener) {
            this.mMakePariListener = makePariBlueToothListener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice btDevice = null;  //创建一个蓝牙device对象
            // 从Intent中获取设备对象
            btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                mToast.uiToast("开始搜索 ...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mToast.uiToast("搜索结束");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (isNewDevice(device)) {
                    mDeviceList.add(device);
                    mDeviceListAdapter.notifyDataSetChanged();
                    //mDeviceListRv.scrollToPosition(mDeviceList.size()-1);
                    Log.e(TAG, "---------------- " + device.getName());
                }
            } else if (ACTIONFILTER.equals(action)) {
                Log.e("action2=", action);
                Log.e("here", "btDevice.getName()");
                try {
                    //1.确认配对
                    ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                    //2.终止有序广播
                    Log.i("order...", "isOrderedBroadcast:" + isOrderedBroadcast() + ",isInitialStickyBroadcast:" + isInitialStickyBroadcast());
                    abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                    //3.调用setPin方法进行配对...
                    boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {

                switch (btDevice.getBondState()) {
                    case BluetoothDevice.BOND_BONDING:// 正在配对
                        mMakePariListener.whilePari(btDevice);
                        break;
                    case BluetoothDevice.BOND_BONDED:// 配对结束
                        mMakePariListener.pairingSuccess(btDevice);
                        break;
                    case BluetoothDevice.BOND_NONE:// 取消配对/未配对
                        mMakePariListener.cancelPari(btDevice);
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 判断搜索的设备是新蓝牙设备，且不重复
     *
     * @param device
     * @return
     */
    private boolean isNewDevice(BluetoothDevice device) {
        boolean repeatFlag = false;
        for (BluetoothDevice tmpDevice : mDeviceList) {
            if (tmpDevice.getAddress().equals(device.getAddress())) {
                repeatFlag = true;
            }
        }
        //不是已绑定状态，且列表中不重复
        return device.getBondState() != BluetoothDevice.BOND_BONDED && !repeatFlag;
    }


}
