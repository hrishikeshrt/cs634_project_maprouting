package iitk.mc.nfcroutingosm;


import org.osmdroid.views.MapView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Fix for zoom problem 
 * 		Source:
 * 		https://code.google.com/p/osmdroid/issues/detail?id=481&start=100
 * 		http://pastebin.com/2Zq78eSr
 * @author Benjamin Schiller
 *
 */


public final class FixedMapView extends MapView {

    private static final int IGNORE_MOVE_COUNT = 2;
    private int moveCount = 0;


    public FixedMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }   

	@Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:

                if (moveCount > 0) {
                    moveCount--;

                    return true;
                }   

                break;

            case MotionEvent.ACTION_POINTER_UP:
                moveCount = IGNORE_MOVE_COUNT;
                break;
        }   
        return super.onTouchEvent(ev);
    }   
}
