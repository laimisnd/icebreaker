package com.example.laimis.myfirstapp;

import java.io.Serializable;

/**
 * Created by laimis on 6/21/2016.
 */
public class BTDevice implements Serializable //extends Parcelable
{
    String name, address;
    long time;
    long firstTime;
    long hailCount=1;
    int numMajorClass;
    String strMajorClass;
    String dbg;

    public BTDevice(String name, String address, long time, int numMajorClass, String strMajorClass, String strDbg){
        this.name = name;
        this.address = address;
        this.firstTime= time;
        this.time=time;
        this.numMajorClass=numMajorClass;
        this.strMajorClass=strMajorClass;
        this.dbg=strDbg;
    }
}
