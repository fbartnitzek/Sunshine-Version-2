package com.example.android.sunshine.app.data;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

/**
 * Created by frank on 31.07.15.
 */
public class MyView extends View {
    private static final String LOG_TAG = MyView.class.getName();
    private double mDirection;
    private String mText;
    private boolean mUpdated = false;
    private Paint mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Context mContext;

    public MyView(Context context) {
        super(context);
        mContext = context;
    }

    public MyView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public MyView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int desiredMin = 100;
        int wMode = MeasureSpec.getMode(widthMeasureSpec);
        int wSize = MeasureSpec.getSize(widthMeasureSpec);
        int hMode = MeasureSpec.getMode(heightMeasureSpec);
        int hSize = MeasureSpec.getSize(heightMeasureSpec);
        int myWidth = wSize;
        int myHeight = hSize;

        if (wMode == MeasureSpec.AT_MOST && myWidth<desiredMin){
            // wrap content anyhow ...
            myWidth = desiredMin;
        }

        if (hMode == MeasureSpec.AT_MOST && myHeight<desiredMin){
            // wrap content anyhow ...
            myHeight = desiredMin;
        }

        setMeasuredDimension(myWidth, myHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (!mUpdated){ //update not yet called
            return;
        }

        int h = getMeasuredHeight();
        int w = getMeasuredWidth();
        int r = w>h?h/2:w/2;

//        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5);
        mPaint.setColor(Color.BLACK);

        canvas.drawCircle(w / 2, h / 2, r, mPaint);

        mPaint.setColor(Color.RED);

        //both dimensions start at upper end - negative angle
        canvas.drawLine(
                w / 2,
                h / 2,
                (float) (w / 2 + r * Math.sin(-mDirection)),
                (float) (h / 2 + r * Math.cos(-mDirection)),
                mPaint);


        mPaint.setTextSize(w / 10);
        mPaint.setStrokeWidth(0);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);

        canvas.drawText(
                mText,
                w / 3,
                h - w / 10,
                mPaint);

        mPaint.setColor(Color.BLACK);
        canvas.drawText(
                "N",
                w / 2 - w/20 ,
                w / 10,
                mPaint);


    }

    public void update(double direction, String windSpeed){
        mUpdated = true;
        mDirection = direction;
        mText = windSpeed;

        AccessibilityManager accessibilityManager = (AccessibilityManager) mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (accessibilityManager != null && accessibilityManager.isEnabled()){
            sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED);
        }
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        event.getText().add(mText); //TODO: windspeed and direction fullqualified ...
        return true;
    }
}
