package com.sibclan.fluxpoint.dreamteamapp;

/**
 * Created by FluxPoint on 5/2/2017.
 */

public class SettingsResponse {
    Boolean submit = false;
    int number_of_panels = 30;
    double size_of_panels = 1.635481;
    double panel_efficiency = 0.08;


    private static final SettingsResponse ourInstance = new SettingsResponse();

    public static SettingsResponse getInstance() {
        return ourInstance;
    }

    private SettingsResponse() {
    }
}
