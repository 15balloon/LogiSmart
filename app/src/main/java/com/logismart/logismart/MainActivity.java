package com.logismart.logismart;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    View ThermoView;
    MapView mapView;
    TextView bt_name; // ble name

    String type; // 모드 (관리자/운반자)
    String ble_name; // 운반자의 기기명

    Button bt_btn; // BLE btn
    Button exit_btn; // BLE 닫기 btn

    ImageView ble_light; // BLE State light
    GradientDrawable drawable; // BLE State light drawable

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private ArrayList<BluetoothGattCharacteristic> mGattCharacteristics;
    private String mDeviceAddress;
    private String mDeviceName;
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 30000; // 30sec
    private BluetoothLeService mBle;

    private boolean mConnected = false;
    private boolean initialStart = true;

    private BackPressCloseHandler backPressCloseHandler;

    public void onBackPressed() {
        this.backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        setContentView(R.layout.activity_main);

        TextView tv = findViewById(R.id.tv);
        bt_name = findViewById(R.id.BT_name);
        bt_btn = findViewById(R.id.BT_btn);
        exit_btn = findViewById((R.id.exit_btn));
        ble_light = findViewById(R.id.ble_light);
        drawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.light);

        Intent intent = getIntent();
        type = intent.getStringExtra("type");

        if (type.equals("admin")) {
            // 여러 설정 변경

            tv.setText(type);
            bt_btn.setText("목록");
        }
        else {
            ble_name = intent.getStringExtra("ble");
            tv.setText(type);
            bt_name.setSelected(true);
            bt_name.setText(ble_name);
        }

        backPressCloseHandler = new BackPressCloseHandler(this);

        mHandler = new Handler();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mGattCharacteristics = new ArrayList<>();
        mDeviceAddress = "";
        mDeviceName = "";
        mBle = new BluetoothLeService();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection_mBle, BIND_AUTO_CREATE);

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            startForegroundService(gattServiceIntent);
//        }
//        else {
//            startService(gattServiceIntent);
//        }

        permissionCheckBLE();

        bt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBluetoothAdapter == null) {
                    Toast.makeText(MainActivity.this, "블루투스 미지원 단말은 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }

                else if (mBluetoothAdapter.isEnabled()) { // 블루투스 ON 상태

                    permissionCheckLocation(); // Location 동의 dialog

                    int locationcheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

                    if (locationcheck == PackageManager.PERMISSION_GRANTED) { // permission location check

                        if (!settingCheckLocation()) { // 위치 꺼져있을 때
                            showDialogForLocationServiceSetting(); // 위치 켜기 dialog
                        }

                        else {
                            ble_connect();
                            // TODO: BLE Connect
                            // service 사용해서 앱 꺼도 연결 해제, 데이터 전송 중단 안 되게
                        }
                    }
                }

                else { // 블루투스 OFF 상태
                    permissionCheckBLE();
                }
            }
        });

        ThermoView = findViewById((R.id.thermoView));

//        getHashKey();

        mapView = (MapView) findViewById(R.id.map_view);
        MapPoint mapPoint = MapPoint.mapPointWithGeoCoord(37.28270048101858, 127.89990486148284);
        mapView.setMapCenterPoint(mapPoint, true);

//        MapCircle marker = new MapCircle(mapPoint, 15, android.graphics.Color.argb(255, 40, 80, 150), android.graphics.Color.argb(255, 50, 100, 200));
//        mapView.addCircle(marker);

        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("현위치");
        marker.setTag(0);
        marker.setMapPoint(mapPoint);
