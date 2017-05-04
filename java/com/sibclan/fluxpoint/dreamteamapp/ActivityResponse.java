package com.sibclan.fluxpoint.dreamteamapp;

/**
 * Created by Ramsey on 4/30/2017.
 */
public class ActivityResponse {
    public String activity_name;
    public String power_string;
    public ActivityItem activity_item;


    public Boolean submit = false;

    private static ActivityResponse ourInstance = new ActivityResponse();

    public static ActivityResponse getInstance() {
        return ourInstance;
    }

    private ActivityResponse() {
    }
}
