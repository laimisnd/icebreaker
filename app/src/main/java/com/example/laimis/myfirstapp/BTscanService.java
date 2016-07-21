package com.example.laimis.myfirstapp;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

public class BTscanService extends Service {

    public static int NOTIFICATION_ID_FOREGROUND_SERVICE=123;
    public static final String NOTIFICATION_HAILED="laimis.BTscanService.HAILED";
    public static final String NOTIFICATION_BTSCAN_FINISHED="laimis.BTscanService.NOTIFICATION_BTSCAN_FINISHED";

    int mStartMode=START_STICKY;       // indicates how to behave if the service is killed

    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    AudioManager mAudioManager;

    //boolean mVolumeLow=false;

    MediaPlayer mediaPlayer ;

    private final IBinder myBinder = new BTLocalBinder();

    private long mBTDiscoveryInterval = 30;//seconds
    //private Timer timer = new Timer();
    /*private TimerTask ttask = new TimerTask() {
        public void run() {
            doBTscan();
        }
    };*/
    //runs without a timer by reposting this handler at the end of the runnable
    boolean mDestroyed = false;
    boolean mDiscoveryStarted = false;
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            doBTscan();
            timerHandler.postDelayed(this, mBTDiscoveryInterval*1000);
            mDiscoveryStarted=true;
        }
    };

    public int mCnt=0;

    long startTime = 0;

    private BluetoothAdapter ba;
    private ArrayList<String> mDevLst;
    private ArrayList<String> mPrevDevLst;

    public LinkedList<String> mlog;

    public LinkedHashMap<String, BTDevice> mHailedDevs;

    BTDevice lastHailedDev;

    private int mHailedDevsSize = 100;
    public  long  mHailTimeoutSecs=3600*12;

    public  boolean mPlayerSound = true;

    public void addLog(String msg) {
        mlog.add(msg);
        if (mlog.size()>75) mlog.removeFirst();
    }
    public void clearLog(){
        //mlog.clear();
    }


    static String formatTime(long ts, String format ) {
        return new SimpleDateFormat(format, Locale.US).format(new Date(ts) );
    }

    public BTscanService() {
    }

    public long getBTDiscoveryInterval(){ return mBTDiscoveryInterval;}

    public void setBTDiscoveryInterval(long pBTDiscoveryInterval) {

        mBTDiscoveryInterval=pBTDiscoveryInterval;
        if (!mDestroyed && mDiscoveryStarted) {
            //timerHandler.removeCallbacks(timerRunnable);
            //timerHandler.postDelayed(timerRunnable, 0);
            startBTDiscovery();
        }
        /*if (timer==null || ttask==null ) return;

        ttask.cancel();
        try {
            ttask = new TimerTask() {
                public void run() {
                    doBTscan();
                }
            };
            timer.scheduleAtFixedRate(ttask, 0, mBTDiscoveryInterval * 1000 );
        } catch  (java.lang.IllegalStateException e)
        {}*/
    }

    public void stopBTDiscovery() {
        timerHandler.removeCallbacks(timerRunnable);
        mDiscoveryStarted=false;
    }

    public void startBTDiscovery() {
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.postDelayed(timerRunnable, 0);
        mDiscoveryStarted=true;
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

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mediaPlayer = MediaPlayer.create(this.getApplicationContext(), R.raw.hello);

        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mSensorManager.registerListener (mSensorReceiver, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);

        //BluetoothManager btm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        //ba = btm.getAdapter();


        addLog("Service Created\n");
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


            /*timer.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            doBTscan();
                        }
                    },
                    0,
                    mBTDiscoveryInterval*1000);*/
            startBTDiscovery();
            //timerHandler.postDelayed(timerRunnable, 0);
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

        mDestroyed=false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {



        // The service is starting, due to a call to startService()
        return mStartMode;
    }

    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        //timer.cancel();
        //ttask.cancel();
        //timerHandler.removeCallbacks(timerRunnable);
        stopBTDiscovery();

        if ( ba != null) {
            ba.cancelDiscovery();
        }

        if (mediaPlayer!=null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
        mSensorManager.unregisterListener(mSensorReceiver);

        mDestroyed=true;
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

    private String getDeviceClassStr(BluetoothDevice bt  )
    {
        String s;
        int numMajorClass;
        boolean hasTelephony;

        if ( bt == null ) return "dev null";

        BluetoothClass btc=bt.getBluetoothClass();
        if (btc == null) return "class null";

        numMajorClass= btc.getMajorDeviceClass();
        hasTelephony=btc.hasService(BluetoothClass.Service.TELEPHONY);

        switch (numMajorClass) {
            case BluetoothClass.Device.Major.HEALTH: s="HEALTH";break;
            case BluetoothClass.Device.Major.PHONE: s="PHONE";break;
            case BluetoothClass.Device.Major.WEARABLE: s="WEARABLE";break;
            default:     s = String.valueOf(numMajorClass);
        }

        if (hasTelephony) s=s+",Telephony";
        return s;
    }

    private final AudioManager.OnAudioFocusChangeListener afChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                public void onAudioFocusChange(int focusChange) {
                    addLog("VOLUME: audio focus received: "+focusChange+" \n");
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK
                            || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                            || focusChange == AudioManager.AUDIOFOCUS_LOSS ) {
                        // Lower the volume
                        if (mediaPlayer!=null && mAudioManager!=null) {
                            mediaPlayer.setVolume(1, 1);
                            addLog("VOLUME: LOW volume\n");
                        }
                    } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN
                            || focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
                            || focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK ) {
                        // Raise it back to normal
                        if (mediaPlayer!=null && mAudioManager!=null) {
                            mediaPlayer.setVolume(mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
                            addLog("VOLUME: MAX volume\n");
                        }
                    }
                }
            };

    int originalVolume;

    public void hailDev(BTDevice dev){

        lastHailedDev = dev;
        try {
            originalVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        //playing sound
        if (mPlayerSound) {
            /*if (!mVolumeLow) {
                addLog("PLAY LOUD: LowrequestAudioFocus \n");
                int result = mAudioManager.requestAudioFocus(afChangeListener,
                // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                            // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        addLog("requestAudioFocus granted\n");
                    } else {
                        addLog("requestAudioFocus not granted\n");
                    }
                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
                //addLog("VOLUME: set max volume \n");
            } else {
                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 1, 0);
                //addLog("VOLUME: set min volume: \n");
            }*/

            addLog("VOLUME: RequestAudioFocus \n");
            int result = mAudioManager.requestAudioFocus(afChangeListener,
                    // Use the music stream.
                    AudioManager.STREAM_MUSIC,
                    // Request permanent focus.
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                addLog("VOLUME: requestAudioFocus granted\n");
            } else {
                addLog("VOLUME: requestAudioFocus not granted\n");
            }

            mediaPlayer.setWakeMode(this.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            /*mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
            {
                @Override
                public void onCompletion(MediaPlayer mp)
                {
                    //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
                   // addLog("VOLUME: reset back to original\n");
                }
            });*/
            mediaPlayer.start(); // no need to call prepare(); create() does that for you

        } else {
            addLog("***Hailing sound turned off \n");
        }

        //sending hail to main window
        Intent intent = new Intent(NOTIFICATION_HAILED);
        sendBroadcast(intent);
    }
    catch (Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        String exceptionAsString = sw.toString();
        addLog(exceptionAsString);

    }
    }

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

                    addLog( "Found BDevice: " + device.getName() + " : " +  device.getAddress() + "  class:" + device.getBluetoothClass().getMajorDeviceClass() + "\n" );

                    if ( device.getBluetoothClass() == null) {
                        addLog( "Class is null, skipping \n" );
                        return;
                    }
                    if ( ! device.getBluetoothClass().hasService(BluetoothClass.Service.TELEPHONY)
                            && device.getBluetoothClass().getMajorDeviceClass() != BluetoothClass.Device.Major.HEALTH
                            && device.getBluetoothClass().getMajorDeviceClass() != BluetoothClass.Device.Major.PHONE
                            && device.getBluetoothClass().getMajorDeviceClass() != BluetoothClass.Device.Major.WEARABLE
                            ) {
                        addLog( "Not human BT device, skipping \n" );
                        return;
                    }

                    mDevLst.add(device.getAddress());
                    addLog( " found dev list size:" + mDevLst.size());

                    //chgeck if new:
                    if ( mPrevDevLst.contains(device.getAddress()) ) {
                        addLog( " existing  device\n" );
                        //update device name if not null: sometimes name comes later on
                        BTDevice dev=mHailedDevs.get(device.getAddress());
                        if (dev !=null)
                            dev.name=device.getName();

                    } else {
                        addLog( " *** Found new device ***\n" );

                        BTDevice dev=mHailedDevs.get(device.getAddress());
                        if ( dev == null ) {

                            addLog("New device in the list\n");
                            addLog("***HAILING NEW***\n");

                            Long st=System.currentTimeMillis();
                            BTDevice ndev=new BTDevice(device.getName(),
                                                        device.getAddress(),
                                                        st,
                                                        (device.getBluetoothClass() != null) ? device.getBluetoothClass().getMajorDeviceClass():0,
                                                        getDeviceClassStr(device)
                            );
                            mHailedDevs.put(device.getAddress(), ndev);

                            hailDev(ndev);

                        } else {
                            if ( System.currentTimeMillis() - dev.time < 1000 * mHailTimeoutSecs )
                                addLog("Too soon to hail again\n");
                            else {
                                addLog("***HAILING EXISTING***\n");
                                addLog("dev last hail time:" + formatTime(dev.time, "yy/MM/dd HH:mm:s") + "\n");
                                addLog("current time:" + formatTime(System.currentTimeMillis(), "yy/MM/dd HH:mm:s") + "\n");
                                addLog("delta time between hails:" + Math.round((System.currentTimeMillis() - dev.time) / 1000) + "\n");
                                dev.time = System.currentTimeMillis();
                                dev.hailCount++;

                                hailDev(dev);
                            }

                        }

                    }

                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                mPrevDevLst=mDevLst;
                mDevLst = new ArrayList<>();

                addLog( "Finished discovery: found "+ mPrevDevLst.size() +" devices\n"  );
                Intent rint = new Intent(NOTIFICATION_BTSCAN_FINISHED);
                sendBroadcast (rint);

            }
        }
    };



        private final SensorEventListener mSensorReceiver = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                addLog("distance " + String.valueOf(event.values[0]) + "\n");
                addLog("MaximumRange " + String.valueOf(mProximitySensor.getMaximumRange()) + "\n");
                /*
                if (event.values[0] < mProximitySensor.getMaximumRange()) {
                    // Lower the volume
                    mVolumeLow = true;
                    addLog("mVolumeLow = true \n");
                } else {
                    // Raise it back to normal
                    mVolumeLow = false;
                    addLog("mVolumeLow = false \n");
                }
                */
            }
        };

}
