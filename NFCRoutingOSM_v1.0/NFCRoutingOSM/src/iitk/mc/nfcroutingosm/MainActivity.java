package iitk.mc.nfcroutingosm;

import iitk.mc.nfcroutingosm.algo.RoutingAlgorithms;
import iitk.mc.nfcroutingosm.datastruct.GeoNode;
import iitk.mc.nfcroutingosm.io.ParseOSMXMLFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.osmdroid.ResourceProxy;
import org.osmdroid.bonuspack.overlays.InfoWindow;
import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.bonuspack.overlays.Marker.OnMarkerDragListener;
import org.osmdroid.bonuspack.overlays.Polyline;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.PathOverlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import android.support.v4.util.LongSparseArray;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;

/**
 * Main Activity of the project
 * @author Benjamin Schiller
 * 
 * Needed Setting:
 * - Smartphone has to allow fetching location data from Network / LAN and GPS in its settings 
 * 
 * Code snippets used for displaying markers on the map:
 * - http://android-er.blogspot.in/2012/05/display-current-location-marker-on.html
 * 
 *
 */
public class MainActivity extends ActionBarActivity {

	/** Problem & Solutions / Ideas: 
	 * How to compute whether user left the navigated path or not
	 *  OPTION1: Get all the geopoints between the 2 points that make one polyline (define step size)
	 *  			In async task check whether the user is still near to the path: http://www.movable-type.co.uk/scripts/latlong.html
	 *  OPTION2: Use PolyLine.isClose to check, its calculated in pixel (trial and error to find pixel distance)
	 *  			If too far away, just recalculate the path from current position
	 */

	private FixedMapView mMapView;
	private MapController mMapController;
	private TextView directionsText;
	private TextView gpsCoordsText;
	private PathOverlay pOverlay;
	private Polyline testLine;
	private TextView showLogsText;
	private MyMarker gpsMarker;
	private MyMarker startMarker;
	private MyMarker targetMarker;
	private ArrayList<MyMarker> gPtStartEndMarkers;
	private ArrayList<MyMarker> gPtLocalAreaMarkers;
	private ArrayList<Polyline> polyLineList;
	private LongSparseArray<HashMap<String, LinkedList<GeoPoint>>> polyLines;
	private Location lastLocation;
	private LongSparseArray<GeoNode> nodes;
	private AsyncTask<Void, LinkedList<GeoNode>, Void> recalculateRouteTask;
	private AsyncTask<Void, String, Void> checkUserRouteAllignmentTask;
	private Polyline polyLineRoad;
	private Polyline polyLineRoadToFirstNode;
	private LocationManager locationManager;
	private static String TAG = "Main";
	private MyMarker closestTargetLocNodeMarker;
	private MyMarker closestCurrLocNodeMarker;
	private boolean userNotonTrack = true;
	private boolean taskRunning;
	private GeoNode closestCurrLocNode = new GeoNode(0, 0, 0);
	private GeoNode closestTargetLocNode = new GeoNode(0, 0, 0);
	private GeoNode targetLocNode;
	private InfoWindow infoWindow;
	private GeoNode currLocNode;
	private boolean isInitPhase = true;
	private boolean isCURATRunning = false; // checkUserRouteAllignmentTask running
	private boolean noTiles = false;
	private LinkedList<GeoNode> pathList;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* Initialize textFields */
		directionsText = (TextView) findViewById(R.id.TXTdirections);
		gpsCoordsText = (TextView) findViewById(R.id.TXTshowGPScoord);
		showLogsText = (TextView) findViewById(R.id.TXTshowLogs);


		// copy IITK Campus file to correct folder
		copyMaptoSDCard();

		/* Initilize FixedMapView, InfoWindow and MapController
		 * 
		 * Information on offline maps
		 * http://stackoverflow.com/questions/22862534/download-maps-for-osmdroid/22868462#22868462
		 * http://stackoverflow.com/questions/25349588/osmdroid-offline-map-not-showing-map-after-scrolling-zoomed-map
		 * http://stackoverflow.com/questions/7634148/osmdroid-how-to-load-offline-map-from-zip-archive-maptilefilearchiveprovider
		 * 
		 * */
		mMapView = (FixedMapView) findViewById(R.id.mapview);
		mMapView.setTileSource(new XYTileSource("MapQuest",
				ResourceProxy.string.mapquest_osm, 15, 17, 256, ".jpg", new String[] {
				"http://otile1.mqcdn.com/tiles/1.0.0/map/",
				"http://otile2.mqcdn.com/tiles/1.0.0/map/",
				"http://otile3.mqcdn.com/tiles/1.0.0/map/",
		"http://otile4.mqcdn.com/tiles/1.0.0/map/"}));
		//		mMapView.setBuiltInZoomControls(true);
		mMapView.setMultiTouchControls(true);
		mMapView.setUseDataConnection(true); //optional, but a good way to prevent loading from the network and test your zip loading. 
		mMapController = (MapController) mMapView.getController();
		mMapController.setZoom(9);

