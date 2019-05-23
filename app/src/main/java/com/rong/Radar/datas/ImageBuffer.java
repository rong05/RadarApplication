package com.rong.Radar.datas;

import java.nio.ByteBuffer;

public class ImageBuffer {
    private String name;
    private int size;
    private ByteBuffer byteBuffer;

    public ImageBuffer() {
    }

    public ImageBuffer(String name, int size, ByteBuffer byteBuffer) {
        this.name = name;
        this.size = size;
        this.byteBuffer = byteBuffer;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    @Override
    public String toString() {
        return "ImageBuffer{" +
                "name='" + name + '\'' +
                ", size=" + size +
                '}';
    }
}
