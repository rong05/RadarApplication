package com.rong.Radar.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
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
    private Paint textPaint; // 文本画笔
    private int screenWidth;
    private int ten;
    private int forty;
    private List<Point> pointList;

    private class Point{
        float x;
        float y;
        float radius =0;
        float angle = 0;

        public Point(float x, float y, float radius, float angle) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.angle = angle;
        }
    }
    public RadarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private Handler mainHandler;
    public void setHandler(Handler handler){
      mainHandler = handler;
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
        pointList = new ArrayList<>();


        ten = (int) (0.0054*screenWidth);
        forty = (int) (0.036*screenWidth);
//        count = Math.min(data.length, titles.length);

        mainPaint = new Paint();
        mainPaint.setAntiAlias(true);
        mainPaint.setColor(Color.WHITE);
        mainPaint.setStyle(Paint.Style.STROKE);

        valuePaint = new Paint();
        valuePaint.setAntiAlias(true);
        valuePaint.setColor(Color.RED);
        valuePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        notchList = new ArrayList<>();
        textPaint = new Paint();
        textPaint.setTextSize(forty);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor(Color.BLUE);
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
        drawRegion(canvas);
        drawText(canvas);
        if(mainHandler != null) {
            Message msg1 = Message.obtain();
            msg1.what = 0;
            msg1.obj = getAllArea();
            mainHandler.sendMessageDelayed(msg1, 500);
        }
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

    private PointBuffer lastPoint;
    private List<Float> notchList;
    private float allArea = 0;
    /**
     * 绘制区域
     *
     * @param canvas
     */
    private void drawRegion(Canvas canvas) {
        if(pointBufferList == null || pointBufferList.isEmpty()){
            return;
        }
        if(!notchList.isEmpty()){
            notchList.clear();
        }
        allArea = 0;
        Path path = new Path();
        valuePaint.setAlpha(127);
        final int size = pointBufferList.size();
        for (int i = 0; i < size; i++) {
            final PointBuffer pointBuffer = pointBufferList.get(i);
            final float radius  =  pointBuffer.radius;
            final float angle =  pointBuffer.angle;
            if(radius > 0) {
                double percent = (radius)* (this.radius /count);
                float x = (float) (centerX + Math.cos(angle * Math.PI / 180) * percent);
                float y = (float) (centerY + Math.sin(angle * Math.PI / 180) * percent);
                path.moveTo(x, y);
                valuePaint.setColor(Color.RED);
                canvas.drawCircle(x, y, ten, valuePaint);
                Point point = new Point(x,y,radius,angle);
                pointList.add(point);
                if(i > 0){
                    final PointBuffer lPointBuffer = pointBufferList.get(i - 1);
                    final float lRadius = lPointBuffer.radius;
                    final float lAngle =  lPointBuffer.angle;
                    if(lRadius != 0) {
                        Log.e("MMM","radius =" +radius + ",lAngle = " + lAngle);
                        allArea += getArea(radius, lRadius,Math.abs(angle - lAngle));
                    }
                }
            }
            if(radius == 0 && lastPoint == null && i != 0){
                lastPoint = pointBufferList.get(i - 1);
            }

        }
        lastPoint = null;
        valuePaint.setColor(Color.RED);
        valuePaint.setAlpha(127);
        valuePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawPath(path, valuePaint);
    }

    public void setPointBufferList(List<PointBuffer> pointBufferList){
        this.pointBufferList = pointBufferList;
        postInvalidate();
        if(!pointList.isEmpty()){
            pointList.clear();
            lastPointView = null;
            nextPointView = null;
        }
    }

    private Point lastPointView = null;
    private Point nextPointView = null;

    /**
     * 绘制区域
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {

        if(lastPointView != null) {
            final String notchS = "当前1点，半径 ="+ lastPointView.radius
                    + " ,角度 =" + lastPointView.angle ;
            float measureText = textPaint.measureText(notchS);
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float baselineY = lastPointView.y - fontMetrics.top;
            textPaint.setAlpha(127);
            canvas.drawText(notchS, 35, 135, textPaint);
        }
        if(nextPointView != null ){
            final float n = getNotch(nextPointView.radius,lastPointView.radius ,Math.abs(nextPointView.angle - lastPointView.angle));
            final String notchS = "当前2点，半径 ="+ nextPointView.radius
                    + " ,角度 =" + nextPointView.angle
                    + ",与前一个点的距离是 =" + n;
            float measureText = textPaint.measureText(notchS);
            Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
            float baselineY = nextPointView.y + fontMetrics.top;
            textPaint.setAlpha(127);
            canvas.drawText(notchS,35,180,textPaint);
            //canvas.drawText(notchS, nextPointView.x - measureText, baselineY, textPaint);
            lastPointView = nextPointView;
            nextPointView = null;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            final float x = event.getX();
            final float y = event.getY();
            Log.d("SSS", "x =" + x + ",y = " + y);
            for (Point point : pointList) {
                if (Math.abs(x - point.x) < 25) {
                    if (Math.abs(y - point.y) < 25) {
                        if (lastPointView == null) {
                            lastPointView = point;
                        } else if (nextPointView == null) {
                            nextPointView = point;
                        }
                        postInvalidate();
                        return true;
                    }
                }
            }
            if (lastPointView != null || nextPointView != null) {
                lastPointView = null;
                nextPointView = null;
                postInvalidate();
            }
        }
        return super.onTouchEvent(event);
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

    private float getArea(float a,float b,float angle){
        //Log.e("SSSS","angle = " + angle);
        return (float) (Math.sin(Math.toRadians(angle) )*a*b*0.5);
    }
    private float getNotch(float a,float b,float angle){
        Log.e("SSSS","angle = " + angle);
        float m = 0;
        if(angle > 180){
            final float ac =Math.abs(360 - angle);
            if(ac <= 90){
               m =  (float) Math.sqrt((a*a)+(b*b)-(Math.sin(Math.toRadians(ac))*a*b*2));
            }else {
                m =  (float) Math.sqrt((a*a)+(b*b)-(Math.cos(Math.toRadians(ac))*a*b*2));
            }
        }else {
            if(angle <= 90){
                m =  (float) Math.sqrt((a*a)+(b*b)-(Math.sin(Math.toRadians(angle))*a*b*2));
            }else {
                m =  (float) Math.sqrt((a*a)+(b*b)-(Math.cos(Math.toRadians(angle))*a*b*2));
            }
        }
        return m;
    }

    public float getAllArea() {
        return allArea;
    }
}