		/* even with offline maps, osmdroid needs to create its own tiles out of it to show the map.
		 * We need to restart the app after the first start. This i done automatically int he following */
		if (noTiles){ 
			Intent mStartActivity = new Intent(this, MainActivity.class);
			int mPendingIntentId = 123456;
			PendingIntent mPendingIntent = PendingIntent.getActivity(this , mPendingIntentId,    mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
			AlarmManager mgr = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
			mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
			System.exit(0);
		}

		// currently only using normal InfoWindow, not MyInfowWindow
		infoWindow = new InfoWindow(R.layout.bonuspack_bubble, mMapView) {
			/* close is being called by mMapView OnTouchListener and closes InfoWindow
			 * if the user is releasing his finger from the screen after dragging the map
			 * (only if the user starts draggin 10m away from window).
			 * InfoWindow only shown of user clicks within 10m of a geoNode 
			 * assembling a Polyline
			 */
			@Override
			public void onOpen(Object item) {

			}

			@Override
			public void onClose() {

			}
		};

		mMapView.setOnTouchListener(new MapView.OnTouchListener() {
			boolean isPressed = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// Problem: Fires only once if mMapView.setBuiltInZoomControls(true); is true
				final Projection pj = mMapView.getProjection();
				GeoPoint eventPosition = (GeoPoint) pj.fromPixels((int)event.getX(), (int)event.getY());
//				directionsText.setText("Viewed pos.: (" + eventPosition.toString() + ")");
				checkIfCloseTo(eventPosition, event);
				return false;
			}

			private void checkIfCloseTo(GeoPoint eventPosition, MotionEvent event) {
				if (eventPosition != null && polyLineRoad != null && polyLineRoad.isCloseTo(eventPosition, 10, mMapView)){
					//Log.d(TAG, "openWindoW");
				}
				else if (eventPosition != null && polyLineRoad != null && !polyLineRoad.isCloseTo(eventPosition, 10, mMapView)){
					if (event.getAction() == MotionEvent.ACTION_UP){
						//						Log.d(TAG, "closeWindoW");
						infoWindow.close();
					}
				}

			}
		});


		/* Init addMarker */
		//		addMarker = new Marker(mMapView);
		closestTargetLocNodeMarker = new MyMarker(mMapView, this);
		closestCurrLocNodeMarker = new MyMarker(mMapView, this);

		/* Set center of Map */
		GeoPoint gPt = new GeoPoint(26.5122155, 80.2253452);
		mMapController.setCenter(gPt);

		/* Add Scale Bar */
		ScaleBarOverlay myScaleBarOverlay = new ScaleBarOverlay(this);
		mMapView.getOverlays().add(myScaleBarOverlay);

		// Init geoPoints Markers
		gPtStartEndMarkers = new ArrayList<MyMarker>();
		gPtLocalAreaMarkers = new ArrayList<MyMarker>();

		/* Add a pathoverlay to the map for testing */
		//		addTestPathoverlay();

		/* set user location marker */
		setInitialUserLocationMarker();