//        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage); // 마커타입을 커스텀 마커로 지정.
//        marker.setCustomImageResourceId(R.drawable.custom_marker_red); // 마커 이미지.
        marker.setMarkerType(MapPOIItem.MarkerType.BluePin); // 기본으로 제공하는 BluePin 마커 모양.
        marker.setSelectedMarkerType(MapPOIItem.MarkerType.RedPin); // 마커를 클릭했을때, 기본으로 제공하는 RedPin 마커 모양.
        mapView.addPOIItem(marker);

    }

    private void getHashKey(){
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo == null)
            Log.e("KeyHash", "KeyHash:null");

        for (Signature signature : packageInfo.signatures) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            } catch (NoSuchAlgorithmException e) {
                Log.e("KeyHash", "Unable to get MessageDigest. signature=" + signature, e);
            }
        }
    }

    private void permissionCheckLocation() { // 위치 퍼미션 체크
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permissionCheckLocation: Success");
        }
        else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
        }
    }

    private void permissionCheckBLE() { // 블루투스 퍼미션 체크
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private boolean settingCheckLocation() { // 위치 서비스 켜져 있는지 체크
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case 1000: { // Location
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "onRequestPermissionsResult: Permission Location Success");
                } else {
                    Log.d(TAG, "onRequestPermissionsResult: Permission Location Retry");

                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        final Snackbar snackbar = Snackbar.make(findViewById(R.id.main_layout), "블루투스 연결을 위해 위치 권한이 필요합니다.\n위치 권한을 승인해주세요.", Snackbar.LENGTH_LONG);
                        snackbar.setAction("권한 설정", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
                            }
                        });
                        snackbar.show();
                    }
                    else {
                        showDialogForPermissionLocation();
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case 1: { // Bluetooth
                if (mBluetoothAdapter.isEnabled()) {
                    Log.d(TAG, "onActivityResult: Bluetooth Allow");
                } else {
                    Log.d(TAG, "onActivityResult: Bluetooth Declined");
                    Toast.makeText(MainActivity.this, "기기 연결을 위해 블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
                }
                break;
            }

            case 1001: { // Location
                if (settingCheckLocation()) {
                    Log.d(TAG, "onActivityResult : Location Setting Success");
                }
                break;
            }
        }
    }

    private void showDialogForPermissionLocation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 정보 권한 설정");
        builder.setMessage("권한 거절로 인해 기능이 제한됩니다.\n" + "권한을 승인해주세요.");
        builder.setPositiveButton("권한 설정", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface paramAnonymousDialogInterface, int paramAnonymousInt){
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
                    startActivity(intent);
                }
            }});
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int paramAnonymousInt) {
                dialog.cancel();
            }});
        builder.create().show();
    }

    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 설정");
        builder.setMessage("블루투스 연결을 위해서는\n" +
                            "위치 서비스 활성화가 필요합니다.\n" + "위치 서비스를 켜주세요.");
        builder.setCancelable(true);
        builder.setPositiveButton("서비스 설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, 1001);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    private void ble_connect() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_ble, null);
        builder.setView(view);

        final ListView device_list = (ListView) view.findViewById(R.id.device_list);
        final Button exit_btn = (Button) view.findViewById(R.id.exit_btn);
        final AlertDialog dialog = builder.create();

        scanLeDevice(true);
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        device_list.setAdapter(mLeDeviceListAdapter);

        device_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
                Log.d(TAG, "onItemClick: " + device);

                if (device == null) return;
                mDeviceAddress = device.getAddress();
                mDeviceName = device.getName();

                Boolean result = mBle.connect(mDeviceAddress);
                Log.d(TAG, "onItemClick: connect result - " + result);

                if (mScanning) {
                    mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                    mScanning = false;
                }
                dialog.dismiss();
            }
        });

        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                mScanning = false;
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
                        Log.d(TAG, "run: Stop Scan");
                    }
                }
            }, SCAN_PERIOD);

            mScanning = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);
                Log.d(TAG, "scanLeDevice: Start Scan");
            }

        } else {

            mScanning = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
            }
        }
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();

            viewHolder.deviceName.setText(deviceName);
            viewHolder.deviceAddress.setText(device.getAddress());


            return view;
        }
    }

    public class BackPressCloseHandler {
        private Activity activity;
        private long backKeyPressedTime;
        private Toast toast;

        public BackPressCloseHandler(Activity context) {
            this.backKeyPressedTime = 0;
            this.activity = context;
        }

        public void onBackPressed() {
            this.toast = Toast.makeText(this.activity, "뒤로가기 버튼을 한 번 더 누르면 앱이 종료됩니다.", Toast.LENGTH_SHORT);

            if (System.currentTimeMillis() > this.backKeyPressedTime + 2000) {
                this.backKeyPressedTime = System.currentTimeMillis();
                this.toast.show();
            } else if (System.currentTimeMillis() <= this.backKeyPressedTime + 2000) {
                this.activity.finishAffinity();
                this.toast.cancel();
                System.runFinalization();
                System.exit(0);

            }
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.d(TAG, "onScanFailed: ERROR");
        }

        private void processResult(final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (result.getDevice().getName() != null) {
                        // type
                        if (type.equals("admin")) {
                            mLeDeviceListAdapter.addDevice(result.getDevice());
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Connect list: " + result.getDevice().getName());
                        }
                        else {
                            if (result.getDevice().getName().contains(ble_name)) {
                                mLeDeviceListAdapter.addDevice(result.getDevice());
                                mLeDeviceListAdapter.notifyDataSetChanged();
                                Log.d(TAG, "Connect list: " + result.getDevice().getName());
                            }
                        }

                    }
                }
            });
        }
    };

    private final ServiceConnection mServiceConnection_mBle = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {

            mBle = (((BluetoothLeService.LocalBinder) service).getService());
            if (!mBle.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            else {
                Log.d(TAG, "onServiceConnected: Initialize Bluetooth");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBle = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver_mBle = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {

                case BluetoothLeService.ACTION_GATT_CONNECTED:
                    Log.d(TAG, "onReceive: CONNECTED");
                    bt_name.setSelected(true);
                    bt_name.setText(mDeviceName);
                    invalidateOptionsMenu();

                    break;

                case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                    Log.d(TAG, "onReceive: DISCONNECTED");
                    invalidateOptionsMenu();
                    break;

                case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                    Log.d(TAG, "onReceive: Services Discovered");
                    if (mDeviceAddress != null && mBle != null) {
                        displayGattServices(mBle.getSupportedGattServices());
                        startDataRead();
                    }
                    break;

                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    Log.d(TAG, "onReceive: Data available");
                    if (mBle != null)
//                        displayData(intent.getStringExtra(mBle.EXTRA_DATA));
                    break;

            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
        if(initialStart) {
            initialStart = false;
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume called");
        super.onResume();
        registerReceiver(mGattUpdateReceiver_mBle, makeGattUpdateIntentFilter());

        if (mBle != null && mDeviceAddress != null)
        {
//            runOnUiThread(this::ble_connect);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause called");
        super.onPause();
//        unregisterReceiver(mGattUpdateReceiver_mBle);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart called");
        if(mBle != null)
            mBle.attach();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop called");
        if(mBle != null)
            mBle.detach();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
//        unbindService(mServiceConnection_mBle);
//        mBle = null;
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                mGattCharacteristics.add(gattCharacteristic);
            }
        }
        Log.d(TAG, "displayGattServices: " + mGattCharacteristics);
    }

    public void startDataRead() {
        if (mConnected) {
            mConnected = false;
            Log.d(TAG, "startDataRead: DisConnected!");
            drawable.setColor(Color.RED);
            ble_light.setImageDrawable(drawable);
            mBle.disconnect();
            mDeviceAddress = null;
            mGattCharacteristics.clear();
        } else {
            mConnected = true;
            drawable.setColor(Color.GREEN);
            ble_light.setImageDrawable(drawable);
            if (mConnected) {
                try {
                    Log.d(TAG, "startDataRead: mConnected True" + mGattCharacteristics);
                    for (BluetoothGattCharacteristic characteristic : mGattCharacteristics) {
                        if (characteristic != null) {
                            ThreadItem(characteristic);
                        }
                    }

                } catch (Exception e) {
                    Log.d("Exception", e.getMessage());
                }
            }
        }
    }

    public synchronized void ThreadItem(final BluetoothGattCharacteristic bluetoothGattCharacteristic) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                if (mDeviceAddress != null && bluetoothGattCharacteristic != null) {
                    try {
                        Log.d(TAG, "run: ThreadItem");
                        if (mBle != null)
                            mBle.setCharacteristicNotification(bluetoothGattCharacteristic, true);
                    } catch (Exception e) {
                        Log.d("Exception", e.getMessage());
                    }
                }
            }
        }).start();
    }

    private void displayData(String data) {
        if (data != null) {
            Log.i("DATA", data);
            String[] uuid = data.split(" ");
            String[] data_item = uuid[1].split("/");
            switch (uuid[0]) {
                case "GPS":
                    // TODO : update UI, data -> server
                    break;
                case "Thermo":
                    // update UI, data -> server
                    break;
            }
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}