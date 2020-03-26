package com.tyz.bluetoothstudy;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import static com.tyz.bluetoothstudy.Params.MSG_DATA;

public class MainActivity extends AppCompatActivity implements Toastinerface {

    final String TAG = "MainActivity";

    TabLayout mTabLyt;
    ViewPager mContentVp;
    MyPagerAdapter mPagerAdapter;
    String[] mTitleList =new String[]{"设备列表","数据传输"};
    List<Fragment> mFragList =new ArrayList<>();

    DeviceListFragment mDeviceListFrag;
    DataTransFragment mDataTransFrag;

    @SuppressLint("HandlerLeak")
    Handler mUiHandler =new Handler(){
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what){
                case Params.MSG_REV_A_CLIENT:
                    Log.e(TAG,"--------- uihandler set mDevice name, go to data frag");
                    BluetoothDevice clientDevice = (BluetoothDevice) msg.obj;
                    mDataTransFrag.showRemoteDevice(clientDevice);
                    mContentVp.setCurrentItem(1);
                    break;
                case Params.MSG_CONNECT_TO_SERVER:
                    Log.e(TAG,"--------- uihandler set mDevice name, go to data frag");
                    BluetoothDevice serverDevice = (BluetoothDevice) msg.obj;
                    mDataTransFrag.showConnectToServer(serverDevice);
                    mContentVp.setCurrentItem(1);
                    break;
                case Params.MSG_SERVER_REV_NEW:
                case Params.MSG_CLIENT_REV_NEW:
                    String newMsgFromClient = msg.obj.toString();
                    mDataTransFrag.updateDataView(newMsgFromClient, Params.REMOTE);
                    break;
                case Params.MSG_WRITE_DATA:
                    String dataSend = msg.obj.toString();
                    mDataTransFrag.updateDataView(dataSend, Params.LOCAL);
                    mDeviceListFrag.writeData(dataSend);
                    break;
                case Params.CONNECT_FAILE:
                    Log.e(TAG,"--------- connect faile");
                    uiToast("连接失败");
                    break;
                case MSG_DATA:
                    Log.e(TAG,"--------- connect faile");
                    Toast.makeText(MainActivity.this,msg.getData().getString("data"),Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initUI();
    }


    /**
     * 返回 mUiHandler
     * @return
     */
    public Handler getUiHandler(){
        return mUiHandler;
    }

    /**
     * 初始化界面
     */
    private void initUI() {
        mTabLyt = (TabLayout) findViewById(R.id.tab_layout);
        mContentVp = (ViewPager) findViewById(R.id.view_pager);

        mTabLyt.addTab(mTabLyt.newTab().setText(mTitleList[0]));
        mTabLyt.addTab(mTabLyt.newTab().setText(mTitleList[1]));

        mDeviceListFrag =new DeviceListFragment();
        mDataTransFrag =new DataTransFragment();
        mDeviceListFrag.setUiToastInerface(this);
        mFragList.add(mDeviceListFrag);
        mFragList.add(mDataTransFrag);

        mPagerAdapter =new MyPagerAdapter(getSupportFragmentManager());
        mContentVp.setAdapter(mPagerAdapter);
        mTabLyt.setupWithViewPager(mContentVp);
    }
    /**
     * Toast 提示
     */
    @Override
    public void uiToast(String data) {
        Message message = new Message();
        Bundle bundle=new Bundle();
        bundle.putString("data", data);
        message.setData(bundle);//bundle传值，耗时，效率低
        message.what = MSG_DATA;
        mUiHandler.sendMessage(message);
    }

    /**
     * ViewPager 适配器
     */
    public class MyPagerAdapter extends FragmentPagerAdapter{

        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragList.get(position);
        }

        @Override
        public int getCount() {
            return mFragList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitleList[position];
        }
    }



}
