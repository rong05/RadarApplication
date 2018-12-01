package com.rong.Radar.view;

import android.support.annotation.NonNull;

public class PointBuffer implements Comparable<PointBuffer> {
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

     @Override
     public String toString() {
          return "PointBuffer{" +
                  "radius=" + radius +
                  ", angle=" + angle +
                  ", number=" + number +
                  '}';
     }

     @Override
     public int compareTo(@NonNull PointBuffer o) {
          int i = (int)(this.getAngle() - o.getAngle());//先按照年龄排序
          if(i == 0){
               return (int)(this.getAngle() - o.getAngle());//如果年龄相等了再用分数进行排序
          }
          return i;
     }
}
