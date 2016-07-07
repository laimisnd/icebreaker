package com.example.laimis.myfirstapp;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;


public class MyActivity extends AppCompatActivity {


    public final static String EXTRA_MESSAGE = "com.mycompany.myfirstapp.MESSAGE";
    public final static int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 123;
    public final static int MY_PERMISSIONS_BLUETOOTH = 124;

    private TextView log;
    private TextView devHist;
    private EditText hailTimeout;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /*private BluetoothAdapter ba;
    private ArrayList<String> mDevLst;
    private ArrayList<String> mPrevDevLst;

    private ArrayList<String> mHailedDevLst;
    private ArrayList<String> mHailedDevTimeLst;

    private LinkedHashMap<String, BTDevice> mHailedDevs;

    private int mCnt=0;
    private int mIter=0;

    private BTDevice tBT;

    private int mHailedDevsSize = 25;
    private long  mHailTimeoutSecs=3600*12;*/


    @Override
    public void onSaveInstanceState(Bundle outState) {

        /*
        outState.putStringArrayList("mPrevDevLst", mPrevDevLst );
        outState.putInt("mCnt", mCnt );

        outState.putStringArrayList("mHailedDevLst", mHailedDevLst);
        outState.putStringArrayList("mHailedDevTimeLst", mHailedDevTimeLst );

        outState.putSerializable("testBTdev", tBT);

        outState.putSerializable("mHailedDevs", mHailedDevs);

        outState.putLong("mHailTimeoutSecs", mHailTimeoutSecs);
*/

        super.onSaveInstanceState(outState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        fetchSrvData();

        /*
        mPrevDevLst = savedInstanceState.getStringArrayList("mPrevDevLst");
        mCnt = savedInstanceState.getInt("mCnt");

        mHailedDevLst=savedInstanceState.getStringArrayList("mHailedDevLst");
        mHailedDevTimeLst=savedInstanceState.getStringArrayList("mHailedDevTimeLst" );

        tBT=(BTDevice)savedInstanceState.getSerializable("testBTdev");

        Map<String, BTDevice> items = (Map<String, BTDevice>) savedInstanceState.getSerializable("mHailedDevs");
        mHailedDevs.putAll(items);
*/

    }

    void fetchSrvData() {

        if (mBTSrv == null) return;
        clearLog();

        Iterator<String> it = mBTSrv.mlog.iterator();
        int ii = 0;
        while (it.hasNext()) {
            ii++;
            addLog(it.next());
        }
//------------------------------------
        clearDevHist();
        Iterator<Map.Entry<String, BTDevice>> itt = mBTSrv.mHailedDevs.entrySet().iterator();
        BTDevice dv;
        ii = 0;
        while (itt.hasNext()) {
            ii++;
            dv = itt.next().getValue();
            //addDevHist(dv.name + "/" + dv.address + "/"+ (Long)(dv.time/1000) + "\n");
            addDevHist(dv);
        }
        addDevHist("Restored List size:" + ii);
//------------------------------------
        addLog("History size:" + mBTSrv.mHailedDevs.size() + "\n");

        /*Iterator<String> itd = mHailedDevLst.iterator();
        while (itd.hasNext()) {
            addDevHist(itd.next().toString());
        }*/
        //mHailTimeoutSecs = savedInstanceState.getLong("mHailTimeoutSecs");

        //editText.setText(Long.toString(mBTSrv.mHailTimeoutSecs));
        hailTimeout.setText(String.valueOf(mBTSrv.mHailTimeoutSecs));

        EditText btdev_dtime = (EditText) findViewById(R.id.btdev_discovery_timeout);
        if ( btdev_dtime != null ) {
            btdev_dtime.setText(String.valueOf(mBTSrv.mBTDiscoveryInterval));
        }

        Switch sw=(Switch)findViewById(R.id.switch_heil_sound);
        if (sw != null ) {
            sw.setChecked(mBTSrv.mPlayerSound );
        }

        Switch swSrv=(Switch)findViewById(R.id.switch_service_onoff);
        if (swSrv != null ) {
            swSrv.setChecked(isBound );
        }

    }

    BTscanService mBTSrv;
    boolean isBound = false;

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            BTscanService.BTLocalBinder binder = (BTscanService.BTLocalBinder) service;
            mBTSrv = binder.getService();
            isBound = true;

            fetchSrvData();
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
        String message = "";
        log = (TextView) findViewById(R.id.log);

        devHist = (TextView) findViewById(R.id.devhist);

        hailTimeout= (EditText) findViewById(R.id.hailTimeout);

                /*
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
        */


        //EditText editText = (EditText) findViewById(R.id.hailTimeout);
        //editText.setText( Long.toString(mHailTimeoutSecs) );


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


        if (mSrvStarted) {
            StartService();
        }

        //TBD timerHandler.postDelayed(timerRunnable, 0);

        Switch sButton = (Switch) findViewById(R.id.switch_heil_sound);

        //Set a CheckedChange Listener for Switch Button
        if (sButton != null) {
            sButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean on){
                    mBTSrv.mPlayerSound = on;
                }
            });
        }

        Switch sButtonSrv = (Switch) findViewById(R.id.switch_service_onoff);

        //Set a CheckedChange Listener for Switch Button
        if (sButtonSrv != null) {
            sButtonSrv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton cb, boolean on) {
                    if (on) StartService();
                    else StopService();
                }
            });
        }

