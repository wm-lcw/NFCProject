package com.ktc.nfc_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.ktc.nfc_project.bean.AppInfo;
import com.ktc.nfc_project.utils.BindListAdapter;
import com.ktc.nfc_project.utils.MyDBOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context mContext;
    private ListView mBindList;
    private LinearLayout mllBindList, mllTips;
    private BindListAdapter mBindAdapter;
    private MyDBOpenHelper myDBHelper;
    private SQLiteDatabase db;


    private final List<AppInfo> mBindInfoList = new ArrayList<>();
    private final static int HANDLER_MESSAGE_SHOW_TIPS = 0;
    private final static int HANDLER_MESSAGE_SHOW_BIND_LIST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        Log.d(TAG, "onCreate: new activity");
        mBindList = findViewById(R.id.bind_List);
        mllBindList = findViewById(R.id.ll_bind_list);
        mllTips = findViewById(R.id.ll_tips);
        myDBHelper = new MyDBOpenHelper(mContext, "my.db", null, 1);
        mContext = this;
        //清空所有已配对信息
        findViewById(R.id.btn_clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clean data!!!");
                showDeleteAllDialog();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在页面重构时刷新列表
        initBindList();
    }

    /**
     * @param
     * @return void
     * @version V1.0
     * Title: initBindList
     * @author Liangcw
     * @description 获取已配对的信息，初始化已配对列表，并设置长按监听事件
     * @createTime 2023/1/4 20:19
     */
    private void initBindList() {

        mBindInfoList.clear();

        db = myDBHelper.getReadableDatabase();
        Cursor cursor =  db.query("nfcdevicerecord", null,null,null,null,null,null);
        //存在数据才返回true
        if(cursor.moveToFirst())
        {
            do{
                String deviceId = cursor.getString(cursor.getColumnIndex("deviceid"));
                String name = cursor.getString(cursor.getColumnIndex("appname"));
                String alias = cursor.getString(cursor.getColumnIndex("alias"));
                mBindInfoList.add(new AppInfo(deviceId, name, alias));
            } while(cursor.moveToNext());
        }
        cursor.close();

        Log.d(TAG, "initBindList: size " + mBindInfoList.size());
        mBindAdapter = new BindListAdapter(mContext, mBindInfoList);
        mBindList.setAdapter(mBindAdapter);
        mBindList.setOnItemLongClickListener(mOnItemLongClickListener);

        if (mBindInfoList.size() <= 0){
            handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_TIPS);
        } else {
            handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_BIND_LIST);
        }
    }

    /**
     * @author Liangcw
     * @description 长按删除配对信息
     * @createTime 2023/1/4 20:12
     */
    AdapterView.OnItemLongClickListener mOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            AppInfo bindInfo = (AppInfo) parent.getItemAtPosition(position);
            String nfc_id = bindInfo.getNfcDeviceId();
            String appName = bindInfo.getLabel();
            initPopWindow(view, nfc_id, appName, position);
            return false;
        }
    };

    @SuppressLint("NewApi")
    private void initPopWindow(View v, String nfc_id, String packageName, int position) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_popip, null, false);
        Button btnChangeName = (Button) view.findViewById(R.id.btn_change_name);
        Button btnDelete = (Button) view.findViewById(R.id.btn_delete);
        //1.构造一个PopupWindow，参数依次是加载的View，宽高
        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //设置加载动画
        popWindow.setAnimationStyle(R.anim.anim_pop);

        //这些为了点击非PopupWindow区域，PopupWindow会消失的，如果没有下面的
        //代码的话，你会发现，当你把PopupWindow显示出来了，无论你按多少次后退键
        //PopupWindow并不会关闭，而且退不出程序，加上下述代码可以解决这个问题
        popWindow.setTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            popWindow.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                    // 这里如果返回true的话，touch事件将被拦截
                    // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
                }
            });
        }
        //要为popWindow设置一个背景才有效
        popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));


        //设置popupWindow显示的位置，参数依次是参照View，x轴的偏移量，y轴的偏移量
        popWindow.showAsDropDown(v, 90, 0);

        //设置popupWindow里的按钮的事件
        btnChangeName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "changeName", Toast.LENGTH_SHORT).show();
                popWindow.dismiss();
                showChangeAliasDialog(nfc_id, position);
            }
        });
        btnDelete.setOnClickListener(new View .OnClickListener() {
            @Override
            public void onClick(View v) {
                popWindow.dismiss();
                showDeleteDialog(nfc_id, packageName, position);
            }
        });
    }

    private void showChangeAliasDialog(String nfc_id, int position) {
        EditText inputText = new EditText(mContext);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("添加备注");
        builder.setMessage("给当前的配对设备添加备注");
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setView(inputText);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        //设置正面按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String changeText = inputText.getText().toString().trim();
                Log.i(TAG, "onClick: changeText " + changeText);
                if (changeText == null || "".equals(changeText)){
                    Toast.makeText(mContext, "请输入备注！", Toast.LENGTH_SHORT).show();
                } else{
                    db = myDBHelper.getReadableDatabase();
                    db.execSQL("UPDATE nfcdevicerecord SET alias = ? WHERE deviceid = ?",
                            new String[]{changeText, nfc_id});
                    Toast.makeText(mContext, "修改成功！", Toast.LENGTH_SHORT).show();
                    mBindAdapter.changeAlias(position,changeText);
                }
                dialog.dismiss();
            }
        });

        //设置反面按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }

    private void showDeleteDialog(String nfc_id, String packageName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("解除绑定app");
        builder.setMessage("是否将此设备与对应app解除绑定?");
        builder.setIcon(R.mipmap.ic_launcher);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        //设置正面按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db = myDBHelper.getReadableDatabase();
                db.execSQL("DELETE FROM nfcdevicerecord WHERE deviceid = ?",
                        new String[]{nfc_id});

                Toast.makeText(mContext, "解除绑定！", Toast.LENGTH_SHORT).show();
                mBindAdapter.remove(position);
                if (mBindAdapter.getCount() <= 0){
                    handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_TIPS);
                } else {
                    handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_BIND_LIST);
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }

    private void showDeleteAllDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("解除绑定所有app");
        builder.setMessage("是否解除所有的绑定?");
        builder.setIcon(R.mipmap.ic_launcher);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        //设置正面按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db = myDBHelper.getReadableDatabase();
                db.execSQL("DELETE FROM nfcdevicerecord");
                mBindAdapter.removeAll();
                handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_TIPS);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }

    final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_SHOW_TIPS) {
                mllTips.setVisibility(View.VISIBLE);
                mllBindList.setVisibility(View.GONE);

            } else if (msg.what == HANDLER_MESSAGE_SHOW_BIND_LIST) {
                mllTips.setVisibility(View.GONE);
                mllBindList.setVisibility(View.VISIBLE);
            }
        }
    };
}