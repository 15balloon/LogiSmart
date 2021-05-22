package com.logismart.logismart;

import android.Manifest;
import android.animation.ObjectAnimator;
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
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.graphics.Color;
import android.graphics.Path;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class MainDriverActivity extends AppCompatActivity implements OnMyChangeListener {

    private static final String TAG = MainDriverActivity.class.getSimpleName();

    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";
    private SharedPreferences mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);


    private final String USER_ID = String.valueOf(mPreferences.getInt("id", 0));
    ThermoView ThermoView;
    ThermoGaugeView ThermoGaugeView;

    MapView mapView;
    TextView bt_name; // Connected ble name
    TextView startingPoint;
    TextView destination;
    TextView manager_name;
    TextView manager_phone;

    String ble_name; // Driver's ble name

    Button bt_btn; // BLE btn
    Button exit_btn; // BLE exit btn

    ImageView ble_light; // BLE State light
    GradientDrawable drawable; // BLE State light drawable
    MapPoint mapPoint;
    MapPOIItem marker;

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

    private Http http;

    private int upperThermo;
    private int lowerThermo;
    private float prevThermo;

    private float startAngle;
    private float sweepAngle;

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

        bt_name = findViewById(R.id.BT_name);
        startingPoint = findViewById(R.id.from);
        destination = findViewById(R.id.to);
        manager_name = findViewById(R.id.Manager_name);
        manager_phone = findViewById(R.id.Manager_phone);
        bt_btn = findViewById(R.id.BT_btn);
        exit_btn = findViewById((R.id.exit_btn));
        ble_light = findViewById(R.id.ble_light);
        drawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.light);

        Intent intent = getIntent();
        ble_name = intent.getStringExtra("ble");
        startingPoint.setText(intent.getStringExtra("from"));
        destination.setText(intent.getStringExtra("to"));
        manager_name.setText(intent.getStringExtra("manager"));
        manager_phone.setText(intent.getStringExtra("phone"));
        bt_name.setSelected(true);
        bt_name.setText(ble_name);

        mHandler = new Handler();
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mGattCharacteristics = new ArrayList<>();
        mBle = new BluetoothLeService();
        mDeviceAddress = "";

        http = new Http();

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection_mBle, BIND_AUTO_CREATE);

        permissionCheckBLE();

        bt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mBluetoothAdapter == null) {
                    Toast.makeText(MainDriverActivity.this, "블루투스 미지원 단말은 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
                }

                else if (mBluetoothAdapter.isEnabled()) { // 블루투스 ON 상태

                    permissionCheckLocation(); // Location 동의 dialog

                    int locationcheck = ContextCompat.checkSelfPermission(MainDriverActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);

                    if (locationcheck == PackageManager.PERMISSION_GRANTED) { // permission location check

                        if (!settingCheckLocation()) { // 위치 꺼져있을 때
                            showDialogForLocationServiceSetting(); // 위치 켜기 dialog
                        }

                        else {
                            ble_connect();
                        }
                    }
                }

                else { // 블루투스 OFF 상태
                    permissionCheckBLE();
                }
            }
        });

        backPressCloseHandler = new BackPressCloseHandler(this);

        mDeviceName = "";

        upperThermo = intent.getIntExtra("upper", 50);
        lowerThermo = intent.getIntExtra("lower", 0);
        prevThermo = (upperThermo + lowerThermo) / 2;
        startAngle = 270;

        ThermoView = findViewById((R.id.thermoView));
        ThermoView.setOnMyChangeListener(this);

        ThermoGaugeView = findViewById(R.id.thermoGaugeView);

//        getHashKey();

        mapView = (MapView) findViewById(R.id.map_view);
        mapPoint = MapPoint.mapPointWithGeoCoord(37.28270048101858, 127.89990486148284);
        mapView.setMapCenterPoint(mapPoint, true);

