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
import android.widget.Button;
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

public class MainActivity extends AppCompatActivity {

    public ArrayList<ProgressBar> pbs = new ArrayList<>();
    public ArrayList<Double> half_hour_energies = new ArrayList<>();
    public ArrayList<Double> remaining_half_hour_energies = new ArrayList<>();
    public ArrayList<ActivityItem> activity_list = new ArrayList<>();
    public ArrayList<TextView> activity_textviews = new ArrayList<>();

    String fetchedTime;
    int fetched_start_hour = 0;
    int fetched_start_half = 0;

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

    double getEnergy(double irradiance) {
        return irradiance * efficiency * number_of_panels * panel_size;
    }


    LocationManager lm;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(activity_list.size() > 0) {
                ProgressBar res = (ProgressBar) findViewById(R.id.reserve_power_progressbar);
                res.setSecondaryProgress(0);
                res.setProgressDrawable(getDrawable(R.drawable.yellowprogress));
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


    LocationListener onLocationChange = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            UpdateLocationVariables(loc);

            lm.removeUpdates(this); //got what we came for
            Button b = (Button) findViewById(R.id.location_button);
            b.setVisibility(View.GONE);
            b.setEnabled(false);
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

    public void UpdateLocationVariables(Location loc) {
        if (loc == null) {
            return;
        }
        longitude = loc.getLongitude();
        latitude = loc.getLatitude();
        url = "https://api.solcast.com.au/radiation/forecasts?longitude=" + longitude +
                "&latitude=" + latitude + "&capacity=10&api_key=uos_eam6ozSnecTk5pTerz5ow-r916Uc&format=csv";
        fetchURL();

       // Toast.makeText(getApplicationContext(), "--long" + longitude + " lat" + latitude, Toast.LENGTH_LONG).show();

    }


    public void fetchURL() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);
        if (permissionCheck != PERMISSION_DENIED) {
            Toast.makeText(getApplicationContext(), "Trying to get predictions and GPS", Toast.LENGTH_LONG).show();

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


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_name:
                Intent i = new Intent(this, PanelSettings.class);
                startActivity(i);
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
        /*
        LinearLayout al = (LinearLayout) findViewById(R.id.activities);
        TextView tv = new TextView(this);
        tv.setText(new String("Sample Activity"));
        al.addView(tv);
//        al.notify();
        */

        Log.v("DEBUG", "Created Main Activity");
    }

    public void POWERDEATH() {
        Toast.makeText(getApplicationContext(), "WE WILL USE ALL RESERVES!!!",
                Toast.LENGTH_LONG).show();
        /*
        ProgressBar reserve = (ProgressBar) findViewById(R.id.reserve_power_progressbar);
        reserve.setSecondaryProgress(100);
        reserve.setProgress(25);
        reserve.setProgressDrawable(getDrawable(R.drawable.darkness));
        */
        //getDrawable(R.drawable.darkness));
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
                        POWERDEATH();
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
        //    Log.v("DEBUG","index: " + i + "  power="+this_half_hour);
            pbs.get(i).setSecondaryProgress((int) (full_power*100/maxE));
            pbs.get(i).setProgress((int) (this_half_hour*100 / maxE));

//            remaining_half_hour_energies.set(i, half_hour_energies.get(i));
        }

        if(reserve_remaining > 0) {
            reserve.setProgress((reserve_remaining*100) / reserve_max);
        }
        else {
            reserve.setProgress((-reserve_remaining*100)/ reserve_max); //this is how much you went over
        }
        String display_predicted_power_text = "Predicted Power Left ["  + (int)((totalkWh - usedTotalkWh)/1000) + "/" + (int)(totalkWh/1000)  + " kWh] (full bar=50kWh)";
        ((TextView)findViewById(R.id.predicted_power_textview)).setText(display_predicted_power_text);

        TextView rtv = (TextView) findViewById(R.id.reserve_textview);
        rtv.setText("Predicted Reserve remaining. [" + reserve_remaining/1000 + "/" + reserve_max/1000 + " kWh]");
//        daily.setProgress( (int)(usedTotalkWh*100) / progress_max);
        daily.setSecondaryProgress( (int)(totalkWh*100) / progress_max );
        daily.setProgress( (int) ( ( totalkWh - usedTotalkWh ) * 100 / progress_max ));
        Log.d("DEBUG", "usedTotalkWh="+usedTotalkWh+ "  and totalkWh="+totalkWh);

    }


    @Override
    public void onResume() {
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        UpdateLocation(findViewById(R.id.location_button));
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
            String desc_string = newname + " - " + ActivityResponse.getInstance().power_string;
            desc_string += "\n" + ActivityResponse.getInstance().activity_item.toString();
            tv.setText(desc_string);
            ll.addView(tv);
            activity_textviews.add(tv);

            ActivityItem ai = ActivityResponse.getInstance().activity_item;
            activity_list.add(ai);
            // ll.notify();

        }
        if (SettingsResponse.getInstance().submit) {
            //means settings came back with changes
            Log.d("DEBUG1", SettingsResponse.getInstance().toString());
            number_of_panels = SettingsResponse.getInstance().number_of_panels;
            efficiency = SettingsResponse.getInstance().panel_efficiency;
            panel_size = SettingsResponse.getInstance().size_of_panels;

        } else {
            Log.d("DEBUG1", "false" + SettingsResponse.getInstance().toString());
        }
        updateEnergies();
        //for all progress bars, update in case anything has changed:

        //   UpdateLocation(ll);
    }


    public void AddActivity(View v) {
//launch the other intent to
        Intent i = new Intent(this, AddActivity.class);
        startActivity(i);
    }


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