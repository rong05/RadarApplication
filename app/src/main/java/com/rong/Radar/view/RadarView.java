package com.rong.Radar.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class RadarView extends View {
    private static final int count = 10; // 数据个数
    private static final  int angleSize = 12;
    private static final float angle = (float) (Math.PI * 2 / angleSize);
    private float radius; // 网格最大半径
    private int centerX; // 中心X
    private int centerY; // 中心Y
    private List<PointBuffer> pointBufferList;
//    private double[] data = { 100, 60, 60, 60, 100, 50, 30, 70 }; // 各维度分值
    private float maxValue = 100; // 数据最大值
    private Paint mainPaint; // 雷达区画笔
    private Paint valuePaint; // 数据区画笔
//    private Paint textPaint; // 文本画笔
    private int screenWidth;
    private int ten;
//    private int forty;
    public RadarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RadarView(Context context) {
        super(context);
        init();
    }

    // 初始化
    private void init() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenWidth = dm.widthPixels;
        pointBufferList = new ArrayList<>();


        ten = (int) (0.0094*screenWidth);
//        forty = (int) (0.036*screenWidth);
//        count = Math.min(data.length, titles.length);

        mainPaint = new Paint();
        mainPaint.setAntiAlias(true);
        mainPaint.setColor(Color.WHITE);
        mainPaint.setStyle(Paint.Style.STROKE);

        valuePaint = new Paint();
        valuePaint.setAntiAlias(true);
        valuePaint.setColor(Color.RED);
        valuePaint.setStyle(Paint.Style.FILL_AND_STROKE);

//        textPaint = new Paint();
//        textPaint.setTextSize(forty);
//        textPaint.setStyle(Paint.Style.FILL);
//        textPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        radius = Math.min(h, w) / 2 * 0.9f;
        centerX = w / 2;
        centerY = h / 2;
        postInvalidate();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        setBackgroundColor(Color.GRAY);
        drawCircle(canvas);
        drawLines(canvas);
//        drawText(canvas);
        drawRegion(canvas);
    }

    /**
     * 绘制圆
     */
    private void drawCircle(Canvas canvas) {
        Path path = new Path();
        float r = radius / (count);
        for (int i = 2; i <= count; i += 2) {
            float curR = r * i;
            for (int j = 1; j <= count; j += 2) {
                if (j == 0) {
                    path.moveTo(centerX + curR, centerY);
                }
            }
            canvas.drawCircle(centerX, centerY, curR, mainPaint);
        }
    }

    /**
     * 绘制直线
     */
    private void drawLines(Canvas canvas) {
        Path path = new Path();
        for (int i = 0; i < angleSize; i++) {
            path.reset();
            path.moveTo(centerX, centerY);
            float x = (float) (centerX + radius * Math.cos(angle * i));
            float y = (float) (centerY + radius * Math.sin(angle * i));
            path.lineTo(x, y);
            canvas.drawPath(path, mainPaint);
        }
    }

    /**
     * 绘制区域
     *
     * @param canvas
     */
    private void drawRegion(Canvas canvas) {
        if(pointBufferList == null || pointBufferList.isEmpty()){
            return;
        }
        Path path = new Path();
        valuePaint.setAlpha(127);
        final int size = pointBufferList.size();
        for (int i = 0; i < size; i++) {
            final PointBuffer pointBuffer = pointBufferList.get(i);
            double percent = pointBuffer.radius / maxValue;
            float x = (float) (centerX + radius * Math.cos(angle * i) * percent);
            float y = (float) (centerY + radius * Math.sin(angle * i) * percent);
            path.moveTo(x, y);
            valuePaint.setColor(Color.RED);
            canvas.drawCircle(x, y, ten, valuePaint);
        }
        valuePaint.setColor(Color.RED);
        valuePaint.setAlpha(127);
        valuePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(path, valuePaint);
    }

    public void setPointBufferList(List<PointBuffer> pointBufferList){
        this.pointBufferList = pointBufferList;
        postInvalidate();
    }

    public float getMaxValue() {
        return maxValue;
    }

    // 设置最大数值
    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
    }

    // 设置蜘蛛网颜色
    public void setMainPaintColor(int color) {
        mainPaint.setColor(color);
    }


    // 设置覆盖局域颜色
    public void setValuePaintColor(int color) {
        valuePaint.setColor(color);
    }
}
