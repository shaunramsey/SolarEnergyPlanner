package com.sibclan.fluxpoint.dreamteamapp;

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
        if(time_minutes >= 10) {
            return duration + "h @ " + watts + " W\n" +
                    " Start Time: " + time_hour + ":" + time_minutes;
        }
        else {
            return duration + "h @ " + watts + " W\n" +
                    " Started @ " + time_hour + ":0" + time_minutes;

        }
    }
}
