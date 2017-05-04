package com.sibclan.fluxpoint.dreamteamapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class AddActivity extends AppCompatActivity {
    ArrayList<Double> times_in_hours = new ArrayList<>();
    ArrayList<Integer> watts = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResponse.getInstance().submit = false;
        setContentView(R.layout.activity_add);
        TimePicker tp= (TimePicker) findViewById(R.id.activity_time);
        tp.setIs24HourView(true);

        Spinner time = (Spinner) findViewById(R.id.activity_duration);

        ArrayAdapter<String> sa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 1; i <= 30; ++i) { //add minutes items
            sa.add(i + " minutes");
            times_in_hours.add(i / 60.0);
        }
        for (int i = 35; i < 60; ++i) {
            sa.add(i + " minutes");
            times_in_hours.add(i / 60.0);
        }
        for (int i = 60; i <= 720; i += 15) {
            int hrs = i / 60;
            int minutes = i - hrs * 60;
            if (hrs == 1) {
                sa.add("1 hr " + minutes + " minutes");
            } else {
                sa.add(hrs + " hours " + minutes + " minutes");
            }
            times_in_hours.add(i / 60.0);
        }
        sa.notifyDataSetChanged();
        time.setAdapter(sa);
        Spinner ws = (Spinner) findViewById(R.id.activity_watts);
        ArrayAdapter<String> wa = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        for (int i = 1; i < 200; ++i) {
            String message = i + " Watts ";
            if(i == 16) message += " -- (LED (100W) Light)";
            if(i == 24) message += " -- (CFL (100W) Light)";
            if(i == 100) message += " -- (100W Incandescent)";
            wa.add(message);
            watts.add(i);
        }
        for (int i = 200; i < 1000; i += 10) {
            String message = i + " Watts ";
            if(i == 250) message += " -- (Rice Cooker)";
            if(i == 400) message += " -- (Fridge)";
            if(i == 450) message += " -- (Desktop Computer)";
            if(i == 500) message += " -- (Washer)";
            wa.add(message);
            watts.add(i);
        }
        for (int i = 1000; i < 2000; i += 100) {
            String message = i + " Watts ";
            if(i == 1400) message += " -- (Coffee Maker)";
            if(i == 1500) message += " -- (Dish Washer)";
            if(i == 1800) message += " -- (Toaster)";
            wa.add(message);
            watts.add(i);
        }
        for (int i = 2000; i < 10001; i += 250) {
            String message = i + " Watts ";
            if(i == 2250) message += " -- (Oven)";
            if(i == 3000) message += " -- (Electric Kettle)";
            if(i == 4500) message += " -- (Central A/C)";
            if(i == 4000) message += " -- (Clothes Dryer)";
            wa.add(message);
            watts.add(i);
        }

        wa.notifyDataSetChanged();
        ws.setAdapter(wa);

    }


    public void submit(View v) {
//set all the simpleton variables to their appropriate values...
        ActivityResponse.getInstance().submit = true;
        TextView tv = (TextView) findViewById(R.id.activity_name);

        TimePicker picker = (TimePicker) findViewById(R.id.activity_time);

        ActivityItem ai = new ActivityItem();

        ai.time_hour = picker.getCurrentHour();
        ai.time_minutes = picker.getCurrentMinute();


        Spinner duration = (Spinner) findViewById(R.id.activity_duration);
        Spinner watt_spinner = (Spinner) findViewById(R.id.activity_watts);
        Double hours = times_in_hours.get(duration.getSelectedItemPosition());
        ai.duration = hours;

        Integer w = watts.get(watt_spinner.getSelectedItemPosition());
        ai.watts = w * 1.0;

        Integer kW = (int)( w.doubleValue() * hours / 1000.0);
        Integer frac = (int)(( w.doubleValue() * hours / 1000.0  - kW.doubleValue()) *1000.0);
        ActivityResponse.getInstance().activity_item = ai;
        ActivityResponse.getInstance().activity_name  = tv.getText().toString();
        ActivityResponse.getInstance().power_string = kW + "." + frac + " kWh";
        finish(); //now we're done with this particular activity
    }

    public void cancel(View v) {
        ActivityResponse.getInstance().submit = false;
        finish();
    }
}
