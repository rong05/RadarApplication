package com.rong.Radar.view;

public class PointBuffer {
     int radius =0;
     int angle = 0;

     public PointBuffer() {
     }

     public PointBuffer(int radius, int angle) {
          this.radius = radius;
          this.angle = angle;
     }

     public int getRadius() {
          return radius;
     }

     public int getAngle() {
          return angle;
     }

     public void setRadius(int radius) {
          this.radius = radius;
     }

     public void setAngle(int angle) {
          this.angle = angle;
     }
}