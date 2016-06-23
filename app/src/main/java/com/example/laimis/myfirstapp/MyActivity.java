package com.example.laimis.myfirstapp;

import android.Manifest;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcelable;
import android.provider.SyncStateContract;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.CellInfo;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;


public class MyActivity extends AppCompatActivity {



    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";
    public final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 123 ;
    public final static int MY_PERMISSIONS_BLUETOOTH = 124 ;

    private TextView log;
    private TextView devHist;
    private BluetoothAdapter ba;
    private ArrayList<String> mDevLst;
    private ArrayList<String> mPrevDevLst;

    private ArrayList<String> mHailedDevLst;
    private ArrayList<String> mHailedDevTimeLst;

    private LinkedHashMap<String, BTDevice> mHailedDevs;

    private int mCnt=0;
    private int mIter=0;

    private BTDevice tBT;

    private int mHailedDevsSize = 25;
    private long  mHailTimeoutSecs=3600*12;


    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        outState.putStringArrayList("mPrevDevLst", mPrevDevLst );
        outState.putInt("mCnt", mCnt );

        outState.putStringArrayList("mHailedDevLst", mHailedDevLst);
        outState.putStringArrayList("mHailedDevTimeLst", mHailedDevTimeLst );

        outState.putSerializable("testBTdev", tBT);

        outState.putSerializable("mHailedDevs", mHailedDevs);

        outState.putLong("mHailTimeoutSecs", mHailTimeoutSecs);

        super.onSaveInstanceState(outState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        mPrevDevLst = savedInstanceState.getStringArrayList("mPrevDevLst");
        mCnt = savedInstanceState.getInt("mCnt");

        mHailedDevLst=savedInstanceState.getStringArrayList("mHailedDevLst");
        mHailedDevTimeLst=savedInstanceState.getStringArrayList("mHailedDevTimeLst" );

        tBT=(BTDevice)savedInstanceState.getSerializable("testBTdev");

        Map<String, BTDevice> items = (Map<String, BTDevice>) savedInstanceState.getSerializable("mHailedDevs");
        mHailedDevs.putAll(items);



                Iterator< Map.Entry<String, BTDevice>> itt = mHailedDevs.entrySet().iterator();
    BTDevice dv;int ii=0;
    while (itt.hasNext()) {
        ii++;
        dv=itt.next().getValue();
        //addDevHist(dv.name + "/" + dv.address + "/"+ (Long)(dv.time/1000) + "\n");
        addDevHist(dv);
    }
        addDevHist("Restored List size:"+ii);

        addLog("History size:"+mHailedDevs.size() +"\n");

        /*Iterator<String> itd = mHailedDevLst.iterator();
        while (itd.hasNext()) {
            addDevHist(itd.next().toString());
        }*/
        mHailTimeoutSecs = savedInstanceState.getLong("mHailTimeoutSecs");
        EditText editText = (EditText) findViewById(R.id.hailTimeout);
        editText.setText( Long.toString(mHailTimeoutSecs) );
    }

    BTscanService mBTSrv;
    boolean isBound = false;

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BTscanService.BTLocalBinder binder = (BTscanService.BTLocalBinder) service;
            mBTSrv = binder.getService();
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        log= (TextView) findViewById(R.id.log);

        devHist= (TextView) findViewById(R.id.devhist);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mDevLst = new ArrayList<String>();
        mPrevDevLst = new ArrayList<String>();

        mHailedDevLst = new ArrayList<String>();
        mHailedDevTimeLst = new ArrayList<String>();

        BluetoothManager btm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        ba = btm.getAdapter();

        addLog("Created\n");
        addLog("mPrevDevLst size: " +mPrevDevLst.size()+ "\n");


        tBT = new BTDevice("name1","addr2", 999);


        mHailedDevs  = new LinkedHashMap<String, BTDevice>(mHailedDevsSize + 1, .75F, false) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > mHailedDevsSize;
            }};


        startTime = System.currentTimeMillis();

        EditText editText = (EditText) findViewById(R.id.hailTimeout);
        editText.setText( Long.toString(mHailTimeoutSecs) );

        //Start service:
        Intent srvIntent = new Intent(this, BTscanService.class);
        startService(srvIntent);

        bindService(srvIntent, myConnection, Context.BIND_AUTO_CREATE);

        //TBD timerHandler.postDelayed(timerRunnable, 0);

