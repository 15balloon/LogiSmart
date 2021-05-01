package com.logismart.logismart;

public class DataLog {
    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    String temp;
    String date;

    public DataLog(String temp, String date){
        this.date = date;
        this.temp = temp;
    }
}
