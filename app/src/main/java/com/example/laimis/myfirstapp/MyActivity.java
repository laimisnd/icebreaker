package com.example.laimis.myfirstapp;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
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
    public final static int MY_PERMISSIONS_WAKE_LOCK = 125;
    public final static int MY_PERMISSIONS_MODIFY_AUDIO_SETTINGS= 126;

    static final int PICK_AUDIO_REQUEST = 1;  // The request code

    private TextView log;
    private TextView devHist;
    private EditText hailTimeout;
    private TextView HailAudio;
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
        /*TextView textLG = (TextView) findViewById(R.id.lastGreeted);
        if ( textLG != null ) {
            outState.putString("lastGreeted",textLG.getText().toString());
        }*/

        outState.putBoolean("mSrvStarted",mSrvStarted);

        super.onSaveInstanceState(outState);

    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mSrvStarted=savedInstanceState.getBoolean("mSrvStarted");

        /*TextView textLG = (TextView) findViewById(R.id.lastGreeted);
        if ( textLG != null ) {
            String s=savedInstanceState.getString("lastGreeted");
            textLG.setText(s);
        }*/

        fetchSrvData();
        displayLastHailed();

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

    void displayLastHailed()
    {

        TextView textLG = (TextView) findViewById(R.id.lastGreeted);
        if ( textLG != null && mBTSrv != null) {
            if (mBTSrv.lastHailedDev != null) {

                String s="!!! "+ getString(R.string.text_last_hailed_device)
                        + " " + ( (mBTSrv.lastHailedDev.name == null) ? getString(R.string.text_device_name_unknown) :mBTSrv.lastHailedDev.name)
                        + " "+ mBTSrv.lastHailedDev.strMajorClass
                        +" "+ formatTime(mBTSrv.lastHailedDev.time, "MM.dd HH:mm:ss") + " !!!";
                textLG.setText(s);
            }
            else {
                addLog("WARNING: lastHailedDev is null");
            }
        } else {
            addLog("WARNING: lastGreeted or mBTSrv is null");
        }

    }

    void fetchSrvData() {

        if (mBTSrv == null) return;

        addLog("srv name:" + mBTSrv.getClass().getName());

        addLog("srv timer ticker:" + mBTSrv.mCnt);

        addLog("srv mHailTimeoutSecs:" + mBTSrv.mHailTimeoutSecs);
        addLog("srv Timer Interval:" + mBTSrv.getBTDiscoveryInterval());
        addLog("srv destroyed:" + mBTSrv.mDestroyed);

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
            //addDevHist(dv.name + "/" + dv.address + "/"+ (Long)(dv.time/1000) );
            addDevHist(dv);
        }
        addDevHist("Restored List size:" + ii);