		// reads NFC Data and starts routine
		startRoutine();

	}

	/**
	 * Copies the map out of the assets folder onto the SDCard. See the readme.txt file
	 * in assets folder for further details.
	 */
	private void copyMaptoSDCard() {
		AssetManager assetManager = getAssets();
		String[] files = null;
		try {
			files = assetManager.list("");
		} catch (IOException e) {
			Log.e("tag", "Failed to get asset file list.", e);
		}
		for(String filename : files) {
			if (filename.endsWith(".zip")){

				InputStream in = null;
				OutputStream out = null;

				File folder = new File(Environment.getExternalStorageDirectory() + "/osmdroid");
				boolean success = true;
				if (!folder.exists()) {
					success = folder.mkdir();
					noTiles = true;
				}
				if (success) {
					try {
						Log.d(TAG, "Map loaded");
						in = assetManager.open(filename);
						File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/osmdroid/", filename);
						out = new FileOutputStream(outFile);
						copyFile(in, out);
						in.close();
						in = null;
						out.flush();
						out.close();
						out = null;
					} catch(IOException e) {
						Log.e("tag", "Failed to copy asset file: " + filename, e);
					}  
				} else {
					// TODO
				}



			}
		}
	}

	private void copyFile(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int read;
		while((read = in.read(buffer)) != -1){
			out.write(buffer, 0, read);
		}
	}

	@Override
	public void onDestroy(){
		isCURATRunning = false;
		super.onDestroy();

	}

	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "NFC TAG Recognized");
		handleIntent(intent);
	}

	/**
	 * Will be called as first method to start the routine path finding operations
	 */
	private void startRoutine(){
		/* TODO check if app called by NFC and get infos, otherwise get example Information */
		HashMap<String, GeoNode> gPtMap = getInitialLocationInfo();

		processLocationUpdate(gPtMap);

		/* Starts an async thread that handles parsing of the OSM-file */
		recalculateRouteTask();
	}

	/**
	 * Checks whether App was called by NFC Tag and gets the Information of the target Node
	 * http://developer.android.com/guide/topics/connectivity/nfc/nfc.html
	 * @return ArrayList of GeoNodes holding firstly the Location of the NFC Tag and secondly the
	 * target location with ID -1. If smartphone doesnt support NFC the returned 
	 * GeoNode's ID will hold the value -2. If the app wasn't called by a NFC Tag it will hold
	 * the value -3.
	 */
	private HashMap<String, GeoNode> getInitialLocationInfo() {
		NdefMessage[] msgs;
		Intent intent= this.getIntent();
		Log.d(TAG, "BLA: " + intent.getAction().length());
		HashMap<String, GeoNode> gPtMap = new HashMap<String, GeoNode>();

		if (NfcAdapter.getDefaultAdapter(this) == null) {
			Log.d(TAG,"No nfcAdapter");
			//TODO FOR TESTING PURPOSING THE NEXT LINE WAS ADDED. DELETE THIS AND USE COMMENTED LINES
			gPtMap.put("Start", new GeoNode(-1, 26.51066, 80.24092));  //curr Location
			//		list.add(new GeoNode(-1, 26.511978, 80.235739)); // target near shop c
			gPtMap.put("Target", new GeoNode(-1, 26.504662, 80.233486)); // target near HC

			//Example Markers
//			gPtMap.put("Mess", new GeoNode(-1, 26.509904, 80.234806)); // mess
//			gPtMap.put("Canteen", new GeoNode(-1, 26.508147, 80.235911)); // canteen

			return gPtMap;
		}
		else if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
			Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
				//				showLogsText.setText(msgs.toString()); 
				Log.d(TAG, "NFC TAG USED: INPUT = " + msgs.toString());
				// TODO process text here
				/* TODO what happens if app is getting called another time. It should load new 
				 * markers, nothing more. Is there a onX() method which is called at a NFC call
				 * and can just update the markers, position and target?
				 */
				return getNFCInfo(rawMsgs);
			}
		}
		else {
			//			showLogsText.setText("No intent");
			Log.d(TAG, "NO NFC INTENT");
			//TODO FOR TESTING PURPOSING THE NEXT LINE WAS ADDED. DELETE THIS AND USE COMMENTED LINES
			gPtMap.put("Start", new GeoNode(-1, 26.51066, 80.24092));  //curr Location
			//		list.add(new GeoNode(-1, 26.511978, 80.235739)); // target near shop c
			gPtMap.put("Target", new GeoNode(-1, 26.504662, 80.233486)); // target near HC

			//Example Markers
//			gPtMap.put("NoNFCIntent", new GeoNode(-1, 26.509904, 80.234806)); // mess
//			gPtMap.put("NoNFCIntent2", new GeoNode(-1, 26.508147, 80.235911)); // canteen

			return gPtMap;
		}
		return gPtMap;


	}

	/**
	 * Process update of route if manally target or start location was changed
	 */
	public void processLocationUpdate() {
		userNotonTrack = true;
		isCURATRunning = false;
	}
	
	/**
	 * If NFC Tag was recognized and new Location data is available, this Method will
	 * update the position of the user (there might be no gps available) and, if the tag held
	 * that information, the target position. It will also call "addLocalAreaMarker(HashMap<String, GeoNode>),
	 * which will add Markers on the map, if the tag held that information.
	 * @param gPtMap
	 */
	private void processLocationUpdate(HashMap<String, GeoNode> gPtMap) {
		if (gPtMap != null && gPtMap.size() > 0){
			if (gPtMap.containsKey("Start")) {
				Log.d(TAG, "lat: " + gPtMap.get("Start").getGPt().getLatitude());
				currLocNode = gPtMap.get("Start");
			}
			if (gPtMap.containsKey("Target")){
				targetLocNode = gPtMap.get("Target");
			}
			userNotonTrack = true;
			isCURATRunning = false;
			addLocalAreaMarker(gPtMap);
		}
		else {
			Log.d(TAG, "gPtMap is empty, because NFC Tag intent couldnt be read.");
		}

	}

	/**
	 * Takes the Parcable-Array of a NFC Tag and parses the Information
	 * of the NFC Tag into a HasMap
	 * @param rawMsgs
	 * @return
	 */
	private HashMap<String, GeoNode> getNFCInfo(Parcelable[] rawMsgs){
		HashMap<String, GeoNode> gPtMap = new HashMap<String, GeoNode>();
		NdefMessage[] msgs;
		String[] splitString;
		if (rawMsgs != null) {
			msgs = new NdefMessage[rawMsgs.length];
			for (int i = 0; i < rawMsgs.length; i++) {
				// String format:  Name,Lat,Lon;Name,Lat,Lon;...
				msgs[i] = (NdefMessage) rawMsgs[i];
				splitString = msgs[i].toString().split(",");
				if (splitString != null){
					if (splitString[0] == "S") splitString[0] = "Start";
					if (splitString[0] == "T") splitString[0] = "Target";
					gPtMap.put(splitString[0], new GeoNode(i*-1, Integer.parseInt(splitString[1]), Integer.parseInt(splitString[2])));
					//TODO parseLong? => may only use proximity regarding to Integer as far as i know
				}
			}
			Log.d(TAG, msgs.toString()); 
		}
		return gPtMap;
	}

	/**
	 * Adds Markers passed by the NFC Tag for the local area on map
	 */
	private void addLocalAreaMarker(HashMap<String, GeoNode> gPtMap){
		// Delete old marker
		if (gPtLocalAreaMarkers != null && gPtLocalAreaMarkers.size() > 0){
			for (int i = 0; i < gPtLocalAreaMarkers.size(); i++){
				mMapView.getOverlays().remove(gPtLocalAreaMarkers.get(i));
			}
			gPtLocalAreaMarkers.clear();
		}
		// Add new marker
		MyMarker marker;
		Iterator<String> it = gPtMap.keySet().iterator();
		while (it.hasNext()){
			String str = it.next();
			if (!str.equals("Start") && !str.equals("Target")) {
				marker = new MyMarker(mMapView, this); 
				marker.setId(gPtMap.get(str).getId());
				marker.setPosition(gPtMap.get(str).getGPt());
				marker.setTitle(str);
				marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
				mMapView.getOverlays().add(marker);
				marker.setIcon(getResources().getDrawable(R.drawable.marker_node));
				gPtLocalAreaMarkers.add(marker);
			}
		}
		mMapView.invalidate();
	}

	/**
	 * Adds start and target marker to current pathlist on map
	 * @param isStart
	 * @param info
	 */
	private void addStartTargetMarker(boolean isStart, String info){
		/* Sets initial marker for user location */
		if (isStart){
			startMarker = new MyMarker(mMapView, this);
			startMarker.setPosition(polyLineRoad.getPoints().get(0));
			startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
			mMapView.getOverlays().add(startMarker);
			mMapView.invalidate();
			startMarker.setIcon(getResources().getDrawable(R.drawable.marker_departure));
			startMarker.setTitle("Location: " + (info.equals("")? "unknown" : info));
			gPtStartEndMarkers.add(startMarker);
			
			
			// set new marker here
			gpsMarker.setPosition(new GeoPoint(currLocNode.getLat(), currLocNode.getLon()));
			gpsMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
			mMapView.getOverlays().add(gpsMarker);
			mMapView.invalidate();
			gpsMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
			gpsMarker.setTitle("Your location");
		}
		else {
			targetMarker = new MyMarker(mMapView, this);
			targetMarker.setPosition(polyLineRoad.getPoints().get(polyLineRoad.getPoints().size()-1));
			targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
			mMapView.getOverlays().add(targetMarker);
			mMapView.invalidate();
			targetMarker.setIcon(getResources().getDrawable(R.drawable.marker_destination));
			int timeToGo = (int) ((Integer.parseInt(info) / 1.4) / 60);
			targetMarker.setTitle("Target Location: " + (closestTargetLocNode.getName().equals("")? "unknown" : closestTargetLocNode.getName()) +  "\n" + "Target distance: " + info + "m, " + timeToGo + " Min. to go."); // ~ 1.4 m/s
			gPtStartEndMarkers.add(targetMarker);
			
		}
	}

	/**
	 * Sets the initial marker for the GPS position
	 */
	private void setInitialUserLocationMarker() {
		// Set up location service for gps tracking and check for locationManager
		checkLocationManager();
		gpsMarker = new MyMarker(mMapView, this);

		//get last known location from GPS only
		lastLocation = locationManager.getLastKnownLocation(
				LocationManager.GPS_PROVIDER);
		if(lastLocation != null){
			updateLoc(lastLocation);
		}
		else { // try to get lastLocation by network
			lastLocation = locationManager.getLastKnownLocation(
					LocationManager.NETWORK_PROVIDER);
			if(lastLocation != null){
				updateLoc(lastLocation);
			}
		}

		/* Sets initial marker for user location */
		if (lastLocation != null) gpsMarker.setPosition(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
		else gpsMarker.setPosition(new GeoPoint(26.508790, 80.229495));
		gpsMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
		mMapView.getOverlays().add(gpsMarker);
		mMapView.invalidate();
		gpsMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
		gpsMarker.setTitle("Start point");

	}

	/**
	 * Check whether locationManager are available
	 */
	private void checkLocationManager() {
		boolean gps_enabled = false,network_enabled = false;
		if(locationManager==null)
			locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		try{
			gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		}catch(Exception ex){}
		try{
			network_enabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		}catch(Exception ex){}

		if(!gps_enabled && !network_enabled){
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Location not found");  // GPS not found
			builder.setMessage("No Locationmanager available. For this App to work, a LocationManager is needed."
					+ "Do you want to activate it now?"); // Want to enable?
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialogInterface, int i) {
					startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
				}
			});
			builder.setNegativeButton("No", null);
			builder.create().show();
			return;

		}

	}

	/**
	 * Heart of the Activity. This asynchonous task checks every 10 seconds, if the user is still on the track and, if not, 
	 * recalculates the route. Its job is also to calculate the ini
	 * @return
	 */
	private AsyncTask<Void, LinkedList<GeoNode>, Void> recalculateRouteTask(){

		if (recalculateRouteTask == null || (recalculateRouteTask != null && !(recalculateRouteTask.getStatus() == AsyncTask.Status.RUNNING))){
			AsyncTask<Void, LinkedList<GeoNode>, Void> task = new AsyncTask<Void, LinkedList<GeoNode>, Void>() {

				@Override
				protected void onPreExecute() {
					isCURATRunning = true;
					taskRunning = true;
				}

				@Override
				protected Void doInBackground(Void... arg0) {
					while (taskRunning){

						AssetManager assetMgr = getAssets();
						Log.d(TAG, "route is being recalculated, cuz user is too far from it");
						try {
							// parse campusMap
							InputStream in = assetMgr.open("campusFullCompressed.osm");
							ParseOSMXMLFile parseOSMXML = new ParseOSMXMLFile(in);

							// get the nodesList 
							// TODO check that a map is only read once, maybe with passing a hashtag to it
							// or checking features, etc.
							if (nodes == null)	nodes = parseOSMXML.parse();
							if (currLocNode != null) {
								Log.d(TAG, "currentNodeLatitude = " + currLocNode.getGPt().getLatitude());
								// even if gps is not available, currLocation holds the Tag's position
								// use gps as current location, calculate the closest GeoNode 
								// that is available in the list of Nodes of the campus map
								// so now we can be sure our nodes are in the NodesList "nodes"
								// TODO If current location is farther away than 20 meters from stated NFC Tag position, use thie tag position
								closestCurrLocNode = parseOSMXML.getClosestGeoNode(nodes, new GeoPoint(currLocNode.getLat(), currLocNode.getLon())); // new GeoPoint(currLocNode.getLat(), currLocNode.getLon())
								closestTargetLocNode = parseOSMXML.getClosestGeoNode(nodes, new GeoPoint(targetLocNode.getLat(), targetLocNode.getLon()));
								//							Log.d(TAG, "currLocNode != null!!! => closestCurrLocNode = " + closestCurrLocNode.getLat());
								//							Log.d(TAG, "currLocNode != null!!! => closestTargetLocNode = " + closestTargetLocNode.getLat());
							}


							// compute the path, get the ordered list of nodes
							RoutingAlgorithms aStar = new RoutingAlgorithms(nodes, currLocNode, closestCurrLocNode, closestTargetLocNode);
							LinkedList<GeoNode> temp = aStar.aStarSearch(); // => send to onProgressUpdate(), draw on map, delete of map if new path is calculated

							// publish the progress to UI
							publishProgress(temp);

							isCURATRunning = true;


						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}


						while (isCURATRunning){
							Log.d(TAG, "User Allignment to route is being checked");
							if (pathList != null && pathList.size() > 0 && !userOnTrack()){

								isCURATRunning = false;
							}
							try {
								Thread.sleep(10000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}

					return null;


				}

				/* checks, if user's current location is at least within 10meters of
				 * a node inside the pathList */
				private boolean userOnTrack() {
					for (int i = 0; i < pathList.size(); i++){
						if (currLocNode.getGPt().distanceTo(pathList.get(i).getGPt()) < 10) {
							return true;
						}
					}
					return false;
				}


				protected void onProgressUpdate(LinkedList<GeoNode>...a){
					Log.d(TAG, "5");
					if (a[0] == null) Log.d(TAG, "No path to target found!!");
					else {
						//						Log.d(TAG, "Size of pathList in onProgressUpdate = " + a[0].size());
						pathList = a[0];
						//						Log.d(TAG, "PathList.size() = " + pathList.size());
						addPolyLineRoute(pathList);
					}
					//			addMarkerToPosition(closestCurrLocNodeMarker, closestCurrLocNode.getGPt(), "Start");

					//					addMarkerToPosition(closestTargetLocNodeMarker, closestTargetLocNode.getGPt(), "Target");

					//			addPathoverlay(nodes, 683475928);
					//			Log.d(TAG, "SparseArrayLength: " + nodes.size());
					//			showLogsText.setText("Closest Node: (" +  a[0].getLat() + ", " + a[0].getLon() + ")");
					//			Log.d(TAG, "Closest Node: (" +  a[0].getLat() + ", " + a[0].getLon() + ")");
					//			addMarkerToPosition(a[0].getGPt(), "Closest Node");

				}

				@Override
				protected void onPostExecute(Void result) {
					/* TODO Secondly rerun the path algo if the user is too far away from calculated route,
					 * in this case update currLocNode */
					if (isInitPhase) {
						isInitPhase = false;
						//						Log.d(TAG, "checkUserRouteAllignmentTask");
						//						checkUserRouteAllignmentTask();
					}
				}
			};
			task.execute((Void[])null);
		}
		else Log.d(TAG, "Task still running or not initialized");
		return recalculateRouteTask;
	}
	
	public void setTargetLocNode(GeoNode targetLocNode) {
		this.targetLocNode = targetLocNode;
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if(NfcAdapter.getDefaultAdapter(this) != null) setupForegroundDispatch(this, NfcAdapter.getDefaultAdapter(this));
		// update location after app was paused
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, myLocationListener);
		locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, myLocationListener);
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		locationManager.removeUpdates(myLocationListener);
		stopForegroundDispatch(this, NfcAdapter.getDefaultAdapter(this));
	}

	/**
	 * Takes aLinkedList of GeoPoints, draws the path in that order on the map
	 * @param geoNodes LinkedList of GeoPoints
	 */
	private void addPolyLineRoute(LinkedList<GeoNode> geoNodes){


		// TODO NEXT: location spoofer, polylines with tags, A star, removal of markers not working, offline maps


		LinkedList<GeoPoint> geoPoints = new LinkedList<GeoPoint>();
		String tag = "";
		Drawable icon = getResources().getDrawable(R.drawable.marker_node);
		GeoNode lastGeoNode = new GeoNode(-5, new GeoPoint(0, 0));
		long distance = 0;
		for (int i = 0; i < geoNodes.size(); i++){
			geoPoints.add(geoNodes.get(i).getGPt());
			if (i >= 1 && i < geoNodes.size()-1){ //dont calculate from gps pos, but from startNode
				distance += geoNodes.get(i).getGPt().distanceTo(geoNodes.get(i+1).getGPt());
			}

			/* add markers whenever the tag changes in the list of geoNodes */
			//			if (geoNodes.get(i).getTag().equals(tag)){
			//				lastGeoNode = geoNodes.get(i);
			//			}
			//			else if (!geoNodes.get(i).getTag().equals("")){
			//				// new marker at lastGeoNode-Position, add tag in title of marker
			//				Marker tempMarker = new Marker(mMapView);
			//				tempMarker.setPosition(lastGeoNode.getGPt());
			//				tempMarker.setTitle(tag);
			//				tempMarker.setIcon(icon);
			//				geoPointMarkers.add(tempMarker);
			//				mMapView.getOverlays().add(tempMarker);
			//				lastGeoNode = geoNodes.get(i);
			//				tag = geoNodes.get(i).getTag();
			//			}
		}

		if (polyLineRoad == null || polyLineRoadToFirstNode == null){
			polyLineRoad = new Polyline(mMapView.getContext());
			polyLineRoadToFirstNode = new Polyline(mMapView.getContext());
			polyLineRoad.setInfoWindow(infoWindow);
		}
		if (geoPoints != null && geoPoints.size() > 1){

			/* remove old Markers and Polylines */
			mMapView.getOverlays().remove(polyLineRoad); // remove old polyline
			mMapView.getOverlays().remove(polyLineRoadToFirstNode);

			if (gPtStartEndMarkers != null && gPtStartEndMarkers.size() > 0){
				for (int i = 0; i < gPtStartEndMarkers.size(); i++){
					mMapView.getOverlays().remove(gPtStartEndMarkers.get(i));
				}
				gPtStartEndMarkers.clear();
			}


			LinkedList<GeoPoint> tempGeoPointList = new LinkedList<GeoPoint>();
			tempGeoPointList.add(geoPoints.get(0));
			tempGeoPointList.add(geoPoints.get(1));
			polyLineRoadToFirstNode.setPoints(tempGeoPointList);
			polyLineRoadToFirstNode.setWidth(5.0f);
			polyLineRoadToFirstNode.setColor(Color.RED);
			geoPoints.removeFirst();
			polyLineRoad.setPoints(geoPoints);
			mMapView.getOverlays().add(polyLineRoadToFirstNode);
			mMapView.getOverlays().add(polyLineRoad);

			//	Log.d(TAG, "ID = " + (geoNodes.get(1).getId() == 2136649409) + " and Name = " + geoNodes.get(1).getName()); 
			addStartTargetMarker(true, geoNodes.size() > 1? geoNodes.get(1).getName() : "");
			addStartTargetMarker(false, distance + "");
			int timeToGo = (int) ((distance / 1.4) / 60);
			showLogsText.setText("Distance to target location: " + distance + "m, " + timeToGo + " Min. to go."); // ~ 1.4 m/s
			directionsText.setText("Target location: " + (!closestTargetLocNode.getName().equals("")? closestTargetLocNode.getName() : "unknown" ));
			mMapView.invalidate();
		}

	}

	/* Updates the location and passes it to setOverlayLoc() */
	private void updateLoc(Location loc){

		/* First update the location marker of User */
		if (lastLocation != null){

			// TODO IF NEW LOC OUTSIDE BOUNDARIES OF MAP => currLocNode = location of tag
			// FOR TESTING:
			currLocNode = new GeoNode(-1, loc.getLatitude(), loc.getLongitude());

			//mMapController.setCenter(locGeoPoint);
			gpsCoordsText.setText("GPS/NET pos.: (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");

			// set new marker
			gpsMarker.setPosition(new GeoPoint(currLocNode.getLat(), currLocNode.getLon()));
			gpsMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
			mMapView.getOverlays().add(gpsMarker);
			mMapView.invalidate();
			gpsMarker.setIcon(getResources().getDrawable(R.drawable.ic_launcher));
			gpsMarker.setTitle("Your location");

			//			if (isInitPhase) {
			//				recalculateRouteTask();
			//			}


		}


	}

	/*
	 * If myLocationListener receives an update, then it calls
	 * updateLoc and passes the current position over 
	 */
	private LocationListener myLocationListener
	= new LocationListener(){

		@Override
		public void onLocationChanged(Location location) {
			// TODO Auto-generated method stub
			//			Log.d(TAG, "Location changed: (" +  location.getLatitude() + ", " + location.getLongitude() + ")");
			updateLoc(location);
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// TODO Auto-generated method stub

		}

	};

	/* Will only be called near GeoPoints. CURRENTLY NOT USED */

	/**
	 * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
	 * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][]{};

		// Notice that this is the same filter as in our manifest.
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check your mime type.");
		}

		adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
	}

	/**
	 * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
	 * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
	 */
	public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}

	public static final String MIME_TEXT_PLAIN = "text/plain";

	/**
	 * Handle NFC Intent
	 * @param intent
	 */
	private void handleIntent(Intent intent) {
		String action = intent.getAction();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Log.d(TAG, "NDEF DISCOVERED");
			String type = intent.getType();
			if (MIME_TEXT_PLAIN.equals(type)) {

				Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
				convertNFCToString(tag);
				//				new NdefReaderTask().execute(tag);

			} else {
				Log.d(TAG, "Wrong mime type: " + type);
			}
		} else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

			// In case we would still use the Tech Discovered Intent
			Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			String[] techList = tag.getTechList();
			String searchedTech = Ndef.class.getName();

			for (String tech : techList) {
				if (searchedTech.equals(tech)) {
					convertNFCToString(tag);
					//					new NdefReaderTask().execute(tag);
					break;
				}
			}
		}
	}

	/**
	 * Converts NFC Tag into a String
	 * @param tag
	 */
	private void convertNFCToString(Tag tag){

		Ndef ndef = Ndef.get(tag);
		if (ndef == null) {

			// NDEF is not supported by this Tag. 

		}

		NdefMessage ndefMessage = ndef.getCachedNdefMessage();

		NdefRecord[] records = ndefMessage.getRecords();
		for (NdefRecord ndefRecord : records) {
			Log.d(TAG, "NDEFrecord");
			if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
				try {
					String result =  readText(ndefRecord);
					if (result != null) {
						Log.d(TAG, "Read content: " + result);
						parseNFCToGeoNodes(result);
					}
				} catch (UnsupportedEncodingException e) {
					Log.e(TAG, "Unsupported Encoding", e);
				}
			}
		}

	}

	/**
	 * Takes the resulting String of the NFC Tag and parses the information into 
	 * a HashMap<String, GeoNode>
	 * @param result
	 */
	private void parseNFCToGeoNodes(String result){
		HashMap<String, GeoNode> gPtMap = new HashMap<String, GeoNode>();
		String[] splitString;
		String[] splitStringTwo;
		splitString = result.split(";");
		Log.d(TAG, "splitString = " + splitString.length);
		for (int i = 0; i < splitString.length; i++) {

			splitStringTwo = splitString[i].toString().split(",");
			Log.d(TAG, "splitStringTwo = "+splitStringTwo.length);
			if (splitStringTwo.length == 3){
				if (splitStringTwo != null){
					if (splitStringTwo[0].equals("S")) splitStringTwo[0] = "Start";
					if (splitStringTwo[0].equals("T")) splitStringTwo[0] = "Target";
					gPtMap.put(splitStringTwo[0], new GeoNode(i*-1, Double.parseDouble(splitStringTwo[1]), Double.parseDouble(splitStringTwo[2])));
					//TODO parseLong? => may only use proximity regarding to Integer as far as i know
				}

			}

			processLocationUpdate(gPtMap);
		}
	}

	/**
	 * Reads a NdefRecord and returns the contained String
	 * @param record
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String readText(NdefRecord record) throws UnsupportedEncodingException {
		/*
		 * See NFC forum specification for "Text Record Type Definition" at 3.2.1 
		 * 
		 * http://www.nfc-forum.org/specs/
		 * 
		 * bit_7 defines encoding
		 * bit_6 reserved for future use, must be 0
		 * bit_5..0 length of IANA language code
		 */
		Log.d(TAG, "readText called");
		byte[] payload = record.getPayload();

		// Get the Text Encoding
		String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

		// Get the Language Code
		int languageCodeLength = payload[0] & 0063;

		// String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
		// e.g. "en"

		// Get the Text
		return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
	}

}
