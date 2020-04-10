package com.bohil.coin.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

public class BoundingBox extends View {
    private Rect rectangle;
    private Paint paint;
    int left, top, right, bottom;

    public BoundingBox(Context context, int left, int top, int right, int bottom){
        super(context);

        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;

        // Creates a rectangle to draw later
        rectangle = new Rect(left, top, right, bottom);

        // Creates the Paint and sets its color
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.STROKE);
    }
    @Override
    protected void onDraw(Canvas canvas){
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawRect(rectangle, paint);
    }
}
