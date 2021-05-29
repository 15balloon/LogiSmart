package com.logismart.logismart;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
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

import java.io.IOException;
import java.util.ArrayList;

public class MainAdminActivity extends AppCompatActivity implements OnMyChangeListener {

    private static final String TAG = MainAdminActivity.class.getSimpleName();

    private SharedPreferences mPreferences;
    private final String SharedPrefFile = "com.logismart.android.SharedPreferences";

    private Http http;

    ThermoView ThermoView;
    ThermoGaugeView ThermoGaugeView;

    MapView mapView;
    TextView bt_name; // ble name
    TextView delivery;
    TextView startingPoint;
    TextView destination;
    TextView driver_name;
    TextView driver_phone;

    LinearLayout info;

    Button bt_btn; // BLE btn
    Button exit_btn; // BLE exit btn

    ImageView ble_light; // BLE State light
    GradientDrawable drawable; // BLE State light drawable
    MapPoint mapPoint;
    MapPOIItem marker;

    private String USER_ID;
    private String USER_NAME;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    private int upperThermo;
    private int lowerThermo;
    private float prevThermo;

    private float startAngle;
    private float sweepAngle;

    private boolean mConnected = false;

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

        info = findViewById(R.id.Manager_info);
        info.setVisibility(View.INVISIBLE);
        bt_name = findViewById(R.id.BT_name);
        delivery = findViewById(R.id.delivery);
        startingPoint = findViewById(R.id.from);
        destination = findViewById(R.id.to);
        driver_name = findViewById(R.id.Manager_name);
        driver_phone = findViewById(R.id.Manager_phone);
        bt_btn = findViewById(R.id.BT_btn);
        exit_btn = findViewById((R.id.exit_btn));
        ble_light = findViewById(R.id.ble_light);
        drawable = (GradientDrawable) ContextCompat.getDrawable(this, R.drawable.light);

        Intent intent = getIntent();
        USER_ID = intent.getStringExtra("id");
        USER_NAME = intent.getStringExtra("name");

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

    public void startDataRead() {
        if (mConnected) {
            try {
                Log.d(TAG, "startDataRead: mConnected True");

            } catch (Exception e) {
                Log.d("Exception", e.getMessage());
            }
        }
    }

    class HttpListThread implements Runnable {
        private JSONArray jArray;
        private boolean check = false;
        private boolean done = false;

        @Override
        public void run() {
            try {
                String result = http.Http(ServerURL.ADMIN_BLE_URL, USER_NAME);
                JSONObject jsonObject = null;
                jsonObject = new JSONObject(result);
                if (jsonObject.getString("result").equals("success")) {
                    Log.d(TAG, "run: Http Success");
                    jArray = jsonObject.getJSONArray("data");
                    check = true;
                    done = true;
                }
                else {
                    Log.d(TAG, "run: Http False");
                    done = true;
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONArray getData() {
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
                return jArray;

            return null;
        }
    }

    private void setMethod(ListViewItem device) {
        bt_name.setText(device.getBleName());
        startingPoint.setText(device.getBleFrom());
        destination.setText(device.getBleTo());
        driver_name.setText(device.getBleDriver());
        driver_phone.setText(device.getBleDriverPhone());
        delivery.setText(device.getShipName());
        upperThermo = device.getUpper();
        lowerThermo = device.getLower();
        prevThermo = (upperThermo + lowerThermo) / 2;

        if (device.getBleConnection() == 1) {
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
        device_list.setAdapter(mLeDeviceListAdapter);

        device_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    mLeDeviceListAdapter.addDevice(task.getData());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                final ListViewItem device = mLeDeviceListAdapter.getDevice(position);
                Log.d(TAG, "onItemClick: " + device);

                if (device == null) return;
                setMethod(device);

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

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<ListViewItem> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<ListViewItem>();
            mInflator = MainAdminActivity.this.getLayoutInflater();
        }

        public void addDevice(JSONArray jArray) throws JSONException {
            if (!jArray.isNull(1)) {
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject jsonObject = jArray.getJSONObject(i);
                    String bleName = jsonObject.getString("name");
                    String bleFrom = jsonObject.getString("from");
                    String bleTo = jsonObject.getString("to");
                    int bleConnection = jsonObject.getInt("connect");
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

    private void displayData(String data) {
        if (data != null) {
            Log.i("Main DATA", data);
            String[] uuid = data.split(" ");
            if (uuid[1].isEmpty())
                return;
            String[] data_item = uuid[1].split("/");
            switch (uuid[0]) {
                case "GPS":
                    mapPoint = MapPoint.mapPointWithGeoCoord(Float.parseFloat(data_item[0]), Float.parseFloat(data_item[1]));
                    mapView.setMapCenterPoint(mapPoint, true);
                    marker.setMapPoint(mapPoint);
                    break;
                case "Thermo":
                    ThermoView.changeValueEvent(Float.parseFloat(data_item[1]));
                    calculateAngle(Float.parseFloat(data_item[1]));
                    gaugeAnimator();
                    break;
            }
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
        TextView deviceFromTo;
    }

}