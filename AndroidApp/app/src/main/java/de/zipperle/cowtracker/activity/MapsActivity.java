package de.zipperle.cowtracker.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import de.zipperle.cowtracker.R;
import de.zipperle.cowtracker.cloudlogic.DistanceCalculator;
import de.zipperle.cowtracker.db.datastructure.Locations;
import de.zipperle.cowtracker.db.handler.DBHandler;
import de.zipperle.cowtracker.db.listener.LocationQueryListener;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationQueryListener {

    private GoogleMap mMap;
    private static final int FINE_LOCATION_PERMISSION_REQUEST = 1;
    private static final int CONNECTION_RESOLUTION_REQUEST = 2;

    private PinpointManager pinpointManager;
    private DBHandler dbHandler;

    private String LOG_TAG;

    private String userId;
    private LatLng myPosition;
    private List<Locations> locations;
    private List<Marker> markers;

    private DistanceCalculator calculator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        LOG_TAG = this.getClass().getSimpleName();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        PinpointConfiguration config = new PinpointConfiguration(
                MapsActivity.this,
                AWSMobileClient.getInstance().getCredentialsProvider(),
                AWSMobileClient.getInstance().getConfiguration()
        );

        pinpointManager = new PinpointManager(config);
        pinpointManager.getSessionClient().startSession();
        pinpointManager.getAnalyticsClient().submitEvents();

        userId = IdentityManager.getDefaultIdentityManager().getCachedUserID();
        Log.d(LOG_TAG, "User: " + userId);

        dbHandler = new DBHandler();
        dbHandler.setLocationQueryListener(this);
        dbHandler.generateTestLocations();

        calculator = new DistanceCalculator();
    }

    @Override
    protected void onStop() {
        super.onStop();
        pinpointManager.getSessionClient().stopSession();
        pinpointManager.getAnalyticsClient().submitEvents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu); //"menu_main" is the XML-File in res
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_logout:

                Log.d(LOG_TAG, "Logout clicked!");
                IdentityManager.getDefaultIdentityManager().signOut();
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        markers = new ArrayList<>();
        updateMyPosition();
        dbHandler.queryLocations();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case FINE_LOCATION_PERMISSION_REQUEST: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    updateMyPosition();
                }
            }
        }
    }

    private void updateMyPosition(){
        // Enabling MyLocation Layer of Google Map
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_LOCATION_PERMISSION_REQUEST);
        }
        mMap.setMyLocationEnabled(true);

        // Getting LocationManager object from System Service LOCATION_SERVICE
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // Creating a criteria object to retrieve provider
        Criteria criteria = new Criteria();

        // Getting the name of the best provider
        String provider = locationManager.getBestProvider(criteria, true);

        // Getting Current Location
        Location location = locationManager.getLastKnownLocation(provider);

        if (location != null) {
            // Getting latitude of the current location
            double latitude = location.getLatitude();

            // Getting longitude of the current location
            double longitude = location.getLongitude();

            myPosition = new LatLng(latitude, longitude);

            Marker tmp = mMap.addMarker(new MarkerOptions()
                    .position(myPosition)
                    .title("MyPosition")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
            markers.add(tmp);
        }
    }

    @Override
    public void updateCowLocations(List<Locations> _locations) {
        locations = _locations;
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(Locations location: locations) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                    Double distance = calculator.calculateDistance(myPosition.latitude, myPosition.longitude, latLng.latitude, latLng.longitude);

                    String title = location.getName() + ": " + distance + "km";
                    Marker tmp = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                    markers.add(tmp);
                }
                updateMapCameraView();
            }
        });
    }

    private void updateMapCameraView(){
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        Log.d(LOG_TAG, "Marker size: " + markers.size());
        for (Marker marker : markers) {
            builder.include(marker.getPosition());
        }
        LatLngBounds bounds = builder.build();
        // Offset from edges of the map in pixel, in this case 10%
        int width = getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (width * 0.10);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
        mMap.animateCamera(cu);
    }
}
