package com.ktc.nfc_project;

import static com.ktc.nfc_project.R.mipmap.ic_launcher;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.net.wifi.SoftApConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ktc.nfc_project.bean.AppInfo;
import com.ktc.nfc_project.utils.FunctionAdapter;
import com.ktc.nfc_project.utils.MyAdapter;
import com.ktc.nfc_project.utils.MyDBOpenHelper;

import java.util.ArrayList;
import java.util.List;



/**
 * @author wm
 */
public class NfcActivity extends AppCompatActivity {


    private static final String TAG = "NfcActivity";
    private Context mContext;
    private final List<AppInfo> mAppInfoList = new ArrayList<>();
    private final List<AppInfo> mFunctionInfoList = new ArrayList<>();
    private ListView mAppListView, mFunctionListView;
    private String NFCId = "";
    private MyDBOpenHelper myDBHelper;
    private SQLiteDatabase db;
    private TextView tvTips;

    private final static int HANDLER_MESSAGE_SHOW_PACKAGE_LIST = 0;
    private final static int HANDLER_MESSAGE_HIDE_PACKAGE_LIST = 1;
    private final static int HANDLER_MESSAGE_SHOW_FUNCTION_LIST = 2;
    private final static String FUNCTION_OPEN_WIFI = "openWifi";
    private final static String FUNCTION_OPEN_BT = "openOrCloseBT";
    private final static String FUNCTION_OPEN_HOTSPOT = "openHotspot";
    private final static String FUNCTION_PACKAGE_NAME = "-";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);
        mAppListView = findViewById(R.id.package_list);
        mFunctionListView = findViewById(R.id.function_list);
        tvTips = findViewById(R.id.tv_tips);
        mContext = this;
        myDBHelper = new MyDBOpenHelper(mContext, "my.db", null, 1);
        getAppProcessName();
        initData();
    }

    private void initData() {
        MyAdapter<String> myAdapter = new MyAdapter<>(mContext, mAppInfoList);
        mAppListView.setAdapter(myAdapter);
        mAppListView.setOnItemClickListener(mOnItemClickListener);

        FunctionAdapter<String> functionAdapter = new FunctionAdapter<>(mContext, mFunctionInfoList);
        mFunctionListView.setAdapter(functionAdapter);
        mFunctionListView.setOnItemClickListener(mOnItemClickListener);

        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        if (null == adapter) {
            Log.d(TAG, "initData: 不支持NFC功能");
        } else if (adapter.isEnabled()) {
            Log.d(TAG, "initData: 支持NFC功能");

            //这里拿了NFC卡的tag中的id数据，可以在NfcAdapter源码中查看，具体能拿到哪些数据
            Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag == null) {
                Log.i(TAG, "initData: tag == null");
                return;
            }

            if (tag.getId() == null) {
                Log.i(TAG, "initData: getId == null");
                return;
            }
            NFCId = bytesToHex(tag.getId());
            Log.d(TAG, "initData: id " + NFCId);

            if (NFCId == null || "".equals(NFCId)) {
                return;
            }

            db = myDBHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM nfcdevicerecord WHERE deviceid = ?",
                    new String[]{NFCId});
            String packageName = "";
            String name = "";
            //存在数据才返回true
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndex("appname"));
                packageName = cursor.getString(cursor.getColumnIndex("packagename"));
            }
            cursor.close();

            Log.i(TAG, "initData: getAppName from db " + packageName);
            if (!"".equals(packageName)) {
                Log.i(TAG, "initData: open app or function " + name);
                if (FUNCTION_PACKAGE_NAME.equals(packageName)) {
                    //执行对应的功能
                    toExecuteFunction(name);
                } else {
                    //跳转app
                    startApp(mContext, packageName);
                }
                //执行完功能或跳转app之后关闭该页面
                finish();
            } else {
                Log.d(TAG, "initData: no record, write new NFC ID");
                showNFCDialog();
            }
        } else {
            Log.d(TAG, "initData: NFC status is close!!!");
        }
    }

    private void toExecuteFunction(String name) {
        if (name == null || "".equals(name)) {
            return;
        }
        Toast.makeText(mContext, "正在执行" + name, Toast.LENGTH_SHORT).show();
        switch (name) {
            case FUNCTION_OPEN_WIFI:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.i(TAG, "toExecuteFunction: system over Android 11, can't open wifi on app");
                    //弹窗显示是否打开
                    startActivity(new Intent("android.settings.panel.action.WIFI"));
                } else {
                    WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (!wifiManager.isWifiEnabled()) {
                        boolean flag = wifiManager.setWifiEnabled(true);
                        Log.d(TAG, "openWlan: " + flag);
                    }
                }
                break;
            case FUNCTION_OPEN_BT:
                BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mBluetoothAdapter.isEnabled()) {
                    mBluetoothAdapter.enable();
                    Log.i(TAG, "openBlueTooth: success");
                } else {
                    mBluetoothAdapter.disable();
                    Log.i(TAG, "closeBlueTooth: success");
                }
                break;
            case FUNCTION_OPEN_HOTSPOT:
                Log.i(TAG, "toExecuteFunction: open hotspot");
                startActivity(new Intent("android.settings.TETHER_SETTINGS"));
            default:
                break;
        }
    }



    private void showNFCDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("识别到NFC！");
        builder.setMessage("请选择绑定app还是绑定特定功能");
        builder.setIcon(ic_launcher);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);


        builder.setPositiveButton("绑定app", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_PACKAGE_LIST);
                dialog.dismiss();

            }
        });

        builder.setNegativeButton("绑定功能", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_FUNCTION_LIST);
                dialog.dismiss();
            }
        });


        builder.setNeutralButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.sendEmptyMessage(HANDLER_MESSAGE_HIDE_PACKAGE_LIST);
                dialog.dismiss();
            }
        });

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();
        //对话框显示的监听事件
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Log.e(TAG, "对话框显示了");
            }
        });
        //对话框消失的监听事件
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                Log.e(TAG, "对话框消失了");
            }
        });
        //显示对话框
        dialog.show();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //在同一个页面接收到NFC的动作，继续调用initData方法去处理
        Log.d(TAG, "onNewIntent: receiver");
        initData();
    }


    /**
     * @author Liangcw
     * @description 获取已安装的系统自带应用信息（Launcher）
     * @createTime 2023/1/3 20:14
     */
    public void getAppProcessName() {

        final PackageManager packageManager = getPackageManager();
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        Drawable functionImageIcon = null;
        final List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        Log.i(TAG, "getAppProcessName: appsSize" + apps.size());
        for (int i = 0; i < apps.size(); i++) {
            //获得图标(此处得到的为Drawable型的，xml文件中的图片是Imageview型的，需要用setImageDrawable进行转换，见MyAdapter.class)
            Drawable imageIcon = apps.get(i).activityInfo.loadIcon(packageManager);
            //获得名称
            String appName = apps.get(i).activityInfo.applicationInfo.loadLabel(packageManager).toString();
            //获得包名
            String packageName = apps.get(i).activityInfo.packageName;
           //Log.i(TAG, "getAppProcessName: appName " + appName + "  " + packageName + "  " + imageIcon);
            mAppInfoList.add(new AppInfo(appName, packageName, imageIcon));
            if ("设置".equals(appName)){
                functionImageIcon = imageIcon;
            }

        }

        mFunctionInfoList.add(new AppInfo(FUNCTION_OPEN_WIFI, FUNCTION_PACKAGE_NAME, functionImageIcon));
        mFunctionInfoList.add(new AppInfo(FUNCTION_OPEN_BT, FUNCTION_PACKAGE_NAME, functionImageIcon));
        mFunctionInfoList.add(new AppInfo(FUNCTION_OPEN_HOTSPOT, FUNCTION_PACKAGE_NAME, functionImageIcon));
    }

    /**
     * 2转16
     */
    private static String bytesToHex(byte[] src) {
        StringBuilder sb = new StringBuilder();
        if (src == null || src.length <= 0) {
            return null;
        }
        String sTemp;
        for (byte b : src) {
            sTemp = Integer.toHexString(0xFF & b);
            if (sTemp.length() < 2) {
                sb.append(0);
            }
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * @param context, packageName
     * @author Liangcw
     * @description 启动app
     * @createTime 2023/1/5 9:21
     */
    public static void startApp(Context context, String packageName) {
        Log.d(TAG, "packageName is " + packageName);
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);
        if (intent != null) {
            android.util.Log.i(TAG, "startApp: intent no null");
            context.startActivity(intent);
        }
    }

    /**
     * @author Liangcw
     * @description 使用Handle来进行appList的显示和隐藏
     * @createTime 2023/1/4 20:02
     */
    final Handler handler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == HANDLER_MESSAGE_SHOW_PACKAGE_LIST) {
                mAppListView.setVisibility(View.VISIBLE);
                mFunctionListView.setVisibility(View.GONE);
                tvTips.setVisibility(View.GONE);
            } else if (msg.what == HANDLER_MESSAGE_SHOW_FUNCTION_LIST){
                mAppListView.setVisibility(View.GONE);
                mFunctionListView.setVisibility(View.VISIBLE);
                tvTips.setVisibility(View.GONE);
            } else if (msg.what == HANDLER_MESSAGE_HIDE_PACKAGE_LIST) {
                mAppListView.setVisibility(View.GONE);
                mFunctionListView.setVisibility(View.GONE);
                tvTips.setVisibility(View.VISIBLE);
            }
        }
    };


    /**
     * @author Liangcw
     * @description 对app列表的点击事件监听，点击绑定该app
     * @createTime 2023/1/4 20:01
     */
    AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (parent == mAppListView){
                AppInfo app = (AppInfo) parent.getItemAtPosition(position);
                String appName = app.getLabel();
                String packageName = app.getPackageName();
                Drawable icon = app.getIcon();
                showBindDialog(appName, packageName, icon);
            } else if (parent == mFunctionListView){
                AppInfo app = (AppInfo) parent.getItemAtPosition(position);
                String appName = app.getLabel();
                String packageName = app.getPackageName();
                Drawable icon = app.getIcon();
                showBindDialog(appName, packageName, icon);

            }
        }
    };


    /**
     * @param appName, packageName
     * Title: showBindDialog
     * @author Liangcw
     * @description 显示绑定app的弹窗
     * @createTime 2023/1/4 20:01
     */
    private void showBindDialog(String appName, String packageName, Drawable icon) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("绑定app或功能");
        builder.setMessage("是否将此NFC卡绑定至 " + appName + "?");
        builder.setIcon(icon);
        //点击对话框以外的区域是否让对话框消失
        builder.setCancelable(true);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Toast.makeText(mContext, "绑定成功！", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                db = myDBHelper.getWritableDatabase();
                ContentValues values = new ContentValues();
                values.put("deviceid", NFCId);
                values.put("appname", appName);
                values.put("packagename", packageName);
                //参数依次是：表名，强行插入null值的数据列的列名，一行记录的数据
                db.insert("nfcdevicerecord", null, values);
                finish();

            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                handler.sendEmptyMessage(HANDLER_MESSAGE_SHOW_PACKAGE_LIST);
                Toast.makeText(mContext, "请重新绑定", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        //创建AlertDialog对象
        AlertDialog dialog = builder.create();

        //显示对话框
        dialog.show();
    }

}