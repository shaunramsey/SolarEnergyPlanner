package com.sibclan.fluxpoint.dreamteamapp;

import android.media.audiofx.BassBoost;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import java.util.Random;


public class PanelSettings extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel_settings);
        SettingsResponse.getInstance().submit = 0;
    }

    public void submit(View v) {
        SettingsResponse.getInstance().submit = 1;
        try {
            SettingsResponse.getInstance().number_of_panels = Integer.valueOf(((EditText) findViewById(R.id.settings_number)).getText().toString());
        } catch (Exception e) {
        }
        try {
            SettingsResponse.getInstance().panel_efficiency = Double.valueOf(((EditText) findViewById(R.id.settings_efficiency)).getText().toString());
            SettingsResponse.getInstance().panel_efficiency /= 100.0; //convert to the decimal it needs to be
        } catch (Exception e) {
        }
        try {
            SettingsResponse.getInstance().size_of_panels = Double.valueOf(((EditText) findViewById(R.id.settings_size)).getText().toString());
        } catch (Exception e) {
        }
        finish();
    }


    public void cancel(View v) {
        SettingsResponse.getInstance().submit = 0;
        finish();
    }
}
