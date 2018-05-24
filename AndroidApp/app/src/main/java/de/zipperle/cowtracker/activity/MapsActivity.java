package de.zipperle.cowtracker.activity;

import android.os.Bundle;
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
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import de.zipperle.cowtracker.R;
import de.zipperle.cowtracker.db.datastructure.Locations;
import de.zipperle.cowtracker.db.handler.DBHandler;
import de.zipperle.cowtracker.db.listener.LocationQueryListener;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationQueryListener {

    private GoogleMap mMap;
    private PinpointManager pinpointManager;
    private DBHandler dbHandler;

    private String LOG_TAG;

    private String userId;
    private List<Locations> locations;
    private List<Marker> markers;

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
    }

    @Override
    protected void onStop(){
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
        dbHandler.queryLocations();
    }


    @Override
    public void onNewLocations(List<Locations> _locations) {
        locations = _locations;
        markers = new ArrayList<>();

        Log.d(LOG_TAG, "List size: " + locations.size());

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for(Locations location: locations){
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    String title = location.getName();
                    Marker tmp = mMap.addMarker(new MarkerOptions().position(latLng).title(title));
                    markers.add(tmp);
                }

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
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
        });
    }
}
