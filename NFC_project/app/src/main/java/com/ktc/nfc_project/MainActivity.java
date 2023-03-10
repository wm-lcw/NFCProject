package com.ktc.nfc_project;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NfcAdapter;
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


/**
 * @author wm
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Context mContext;
    private ListView mBindList;
    private LinearLayout mllBindList, mllTips, mllOpenNfc;
    private BindListAdapter mBindAdapter;
    private MyDBOpenHelper myDBHelper;
    private SQLiteDatabase db;


    private final List<AppInfo> mBindInfoList = new ArrayList<>();
    private final static int HANDLER_MESSAGE_SHOW_TIPS = 0;
    private final static int HANDLER_MESSAGE_SHOW_BIND_LIST = 1;
    private final static int HANDLER_MESSAGE_OPEN_NFC = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        Log.d(TAG, "onCreate: new activity");
        initData();

    }

    private void initData() {
        mBindList = findViewById(R.id.bind_List);
        mllBindList = findViewById(R.id.ll_bind_list);
        mllTips = findViewById(R.id.ll_tips);
        mllOpenNfc = findViewById(R.id.ll_openNFC);
        mllOpenNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: jump to open NFC");
                startActivity(new Intent("android.settings.panel.action.NFC"));
            }
        });
        myDBHelper = new MyDBOpenHelper(mContext, "my.db", null, 1);
        mContext = this;
        //???????????????????????????
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
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (null == adapter) {
            Log.d(TAG, "onResume: ?????????NFC??????");
            Toast.makeText(mContext, "??????????????????NFC?????????", Toast.LENGTH_SHORT).show();
            finish();
        } else if (adapter.isEnabled()) {
            Log.d(TAG, "onResume: NFC enable");
            //??????????????????????????????,??????NFC???????????????????????????
            initBindList();
        } else {
            Log.d(TAG, "onResume:NFC disable");
            handler.sendEmptyMessage(HANDLER_MESSAGE_OPEN_NFC);
        }

    }

    /**
     * @param
     * @return void
     * @version V1.0
     * Title: initBindList
     * @author Liangcw
     * @description ?????????????????????????????????????????????????????????????????????????????????
     * @createTime 2023/1/4 20:19
     */
    private void initBindList() {

        mBindInfoList.clear();

        db = myDBHelper.getReadableDatabase();
        Cursor cursor =  db.query("nfcdevicerecord", null,null,null,null,null,null);
        //?????????????????????true
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
     * @description ????????????????????????
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
        //1.????????????PopupWindow???????????????????????????View?????????
        final PopupWindow popWindow = new PopupWindow(view,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        //??????????????????
        popWindow.setAnimationStyle(R.anim.anim_pop);

        //?????????????????????PopupWindow?????????PopupWindow????????????????????????????????????
        //???????????????????????????????????????PopupWindow????????????????????????????????????????????????
        //PopupWindow????????????????????????????????????????????????????????????????????????????????????
        popWindow.setTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            popWindow.setTouchInterceptor(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return false;
                    // ??????????????????true?????????touch??????????????????
                    // ????????? PopupWindow???onTouchEvent?????????????????????????????????????????????dismiss
                }
            });
        }
        //??????popWindow???????????????????????????
        popWindow.setBackgroundDrawable(new ColorDrawable(0x00000000));


        //??????popupWindow???????????????????????????????????????View???x??????????????????y???????????????
        popWindow.showAsDropDown(v, 90, 0);

        //??????popupWindow?????????????????????
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
        builder.setTitle("????????????");
        builder.setMessage("????????????????????????????????????");
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setView(inputText);
        //??????????????????????????????????????????????????????
        builder.setCancelable(true);

        //??????????????????
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String changeText = inputText.getText().toString().trim();
                Log.i(TAG, "onClick: changeText " + changeText);
                if (changeText == null || "".equals(changeText)){
                    Toast.makeText(mContext, "??????????????????", Toast.LENGTH_SHORT).show();
                } else{
                    db = myDBHelper.getReadableDatabase();
                    db.execSQL("UPDATE nfcdevicerecord SET alias = ? WHERE deviceid = ?",
                            new String[]{changeText, nfc_id});
                    Toast.makeText(mContext, "???????????????", Toast.LENGTH_SHORT).show();
                    mBindAdapter.changeAlias(position,changeText);
                }
                dialog.dismiss();
            }
        });

        //??????????????????
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //??????AlertDialog??????
        AlertDialog dialog = builder.create();

        //???????????????
        dialog.show();
    }

    private void showDeleteDialog(String nfc_id, String packageName, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("????????????app");
        builder.setMessage("???????????????????????????app?????????????");
        builder.setIcon(R.mipmap.ic_launcher);
        //??????????????????????????????????????????????????????
        builder.setCancelable(true);

        //??????????????????
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db = myDBHelper.getReadableDatabase();
                db.execSQL("DELETE FROM nfcdevicerecord WHERE deviceid = ?",
                        new String[]{nfc_id});

                Toast.makeText(mContext, "???????????????", Toast.LENGTH_SHORT).show();
                mBindAdapter.remove(position);
                if (mBindAdapter.getCount() <= 0){
                    handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_TIPS);
                } else {
                    handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_BIND_LIST);
                }
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //??????AlertDialog??????
        AlertDialog dialog = builder.create();

        //???????????????
        dialog.show();
    }

    private void showDeleteAllDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("??????????????????app");
        builder.setMessage("????????????????????????????");
        builder.setIcon(R.mipmap.ic_launcher);
        //??????????????????????????????????????????????????????
        builder.setCancelable(true);

        //??????????????????
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                db = myDBHelper.getReadableDatabase();
                db.execSQL("DELETE FROM nfcdevicerecord");
                mBindAdapter.removeAll();
                handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_TIPS);
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        //??????AlertDialog??????
        AlertDialog dialog = builder.create();

        //???????????????
        dialog.show();
    }

    final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_SHOW_TIPS) {
                mllTips.setVisibility(View.VISIBLE);
                mllBindList.setVisibility(View.GONE);
                mllOpenNfc.setVisibility(View.GONE);
            } else if (msg.what == HANDLER_MESSAGE_SHOW_BIND_LIST) {
                mllTips.setVisibility(View.GONE);
                mllBindList.setVisibility(View.VISIBLE);
                mllOpenNfc.setVisibility(View.GONE);
            } else if (msg.what == HANDLER_MESSAGE_OPEN_NFC) {
                mllTips.setVisibility(View.GONE);
                mllBindList.setVisibility(View.GONE);
                mllOpenNfc.setVisibility(View.VISIBLE);
            }
        }
    };
}