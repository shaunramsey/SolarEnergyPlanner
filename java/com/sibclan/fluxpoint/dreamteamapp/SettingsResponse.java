package com.sibclan.fluxpoint.dreamteamapp;

/**
 * Created by FluxPoint on 5/2/2017.
 */

public class SettingsResponse {
    int submit = 0;
    int number_of_panels = 30;
    double size_of_panels = 1.635481;
    double panel_efficiency = 0.08;
    public double longitude = -155.487192; //hi-seas getLongitude
    public double latitude = 19.602378; //hi-seas getLatitude;


    private static final SettingsResponse ourInstance = new SettingsResponse();

    public static SettingsResponse getInstance() {
        return ourInstance;
    }

    private SettingsResponse() {
    }
}
