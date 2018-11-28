package com.rong.Radar.view;

public class PointBuffer {
     float radius =0;
     float angle = 0;
     int number = -1;

     public PointBuffer() {
     }

     public PointBuffer(int radius, int angle) {
          this.radius = radius;
          this.angle = angle;
     }

     public float getRadius() {
          return radius;
     }

     public float getAngle() {
          return angle;
     }

     public void setRadius(float radius) {
          this.radius = radius;
     }

     public void setAngle(float angle) {
          this.angle = angle;
     }

     public int getNumber() {
          return number;
     }

     public void setNumber(int number) {
          this.number = number;
     }
}
