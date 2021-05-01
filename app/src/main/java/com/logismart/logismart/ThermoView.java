package com.logismart.logismart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;

public class ThermoView extends View {
    Context context;

    float value; // temperature value

    Rect progressRectDst;
    Rect barRectDst;

    int textColor;

    ArrayList<OnMyChangeListener> listeners;

    public ThermoView(Context context) {
        super(context);
        this.context = context;
        init(null);
    }

    public ThermoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    public ThermoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        progressRectDst = new Rect(10, 10, 300, 300);
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

    @Override
    protected void onDraw (Canvas canvas) {
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(50);
        paint.setAntiAlias(true);

        int size = 700;
        int left = 200;
        int top = 100;

        RectF arc = new RectF(left, top,left+size,top+size);

        paint.setColor(Color.RED);
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawArc(arc, 150, 30, false, paint);
        canvas.drawArc(arc, 0, 30, false, paint);

        paint.setColor(Color.GREEN);
        paint.setStrokeCap(Paint.Cap.BUTT);
        canvas.drawArc(arc, 180, 180, false, paint);

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(size/2+left, top, 25, paint);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        canvas.drawCircle(size/2+left, top, 20, paint);

        paint.setTextSize(120);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setColor(textColor);
        canvas.drawText(String.valueOf(value)+"ºC", left+size/2, top+size/2+30, paint);
    }
}
