package iitk.mc.nfcroutingosm.snippets;

public class SnippetsNFCRoutingOSM {

	/**
	 * Code Snippets of MainActivity.java
	 */
	
	//	private void addPathoverlay(LongSparseArray<GeoNode> nodeList, long start) {
	//		/* Adds a pathoverlay for testing purpose*/
	//
	//		//addNavigatedRoute(new GeoPoint(26.508790, 80.229495));
	//		//addPolyLine(new GeoPoint(26.508790, 80.229495));
	//
	//
	//		GeoNode currentNode = nodeList.get(start); 
	//		LinkedList<GeoPoint> testList = new LinkedList<GeoPoint>();
	//		while (currentNode.getNextNodes().size() > 0){
	//			GeoNode nextNode = currentNode.getNextNodes().keySet().iterator().next();
	//			testList.add(nextNode.getGPt());
	//			
	//			currentNode = nextNode;
	//		}
	//		addPolyLineRoute(testList);
	//
	//		//		LinkedList<GeoPoint> testList = new LinkedList<GeoPoint>();
	//		//		if (lastLocation != null) testList.add(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
	//		//		testList.add(new GeoPoint(26.508790, 80.229495));
	//		//		testList.add(new GeoPoint(26.515990, 80.229795));
	//		//		testList.add(new GeoPoint(26.519990, 80.238795));
	//		//		addPolyLineRoute(testList);
	//
	//	}
	
	//	/**
	//	 * Add a Path from the User's current position to the target position, following the road
	//	 * @param target
	//	 */
	//	private void addPolyLine(GeoPoint target){
	//		ArrayList<GeoPoint> testList = new ArrayList<GeoPoint>();
	//		testList.add(new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude()));
	//		testList.add(target);
	//		Polyline test = new Polyline(mMapView.getContext());
	//		test.setPoints(testList);
	//		mMapView.getOverlays().add(test);
	//		mMapView.invalidate();
	//
	//		//TODO should be implemented in linkedList / ArrayList
	//		// the overlay should be a field, to be able to reference and remove it, if the path is refreshed
	//	}

	//	/**
	//	 * Adds a straight line from the user's current position to the target position
	//	 * @param target
	//	 */
	//	private void addNavigatedRoute(GeoPoint target){
	//		RoadManager roadManager = new OSRMRoadManager();
	//		ArrayList<GeoPoint> waypoints = new ArrayList<GeoPoint>();
	//		waypoints.add(new GeoPoint(lastLocation));
	//		waypoints.add(target);
	//
	//		// retreive the road between those points
	//		Road road = roadManager.getRoad(waypoints);
	//
	//		// build a Polyline with the route shape:
	//		Polyline roadOverlay = RoadManager.buildRoadOverlay(road, this);
	//
	//		// Add this Polyline to the overlays of the map:
	//		mMapView.getOverlays().add(roadOverlay);
	//		mMapView.invalidate();
	//	}
	

	/* Adds the new location geopoint to our ItemizedIconOverlay-Array 
	private void setOverlayLoc(Location overlayloc){

		GeoPoint overlocGeoPoint = new GeoPoint(overlayloc);
		//---
		overlayItemArray.clear();

		OverlayItem newMyLocationItem = new OverlayItem(
				"My Location", "My Location", overlocGeoPoint);
		overlayItemArray.add(newMyLocationItem);
		//---
	} */

	/*
	 * Customized ItemizedIconOverlay to draw the location on the map
	 */
	/*private class MyItemizedIconOverlay extends ItemizedIconOverlay<OverlayItem>{

		public MyItemizedIconOverlay(
				List<OverlayItem> pList,
				org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener<OverlayItem> pOnItemGestureListener,
				ResourceProxy pResourceProxy) {
			super(pList, pOnItemGestureListener, pResourceProxy);
			// TODO Auto-generated constructor stub
		}

		@Override
		public void draw(Canvas canvas, MapView mapview, boolean arg2) {
			// TODO Auto-generated method stub
			super.draw(canvas, mapview, arg2);

			if(!overlayItemArray.isEmpty()){

				//since only one item in it in current implementation, get(0) gets
				// the location marker
				GeoPoint in = overlayItemArray.get(0).getPoint();

				Point out = new Point();
				mapview.getProjection().toPixels(in, out);

				Bitmap bm = BitmapFactory.decodeResource(getResources(), 
						R.drawable.ic_launcher);
				canvas.drawBitmap(bm, 
						out.x - bm.getWidth()/2,  //shift the bitmap center
						out.y - bm.getHeight()/2,  //shift the bitmap center
						null);

			}
		}

		@Override
		public boolean onSingleTapUp(MotionEvent event, MapView mapView) {
			// TODO Auto-generated method stub
			//return super.onSingleTapUp(event, mapView);
			return false;
		}
	} */
	
}
