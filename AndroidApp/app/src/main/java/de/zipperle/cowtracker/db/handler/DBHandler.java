package de.zipperle.cowtracker.db.handler;

import android.util.Log;

import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;


import java.util.Calendar;
import java.util.List;

import de.zipperle.cowtracker.db.datastructure.Locations;
import de.zipperle.cowtracker.db.listener.LocationQueryListener;

public class DBHandler {

    private String LOG_TAG;

    AmazonDynamoDBClient client;
    DynamoDBMapper dynamoDBMapper;

    private LocationQueryListener locationQueryListener;

    public DBHandler(){
        LOG_TAG = this.getClass().getSimpleName();

        Log.d(LOG_TAG, IdentityManager.getDefaultIdentityManager().getCredentialsProvider().toString());
        Log.d(LOG_TAG, IdentityManager.getDefaultIdentityManager().getConfiguration().toString());
        client = new AmazonDynamoDBClient(IdentityManager.getDefaultIdentityManager().getCredentialsProvider());
        dynamoDBMapper = DynamoDBMapper.builder()
                .awsConfiguration(IdentityManager.getDefaultIdentityManager().getConfiguration())
                .dynamoDBClient(client)
                .build();
    }

    public void createLocation(final String _cowName, final Double _latitude, final Double _longitude) {
        final Locations locationItem = new Locations();

        Log.d(LOG_TAG, IdentityManager.getDefaultIdentityManager().getCachedUserID());

        locationItem.setUserId(IdentityManager.getDefaultIdentityManager().getCachedUserID());
        locationItem.setName(_cowName);
        locationItem.setLatitude(_latitude);
        locationItem.setLongitude(_longitude);
        locationItem.setTimestamp(new java.sql.Timestamp(Calendar.getInstance().getTime().getTime()).toString());

        new Thread(new Runnable() {
            @Override
            public void run() {
                dynamoDBMapper.save(locationItem);
                Log.d(LOG_TAG, "Item saved!");
            }
        }).start();
    }

    public void queryLocations() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                Locations loc = new Locations();
                loc.setUserId(IdentityManager.getDefaultIdentityManager().getCachedUserID());

                DynamoDBQueryExpression queryExpression = new DynamoDBQueryExpression()
                        .withHashKeyValues(loc);

                List<Locations> locations = dynamoDBMapper.query(Locations.class, queryExpression);

                locationQueryListener.onNewLocations(locations);
            }
        }).start();
    }

    public void generateTestLocations(){
        createLocation("Berta",48.055,8.18);
        createLocation("Lisa", 48.055,8.19);
        createLocation("Rosi", 48.054,8.19);
        createLocation("Alma", 48.056,8.19);
        createLocation("Gerda", 48.055,8.20);
    }

    public void setLocationQueryListener(LocationQueryListener _locationQueryListener){
        this.locationQueryListener = _locationQueryListener;
    }
}
