package de.zipperle.cowtracker.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

public class CalculateDistanceHandler implements RequestStreamHandler {
	
	private double lat1, lng1, lat2, lng2, distance;
	
	private String statusCode;
	private JSONObject headers;

	private JSONParser parser;
	
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    	LambdaLogger logger = context.getLogger();
    	// logger.log("Input: " + input.toString() + " Context " + context.toString());  
    	BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        parser = new JSONParser();
        statusCode = "200";
        
    	try {
            JSONObject event = (JSONObject) parser.parse(reader);
			
            if (event.get("headers") != null) {
                headers = (JSONObject) event.get("headers");
            }
            
            if (event.get("queryStringParameters") != null) {
                JSONObject parameters = (JSONObject) event.get("queryStringParameters");
                if (parameters.get("lat1") != null) {
                    lat1 = Double.valueOf(parameters.get("lat1").toString());
                }
                if (parameters.get("lng1") != null) {
                	lng1 = Double.valueOf(parameters.get("lng1").toString());
                }
                if (parameters.get("lat2") != null) {
                	lat2 = Double.valueOf(parameters.get("lat2").toString());
                }
                if (parameters.get("lng2") != null) {
                	lng2 = Double.valueOf(parameters.get("lng2").toString());
                }
            }
            
            distance = Double.valueOf(calcDistance(lat1, lng1, lat2, lng2, "k"));
            
            JSONObject responseBody = new JSONObject();
            responseBody.put("input", event.toJSONString());
            responseBody.put("distance", distance);
            
            JSONObject headerJson = headers;

            responseJson.put("isBase64Encoded", false);
            responseJson.put("statusCode", statusCode);
            responseJson.put("headers", headerJson);
            responseJson.put("body", responseBody.toString());  
            
            
    	} catch(ParseException pex) {
            responseJson.put("statusCode", "400");
            responseJson.put("exception", pex);
        } 

		logger.log(responseJson.toJSONString());
        OutputStreamWriter writer;
        
		try {
			writer = new OutputStreamWriter(outputStream, "UTF-8");
			writer.write(responseJson.toJSONString());  
	        writer.close();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}      
    }
    
    /**
     * This function calculates the distance between two coordinates
     * Code Logic from GeoDataSource.com
     * @param lat1
     * @param lon1
     * @param lat2
     * @param lon2
     * @param unit
     * @return distance between the two coordinates
     */
    private static double calcDistance(double lat1, double lon1, double lat2, double lon2, String unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == "K") {
			dist = dist * 1.609344;
		} else if (unit == "N") {
			dist = dist * 0.8684;
		}
		
		DecimalFormat df = new DecimalFormat("#.##");      
		dist = Double.valueOf(df.format(dist));

		return (dist);
	}

    /**
     * This function converts decimal degrees to radians		
     * @param deg
     * @return
     */
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/**
	 * This function converts radians to decimal degrees	
	 * @param rad
	 * @return
	 */
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
	
	// f
}