//        MapCircle marker = new MapCircle(mapPoint, 15, android.graphics.Color.argb(255, 40, 80, 150), android.graphics.Color.argb(255, 50, 100, 200));
//        mapView.addCircle(marker);

        marker = new MapPOIItem();
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
        if (ContextCompat.checkSelfPermission(MainDriverActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "permissionCheckLocation: Success");
        }
        else {
            ActivityCompat.requestPermissions(MainDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
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
                                ActivityCompat.requestPermissions(MainDriverActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1000);
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
                    Toast.makeText(MainDriverActivity.this, "기기 연결을 위해 블루투스를 켜주세요.", Toast.LENGTH_SHORT).show();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainDriverActivity.this);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(MainDriverActivity.this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(MainDriverActivity.this);
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
            mInflator = MainDriverActivity.this.getLayoutInflater();
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
                        if (result.getDevice().getName().contains(ble_name)) {
                            mLeDeviceListAdapter.addDevice(result.getDevice());
                            mLeDeviceListAdapter.notifyDataSetChanged();
                            Log.d(TAG, "Connect list: " + result.getDevice().getName());
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
                        try {
                            http.Http(ServerURL.CARRIER_CONNECTION_URL, USER_ID, "connect");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        displayGattServices(mBle.getSupportedGattServices());
                        startDataRead();
                    }
                    break;

                case BluetoothLeService.ACTION_DATA_AVAILABLE:
                    Log.d(TAG, "onReceive: Data available");
                    if (mBle != null) {
                        try {
                            displayData(intent.getStringExtra(mBle.EXTRA_DATA));
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;

            }
        }
    };

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume called");
        super.onResume();
        if (initialStart) {
            registerReceiver(mGattUpdateReceiver_mBle, makeGattUpdateIntentFilter());
            initialStart = false;
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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.d(TAG, "onNewIntent called");
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

    public synchronized void ThreadServer(final String[] data) {

        new Thread(new Runnable() {

            @Override
            public void run() {
                String[] data_item = data[1].split("/");
                String result = "";
                try {
                    switch (data[0]) {
                        case "GPS":
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mapPoint = MapPoint.mapPointWithGeoCoord(Float.parseFloat(data_item[0]), Float.parseFloat(data_item[1]));
                                    mapView.setMapCenterPoint(mapPoint, true);
                                    marker.setMapPoint(mapPoint);
                                }
                            });

                            result = http.Http(ServerURL.CARRIER_GPS_URL, USER_ID, data_item[0], data_item[1]);
                            break;

                        case "Thermo":
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ThermoView.changeValueEvent(Float.parseFloat(data_item[1]));
                                    calculateAngle(Float.parseFloat(data_item[1]));
                                    gaugeAnimator();
                                }
                            });

                            result = http.Http(ServerURL.CARRIER_THERMO_URL, USER_ID, data_item[1]);
                            break;
                    }

                    if (!result.isEmpty()) {
                        JSONObject jsonObject = new JSONObject(result);
                        if (jsonObject.getString("result").equals("success")) {
                            Log.d(TAG, "displayData: Data send Success");
                        } else {
                            Log.d(TAG, "displayData: Data send Fail");
                        }
                    }

                } catch (IOException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void displayData(String data) throws IOException, JSONException {
        if (data != null) {
            Log.i("Main DATA", data);
            String[] uuid = data.split(" ");
            if (uuid[1].isEmpty())
                return;
            ThreadServer(uuid);
        }
    }

    @Override
    public void onChange(float value) {
        // push alarm when dangerous?
        // change background color?
    }

    public void calculateAngle(float currentThermo) {
        sweepAngle = (currentThermo - prevThermo) * 180 / (upperThermo - lowerThermo);
        prevThermo = currentThermo;
    }

    public void gaugeAnimator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Path path = new Path();
            path.arcTo(-360f, 0f, 340f, 700f, startAngle, sweepAngle, true);
            ObjectAnimator animator = ObjectAnimator.ofFloat(ThermoGaugeView, View.X, View.Y, path);
            animator.setDuration(300);
            animator.start();
            startAngle += sweepAngle;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

}