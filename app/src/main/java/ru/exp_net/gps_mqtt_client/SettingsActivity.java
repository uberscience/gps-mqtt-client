package ru.exp_net.gps_mqtt_client;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SettingsActivity extends AppCompatActivity implements OnClickListener  {

    EditText editTextServer, editTextUsername, editTextPassword, editTextSubTopic, editTextPubTopic, editTextRssiTopic;
    Button btnSave;

    SharedPreferences sPref;

    final String SAVED_SERVER    = "SAVED_SERVER";
    final String SAVED_USER_NAME = "SAVED_USER_NAME";
    final String SAVED_PASSWORD  = "SAVED_PASSWORD";
    final String SAVED_SUBSCRIBE_TOPIC  = "SAVED_SUBSCRIBE_TOPIC";
    final String SAVED_PUBLISH_TOPIC  = "SAVED_PUBLISH_TOPIC";
    final String SAVED_RSSI_TOPIC  = "SAVED_RSSI_TOPIC";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);

        editTextServer =   (EditText) findViewById(R.id.editTextServer);
        editTextUsername = (EditText) findViewById(R.id.editTextUsername);
        editTextPassword = (EditText) findViewById(R.id.editTextPassword);
        editTextSubTopic = (EditText) findViewById(R.id.editTextSubTopic);
        editTextPubTopic = (EditText) findViewById(R.id.editTextPubTopic);
        editTextRssiTopic = (EditText) findViewById(R.id.editTextRssi);
        btnSave = (Button) findViewById(R.id.buttonSaveCfg);
        btnSave.setOnClickListener(this);
        loadCFG();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSaveCfg:
                saveCFG();
                break;
            default:
                break;
        }
    }

    void saveCFG() {
        sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        Editor ed = sPref.edit();
        ed.putString(SAVED_SERVER,          editTextServer.getText().toString());
        ed.putString(SAVED_USER_NAME,       editTextUsername.getText().toString());
        ed.putString(SAVED_PASSWORD,        editTextPassword.getText().toString());
        ed.putString(SAVED_SUBSCRIBE_TOPIC, editTextSubTopic.getText().toString());
        ed.putString(SAVED_PUBLISH_TOPIC,   editTextPubTopic.getText().toString());
        ed.putString(SAVED_RSSI_TOPIC,      editTextRssiTopic.getText().toString());
        ed.commit();
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    void loadCFG() {
        sPref = getSharedPreferences("MqConnCfg",MODE_PRIVATE);
        String savedTextServer = sPref.getString(SAVED_SERVER, "");
        String savedTextUsername = sPref.getString(SAVED_USER_NAME, "");
        String savedTextPassword = sPref.getString(SAVED_PASSWORD, "");
        String savedTextSubTopic = sPref.getString(SAVED_SUBSCRIBE_TOPIC, "");
        String savedTextPubTopic = sPref.getString(SAVED_PUBLISH_TOPIC, "");
        String savedTextRssiTopic = sPref.getString(SAVED_RSSI_TOPIC, "");
        if((sPref.getString(SAVED_SERVER, "").length())>0)         editTextServer.setText(savedTextServer);
        if((sPref.getString(SAVED_USER_NAME, "").length())>0)      editTextUsername.setText(savedTextUsername);
        if((sPref.getString(SAVED_PASSWORD, "").length())>0)       editTextPassword.setText(savedTextPassword);
        if((sPref.getString(SAVED_SUBSCRIBE_TOPIC, "").length())>0)editTextSubTopic.setText(savedTextSubTopic);
        if((sPref.getString(SAVED_PUBLISH_TOPIC, "").length())>0)  editTextPubTopic.setText(savedTextPubTopic);
        if((sPref.getString(SAVED_RSSI_TOPIC, "").length())>0)  editTextRssiTopic.setText(savedTextRssiTopic);
        //Toast.makeText(this, "Text loaded", Toast.LENGTH_SHORT).show();
    }
}
