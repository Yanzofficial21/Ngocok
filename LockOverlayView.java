package com.hpcontrol;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

public class LockOverlayView extends FrameLayout {
    
    public LockOverlayView(Context context) {
        super(context);
        
        setBackgroundColor(Color.BLACK);
        
        TextView tv = new TextView(context);
        tv.setText("🔒 DEVICE LOCKED 🔒\n\nContact administrator to unlock");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(24);
        tv.setGravity(android.view.Gravity.CENTER);
        
        addView(tv, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        
        // Block all touches
        setOnTouchListener((v, event) -> true);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            // Force focus jika kehilangan
            setVisibility(View.VISIBLE);
            bringToFront();
        }
    }
}