/*

btSrv  = new Intent(this, BTscanService.class);
stopService(btSrv);

 */
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        /*if ( ba != null) {
            ba.cancelDiscovery();
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);

        timerHandler.removeCallbacks(timerRunnable);
        */

        if (isBound) {
            // Detach our existing connection.
            if (myConnection != null) {
                unbindService(myConnection);
                isBound = false;
            }
        }


    }

//    long startTime = 0;
    /*Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {


            findNewBTs();

            timerHandler.postDelayed(this, 20000);
        }
    };
*/

    boolean mSrvStarted = true;

    void StartService() {
        //Start service:
        Intent srvIntent = new Intent(this, BTscanService.class);
        startService(srvIntent);

        bindService(srvIntent, myConnection, Context.BIND_AUTO_CREATE);
        addLog("Start and bind service called");
    }

    void StopService() {
        Intent srvIntent = new Intent(this, BTscanService.class);
        //if (myConnection != null)  unbindService(myConnection);
        if (isBound) {
            // Detach our existing connection.
            if (myConnection != null) {
                unbindService(myConnection);
                isBound = false;
            }
        }

        stopService(srvIntent);
        addLog("stopService called");
    }


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

        if (id == R.id.action_stop) {
            StopService();
            mSrvStarted = false;
            //finishAffinity();

            return true;
        }

        if (id == R.id.action_start) {
            StartService();
            mSrvStarted = true;
            return true;
        }


        return super.onOptionsItemSelected(item);
    }

    static String formatTime(long ts, String format) {
        return new SimpleDateFormat(format, Locale.US).format(new Date(ts));
    }

/*
    protected void findNewBTs() {

        String message = "";

        int permissionCheck = ContextCompat.checkSelfPermission(this,
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

/*
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

        //mCnt++;



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
*/
    public void sendMessageHeilSound(View view) {

        addLog("sendMessageHeilSound was touched "  );

        Switch sw=(Switch)findViewById(R.id.switch_heil_sound);
        if (sw == null ) return;
        if ( mBTSrv == null ) return;

        mBTSrv.mPlayerSound = sw.isChecked();
        addLog("sendMessageHeilSound was touched " + mBTSrv.mPlayerSound );

    }

    public void sendMessage(View view) {

        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();

        //findNewBTs();
        //tBT.time++;
        //addLog(tBT.name  + "/" + tBT.address + "/" + tBT.time);
        //EditText editText = (EditText) findViewById(R.id.hailTimeout);
        //Caused by: java.lang.NumberFormatException: Invalid long: ""
        try {
            mBTSrv.mHailTimeoutSecs = Long.parseLong(hailTimeout.getText().toString());
        } catch (NumberFormatException ex){
            addLog("ERROR: hailTimeout EditText is not a number +\n" );
        }

        try {
            EditText btdev_dtime = (EditText) findViewById(R.id.btdev_discovery_timeout);
            if (btdev_dtime != null)
                mBTSrv.mBTDiscoveryInterval = Long.parseLong(btdev_dtime.getText().toString());
        } catch (NumberFormatException ex){
            addLog("ERROR: hailTimeout EditText is not a number +\n" );
        }

        fetchSrvData();

        //-------------------------------------------------------
        //mIter++;

        //TBD timerHandler.postDelayed(timerRunnable, 0);

        addLog("srv name:" + mBTSrv.getClass().getName() + "\n");

        addLog("srv ticker:" + mBTSrv.mCnt + "\n");

        addLog("srv mHailTimeoutSecs:" + mBTSrv.mHailTimeoutSecs + "\n");

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
        addDevHist(d.name + "/" + d.address + "/hailed: #" + d.hailCount + " " + formatTime(d.time, "HH:mm:ss") + "/found:" + formatTime(d.firstTime, "yy.MM.dd HH:mm:ss") + "\n");
    }

    protected void addDevHist(String s) {

        String message = devHist.getText().toString();
        message = message + s;
        devHist.setText(message);

    }

    protected void clearDevHist() {
        devHist.setText("Hailed Devices:\n");
    }


    protected void addLog(String s) {

        String message = log.getText().toString();
        message = message + s;
        log.setText(message);

    }

    protected void clearLog() {

        log.setText("Log:\n");

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "My Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.laimis.myfirstapp/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "My Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.laimis.myfirstapp/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    /**
     * The BroadcastReceiver that listens for discovered devices and changes the title when
     * discovery is finished
     */
/*
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





                    }



                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mPrevDevLst=mDevLst;
                mDevLst = new ArrayList<String>();

                addLog( "Finished discovery: found "+ mPrevDevLst.size() +" devices\n"  );


            }
        }
    };
*/
}
