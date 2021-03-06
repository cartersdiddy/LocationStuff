package com.example.chrisbennett.locationstuff;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    GoogleApiClient client;
    Location loc;
    LocationRequest request;
    String dateTime;
    String address;
    boolean requesting;
    boolean mAddressRequested;
    private AddressResultReceiver mResultReceiver;

    protected static final String REQUESTING_LOCATION_UPDATES_KEY = "req_key";
    protected static final String LOCATION_KEY = "loc_key";
    protected static final String LAST_UPDATED_TIME_STRING_KEY = "time_key";
    protected static final String LOCATION_ADDRESS_KEY = "addr_key";

    ProgressBar progBar;
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAddressRequested = false;
        address = "";

        updateValuesFromBundle(savedInstanceState);
        //updateUI();

        progBar = (ProgressBar) findViewById(R.id.progressBar);
        button = (Button) findViewById(R.id.btnAddr);
        //createLocationRequest();
        makeClient();
        mResultReceiver = new AddressResultReceiver(new Handler());
        Log.v("create","got through create");
    }

    protected synchronized void makeClient() {
        if(client == null) {
            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        client.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(client.isConnected()) {
            client.disconnect();
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (client.isConnected() && !requesting) {
            startLocationUpdates();
        }
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                requesting);
        savedInstanceState.putParcelable(LOCATION_KEY, loc);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, dateTime);
        savedInstanceState.putString(LOCATION_ADDRESS_KEY, address);

        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onConnected(Bundle hint)  {

        try {
            loc = LocationServices.FusedLocationApi.getLastLocation(client);
        }
        catch(SecurityException e) {
            Log.e("loc", "Security issue with location: " + e.getMessage());
        }
        if(loc != null) {

            if(!Geocoder.isPresent()) {
                Toast.makeText(this,"No address resolution available",Toast.LENGTH_LONG).show();
                return;
            }
/*
            if(requesting) {
                startLocationUpdates();
            }
*/

            if (mAddressRequested) {
                startIntentService();
            }
        }
    }


    @Override
    public void onConnectionSuspended(int x) {
        Log.i("loc", "Connection suspended");
        client.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("loc", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    protected void createLocationRequest() {
        request = new LocationRequest();
        request.setInterval(2000);
        request.setFastestInterval(1000);
        /*PRIORITIES:
            PRIORITY_BALANCED_POWER_ACCURACY
            PRIORITY_HIGH_ACCURACY
            PRIORITY_LOW_POWER
            PRIORITY_NO_POWER
        */
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);


    }

    protected void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(client, request, this);
        }
        catch(SecurityException e) {
            Log.e("loc", "Security issue with location: " + e.getMessage());
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                client, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        loc = location;
        dateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
    }

    private void updateUI() {
        TextView lat = (TextView) findViewById(R.id.txtLat);
        TextView lon = (TextView) findViewById(R.id.txtLon);
        TextView alt = (TextView) findViewById(R.id.txtTime);
        TextView addr = (TextView) findViewById(R.id.txtAddress);

        if(loc!=null) {
            lat.setText(String.valueOf(loc.getLatitude()));
            lon.setText(String.valueOf(loc.getLongitude()));
            dateTime = DateFormat.getDateTimeInstance().format(new Date());
            alt.setText(dateTime);
        }
        if (mAddressRequested) {
            progBar.setVisibility(ProgressBar.VISIBLE);
            button.setEnabled(false);

        }
        else {
            progBar.setVisibility(ProgressBar.GONE);
            button.setEnabled(true);
            addr.setText(address);
        }
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                requesting = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocationis not null.
                loc = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if(savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                address = savedInstanceState.getParcelable(LOCATION_ADDRESS_KEY);
            }
            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                dateTime = savedInstanceState.getString(
                        LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }


    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(FetchAddressIntentService.Constants.RECEIVER, mResultReceiver);
        intent.putExtra(FetchAddressIntentService.Constants.LOCATION_DATA_EXTRA, loc);
        startService(intent);
    }


    public void getAddress(View view) {
        // Only start the service to fetch the address if GoogleApiClient is
        // connected.
        if (client.isConnected() && loc != null) {
            startIntentService();
        }
        // If GoogleApiClient isn't connected, process the user's request by
        // setting mAddressRequested to true. Later, when GoogleApiClient connects,
        // launch the service to fetch the address. As far as the user is
        // concerned, pressing the Fetch Address button
        // immediately kicks off the process of getting the address.
        mAddressRequested = true;
        updateUI();
    }

    @SuppressLint("ParcelCreator")
    class AddressResultReceiver extends ResultReceiver {

        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            // Display the address string
            // or an error message sent from the intent service.
            address = resultData.getString(FetchAddressIntentService.Constants.RESULT_DATA_KEY);
            updateUI();

            // Show a toast message if an address was found.
            if (resultCode == FetchAddressIntentService.Constants.SUCCESS_RESULT) {
                Toast.makeText(getApplicationContext(), "found address!",Toast.LENGTH_SHORT).show();
            }

            mAddressRequested = false;
            updateUI();

        }
    }

}
