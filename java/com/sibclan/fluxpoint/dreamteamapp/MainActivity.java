package com.sibclan.fluxpoint.dreamteamapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;



import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.pm.PackageManager.PERMISSION_DENIED;


//TODO: refactor this thing so it has some logical sense and is so much cleaner
//the 48 hour hack fest did no wonders for organization in this file
//TODO: see if we can bring the API required level down to 15 to hit more devices
//there are only a couple spots where we hit higher than 15
//TODO: is there a bug with POWERDEATH function? get some bug reports and see where this leads
public class MainActivity extends AppCompatActivity {

    public ArrayList<ProgressBar> pbs = new ArrayList<>();
    public ArrayList<Double> half_hour_energies = new ArrayList<>();
    public ArrayList<Double> remaining_half_hour_energies = new ArrayList<>();
    public ArrayList<ActivityItem> activity_list = new ArrayList<>();
    public ArrayList<TextView> activity_textviews = new ArrayList<>();

    String fetchedTime;
    int fetched_start_hour = 0;
    int fetched_start_half = 0;

    static final double  hi_seas_longitude = -155.487192; //hi-seas getLongitude
    static final double hi_seas_latitude = 19.602378; //hi-seas getLatitude;

    public double longitude = -155.487192; //hi-seas getLongitude
    public double latitude = 19.602378; //hi-seas getLatitude;
    //    let url = "http://api.solcast.com.au/radiation/forecasts?longitude=-76.07&latitude=32.229&capacity=10&api_key=uos_eam6ozSnecTk5pTerz5ow-r916Uc&format=csv"
    String url = "https://api.solcast.com.au/radiation/forecasts?longitude=-155.487192&latitude=19.602378&capacity=10&api_key=uos_eam6ozSnecTk5pTerz5ow-r916Uc&format=csv";
    // double energy = v*1.0 * efficiency * number_of_panels * panel_size;

    double efficiency = 0.08; //8% efficiency
    double number_of_panels = 30; //30 solar panels
    double panel_size = 1.635481; //each panel is 1.64 m^2
    static final double MAX_ENERGIES = 2000.0;
    static final int reserve_max = 20000; //50kwh
    static final int progress_max= 50000;

    LocationManager lm;



    //given an incoming irradiance, how much of that energy is actually converted into wattage. This final result is just Watts
    //it will need to be multiplied by the number of hours to get Watt hours
    //incoming values are irradiance in W / m^2. By multiplying by the "size" in m^2, and the efficiency (percent converted into
    //usable energy, and number of panels (which gives us the TOTAL size of our solar array), we get the amount of usable
    //energy in terms of W.
    double getEnergy(double irradiance) {
        return irradiance * efficiency * number_of_panels * panel_size;
    }




    //this is related to the back button on most android phones.
    //when we press back we want to remove the last activity added
    //this also means removing a textview from the linear layout where they were placed
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(activity_list.size() > 0) {
                ProgressBar res = (ProgressBar) findViewById(R.id.reserve_power_progressbar);
                res.setSecondaryProgress(100);
                res.setProgressDrawable(getDrawable(R.drawable.progressbars));
                activity_list.remove (activity_list.size() -1 );//nuke the last one
                updateEnergies();
                //todo - fix the list of activities - will show even tho it is popped
            }
            if(activity_textviews.size() > 0){
                LinearLayout ll = (LinearLayout) findViewById(R.id.activities);
                ll.removeView(activity_textviews.get(activity_textviews.size() -1 )); //remove the last one that was added
                activity_textviews.remove( activity_textviews.size() -1 );
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }


    //when the location actually changes based on GPS, then we'll update the variables for lat/long
    LocationListener onLocationChange = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            UpdateLocationVariables(loc);