//------------------------------------
        addLog("History size:" + mBTSrv.mHailedDevs.size() );

        /*Iterator<String> itd = mHailedDevLst.iterator();
        while (itd.hasNext()) {
            addDevHist(itd.next().toString());
        }*/
        //mHailTimeoutSecs = savedInstanceState.getLong("mHailTimeoutSecs");

        //editText.setText(Long.toString(mBTSrv.mHailTimeoutSecs));
        hailTimeout.setText(String.valueOf(mBTSrv.mHailTimeoutSecs));

        EditText btdev_dtime = (EditText) findViewById(R.id.btdev_discovery_timeout);
        if ( btdev_dtime != null ) {
            btdev_dtime.setText(String.valueOf(mBTSrv.getBTDiscoveryInterval()));
        }

        Switch sw=(Switch)findViewById(R.id.switch_heil_sound);
        if (sw != null ) {
            sw.setChecked(mBTSrv.mPlayerSound );
        }

        Switch swSrv=(Switch)findViewById(R.id.switch_service_onoff);
        if (swSrv != null ) {
            //swSrv.setChecked(isBound );
            swSrv.setChecked(mBTSrv.mDiscoveryStarted);
            addLog("mDiscoveryStarted: "+mBTSrv.mDiscoveryStarted);
        }

        HailAudio.setText(getDisplayName(mBTSrv.getAudioUri()));

        displayLastHailed();
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

        HailAudio = (TextView) findViewById(R.id.hailAudio);


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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED
                ) {
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WAKE_LOCK},
                    MY_PERMISSIONS_WAKE_LOCK);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            message = message + "Wake Lock  permision is not granted damn you";
            log.setText(message);

            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.MODIFY_AUDIO_SETTINGS) != PackageManager.PERMISSION_GRANTED
                ) {
            // Should we show an explanation?
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS},
                    MY_PERMISSIONS_MODIFY_AUDIO_SETTINGS);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
            message = message + "MODIFY_AUDIO_SETTINGS  permision is not granted damn you";
            log.setText(message);

            return;
        }


        IntentFilter filter = new IntentFilter(BTscanService.NOTIFICATION_HAILED);
        this.registerReceiver(mReceiver, filter);

        filter = new IntentFilter(BTscanService.NOTIFICATION_BTSCAN_FINISHED);
        this.registerReceiver(mReceiver, filter);


        if (savedInstanceState != null) mSrvStarted=savedInstanceState.getBoolean("mSrvStarted");
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
                    if (mBTSrv == null) return;
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
                    if (mBTSrv == null) return;
                    if (on) mBTSrv.startBTDiscovery(); else mBTSrv.stopBTDiscovery();
                    addLog("discovery button:  " + on + " srv field:"+mBTSrv.mDiscoveryStarted);
                    //if (on) StartService(); else StopService();
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

        this.unregisterReceiver(mReceiver);

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




    @Override
    protected void onResume() {
        super.onResume();

        EditText mEt;
        LinearLayout mLinearLayout;

        mEt = (EditText) findViewById(R.id.btdev_discovery_timeout);
        mLinearLayout = (LinearLayout) findViewById(R.id.linear_main_layout);
        //do not give the editbox focus automatically when activity starts
        if (mEt!=null)        mEt.clearFocus();
        if (mLinearLayout!=null) mLinearLayout.requestFocus();
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
        mSrvStarted=true;
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
        mSrvStarted=false;
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
            //finishAffinity();

            return true;
        }

        if (id == R.id.action_start) {
            StartService();
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
        addLog(String.format("#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss")  );


        if (ba.isDiscovering()) {
            addLog("BT discovery in progress...");
        } else {



            clearLog();
            addLog(String.format("#%d ", mCnt) + formatTime(System.currentTimeMillis(),"yy/MM/dd HH:mm:ss")   );
            addLog("History size:"+mHailedDevs.size() );
            addLog("BT discovery started, prev list size: "+mPrevDevLst.size());


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
    /*public void sendMessageHeilSound(View view) {

        addLog("sendMessageHeilSound was touched "  );

        Switch sw=(Switch)findViewById(R.id.switch_heil_sound);
        if (sw == null ) return;
        if ( mBTSrv == null ) return;

        mBTSrv.mPlayerSound = sw.isChecked();
        addLog("sendMessageHeilSound was touched " + mBTSrv.mPlayerSound );

    }
    */

    public void btPickContact(View view) {
        Intent pickAudioIntent = new Intent(Intent.ACTION_GET_CONTENT);
        pickAudioIntent.setType("audio/*"); // Show user only contacts w/ phone numbers
        startActivityForResult(Intent.createChooser(pickAudioIntent, getString(R.string.chooser_pick_audio) ), PICK_AUDIO_REQUEST);
    }

    private String selectedAudioPath;
    private String filemanagerstring;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_AUDIO_REQUEST) {
                Uri selectedAudioUri = data.getData();
                if (selectedAudioUri != null )
                    if ( mBTSrv != null ) {
                        mBTSrv.setAudioUri(selectedAudioUri);
                        HailAudio.setText(getDisplayName(selectedAudioUri));
                    }
                    else
                        addLog("ERROR: can't set Audio URI, damned service is null");
                else
                    addLog("ERROR: can't set Audio URI, damned URI is null");
                /*
                //OI FILE Manager
                filemanagerstring = selectedAudioUri.getPath();

                //MEDIA GALLERY
                selectedAudioPath = getPath(selectedAudioUri);

                //DEBUG PURPOSE - you can delete this if you want
                if(selectedAudioPath!=null)
                    addLog(selectedAudioPath);
                else addLog("selectedAudioPath is null");
                if(filemanagerstring!=null)
                    addLog(filemanagerstring);
                else addLog("filemanagerstring is null");

                //NOW WE HAVE OUR WANTED STRING
                if(selectedAudioPath!=null)
                    addLog("selectedAudioPath is the right one for you!");
                else
                    addLog("filemanagerstring is the right one for you!");*/
            }
        }
    }


    public String getDisplayName(Uri uri) {
        if (uri == null) return "";
        String[] projection = { MediaStore.Audio.Media.DISPLAY_NAME };
        Cursor cursor = this.getContentResolver().query(uri, projection, null, null, null);
        if(cursor!=null  && cursor.moveToFirst() )
        {
            return cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
        }
        else return "";
    }


    public void btExit(View view) {
        StopService();
        System.exit(0);
    }

    public void sendMessage(View view) {

        clearLog();

        if (mBTSrv==null)
        {
            addLog("Start the service if you want to do anything" );
            return;
        }
        else
        {
            addLog(formatTime(System.currentTimeMillis(), "HH:mm:ss")+"Service ref found");
        }

        try {
            mBTSrv.mHailTimeoutSecs = Long.parseLong(hailTimeout.getText().toString());
        } catch (NumberFormatException ex){
            addLog("ERROR: hailTimeout EditText is not a number" );
        }

        try {
            EditText btdev_dtime = (EditText) findViewById(R.id.btdev_discovery_timeout);
            if (btdev_dtime != null)
                mBTSrv.setBTDiscoveryInterval(Long.parseLong(btdev_dtime.getText().toString()));
        } catch (NumberFormatException ex){
            addLog("ERROR: hailTimeout EditText is not a number" );
        }

        fetchSrvData();
        //displayLastHailed();

        //-------------------------------------------------------
        //mIter++;

        //TBD timerHandler.postDelayed(timerRunnable, 0);



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
        addDevHist(d.name + "/" + d.address + "/hailed: #" + d.hailCount + " " + formatTime(d.time, "MM.dd HH:mm:ss") + " " + d.strMajorClass);
    }

    protected void addDevHist(String s) {

        String message = devHist.getText().toString();
        message = message + s;
        devHist.setText(message);

    }

    protected void clearDevHist() {
        devHist.setText("Hailed Devices:");
    }


    protected void addLog(String s) {

        String message = log.getText().toString();
        message = message + s + "\n";
        log.setText(message);

    }

    protected void clearLog() {

        log.setText("Log:");

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

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            addLog( "Received action: " + action );
            // When discovery finds a device
            if ( action.equals(BTscanService.NOTIFICATION_HAILED) ) {

                clearLog();
                fetchSrvData();
                //displayLastHailed();

                addLog( "RECEIVED: new hail"  );
            }

            if ( action.equals(BTscanService.NOTIFICATION_BTSCAN_FINISHED) ) {
                clearLog();
                fetchSrvData();
                //displayLastHailed();
                addLog( "RECEIVED: bt scan done"  );
            }
        }
    };

}
