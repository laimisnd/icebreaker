package com.example.laimis.myfirstapp;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class BTscanService extends Service {

    public static int NOTIFICATION_ID_FOREGROUND_SERVICE=123;

    int mStartMode=START_STICKY;       // indicates how to behave if the service is killed
    private final IBinder myBinder = new BTLocalBinder();

    public long mBTDiscoveryInterval = 30;//seconds
    private Timer timer = new Timer();

    public int mCnt=0;

    long startTime = 0;

    private BluetoothAdapter ba;
    private ArrayList<String> mDevLst;
    private ArrayList<String> mPrevDevLst;

    public LinkedList<String> mlog;

    public LinkedHashMap<String, BTDevice> mHailedDevs;


    private int mHailedDevsSize = 100;
    public  long  mHailTimeoutSecs=3600*12;

    public  boolean mPlayerSound = true;

    public void addLog(String msg) {
        mlog.add(msg);
        if (mlog.size()>20) mlog.removeFirst();
    }
    public void clearLog(){
        //mlog.clear();
    }


    static String formatTime(long ts, String format ) {
        return new SimpleDateFormat(format, Locale.US).format(new Date(ts) );
    }

    public BTscanService() {
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return myBinder;
    }

    public class BTLocalBinder extends Binder {
        BTscanService getService() {
            return BTscanService.this;
        }
    }

    @Override
    public void onCreate() {

        mlog=new LinkedList<>() ;
        // The service is being created

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mDevLst = new ArrayList<>();
        mPrevDevLst = new ArrayList<>();


        //BluetoothManager btm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //ba = btm.getAdapter();


        addLog("Created\n");
        addLog("mPrevDevLst size: " +mPrevDevLst.size()+ "\n");



        mHailedDevs  = new LinkedHashMap<String, BTDevice>(mHailedDevsSize + 1, .75F, false) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > mHailedDevsSize;
            }};

        startTime = System.currentTimeMillis();

        ba = BluetoothAdapter.getDefaultAdapter();
        if ( ba == null ) {
            //tbd: report start failure somehow...
            addLog("ERROR: BluetoothAdapter.getDefaultAdapter returns null");
        } else {


            timer.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            doBTscan();
                        }
                    },
                    0,
                    mBTDiscoveryInterval*1000);
        }

        Intent notificationIntent = new Intent(this, MyActivity.class);

        //notificationIntent.setAction(SyncStateContract.Constants.ACTION.MAIN_ACTION);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Icebreaker")
                .setTicker("Icebreaker")
                .setContentText("Icebreaking")
                .setSmallIcon(R.drawable.ic_icebreaker)
                //.setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setWhen(System.currentTimeMillis())
                //.addAction(android.R.drawable.ic_media_previous,"Previous", ppreviousIntent)
                //.addAction(android.R.drawable.ic_media_play, "Play",pplayIntent)
                //.addAction(android.R.drawable.ic_media_next, "Next",pnextIntent)
                .build();

        startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, notification);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        // The service is starting, due to a call to startService()
        return mStartMode;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        timer.cancel();

        if ( ba != null) {
            ba.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);

        super.onDestroy();
    }

    public void doBTscan() {

            String message="";

       /* int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if ( permissionCheck != PackageManager.PERMISSION_GRANTED ) {
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
        // app-defined int constant. The callback method gets the
        // result of the request.
            message = message + "permision is not granted damn you";
        } else {

            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            CellInfo c = telephonyManager.getAllCellInfo().get(0);

            CellInfoLte ci = (CellInfoLte) c;

            int lvl = ci.getCellSignalStrength().getLevel();

//        android.telephony.SignalStrength signalStrength = new android.telephony.SignalStrength();

//        CellSignalStrengthGsm cs = new  CellSignalStrengthGsm();

            message = message + " level:" + lvl + " class:" + c.getClass().getName();
        }
*/
//-------------------------------------------------


            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                    ) {

                message = message + "BT permision is not granted damn you";
                addLog(message);

                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                    ) {
                message = message + "BT permision is not granted damn you";
                addLog(message);

                return;
            }

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                message = message + "ACCESS_COARSE_LOCATION permision is not granted damn you";
                addLog(message);

                return;
            }

            mCnt++;

        /*long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        int hh = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes  % 60;*/

            //addLog(String.format("#%d %d:%02d:%02d ", mCnt, hh, minutes, seconds));
            addLog(String.format(Locale.US, "#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss") +"\n" );

            if ( ba == null ) {
                addLog("ERROR: Bluetooth adapter is null. Try starting the service again");
                return;
            }

            if (ba.isDiscovering()) {
                addLog("BT discovery in progress...\n");
            } else {



                clearLog();
                addLog(String.format(Locale.US, "#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss") +"\n"  );
                addLog("History size:"+mHailedDevs.size() +"\n");
                addLog("BT discovery started, prev list size: "+mPrevDevLst.size()+" \n");


                mDevLst.clear();

                ba.startDiscovery();


            }

            //message = message + ;
            //log.setText(message);

            //Intent intent = new Intent(this, DisplayMessageActivity.class);
            //intent.putExtra(EXTRA_MESSAGE, message);
            //startActivity(intent);

    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            addLog( "Received action: " + action + ":\n");
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {

                    addLog( "Found BDevice: " + device.getName() + " : " +  device.getAddress() + "\n" );


                    mDevLst.add(device.getAddress());
                    addLog( " found dev list size:" + mDevLst.size());

                    //chgeck if new:
                    if ( mPrevDevLst.contains(device.getAddress()) ) {
                        addLog( " existing  device\n" );
                    } else {
                        addLog( " *** Found new device ***\n" );

                        BTDevice dev=mHailedDevs.get(device.getAddress());
                        if ( dev == null ) {

                            addLog("New device in the list\n");
                            addLog("***HAILING NEW***\n");

                            Long st=System.currentTimeMillis();
                            BTDevice ndev=new BTDevice(device.getName(), device.getAddress(), st );
                            mHailedDevs.put(device.getAddress(), ndev);

                            //addDevHist(device.getName() + "/" + device.getAddress() + "/"+ formatTime(st, "yy.MM.dd HH:mm:ss") + "\n");
                            //addDevHist(ndev);

                            //MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.hello);
                            //mediaPlayer.start(); // no need to call prepare(); create() does that for you

                        } else {
                            if ( System.currentTimeMillis() - dev.time < 1000 * mHailTimeoutSecs )
                                addLog("Too soon to hail again\n");
                            else
                                addLog("***HAILING EXISTING***\n");
                            dev.time= System.currentTimeMillis();
                            dev.hailCount++;
                            //redraw:

                            //addLog("History size:"+mHailedDevs.size() +"\n");

                            //MediaPlayer mediaPlayer = MediaPlayer.create(context, R.raw.hello);
                            //mediaPlayer.start(); // no need to call prepare(); create() does that for you

                        }

                        //clean old devices
                        /*
                        int msz=mHailedDevs.size();
                        if ( msz > mHailedDevsSize) {
                            addLog("cleanup started, list size:"+msz+"\n");
                            Iterator< Map.Entry<String, BTDevice>> itt = mHailedDevs.entrySet().iterator();

                            while (itt.hasNext()) {

                                if ( itt.next().getValue().time + 1000 * mHailTimeoutSecs < System.currentTimeMillis()  ) {
                                    itt.remove();
                                    msz--;

                                    if (msz<=mHailedDevsSize) break;
                                }
                            }
                            addLog("cleanup ended, list size:"+msz+"\n");
                        }*/



/*
                        //try to find this device in the list.
                        boolean bfound=false;
                        boolean btoosoon=false;
                        for (int i = 0; i < mHailedDevLst.size(); i++) {
                            if ( mHailedDevLst.get(i).equals(device.getAddress()) ) {
                                bfound=true;
                                if ( System.currentTimeMillis() - Long.parseLong(mHailedDevTimeLst.get(i).toString()) < 1000 * 3600 * 12 )
                                        btoosoon=true;
                            }
                        }
                        if (bfound) {
                            if (btoosoon) {
                                addLog("Too soon to hail again\n");
                            } else {
                                addLog("***HAILING***\n");
                            }
                        } else {
                            addLog("New device in the list\n");
                            addLog("***HAILING***\n");
                            mHailedDevLst.add(device.getAddress());
                            mHailedDevTimeLst.add(  new Long (System.currentTimeMillis()).toString()  );
                            addDevHist(device.getName() + "\n");
                        }*/

                        //clean old devices
                        /*
                        Iterator<String> itt = mHailedDevTimeLst.iterator();
                        Iterator<String> itd = mHailedDevLst.iterator();
                        long tm;
                        while (itt.hasNext()) {

                            tm=Long.parseLong(itt.next().toString());
                            if (itd.hasNext())
                                itd.next();
                            else break; //damn error, lists are desynced
                            if ( tm + 1000 * 3600 * 12 < System.currentTimeMillis()  ) {
                                itt.remove();
                                itd.remove();
                            }
                                // If you know it's unique, you could `break;` here
                        } */



                    }

                    //TBD mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    //android:id="@+id/edit_message
                    //findViewById(R.id.edit_message);

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mPrevDevLst=mDevLst;
                mDevLst = new ArrayList<>();

                addLog( "Finished discovery: found "+ mPrevDevLst.size() +" devices\n"  );

                //setProgressBarIndeterminateVisibility(false);
                //setTitle(R.string.select_device);
                //if (mNewDevicesArrayAdapter.getCount() == 0) {
                //    String noDevices = getResources().getText(R.string.none_found).toString();
                //     mNewDevicesArrayAdapter.add(noDevices);
                // }
            }
        }
    };

}