            lm.removeUpdates(this); //got what we came for
            /* //used to have a button that you could push to frce the lat/long update, I remove it when we get the lat long
            //I didn't want to remove this code in case this is an idea I wanted to come back to here
            Button b = (Button) findViewById(R.id.location_button);
            b.setVisibility(View.GONE);
            b.setEnabled(false);
            */
            Log.v("DEBUG", "New lat,long is " + longitude + ", " + latitude);
        }

        @Override
        public void onProviderDisabled(String provider) {
            // Toast.makeText(getApplicationContext(), "pdlong" + longitude + " lat" + latitude,                     Toast.LENGTH_LONG).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
//            Toast.makeText(getApplicationContext(), "pelong" + longitude + " lat" + latitude,                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onStatusChanged(String provider, int status,
                                    Bundle extras) {
        //    Toast.makeText(getApplicationContext(), "osclong" + longitude + " lat" + latitude,                     Toast.LENGTH_LONG).show();
        }
    };


    //we have to ask for permission to use the GPS and the Internet
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == 999) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                UpdateLocation(findViewById(R.id.location_button));
            } else { //didn't get perms ask again in 5 mins use
                TimerTask tt = new TimerTask() {
                    @Override
                    public void run() {
                        UpdateLocation(findViewById(R.id.location_button));
                    }
                };
                Timer t = new Timer();
                t.schedule(tt, 3000000);
            }
        } else if (requestCode == 998) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchURL();
            }
        }
    }



    //we got the long and lat, let's find the proper url and fetch in all the data
    public void UpdateLocationVariables(Location loc) {
        if (loc == null) {
            longitude = hi_seas_longitude;
            latitude = hi_seas_latitude;
        }
        else {
            longitude = loc.getLongitude();
            latitude = loc.getLatitude();
        }
        Toast.makeText(getApplicationContext(), " Updated for location: Long:" + longitude + " Lat:" + latitude, Toast.LENGTH_LONG).show();
        url = "https://api.solcast.com.au/radiation/forecasts?longitude=" + longitude +
                "&latitude=" + latitude + "&capacity=10&api_key=uos_eam6ozSnecTk5pTerz5ow-r916Uc&format=csv";
        fetchURL();

       // Toast.makeText(getApplicationContext(), "--long" + longitude + " lat" + latitude, Toast.LENGTH_LONG).show();

    }


    //get the URL with the predictions for the following day, set some internal variables for the calculations in the futre
    public void fetchURL() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permissionCheck != PERMISSION_DENIED) {
            //Toast.makeText(getApplicationContext(), "Trying to get predictions and GPS", Toast.LENGTH_LONG).show();

            //then we can do this stuff!
            try {
                Log.v("DEBUG", "myurl=" + url);
                SimpleDateFormat format_to = new SimpleDateFormat("HH:mm"); //intentionally don't want local format here, didn't surpress
                fetchedTime = format_to.format(new Date());
                fetched_start_hour = Integer.valueOf(fetchedTime.substring(0, 2));
                fetched_start_half = Integer.valueOf(fetchedTime.substring(3, 5));
                if (fetched_start_half < 30) {
                    fetched_start_half = 30; //in this way 12:15 becomes 12:30
                } else {
                    fetched_start_hour++;  //increment theh our we're at, like :11:45 becomes 12:
                    fetched_start_half = 0; //starts on the next hour -- 45 becomes 00
                }
                Log.v("DEBUG", "times=" + fetched_start_hour + " : " + fetched_start_half);

                Log.v("DEBUG", "fetched time = " + fetchedTime);
                new HttpTask().execute(url);
            } catch (Exception e) {
                Log.v("DEBUG", e.getMessage() + " ERROR in fetch ");
            }
        } else { //see if they'll give it to us
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 998);
            Toast.makeText(getApplicationContext(), "Unable to get live data from the web", Toast.LENGTH_LONG).show();
        }
    }

   //occasionally update the GPS when the conditions say that we should do so (moving 10m or waiting a long long time)
    public void UpdateLocation(View v) {
        Log.v("DEBUG", "Entered UpdateLocation");
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        Log.v("DEBUG", permissionCheck + "  denied is: " + PERMISSION_DENIED);
        if (permissionCheck != PERMISSION_DENIED) {
            //    LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100000, 10, onLocationChange);
            UpdateLocationVariables(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER));
          //  Toast.makeText(getApplicationContext(), "Fetching old GPS = long" + longitude + " lat" + latitude,   Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 999);
            Toast.makeText(getApplicationContext(), "Unable to get current location",  Toast.LENGTH_LONG).show();
        }
    }


    //this is the menu/settings icon in the title bar
    //we can have multiple menu items in there if we want - perhaps adding the location as a second button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    //when something in the settings/title bar is selected
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_panel:
                Intent i = new Intent(this, PanelSettings.class);
                startActivity(i);
                return true;
            case R.id.action_location:
                Intent i2 = new Intent(this, LocationSettings.class);
                startActivity(i2);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout ll = (LinearLayout) findViewById(R.id.energy);
        TextView rtv = (TextView) findViewById(R.id.reserve_textview);
        rtv.setText("Predicted Reserve remaining. Began w/ " + reserve_max/1000 + " kWh");

        for (int i = 0; i < 48; ++i) {
            ProgressBar p = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            if (i < 16 || i > 32)
                p.setProgress(0);
            else {
                p.setProgress((int) (50 * Math.sin((i - 16) / 16.0 * Math.PI)));
            }
            p.setIndeterminate(false);
            p.setProgressDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.progressbars, null));
            pbs.add(p);
            half_hour_energies.add(0.0);
            remaining_half_hour_energies.add(0.0);
            ll.addView(pbs.get(i));
        }
        ll.addView(new TextView(this));
        Log.v("DEBUG", "Created Main Activity");
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        UpdateLocation(findViewById(R.id.location_button));
        Log.v("DEBUG", "Called Update Location and completed");
    }

    public void POWERDEATH() {
        ProgressBar res = (ProgressBar) findViewById(R.id.reserve_power_progressbar);
        res.setSecondaryProgress(0);
        res.setProgress(0);
        Toast.makeText(getApplicationContext(), "WE WILL USE ALL RESERVES!!!", Toast.LENGTH_SHORT).show();

        //TODO: maybe? update reserves to darkness.xml and allow the negatives to show how in debt reserves are
    }

    public void updateEnergies() {
        double maxE = 2000;
        double totalkWh = 0;
        int reserve_remaining = reserve_max; //wait till we nuke some of it
        for (int i = 0; i < pbs.size(); ++i) {
            double this_half_hour = getEnergy(half_hour_energies.get(i)) * 0.5; //half hour energies stays in irradiance
            totalkWh += this_half_hour;
            pbs.get(i).setProgress((int) (this_half_hour / maxE));
            remaining_half_hour_energies.set(i, getEnergy(half_hour_energies.get(i))*0.5); //set them to max for now - in Wh
        }
        ProgressBar daily = (ProgressBar) findViewById(R.id.total_daily_energy_progressbar);
        ProgressBar reserve = (ProgressBar) findViewById(R.id.reserve_power_progressbar);
        //at this point, all progress bars are full ---but we must modify based on activities
        double usedTotalkWh = 0;
        for (int i = 0; i < activity_list.size(); ++i) {
            ActivityItem ai = activity_list.get(i);
            Log.v("INDEX", "Activity Item is index " + i);
            usedTotalkWh += ai.watts*ai.duration;
            //go through each activity
            //find the bar that correlates to its start time.
            // progress bar 0 pbs(0) corresponds to fetched_start_hour and fetched_start_half
            int hr = (24 + ai.time_hour - fetched_start_hour) % 24;
            int index = 2 * hr;
            if (fetched_start_half == 0 && ai.time_minutes > 30) {
                index++;
            }
            if (fetched_start_half == 30 && ai.time_minutes < 30) {
                index--;
            }
            index = (index + 48) % 48;
            Log.v("INDEX", "index is " + index);
            Log.v("DEBUG", "Fetch: " + fetched_start_hour + ":" + fetched_start_half + " [AI:" + i + "]"
                    + ai.time_hour + ":" + ai.time_minutes + "  [index=" + index + "]");
            //for the first and last, we might use up parts of a half hour
            int amt = 30 - ai.time_minutes % 30; //time it will use of this half hour
            int minutes_left = (int) (ai.duration * 60);
            if (amt > minutes_left)
                amt = minutes_left; //but don't use more time than we've got
            minutes_left -= amt;
            //okay finally, we know how much time is going to be used in this halfhour index block
            //so how much power is that:
            double power_used = amt /60.0 * ai.watts;
            if (power_used < remaining_half_hour_energies.get(index)) { //great we have this power generating
                remaining_half_hour_energies.set(index, remaining_half_hour_energies.get(index) - power_used);
            } else { //otherwise we use it all up and some reserve too
                power_used -= remaining_half_hour_energies.get(index); //use it all up
                remaining_half_hour_energies.set(index, 0.0); // no more power in that half_hour
                if (power_used < reserve_remaining) { //great let's use reserve
                    reserve_remaining -= power_used;
                } else {
                    reserve_remaining -= power_used; //it's going negative from here on out
                    //we ran out of reserve- WERE ALL GOING TO DIE
                    POWERDEATH();

                }
            }
            index = (index + 1) % 48;
            Log.v("INDEX", "entering loop index is " + index);
            while (minutes_left > 0) { //we need to use all these

                amt = 30;
                if (minutes_left < amt) {
                    amt = minutes_left;
                }
                minutes_left -= amt;
                power_used = amt/60.0 * ai.watts;
                Log.v("DEBUG","Minutes_Left="+minutes_left + "amt=" +amt + " power_used = " + power_used + " current index="+index);
                Log.v("DEBUG", "power = " + half_hour_energies.get(index) + " remaining power =" + remaining_half_hour_energies.get(index));
                if (power_used < remaining_half_hour_energies.get(index)) {
                    Log.v("DEBUG", "used all from the solar panels");
                    remaining_half_hour_energies.set(index, remaining_half_hour_energies.get(index) - power_used);
                } else {
                    power_used -= remaining_half_hour_energies.get(index);
                    remaining_half_hour_energies.set(index, 0.0);
                    Log.v("DEBUG", "solar panel is dry now - used it all up + power needed is now: " + power_used);
                    if (power_used < reserve_remaining) { //great reserve is saving us
                        reserve_remaining -= power_used;
                        Log.v("DEBUG","There's plenty of reserve left: reserve_remaining="+reserve_remaining);
                    } else {
                        reserve_remaining -= power_used;
                    }
                }
                index = (index + 1) % 48;
                Log.v("INDEX", "before end of loop index is " + index);
            }
            Log.v("DEBUG","Minutes_Left="+minutes_left + " last power_used = " + power_used);
            //at this point, we walk through half hour blocks doing the same thing
        }

        //now go through progress bars and update them - update predicted power left for today and predicted reserve remaining
        for(int i = 0; i < 48; ++i) {
            double this_half_hour = remaining_half_hour_energies.get(i);
            double full_power = getEnergy(half_hour_energies.get(i)) * 0.5;
            pbs.get(i).setSecondaryProgress((int) (full_power*100/maxE));
            pbs.get(i).setProgress((int) (this_half_hour*100 / maxE));
        }

        if(reserve_remaining > 0) {
            reserve.setProgress((reserve_remaining*100) / reserve_max);
        }
        else {
            POWERDEATH();
            reserve.setProgressDrawable(getDrawable(R.drawable.yellowprogress));
            reserve.setProgress((reserve_remaining*100)/ reserve_max); //this is how much you went over
        }
        String display_predicted_power_text = "Predicted Overall Daily Left ["  + (int)((totalkWh - usedTotalkWh)/1000) + "/" + (int)(totalkWh/1000)  + " kWh] (full bar=50kWh)";
        ((TextView)findViewById(R.id.predicted_power_textview)).setText(display_predicted_power_text);

        TextView rtv = (TextView) findViewById(R.id.reserve_textview);
        rtv.setText("Predicted Reserve remaining. [" + reserve_remaining/1000 + "/" + reserve_max/1000 + " kWh]");
        daily.setSecondaryProgress( (int)(totalkWh*100) / progress_max );
        daily.setProgress( (int) ( ( totalkWh - usedTotalkWh ) * 100 / progress_max ));
        Log.d("DEBUG", "usedTotalkWh="+usedTotalkWh+ "  and totalkWh="+totalkWh);

    }


    @Override
    public void onResume() {

        Log.v("DEBUG", "resuming Main Activity + " + ActivityResponse.getInstance().activity_name + " + " +
                ActivityResponse.getInstance().submit);
        super.onResume();


        LinearLayout ll = (LinearLayout) findViewById(R.id.activities);
        if (ActivityResponse.getInstance().submit) {
            //else, too bad, so sad
            TextView tv = new TextView(this);
            String name = ActivityResponse.getInstance().activity_name;
            String newname = name.replace("\n", "");
            if (newname == null || newname.length() == 0) {
                newname = "Unnamed Activity";
            }
            Log.v("DEBUG", newname + " " + name);
            String desc_string = "   " + newname + " - " + ActivityResponse.getInstance().power_string;
            desc_string += "\n  " + ActivityResponse.getInstance().activity_item.toString();
            tv.setText(desc_string);
            if(ActivityResponse.getInstance().activity_item.watts < 1000)  //green zone is below 1000 watts = adjust to taste
                tv.setBackground(getDrawable(R.drawable.background_activity_textview));
            else if(ActivityResponse.getInstance().activity_item.watts < 3000) //yellow zone is below 3000 watts - adjust to taste
                tv.setBackground(getDrawable(R.drawable.background_yellow_textview));
            else  //danger zone otherwise
                tv.setBackground(getDrawable(R.drawable.background_red_textview));
            ll.addView(tv);

            //grey line separator
            /*
            View v = new View(this);
            v.setLayoutParams(new LinearLayout.LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 3));
            v.setBackgroundColor( Color.DKGRAY );
            ll.addView(v);
            */

            activity_textviews.add(tv);

            ActivityItem ai = ActivityResponse.getInstance().activity_item;
            activity_list.add(ai);
            // ll.notify();

        }
        if (SettingsResponse.getInstance().submit == 1) { //the settings panel was updated
            //means settings came back with changes
            Log.d("DEBUG1", SettingsResponse.getInstance().toString());
            number_of_panels = SettingsResponse.getInstance().number_of_panels;
            efficiency = SettingsResponse.getInstance().panel_efficiency;
            panel_size = SettingsResponse.getInstance().size_of_panels;

        } else if(SettingsResponse.getInstance().submit == 2) { //a new location was set in location settings
            Location loc = new Location("dummy");
            loc.setLatitude(SettingsResponse.getInstance().latitude);
            loc.setLongitude(SettingsResponse.getInstance().longitude);
            UpdateLocationVariables(loc); //defaults to hi-seas habitat

        } else if(SettingsResponse.getInstance().submit == 3) { //we want to poll our GPS again to get location
            UpdateLocation(ll);
        } else {
            Log.d("DEBUG1", "false" + SettingsResponse.getInstance().toString());
        }

        updateEnergies();
    }


    //the button onclick that adds an activity
    public void AddActivity(View v) {
//launch the other intent to
        Intent i = new Intent(this, AddActivity.class);
        startActivity(i);
    }

