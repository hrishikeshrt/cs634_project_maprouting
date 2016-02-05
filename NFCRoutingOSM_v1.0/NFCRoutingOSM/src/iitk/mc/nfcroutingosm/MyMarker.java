package iitk.mc.nfcroutingosm;

import iitk.mc.nfcroutingosm.datastruct.GeoNode;

import org.osmdroid.bonuspack.overlays.Marker;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;

public class MyMarker extends Marker {

	MainActivity main;
	Long id = 0L;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public MyMarker(MapView mapView) {
		super(mapView);
		// TODO Auto-generated constructor stub
	}

	public MyMarker(MapView mapView, MainActivity main) {
		super(mapView);
		this.main = main;
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean onDoubleTap(final MotionEvent e, final MapView mapView) {
//		boolean touched = hitTest(e, mapView);
//		if (touched){
//			if (main != null){
//				GeoNode node = new GeoNode(id, this.getPosition());
//				node.setName(this.getTitle());
//				main.setTargetLocNode(node);
//				main.processLocationUpdate();
//			}
//			Log.d("TEST", "Marker touched");
//		}
		return false;
	}

	@Override 
	public boolean onLongPress(final MotionEvent event, final MapView mapView) {

		boolean touched = hitTest(event, mapView);
		if (touched){
			if (main != null){
				GeoNode node = new GeoNode(id, this.getPosition());
				node.setName(this.getTitle());
				main.setTargetLocNode(node);
				main.processLocationUpdate();
				Vibrator v = (Vibrator) main.getSystemService(Context.VIBRATOR_SERVICE);
				 // Vibrate for 500 milliseconds
				 v.vibrate(500);
				Log.d("TEST", "LONG PRESSED");
			}
			if (mDraggable){
				//starts dragging mode:
				mIsDragged = true;
				closeInfoWindow();
				if (mOnMarkerDragListener != null)
					mOnMarkerDragListener.onMarkerDragStart(this);
				moveToEventPosition(event, mapView);
			}
		}
		return touched;
	}

}
