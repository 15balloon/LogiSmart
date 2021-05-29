package com.logismart.logismart;

public class ListViewItem {
    private String bleName;
    private String bleFrom;
    private String bleTo;
    private int bleConnection;
    private String bleDriver;
    private String bleDriverPhone;
    private String shipName;
    private int upper;
    private int lower;

    public void setBleName(String bleName) {
        this.bleName = bleName;
    }

    public void setBleFrom(String bleFrom) {
        this.bleFrom = bleFrom;
    }

    public void setBleTo(String bleTo) {
        this.bleTo = bleTo;
    }

    public void setBleConnection(int bleConnection) {
        this.bleConnection = bleConnection;
    }

    public void setBleDriver(String bleDriver) {
        this.bleDriver = bleDriver;
    }

    public void setBleDriverPhone(String bleDriverPhone) {
        this.bleDriverPhone = bleDriverPhone;
    }

    public void setShipName(String shipName) {
        this.shipName = shipName;
    }

    public void setUpper(int upper) {
        this.upper = upper;
    }

    public void setLower(int lower) {
        this.lower = lower;
    }

    public String getBleName() {
        return bleName;
    }

    public String getBleFrom() {
        return bleFrom;
    }

    public String getBleTo() {
        return bleTo;
    }

    public String getBleFromTo() { return bleFrom + " -> " + bleTo; };

    public int getBleConnection() {
        return bleConnection;
    }

    public String getBleDriver() {
        return bleDriver;
    }

    public String getBleDriverPhone() {
        return bleDriverPhone;
    }

    public String getShipName() {
        return shipName;
    }

    public int getUpper() {
        return upper;
    }

    public int getLower() {
        return lower;
    }
}