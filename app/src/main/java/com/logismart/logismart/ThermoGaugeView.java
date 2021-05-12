package com.logismart.logismart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class ThermoGaugeView extends View {
    Context context;

    Rect barRectDst;

    float circlex;
    float circley;

    int textColor;

    ArrayList<OnMyChangeListener> listeners;

    public ThermoGaugeView(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public ThermoGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    public ThermoGaugeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        barRectDst = new Rect(10,10,300,300);

        if(attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TemperatureView);
            textColor = a.getColor(R.styleable.TemperatureView_ccolor, Color.BLACK);
        }
        listeners = new ArrayList<>();
    }

    public void setOnMyChangeListener(OnMyChangeListener listener) {
        listeners.add(listener);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = 0;
        int height = 0;

        if (widthMode == MeasureSpec.AT_MOST) {
            width = 1100;
        } else if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            height = 800;
        } else if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        }

        setMeasuredDimension(width, height);
    }

    public void changeValueEvent(float value) {
        invalidate();
        for (OnMyChangeListener listener : listeners) {
            listener.onChange(value);
        }
    }

    @Override
    protected void onDraw (Canvas canvas) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);

        int size = 700;
        int left = 200;
        int top = 100;

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size/2+left, top, 25, paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        canvas.drawCircle(size/2+left, top, 20, paint);

    }
}
