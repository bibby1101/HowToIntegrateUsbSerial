package com.bibby.howtointegrateusbserial;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UsbActivity extends AppCompatActivity {

    public static final String TAG = UsbActivity.class.getSimpleName();

    ArrayList<String> data = new ArrayList<>();
    RecyclerView info;
    private MyAdapter myAdapter;


    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    PendingIntent mPermissionIntent;
    public UsbSerialDriver driver;
    boolean isExit = false;
    Thread ReadSerial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        Utils.logThread();

        ShowLog("onCreate()");

        myAdapter = new MyAdapter(data);
        info = (RecyclerView) this.findViewById(R.id.info);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        info.setLayoutManager(layoutManager);

        info.setAdapter(myAdapter);

        mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, filter);

        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_ATTACHED));
        registerReceiver(mUsbDeviceReceiver, new IntentFilter(UsbManager.ACTION_USB_ACCESSORY_DETACHED));

        connect();
    }

    public void connect(){
        isExit = false;

        // Get UsbManager from Android.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        //here do emulation to ask all connected usb device for permission
        for (final UsbDevice usbDevice : manager.getDeviceList().values()) {
            //add some conditional check if necessary
            //if(isWeCaredUsbDevice(usbDevice)){
            if(manager.hasPermission(usbDevice)){
                //if has already got permission, just goto connect it
                //that means: user has choose yes for your previously popup window asking for grant perssion for this usb device
                //and also choose option: not ask again

                // Find the first available driver.
                driver = UsbSerialProber.acquire(manager);

                if (driver != null) {

                    try {
                        driver.open();
                        driver.setBaudRate(9600);

                        ReadSerial = new ReadSerial();
                        ReadSerial.start();
                        ShowLog("device open");
                    } catch (IOException e) {
                        // Deal with error.
                        ShowLog("io exception");
                    } finally {
                /*
				try {
					driver.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				*/
                    }
                }
                else{
                    ShowToast("沒找到裝置");
                }

            }else{
                //this line will let android popup window, ask user whether to allow this app to have permission to operate this usb device
                manager.requestPermission(usbDevice, mPermissionIntent);
            }
            //}
        }
    }

    public void disconnect(){
        isExit = true;

        if(driver!=null){
            try {
                driver.close();
                driver = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private List<String> mData;

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.info_text.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public TextView info_text;
            public ViewHolder(View itemView) {
                super(itemView);
                info_text = (TextView) itemView.findViewById(R.id.info_text);
            }
        }
        public MyAdapter(List<String> data){
            this.mData = data;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        ShowLog("onResume()");
    }

    @Override
    protected void onPause() {
        super.onPause();
        ShowLog("onPause()");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUsbReceiver);
        unregisterReceiver(mUsbDeviceReceiver);
        disconnect();
    }

    public void ShowLog(final String msg){
        Log.d(TAG, "==========\r\n" + msg + "\r\n==========");
        data.add(msg);
        if(myAdapter!=null){
            myAdapter.notifyDataSetChanged();
        }
    }

    public void ShowToast(final String msg){
        Toast.makeText(UsbActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            ShowLog("onReceive():" + action);
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){
                            ShowLog("permission granted for device VID=" + device.getVendorId() + " PID=" + device.getProductId());
                            connect();
                        }
                    }
                    else {
                        ShowLog("permission denied for device VID=" + device.getVendorId() + " PID=" + device.getProductId());
                    }
                }
            }
        }
    };

    private final BroadcastReceiver mUsbDeviceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                device.toString();
                ShowLog("ACTION_USB_DEVICE_ATTACHED");
                ShowToast("ACTION_USB_DEVICE_ATTACHED");

                connect();

            }else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
//                device.toString();
                ShowLog("ACTION_USB_DEVICE_DETACHED");
                ShowToast("ACTION_USB_DEVICE_DETACHED");

                disconnect();

            }else if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
                ShowLog("ACTION_USB_ACCESSORY_ATTACHED");
            }else if(UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
                ShowLog("ACTION_USB_ACCESSORY_DETACHED");
            }
        }

    };

    public byte[] getHexStringToHex(String strValue) { // 41 41 01 5A 5A
        int intCounts = strValue.length() / 2;
        String strReturn = "";
        String strHex = "";
        int intHex = 0;
        byte byteData[] = new byte[intCounts];
        try {
            for (int intI = 0; intI < intCounts; intI++) {
                strHex = strValue.substring(0, 2);
                strValue = strValue.substring(2);
                intHex = Integer.parseInt(strHex, 16);
                if (intHex > 128)
                    intHex = intHex - 256;
                byteData[intI] = (byte) intHex;
            }
            strReturn = new String(byteData,"ISO8859-1");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return byteData;
    }

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public class ReadSerial extends Thread {
        @Override
        public void run() {

            Utils.logThread();

            String result = "";

            byte[] buffer2 = new byte[1024];
            int bytes2;
            String input = "";
            int numBytesRead;

            ByteArrayOutputStream inflatedStream = new ByteArrayOutputStream(10);

            while(true){
                try {
                    if(isExit)break;

                    while((numBytesRead=driver.read(buffer2, 0))!=0){
                        inflatedStream.write(buffer2, 0, numBytesRead);
                    }

                    if(inflatedStream.size()!=0) {
                        final byte[] Data = inflatedStream.toByteArray();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ShowLog(bytesToHex(Data));
                            }
                        });

                        inflatedStream.reset();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
