package com.ktc.nfc_project.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.ktc.nfc_project.R;
import com.ktc.nfc_project.bean.AppInfo;

import java.util.List;

/**
 * @author liangcw
 * @description: BindListAdapter
 * @date 2023/1/4 15:43
 */
public class BindListAdapter extends BaseAdapter {

    private List<AppInfo> mBindInfoList;
    private Context mContext;
    private static final String TAG = "BindListAdapter";

    public BindListAdapter(@NonNull Context context, List<AppInfo> objects) {
        mBindInfoList = objects;
        mContext = context;
    }


    @Override
    public int getCount() {
        return mBindInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mBindInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            //获取LayoutInflater实例
            convertView = LayoutInflater.from(mContext).inflate(R.layout.user_item2,null);
            holder = new ViewHolder();
            holder.id = (TextView) convertView.findViewById(R.id.item_id);
            holder.appName = (TextView) convertView.findViewById(R.id.item_app);
            holder.alias = (TextView) convertView.findViewById(R.id.item_alias);
            //将Holder存储到convertView中
            convertView.setTag(holder);
        } else {
            //convertView不为空时，从convertView中取出Holder
            holder = (ViewHolder) convertView.getTag();
        }

        holder.id.setText(mBindInfoList.get(position).getNfcDeviceId());
        holder.appName.setText(mBindInfoList.get(position).getLabel());
        holder.alias.setText(mBindInfoList.get(position).getAlias());
        return convertView;
    }


    static class ViewHolder {
        TextView id;
        TextView appName;
        TextView alias;

    }


    public void remove(int position) {
        if(mBindInfoList != null) {
            mBindInfoList.remove(position);
        }
        notifyDataSetChanged();
    }

    public void removeAll() {
        if(mBindInfoList != null) {
            mBindInfoList.clear();
        }
        notifyDataSetChanged();
    }

    public void changeAlias(int position,String changeText) {
        if(mBindInfoList != null) {
            mBindInfoList.get(position).setAlias(changeText);
        }
        notifyDataSetChanged();
    }

}