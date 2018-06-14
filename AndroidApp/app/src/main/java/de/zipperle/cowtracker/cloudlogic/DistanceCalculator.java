package de.zipperle.cowtracker.cloudlogic;

import android.util.Log;

import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.api.ida0wpi69e3d.CalculateDistanceMobileHubClient;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.apigateway.ApiClientFactory;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringUtils;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class DistanceCalculator {
    private String LOG_TAG = this.getClass().getSimpleName();
    private CalculateDistanceMobileHubClient apiClient;
    private double distance;

    public DistanceCalculator(){
        // Create the client
        apiClient = new ApiClientFactory()
                .credentialsProvider(AWSMobileClient.getInstance().getCredentialsProvider())
                .build(CalculateDistanceMobileHubClient.class);
    }

    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        // Create components of api request
        final String method = "GET";

        final String path = "/calculateDistance";

        final String body = "";
        final byte[] content = body.getBytes(StringUtils.UTF8);

        final Map parameters = new HashMap<>();
        parameters.put("lang", "en_US");
        parameters.put("lat1", Double.toString(lat1));
        parameters.put("lng1", Double.toString(lng1));
        parameters.put("lat2", Double.toString(lat2));
        parameters.put("lng2", Double.toString(lng2));

        final Map headers = new HashMap<>();

        // Use components to create the api request
        ApiRequest localRequest =
                new ApiRequest(apiClient.getClass().getSimpleName())
                        .withPath(path)
                        .withHttpMethod(HttpMethodName.valueOf(method))
                        .withHeaders(headers)
                        .addHeader("Content-Type", "application/json")
                        .withParameters(parameters);

        // Only set body if it has content.
        if (body.length() > 0) {
            localRequest = localRequest
                    .addHeader("Content-Length", String.valueOf(content.length))
                    .withBody(content);
        }

        final ApiRequest request = localRequest;

        // Make network call on background thread
        Thread getData = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.d(LOG_TAG,
                            "Invoking API w/ Request : " +
                                    request.getHttpMethod() + ":" +
                                    request.getPath());

                    final ApiResponse response = apiClient.execute(request);

                    final InputStream responseContentStream = response.getContent();
                    final String responseData = IOUtils.toString(responseContentStream);
                    Log.d(LOG_TAG, "Response : " + responseData);

                    Log.d(LOG_TAG, response.getStatusCode() + " " + response.getStatusText());

                    JSONObject responseJSONObject = new JSONObject(responseData);
                    distance = Double.valueOf(responseJSONObject.get("distance").toString());
                    Log.d(LOG_TAG, "Distance: " + distance);
                } catch (final Exception exception) {
                    Log.e(LOG_TAG, exception.getMessage(), exception);
                    exception.printStackTrace();
                }
            }
        });

        getData.start();
        try {
            getData.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return distance;
    }
}
