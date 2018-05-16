package ru.exp_net.gps_mqtt_client;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.renderscript.ScriptGroup;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;


import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;

    final String DOWNLOADED_DATA = "DOWNLOADED_DATA";
    final String DOWNLOADED_DATA_RSSI = "DOWNLOADED_DATA_RSSI";
    Handler handler;
    Marker Point, MyPosition;

    int idColIndex;
    int timeColIndex;
    int gpsColIndex;
    int rssiColIndex;

    SQLiteDatabase db;
    MainActivity.DBHelper dbHelper;
    Cursor c;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();

        dbHelper = new MainActivity.DBHelper(this);
        db = dbHelper.getWritableDatabase();


        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                animateMarker(MyPosition,new LatLng(location.getLatitude(),location.getLongitude()),false, "MyPosition");
                //Toast.makeText(MapsActivity.this,location.getLatitude()+", "+location.getLongitude(),Toast.LENGTH_LONG).show();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);
            }
        };

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                config();
                break;
            default:
                break;
        }
    }

    void config(){
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.INTERNET}
                        ,10);
            }
            return;
        }
        // this code won't execute IF permissions are not allowed, because in the line above there is return statement.
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5, locationListener);
        MyPosition = mMap.addMarker(new MarkerOptions().position(new LatLng(0,0)).title("MyPosition"));
        /*b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //noinspection MissingPermission

            }
        });*/
    }


    private Runnable updateData = new Runnable(){
        public void run(){
            //call the service here
            updateCoord();
            ////// set the interval time here
            handler.postDelayed(updateData,1000);
        }
    };



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        config();
        //SharedPreferences sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        c = db.query("mytable", null, "id = (SELECT MAX(id) FROM mytable);", null, null, null, null);

        /*String coord = sPref.getString(DOWNLOADED_DATA, "");//"62.0276333, 129.7288333";
        String rssi  = sPref.getString(DOWNLOADED_DATA_RSSI, "");*/

        timeColIndex = c.getColumnIndex("time");
        gpsColIndex = c.getColumnIndex("GPSC");
        rssiColIndex = c.getColumnIndex("RSSI");


        if(c.moveToFirst()) {
            String time =  c.getString(timeColIndex);
            String[] pTime = time.split(" ");
            String coord = c.getString(gpsColIndex);//"62.0276333, 129.7288333";
            String rssi  = c.getString(rssiColIndex);
            if (coord.matches("(?i).*[A-zА-я].*") | coord.length() == 0)
                Toast.makeText(MapsActivity.this, "invalid data", Toast.LENGTH_LONG).show();
            else {
                String[] coordQuan = coord.split(", ");
                double lat = Double.parseDouble(coordQuan[0]);
                double lng = Double.parseDouble(coordQuan[1]);
                // Add a marker in Sydney and move the camera
                LatLng Lastloc = new LatLng(lat, lng);
                Point = mMap.addMarker(new MarkerOptions().position(Lastloc).title(pTime[1]+", rssi: "+rssi));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(Lastloc, 12));
            }
        }
        updateData.run();
    }

    public void animateMarker(final Marker marker, final LatLng toPosition,
                              final boolean hideMarker, String rssi) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        android.graphics.Point startPoint = proj.toScreenLocation(marker.getPosition());
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 500;

        final android.view.animation.Interpolator interpolator = new LinearInterpolator();
        marker.setTitle(rssi);
        marker.hideInfoWindow();
        marker.showInfoWindow();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed
                        / duration);
                double lng = t * toPosition.longitude + (1 - t)
                        * startLatLng.longitude;
                double lat = t * toPosition.latitude + (1 - t)
                        * startLatLng.latitude;
                marker.setPosition(new LatLng(lat, lng));

                if (t < 1.0) {
                    // Post again 16ms later.
                    handler.postDelayed(this, 16);
                } else {
                    if (hideMarker) {
                        marker.setVisible(false);
                    } else {
                        marker.setVisible(true);
                    }
                }
            }
        });
    }

    public void updateCoord(){
        SharedPreferences sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        //String coord = sPref.getString(DOWNLOADED_DATA, "");//"62.0276333, 129.7288333";
        c = db.query("mytable", null, "id = (SELECT MAX(id) FROM mytable);", null, null, null, null);

        /*String coord = sPref.getString(DOWNLOADED_DATA, "");//"62.0276333, 129.7288333";
        String rssi  = sPref.getString(DOWNLOADED_DATA_RSSI, "");*/
        timeColIndex = c.getColumnIndex("time");
        gpsColIndex = c.getColumnIndex("GPSC");
        rssiColIndex = c.getColumnIndex("RSSI");

        if(c.moveToFirst()) {

            String time = c.getString(timeColIndex);
            String[] pTime = time.split(" ");
            String coord = c.getString(gpsColIndex);//"62.0276333, 129.7288333";
            String rssi = c.getString(rssiColIndex);
            if (coord.matches("(?i).*[A-zА-я].*") || coord.length() == 0)
                ; //Toast.makeText(MapsActivity.this, "invalid data", Toast.LENGTH_LONG).show();
            else {
                String[] coordQuan = coord.split(", ");
                double lat = Double.parseDouble(coordQuan[0]);
                double lng = Double.parseDouble(coordQuan[1]);
                // Add a marker in Sydney and move the camera
                LatLng lastlock = new LatLng(lat, lng);
                animateMarker(Point, lastlock, false, pTime[1]+", rssi: "+rssi);
                //Point.setPosition(sydney);
            }
        }
    }

    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        c.close();
        dbHelper.close();
        handler.removeCallbacks(updateData);
        finish();
    }

}