/*

btSrv  = new Intent(this, BTscanService.class);
stopService(btSrv);

 */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if ( ba != null) {
            ba.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);

        timerHandler.removeCallbacks(timerRunnable);

        if (isBound) {
            // Detach our existing connection.
            if (myConnection != null ) {
                unbindService(myConnection);
                isBound = false;
            }
        }



    }

    long startTime = 0;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {


            findNewBTs();

            timerHandler.postDelayed(this, 20000);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_my, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_exit) {


            Intent srvIntent = new Intent(this, BTscanService.class);
            stopService(srvIntent);



            finishAffinity();

            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    String formatTime(long ts, String format ) {
                return new SimpleDateFormat(format).format(new Date(ts) );
    }


    protected void findNewBTs() {

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
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH_ADMIN},
                    MY_PERMISSIONS_BLUETOOTH);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            message = message + "BT permision is not granted damn you";
            log.setText(message);

            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                ) {
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH},
                    MY_PERMISSIONS_BLUETOOTH);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            message = message + "BT permision is not granted damn you";
            log.setText(message);

            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            message = message + "ACCESS_COARSE_LOCATION permision is not granted damn you";
            log.setText(message);

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
        addLog(String.format("#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss") +"\n" );


        if (ba.isDiscovering()) {
            addLog("BT discovery in progress...\n");
        } else {



            clearLog();
            addLog(String.format("#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss") +"\n"  );
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


    public void sendMessage(View view) {

        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();

        //findNewBTs();
        //tBT.time++;
        //addLog(tBT.name  + "/" + tBT.address + "/" + tBT.time);
        EditText editText = (EditText) findViewById(R.id.hailTimeout);
        mHailTimeoutSecs = Long.parseLong (editText.getText().toString());

        //-------------------------------------------------------
        mIter++;

        //TBD timerHandler.postDelayed(timerRunnable, 0);

        addLog("srv name:"+mBTSrv.getClass().getName()+"\n");

        addLog("srv ticker:" + mBTSrv.mCnt + "\n");

       /* BTDevice ndev=new BTDevice("name1", "addr1"+mIter, 123);
        mHailedDevs.put("addr1"+mIter, ndev);

        ndev=new BTDevice("name2", "addr2"+mIter, 124);
        mHailedDevs.put("addr2"+mIter, ndev);

        ndev=new BTDevice("name3", "addr3"+mIter, 125);
        mHailedDevs.put("addr3"+mIter, ndev);



        clearDevHist();
        Iterator< Map.Entry<String, BTDevice>> itt = mHailedDevs.entrySet().iterator();
        BTDevice dv;
        int ii=0;
        while (itt.hasNext()) {
            ii++;
            dv=itt.next().getValue();
            addDevHist(dv);
        }
        addDevHist("List size:"+ii+" htsize():" + mHailedDevs.size());*/

    }

    protected void addDevHist(BTDevice d) {
        addDevHist(d.name + "/" + d.address + "/hailed: #"+ d.hailCount +" " + formatTime(d.time, "HH:mm:ss") + "/found:" + formatTime(d.firstTime, "yy.MM.dd HH:mm:ss") + "\n");
    }

    protected void addDevHist(String s) {

        String message=devHist.getText().toString();
        message = message + s ;
        devHist.setText(message);

    }

    protected void clearDevHist() {
        devHist.setText("Hailed Devices:\n");
    }


    protected void addLog(String s) {

        String message=log.getText().toString();
        message = message + s ;
        log.setText(message);

    }

    protected void clearLog() {

        log.setText("Log:\n");

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
                            addDevHist(ndev);

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
                                clearDevHist();
                                Iterator< Map.Entry<String, BTDevice>> itt = mHailedDevs.entrySet().iterator();
                                BTDevice dv;
                                int ii=0;
                                while (itt.hasNext()) {
                                    ii++;
                                    dv=itt.next().getValue();
                                    addDevHist(dv);
                                }
                                addDevHist("List size:"+ii);
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
                mDevLst = new ArrayList<String>();

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
