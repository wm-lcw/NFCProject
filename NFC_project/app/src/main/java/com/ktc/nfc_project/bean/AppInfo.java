package com.ktc.nfc_project.bean;

import android.graphics.drawable.Drawable;

/**
 * @author liangcw
 * @date 2023/1/4 15:45
 */
public class AppInfo {

    private String nfcDeviceId;
    private String alias;
    public String label;
    public String packageName;
    private Drawable icon;

    public AppInfo(String label, String packageName, Drawable icon) {
        this.label = label;
        this.packageName = packageName;
        this.icon = icon;
    }

    public AppInfo(String nfcDeviceId, String label, String alias) {
        this.nfcDeviceId = nfcDeviceId;
        this.label = label;
        this.alias = alias;

    }

    public String getNfcDeviceId() {
        return nfcDeviceId;
    }

    public void setNfcDeviceId(String nfcDeviceId) {
        this.nfcDeviceId = nfcDeviceId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Drawable getIcon() {
        return icon;
    }

    public void setIcon(Drawable icon) {
        this.icon = icon;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "nfcDeviceId='" + nfcDeviceId + '\'' +
                ", alias='" + alias + '\'' +
                ", label='" + label + '\'' +
                ", packageName='" + packageName + '\'' +
                ", icon=" + icon +
                '}';
    }
}