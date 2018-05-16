package ru.exp_net.gps_mqtt_client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;


public class HistoryActivity extends AppCompatActivity implements View.OnClickListener {

    ListView ListViewHistory;
    Button btnClearHistory, btnOnMap;
    Handler handler;

    long LastRowID;
    ContentValues cv;
    SQLiteDatabase db;
    MainActivity.DBHelper dbHelper;
    Cursor c;

    ArrayList<String> data = new ArrayList<>();
    ArrayAdapter<String> adapter;

    int idColIndex;
    int timeColIndex;
    int gpsColIndex;
    int rssiColIndex;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        handler = new Handler();

        ListViewHistory = (ListView) findViewById(R.id.ListViewHistory);
        btnClearHistory = (Button)   findViewById(R.id.buttonClearHistory);
        btnOnMap        = (Button)   findViewById(R.id.buttonOnmap);
        btnClearHistory.setOnClickListener(this);
        btnOnMap.setOnClickListener(this);


        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
        ListViewHistory.setAdapter(adapter);

        dbHelper = new MainActivity.DBHelper(this);
        cv = new ContentValues();
        db = dbHelper.getWritableDatabase();
        c = db.query("mytable", null, null, null, null, null, null);

        if (c.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            idColIndex = c.getColumnIndex("id");
            timeColIndex = c.getColumnIndex("time");
            gpsColIndex = c.getColumnIndex("GPSC");
            rssiColIndex = c.getColumnIndex("RSSI");

            do {
                data.add(0,c.getInt(idColIndex)+" "+c.getString(timeColIndex)+" "+c.getString(gpsColIndex)+" "+c.getString(rssiColIndex));
                adapter.notifyDataSetChanged();
                LastRowID = c.getInt(idColIndex);
                // получаем значения по номерам столбцов и пишем все в лог
                // переход на следующую строку
                // а если следующей нет (текущая - последняя), то false - выходим из цикла
            } while (c.moveToNext());
            addDataTask.run();
        } else
        c.close();
    }

    private Runnable addDataTask = new Runnable(){
        public void run(){
            //call the service here
            addData();
            ////// set the interval time here
            handler.postDelayed(addDataTask,1000);
        }
    };

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOnmap:

                break;
            case R.id.buttonClearHistory:
                int clearCount = db.delete("mytable", null, null);
                db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = 'mytable'");
                adapter.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(HistoryActivity.this,"Удалено "+clearCount+" записей",Toast.LENGTH_LONG).show();
                break;
            default:
                break;
        }
    }

    public void addData(){
        c = db.query("mytable", null, "id = (SELECT MAX(id) FROM mytable);", null, null, null, null);

        if (c.moveToFirst()) {
            // определяем номера столбцов по имени в выборке
            idColIndex = c.getColumnIndex("id");
            timeColIndex = c.getColumnIndex("time");
            gpsColIndex = c.getColumnIndex("GPSC");
            rssiColIndex = c.getColumnIndex("RSSI");

            if(c.getInt(idColIndex)>LastRowID){
                data.add(0,c.getInt(idColIndex)+" "+c.getString(timeColIndex)+" "+c.getString(gpsColIndex)+" "+c.getString(rssiColIndex));
                adapter.notifyDataSetChanged();
                LastRowID = c.getInt(idColIndex);
            }

        } else
            c.close();
    }


    @Override
    public void onBackPressed() {
        // super.onBackPressed();
        dbHelper.close();
        handler.removeCallbacks(addDataTask);
        finish();
    }
}
