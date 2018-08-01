package com.example.lenovo.testing;

public class Time_Stamp {
    private String dateStamp;
    private String locStamp;

    public Time_Stamp(String DatS, String locS) {
        this.dateStamp = DatS;
        this.locStamp = locS;
    }

    public String getDate() {
        return this.dateStamp;
    }
    public String getLocation() {
        return this.locStamp;
    }
}
