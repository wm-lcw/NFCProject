package com.ktc.nfc_project.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ktc.nfc_project.R;
import com.ktc.nfc_project.bean.AppInfo;

import java.util.List;

/**
 * @author liangcw
 * @date 2023/1/4 15:43
 */
public class MyAdapter<T> extends BaseAdapter {

    private final List<AppInfo> mAppInfoList;
    private final Context mContext;
    private static final String TAG = "MyAdapter";

    public MyAdapter(@NonNull Context context, List<AppInfo> objects) {
        mAppInfoList = objects;
        mContext = context;
    }


    @Override
    public int getCount() {
        return mAppInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mAppInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            //获取LayoutInflater实例
            convertView = LayoutInflater.from(mContext).inflate(R.layout.user_item,null);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.item_name);
            holder.icon = (ImageView) convertView.findViewById(R.id.iv_icon);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            //convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvName.setText(mAppInfoList.get(position).getLabel());
        holder.icon.setImageDrawable(mAppInfoList.get(position).getIcon());
        return convertView;
    }


    static class ViewHolder {
        TextView tvName;
        ImageView icon;

    }
}