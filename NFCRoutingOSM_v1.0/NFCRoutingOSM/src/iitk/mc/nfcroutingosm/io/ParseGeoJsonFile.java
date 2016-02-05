//package iitk.mc.nfcroutingosm.io;
//
//
//import java.io.File;
//
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import android.util.Log;
// //TODO FOLLOWING LIBRARIES ARE NEEDED FOR THIS TO WORK
//import com.cocoahero.android.geojson.GeoJSON;
//import com.cocoahero.android.geojson.GeoJSONObject;
///**
// * Parses in a GeoJSON File into a linked list structure that can be traversed by a searching algorithm like A-Star
// * @author Benjamin Schiller
// *
// */
//public class ParseGeoJsonFile {
//
//	private static String GEOJSON_EXAMPLE = 
//			"{"
//					+ "\"type\":\"FeatureCollection\","
//					+ "\"generator\":\"JOSM\","
//					+ "\"bbox\":["
//					+        "80.23528,"
//					+        "26.51154,"
//					+        "80.23557,"
//					+        "26.51339"
//					+     "],"
//					+ "\"features\":["
//					+              "{"
//					+                  "\"type\":\"Feature\","
//					+                  "\"properties\":{"
//					+                      "\"name\":\"Shopping Centre Road\","
//					+                      "\"highway\":\"residential\""
//					+                  "},"
//					+                  "\"geometry\":{"
//					+                     "\"type\":\"LineString\","
//					+                      "\"coordinates\":["
//					+                          "["
//					+                             "80.2353211,"
//					+                              "26.5119416"
//					+                          "],"
//					+                          "["
//					+                             "81.2353211,"
//					+                              "27.5119416"
//					+                          "]"
//					+						"]"
//					+					"}"
//					+				"},"
//					+              "{"
//					+                  "\"type\":\"Feature\","
//					+                  "\"properties\":{"
//					+                      "\"name\":\"Test Centre Road\","
//					+                      "\"highway\":\"residential\""
//					+                  "},"
//					+                  "\"geometry\":{"
//					+                     "\"type\":\"LineString\","
//					+                      "\"coordinates\":["
//					+                          "["
//					+                             "81.2353211,"
//					+                              "27.5119416"
//					+                          "]"
//					+						"]"
//					+					"}"
//					+				"}"		
//					+				"]"		
//					+ "}";
//
//	private static String TAG = "ParseGeoJsonFile";
//
//	public ParseGeoJsonFile(){
//	}
//
//	public void parse(){
//		try {
//			GeoJSONObject geoJSON; 
//			geoJSON = GeoJSON.parse(GEOJSON_EXAMPLE); // parse the GeoJSON file
//			JSONObject jsonObj = geoJSON.toJSON(); // convert to JSON
//			JSONArray jsonArray = jsonObj.getJSONArray("features"); // get features of the file as Array
//			//Log.d(TAG, jsonObjectFeature.toString(1));
//			//iterate over all features
//			for (int i = 0; i < jsonArray.length(); i++){
//				JSONObject jsonObjectFeature = jsonArray.getJSONObject(i); // get first feature 
//
//				// get the geometry part of this feature to check if it is a road
//				JSONObject jsonObjectFeatureGeometry = jsonObjectFeature.getJSONObject("geometry"); // gets the geometry part
//				if (jsonObjectFeatureGeometry.getString("type").equals("LineString")){ // check if it is a road
//					// since it is a road, we can iterate over all coordinates
//					JSONArray  jsonArrayFeatureGeometryCoords = jsonObjectFeatureGeometry.getJSONArray("coordinates");
//					for (int j = 0; j < jsonArrayFeatureGeometryCoords.length(); j++){
//						Log.d(TAG, "Feature " + i + ", Coord "+ j +" is "+jsonArrayFeatureGeometryCoords.get(j).toString());
//					}
//				}
//			}
//
//		}
//		catch (JSONException e) {
//			e.printStackTrace();
//		}
//
//
//	}
//
//}
