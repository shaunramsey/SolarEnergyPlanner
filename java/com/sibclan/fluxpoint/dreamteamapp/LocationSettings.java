package com.sibclan.fluxpoint.dreamteamapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.Random;


//TODO: perhaps add an enum for all these submit returns
public class LocationSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_settings);
        SettingsResponse.getInstance().submit = 0;
    }

    public void cancel(View v) {
        SettingsResponse.getInstance().submit = 0;
        finish();
    }

    public void random(View v) {
        Random r = new Random();
        SettingsResponse.getInstance().submit = 2;
        SettingsResponse.getInstance().latitude = r.nextInt(180)-90; //random lat
        SettingsResponse.getInstance().longitude = r.nextInt(360)-180; //random lon
        finish();
    }
    public void specific(View v) {
        SettingsResponse.getInstance().submit = 2;
        SettingsResponse.getInstance().latitude = Double.valueOf(((EditText) findViewById(R.id.settings_latitude)).getText().toString());
        SettingsResponse.getInstance().longitude = Double.valueOf(((EditText) findViewById(R.id.settings_longitude)).getText().toString());
        finish();
    }

    public void hiseas(View v) {
        SettingsResponse.getInstance().submit = 2;
        SettingsResponse.getInstance().latitude = 19.602378; //hi-seas getLongitude
        SettingsResponse.getInstance().longitude = -155.487192; //hi-seas lat
        finish();
    }

    public void update(View v) {
        SettingsResponse.getInstance().submit = 3;
        finish();
    }


}
