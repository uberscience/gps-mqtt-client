package ru.exp_net.gps_mqtt_client;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.internal.MessageCatalog;
import org.w3c.dom.Text;

import java.io.UnsupportedEncodingException;
import java.sql.RowId;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    MqttAndroidClient client;
    MqttConnectOptions options;
    IMqttToken subGPS, subRSSI;
    EditText editTextPublishData;
    Button buttonPublish;
    String clientId;
    TextView SubscribeData, textViewRSSI, LastSubDataTime;
    String subdata;
    ContentValues cv;
    SQLiteDatabase db;
    DBHelper dbHelper;
    Cursor c;
    long rowID;
    int  updCount;
    //String subdatarssi;
    final String SAVED_SERVER    = "SAVED_SERVER";
    final String SAVED_USER_NAME = "SAVED_USER_NAME";
    final String SAVED_PASSWORD  = "SAVED_PASSWORD";
    final String SAVED_SUBSCRIBE_TOPIC  = "SAVED_SUBSCRIBE_TOPIC";
    final String SAVED_PUBLISH_TOPIC  = "SAVED_PUBLISH_TOPIC";
    final String SAVED_RSSI_TOPIC  = "SAVED_RSSI_TOPIC";
    final String DOWNLOADED_DATA = "DOWNLOADED_DATA";
    final String DOWNLOADED_DATA_RSSI = "DOWNLOADED_DATA_RSSI";

    SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = new DBHelper(this);
        SubscribeData = (TextView) findViewById(R.id.textViewSubscribeData);
        textViewRSSI = (TextView) findViewById(R.id.textViewRssi);
        LastSubDataTime = (TextView) findViewById(R.id.textLSDtime);
        sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        String savedTextServer = sPref.getString(SAVED_SERVER, "");
        //String savedDowloadedData = sPref.getString(DOWNLOADED_DATA, "");
        //SubscribeData.setText(savedDowloadedData);
        //textViewRSSI.setText(sPref.getString(DOWNLOADED_DATA_RSSI, ""));
        //**************MQTT****************
        clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+savedTextServer, clientId);
        //**************MQTT****************

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        buttonPublish = (Button) findViewById(R.id.buttonPublish);
        buttonPublish.setOnClickListener(this);

        // создаем объект для создания и управления версиями БД

        cv = new ContentValues();
        db = dbHelper.getWritableDatabase();
        c = db.query("mytable", null, "id = (SELECT MAX(id) FROM mytable);", null, null, null, null);

        if(c.moveToFirst()) {
            LastSubDataTime.setText(": "+c.getString(c.getColumnIndex("time")));
            textViewRSSI.setText(c.getString(c.getColumnIndex("RSSI")));
            SubscribeData.setText(c.getString(c.getColumnIndex("GPSC")));
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonPublish:

                sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
                String topic = sPref.getString(SAVED_PUBLISH_TOPIC, "");
                editTextPublishData = (EditText) findViewById(R.id.editTextPublishData);
                String payload = editTextPublishData.getText().toString();
                byte[] encodedPayload = new byte[0];
                if(client.isConnected()) {
                    try {
                        encodedPayload = payload.getBytes("UTF-8");
                        MqttMessage message = new MqttMessage(encodedPayload);
                        client.publish(topic, message);
                    } catch (UnsupportedEncodingException | MqttException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(MainActivity.this, "Published", Toast.LENGTH_LONG).show();
                }
                else Toast.makeText(MainActivity.this, "not Connected", Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
            dbHelper.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        final Toolbar TLB_color = (Toolbar) findViewById(R.id.toolbar);

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if(client.isConnected()){
                disconnect();
            }
            TLB_color.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        final Toolbar TLB_color = (Toolbar) findViewById(R.id.toolbar);
        IMqttToken token;
        if (id == R.id.btn_connect) {
            if(client.isConnected()){
                disconnect();
                //client.close();
            }
            sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
            String savedTextServer = sPref.getString(SAVED_SERVER, "");
            String savedTextUserName = sPref.getString(SAVED_USER_NAME, "");
            String savedTextPassword = sPref.getString(SAVED_PASSWORD, "");
            clientId = MqttClient.generateClientId();
            client = new MqttAndroidClient(this.getApplicationContext(), "tcp://"+savedTextServer, clientId);
            try {
                if((savedTextUserName.length())>0){
                    options = new MqttConnectOptions();
                    options.setUserName(savedTextUserName);
                    options.setPassword(savedTextPassword.toCharArray());
                    token = client.connect(options);
                }
                else {
                    token = client.connect();
                }
                token.setActionCallback(new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // We are connected
                        //Log.d(TAG, "onSuccess");
                        subscribe(subGPS, SAVED_SUBSCRIBE_TOPIC);
                        subscribe(subRSSI,SAVED_RSSI_TOPIC);
                        TLB_color.setBackgroundColor(getResources().getColor(R.color.green));
                        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_LONG).show();
                        client.setCallback(new MqttCallback() {
                            @Override
                            public void connectionLost(Throwable cause) {

                            }

                            @Override
                            public void messageArrived(String topic, MqttMessage message) throws Exception {



                                subdata = new String(message.getPayload());
                                SharedPreferences.Editor ed = sPref.edit();
                                     if (topic.equals(sPref.getString(SAVED_SUBSCRIBE_TOPIC,""))){
                                         //ed.putString(DOWNLOADED_DATA,  subdata);
                                         //ed.commit();
                                         //SubscribeData.setText(subdata);

                                         cv.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                                         cv.put("GPSC", subdata);
                                         //rowID = db.insert("mytable", null, cv);
                                         rowID = db.insert("mytable", null, cv);
                                         cv.clear();
                                         //rowID = db.insert("mytable", null, cv);
                                         //updCount = db.update("mytable", cv, "id = ?", new String[] { Long.toString(rowID) });
                                         //currentDateandTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                                         //Toast.makeText(MainActivity.this, currentDateandTime,Toast.LENGTH_LONG).show();
                                         c = db.query("mytable", null, "id = (SELECT MAX(id) FROM mytable);", null, null, null, null);
                                         c.moveToLast();
                                         LastSubDataTime.setText(": "+c.getString(c.getColumnIndex("time")));
                                         textViewRSSI.setText(c.getString(c.getColumnIndex("RSSI")));
                                         SubscribeData.setText(c.getString(c.getColumnIndex("GPSC")));

                                         //updCount = db.update("mytable", cv, "id = ?", new String[] { Long.toString(rowID) });
                                         //Toast.makeText(MainActivity.this, "rowID = "+rowID ,Toast.LENGTH_LONG).show();
                                     }
                                else if (topic.equals(sPref.getString(SAVED_RSSI_TOPIC,""))){
                                             ed.putString(DOWNLOADED_DATA_RSSI,  subdata);
                                         ed.commit();
                                         textViewRSSI.setText(subdata);
                                         cv.put("RSSI", subdata);
                                         //updCount = db.update("mytable", cv, "id = ?", new String[] { Long.toString(rowID) });
                                         //rowID = 0;

                                         //Toast.makeText(MainActivity.this, c.getInt(c.getColumnIndex("id"))+", "+c.getString(c.getColumnIndex("time"))+", "+c.getString(c.getColumnIndex("GPSC"))+", "+c.getString(c.getColumnIndex("RSSI")),Toast.LENGTH_LONG).show();
                                     }

                                //Toast.makeText(MainActivity.this, sPref.getString(DOWNLOADED_DATA, ""),Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void deliveryComplete(IMqttDeliveryToken token) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        // Something went wrong e.g. connection timeout or firewall problems
                        //Log.d(TAG, "onFailure");
                        TLB_color.setBackgroundColor(getResources().getColor(R.color.red));
                        Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
            // Handle the camera action
        } else if (id == R.id.btn_disconnect) {
            if(client.isConnected()){
                disconnect();
                TLB_color.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDark));
            }
        } else if (id == R.id.btn_map) {
            Intent intent = new Intent(this, MapsActivity.class);
            startActivity(intent);

        } else if (id == R.id.btn_history) {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        }



        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void disconnect (){
        try {
            IMqttToken disconToken = client.disconnect();
            disconToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    // we are now successfully disconnected
                    Toast.makeText(MainActivity.this, "disconnected", Toast.LENGTH_LONG).show();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // something went wrong, but probably we are disconnected anyway
                    Toast.makeText(MainActivity.this, "could not disconnect", Toast.LENGTH_LONG).show();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void subscribe(IMqttToken token, String saved_topic){

        sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        String topic = sPref.getString(saved_topic, "");
        int qos = 1;

        try {
            token = client.subscribe(topic, qos);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Toast.makeText(MainActivity.this, "Subscribed", Toast.LENGTH_LONG).show();

                    // The message was published
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Toast.makeText(MainActivity.this, "could not subscribe", Toast.LENGTH_LONG).show();
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    static class DBHelper extends SQLiteOpenHelper {

        public DBHelper(Context context) {
            // конструктор суперкласса
            super(context, "myDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            //Log.d(LOG_TAG, "--- onCreate database ---");
            // создаем таблицу с полями
            db.execSQL("create table mytable ("
                    + "id integer primary key autoincrement,"
                    + "time text,"
                    + "RSSI text,"
                    + "GPSC text" + ");");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
}



}
