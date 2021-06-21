package com.logismart.logismart;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainAdminActivity extends AppCompatActivity implements OnMyChangeListener {

    private static final String TAG = MainAdminActivity.class.getSimpleName();

    private SharedPreferences mPreferences;
    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";

    private Http http;
    HttpDataThread dataTask = null;
    Timer mTimer;
    TimerTask t;

    ThermoView ThermoView;
    ThermoGaugeView ThermoGaugeView;

    MapView mapView;
    TextView bt_name; // ble name
    TextView delivery;
    TextView startingPoint;
    TextView destination;
    TextView driverName;
    TextView driverPhone;

    LinearLayout info;

    Button bt_btn; // BLE btn
    Button exit_btn; // BLE exit btn

    ImageView ble_light; // BLE State light
    GradientDrawable drawable; // BLE State light drawable
    MapPoint mapPoint;
    MapPOIItem marker;

    private String USER_ID;
    private String USER_NAME;
    private int DRIVER_ID;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private int upperThermo;
    private int lowerThermo;
    private float prevThermo;

    private float startAngle;
    private float sweepAngle;

    private boolean mConnected = false;
    private Handler mHandler;

    private BackPressCloseHandler backPressCloseHandler;

    public void onBackPressed() {
        this.backPressCloseHandler.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        setContentView(R.layout.activity_main);

        http = new Http();
        mHandler = new Handler();

        info = findViewById(R.id.Manager_info);
        bt_name = findViewById(R.id.BT_name);
        delivery = findViewById(R.id.delivery);
        startingPoint = findViewById(R.id.from);
        destination = findViewById(R.id.to);
        driverName = findViewById(R.id.Manager_name);
        driverPhone = findViewById(R.id.Manager_phone);
        bt_btn = findViewById(R.id.BT_btn);
        exit_btn = findViewById((R.id.exit_btn));
        ble_light = findViewById(R.id.ble_light);
        drawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.light);

        mPreferences = getSharedPreferences(SharedPrefFile, MODE_PRIVATE);
        USER_ID = mPreferences.getString("admin_id", "null");
        USER_NAME = mPreferences.getString("admin_name", "null");

        bt_btn.setText("목록");

        bt_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ble_list();
            }
        });

        backPressCloseHandler = new BackPressCloseHandler(this);

        upperThermo = 50;
        lowerThermo = 0;
        prevThermo = (upperThermo + lowerThermo) / 2;
        startAngle = 270;

        ThermoView = findViewById((R.id.thermoView));
        ThermoView.setOnMyChangeListener(this);

        ThermoGaugeView = findViewById(R.id.thermoGaugeView);

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

        dataTask = new HttpDataThread();
        new Thread(dataTask).start();

        mTimer = new Timer();
        t = null;
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

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy called");
        super.onDestroy();
    }

    class HttpListThread implements Runnable {
        private JSONArray jArray;
        private boolean check = false;
        private boolean done = false;

        @Override
        public void run() {
            try {
                String result = http.Http(ServerURL.ADMIN_BLE_URL, USER_NAME);
                JSONObject jsonObject = new JSONObject(result);
                if (jsonObject.getString("result").equals("success")) {
                    Log.d(TAG, "run: Http Success");
                    jArray = jsonObject.getJSONArray("data");
                    check = true;
                }
                else {
                    Log.d(TAG, "run: Http False");
                }
                done = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONArray getData() {
            if (!done) {
                synchronized (this) {
                    try {
                        Log.d(TAG, "getData: wait!");
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (check) {
                Log.d(TAG, "getData: return data");
                return jArray;
            }

            return null;
        }
    }

    private TimerTask createTimerTask() {
        TimerTask timerTask = new TimerTask() {
            public void run()
            {
                dataTask.run();
                JSONObject jsonObject = dataTask.getData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            displayData(jsonObject);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        };
        return timerTask;
    }

    private void setmConnected(int connState) {
        if (connState == 1) {
            drawable.setColor(Color.GREEN);
            ble_light.setImageDrawable(drawable);
            mConnected = true;
        }
        else {
            drawable.setColor(Color.RED);
            ble_light.setImageDrawable(drawable);
            mConnected = false;
        }
    }

    private void setMethod(ListViewItem device) throws JSONException {
        bt_name.setText(device.getBleName());
        startingPoint.setText(device.getBleFrom());
        destination.setText(device.getBleTo());
        DRIVER_ID = device.getBleDriverId();
        driverName.setText(device.getBleDriver());
        driverPhone.setText(device.getBleDriverPhone());
        delivery.setText(device.getShipName());
        upperThermo = device.getUpper();
        lowerThermo = device.getLower();
        prevThermo = (upperThermo + lowerThermo) / 2;
        startAngle = 270;

        setmConnected(device.getBleConnection());
        displayData(null);
    }

    private void ble_list() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainAdminActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(R.layout.activity_ble, null);
        builder.setView(view);

        final ListView device_list = (ListView) view.findViewById(R.id.device_list);
        final Button exit_btn = (Button) view.findViewById(R.id.exit_btn);
        final AlertDialog dialog = builder.create();

        HttpListThread task = new HttpListThread();
        new Thread(task).start();
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        getLeDevice(task);
        device_list.setAdapter(mLeDeviceListAdapter);

        device_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                final ListViewItem device = mLeDeviceListAdapter.getDevice(position);
                Log.d(TAG, "onItemClick: " + device);

                if (device == null) return;

                try {
                    setMethod(device);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dataRead();

                dialog.dismiss();
            }
        });

        exit_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void getLeDevice(HttpListThread task) {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    mLeDeviceListAdapter.addDevice(task.getData());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<ListViewItem> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<ListViewItem>();
            mInflator = MainAdminActivity.this.getLayoutInflater();
        }

        public void addDevice(JSONArray jArray) throws JSONException {
            if (!jArray.isNull(0)) {
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject jsonObject = jArray.getJSONObject(i);
                    String bleName = jsonObject.getString("name");
                    String bleFrom = jsonObject.getString("from");
                    String bleTo = jsonObject.getString("to");
                    int bleConnection = jsonObject.getInt("connect");
                    int bleDriverId = jsonObject.getInt("driverId");
                    String bleDriver = jsonObject.getString("driverName");
                    String bleDriverPhone = jsonObject.getString("driverPhone");
                    String shipName = jsonObject.getString("shipName");
                    int upper = jsonObject.getInt("upper");
                    int lower = jsonObject.getInt("lower");

                    ListViewItem item = new ListViewItem();
                    item.setBleName(bleName);
                    item.setBleFrom(bleFrom);
                    item.setBleTo(bleTo);
                    item.setBleConnection(bleConnection);
                    item.setBleDriverId(bleDriverId);
                    item.setBleDriver(bleDriver);
                    item.setBleDriverPhone(bleDriverPhone);
                    item.setShipName(shipName);
                    item.setUpper(upper);
                    item.setLower(lower);

                    mLeDevices.add(item);
                }
            }
        }

        public ListViewItem getDevice(int i) {
            return mLeDevices.get(i);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int position) {
            return mLeDevices.get(position);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View view, ViewGroup viewGroup) {
            MainAdminActivity.ViewHolder viewHolder;
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new MainAdminActivity.ViewHolder();
                viewHolder.deviceFromTo = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (MainAdminActivity.ViewHolder) view.getTag();
            }

            final ListViewItem device = mLeDevices.get(position);

            viewHolder.deviceName.setText(device.getBleName());
            viewHolder.deviceFromTo.setText(device.getBleFromTo());

            return view;
        }
    }

    class HttpDataThread implements Runnable {
        JSONObject jsonObject = null;
        private boolean check = false;
        private boolean done = false;

        @Override
        public void run() {
            done = false;
            check = false;
            try {
                String result = http.Http(ServerURL.ADMIN_DATA_URL, Integer.toString(DRIVER_ID));

                if (!result.isEmpty()) {
                    jsonObject = new JSONObject(result);
                    if (jsonObject.getString("result").equals("success")) {
                        Log.d(TAG, "run: Http Success");
                        check = true;
                    }
                    else {
                        Log.d(TAG, "run: Http False");
                    }
                }
                done = true;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONObject getData() {
            if (!done) {
                synchronized (this) {
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (check)
                return jsonObject;

            return null;
        }
    }

    public void dataRead() {
        Log.d(TAG, "dataRead: mConnected - " + mConnected);

        if (mConnected) {
            Log.d(TAG, "dataRead: run" + t);
            if (t == null) {
                t = createTimerTask();
                mTimer.schedule(t, 0, 1000);
            }

        }
        else {
            Log.d(TAG, "dataRead: cancel");
            if (t != null) {
                t.cancel();
                t = null;
            }
        }

    }

    private void displayData(JSONObject data) throws JSONException {
        String LAT = "37.28270048101858";
        String LON = "127.89990486148284";
        float thermo = prevThermo;
        int connState = 0;

        if (data != null) {
            Log.d(TAG, "displayData: " + data);

            LAT = data.getString("lat");
            LON = data.getString("lon");
            thermo = (float) data.getDouble("thermo");
            connState = data.getInt("conn");

            setmConnected(connState);

            if (thermo == -404.0) {
                thermo = 0;
            }
            ThermoView.changeValueEvent(thermo);
        }
        else {
            ThermoView.changeValueEvent(0);
        }

        if (LAT != "null" && LON != "null") {
            mapPoint = MapPoint.mapPointWithGeoCoord(Float.parseFloat(LAT), Float.parseFloat(LON));
            mapView.setMapCenterPoint(mapPoint, true);
            marker.setMapPoint(mapPoint);
        }

        calculateAngle(thermo);
        gaugeAnimator();

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
        TextView deviceFromTo;
    }

}