package com.sibclan.fluxpoint.dreamteamapp;

import java.text.DecimalFormat;

import static android.R.attr.angle;

/**
 * Created by FluxPoint on 5/3/2017.
 */

public class ActivityItem {

    public double duration=0; //in terms of hours
    public double watts=0; // kkk
    public int time_hour=0;
    public int time_minutes=0;

    @Override
    public String toString() {
        DecimalFormat df = new DecimalFormat("#.00");
        String durationFormatted = df.format(duration);
        if(time_minutes >= 10) {
            return durationFormatted + "hrs @ " + watts + " W\n" +
                    "  Started @ " + time_hour + ":" + time_minutes;
        }
        else {
            return durationFormatted + "hrs @ " + watts + " W\n" +
                    "  Started @ " + time_hour + ":0" + time_minutes;

        }
    }
}