//TODO: can I even move this to its own class? this might be the powerdeath issue as I let
    //it interact with the private variables of the enclosing class I think
    private class HttpTask extends AsyncTask<String, Integer, String> {

        int totalSum = 0;
        double maxE = 0;

        @Override
        protected String doInBackground(String... urls) {
            // TODO Auto-generated method stub
            String url0 = urls[0];
            String response = "";
            try {
                URL oracle = new URL(url0);
                URLConnection yc = oracle.openConnection();

                BufferedReader in = new BufferedReader(new InputStreamReader((yc.getInputStream())));
                String inputLine;
                int count = 0;
                int sum = 0;
                inputLine = in.readLine();
                Log.v("lines", inputLine);
                while (((inputLine = in.readLine()) != null) && count < 50) {
                    Log.v("lines", count + ": " + inputLine);
                    response += inputLine + "\n";
                    String[] sep = inputLine.split(","); //comma separated file splitting here
                    Integer v = Integer.valueOf(sep[0]);
                    //if(v > maxE) { maxE = v; }
                    sum += v;
                    count++;
                }
                in.close();
                totalSum = sum;

            } catch (Exception e) {
                Log.v("ERROR", e.getMessage() + " " + e.toString());
            }

            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            Log.v("onPostExecute", "HTTP RESPONSE" + response);
            String[] lines = response.split("\n");
            maxE = MAX_ENERGIES; // for a half hour
            Log.d("DEBUG", "half hours are out of: " + maxE);
            //double total_energy = 0;
            for (int i = 1; i <= 48 && i < lines.length; i++) {
                String[] sep = lines[i].split(",");
                Integer v = Integer.valueOf(sep[0]);
                //double energy = getEnergy(v) * 0.5; //only used for 30 minutes
                //double pct = energy / maxE; //percentage of energy out of normalish
              //  pbs.get(i - 1).setProgress((int) (pct * 100));
                half_hour_energies.set(i - 1, v * 1.0); //the full energy value
                //total_energy += energy;
                updateEnergies();
            }
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            // TODO Auto-generated method stub
            super.onProgressUpdate(values);
        }

    }

}