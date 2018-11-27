package com.rong.Radar.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.rong.Radar.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DialogItemAdapter extends BaseAdapter {
    //这里可以传递个对象，用来控制不同的item的效果
    //比如每个item的背景资源，选中样式等
    public List<BluetoothDevice> list;
    LayoutInflater inflater;

    public DialogItemAdapter(Context context, Map<String,BluetoothDevice> list) {
        this.list = new ArrayList<>();
        Iterator iter  = list.keySet().iterator();
        while (iter.hasNext()){
            String key = (String) iter.next();
            this.list.add(list.get(key));
        }
        inflater = LayoutInflater.from(context);
    }

    public void setList(Map<String,BluetoothDevice> list) {
        this.list = new ArrayList<>();
        Iterator iter  = list.keySet().iterator();
        while (iter.hasNext()){
            String key = (String) iter.next();
            this.list.add(list.get(key));
        }
        this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public BluetoothDevice getItem(int i) {
        if (i == getCount() || list == null) {
            return null;
        }
        return list.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.dialog_item, null);
            holder.tv_name = (TextView) convertView.findViewById(R.id.tv_name);
            holder.tv_id = (TextView) convertView.findViewById(R.id.tv_id);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        BluetoothDevice device = getItem(position);
        if(device != null) {
            holder.tv_id.setText(((position+1) < 10 ? ("0" +(position+1) ): (position+1)) + "");
            final String name = device.getName();
            if(!TextUtils.isEmpty(name)){
                holder.tv_name.setText(name);
            }else {
                holder.tv_name.setText(device.getAddress());
            }
        }
        return convertView;
    }

    public static class ViewHolder {
        public TextView tv_name;
        public TextView tv_id;
    }
}
