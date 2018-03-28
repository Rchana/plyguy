package com.bme.plyguy.myapplication;

import java.io.IOException;
import java.util.Random;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Runnable;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class ArduinoMain extends Activity {

    //Declare buttons & editText
    Button functionOne, checkSockStatus;
    Button changedSocks;
    Button snooze;
    TextView forceValue;
    TextView weightValue;
    TextView statusTitle;
    TextView plyMessage;
    BottomNavigationView navigation;
    ImageView circle;
    ImageView checkmark;
    ImageView upDown;
    ProgressBar progressSpinner;

    int firstFSR; // represents first FSR bluetooth serial data after parsing to int
    int secondFSR; // represents second FSR bluetooth serial data after parsing to int
    int thirdFSR; // represents second FSR bluetooth serial data after parsing to int
    int[] forceValuesMovingAverageFirstFSR = new int[10];
    int[] forceValuesMovingAverageSecondFSR = new int[10];
    int[] forceValuesMovingAverageThirdFSR = new int[10];

    int newForceValueCount;

    int numGoodPressureCycles;
    int numBadPressureCycles;
    int sideSum;

    // pause runnable variables
    boolean stayAtOnePly = false;
    boolean stayAtTwoPly = false;
    boolean removeOnePly = false;

    // import fields to constantly update GUI
    int time = 0;
    final Handler myHandler = new Handler();

    // private EditText editText;

    //Memeber Fields
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inputStream = null;

    // UUID service - This is the type of Bluetooth device that the BT module is
    // It is very likely yours will be the same, if not google UUID for your manufacturer
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module
    public String newAddress = null;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    finish();
                    return true;
//                    mTextMessage.setText(R.string.title_home);
//                    return true;
                case R.id.navigation_dashboard:
                    stayAtOnePly = false;
                    return true;
                case R.id.navigation_notifications:
                    return true;
            }
            return false;
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino_main);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        //Initialising buttons in the view
        //mDetect = (Button) findViewById(R.id.mDetect);
        statusTitle = findViewById(R.id.statusTitle);
        functionOne = (Button) findViewById(R.id.functionOne);
        checkSockStatus = (Button) findViewById(R.id.checkSockStatus);
        forceValue = findViewById(R.id.forceValue);
        weightValue = findViewById(R.id.weightValue);
        circle = findViewById(R.id.circle);
        checkmark = findViewById(R.id.checkmark);
        upDown = findViewById(R.id.upDown);
        plyMessage = findViewById(R.id.plyMessage);
        changedSocks = findViewById(R.id.changedSocks);
        snooze = findViewById(R.id.snooze);
        progressSpinner = findViewById(R.id.progressSpinner);
        //getting the bluetooth adapter value and calling checkBTstate function
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        /*
         *  Buttons are set up with onclick listeners so when pressed a method is called
         *  In this case send data is called with a value and a toast is made
         *  to give visual feedback of the selection made
         */

        Timer myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {UpdateGUI();}
        }, 0, 110);

        functionOne.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // sendData("1");
                Toast.makeText(getBaseContext(), "Function 1", Toast.LENGTH_SHORT).show();
            }
        });

        checkSockStatus.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            if(firstFSR > 1000) {
                weightValue.setText("Last manually detected weight: " + ">> 10 kg");
            } else {
                weightValue.setText("Last manually detected weight: " + String.valueOf(firstFSR/100.0) + "kg");
            }

            Random rand = new Random();

            // nextInt is normally exclusive of the top value,
            // so add 1 to make it inclusive
            int randomNum = rand.nextInt((24 - 21) + 1) + 21;
            AlertDialog.Builder builder = new AlertDialog.Builder(ArduinoMain.this);
            builder.setMessage(Html.fromHtml("<b>Temperature:</b> " + randomNum + (char) 0x00B0 + "C" + "<br><b>Last Used:</b> March 28, 2018" + "<br><b>Last Doctor Visit:</b> March 24, 2018"))
                    .setCancelable(false)
                    .setPositiveButton("DISMISS", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            //do things
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            }
        });

        changedSocks.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            statusTitle.setText("Checking Ply Amount");
            statusTitle.setBackgroundColor(getResources().getColor(R.color.colorDarkBlue));
            plyMessage.setVisibility(View.INVISIBLE);
            circle.setVisibility(View.INVISIBLE);
            changedSocks.setVisibility(View.INVISIBLE);
            snooze.setVisibility(View.INVISIBLE);
            upDown.setVisibility(View.INVISIBLE);
            forceValue.setText("Checking Ply Amount");
            progressSpinner.setVisibility(View.VISIBLE);
            progressSpinner.getIndeterminateDrawable().setColorFilter(0xFFcc0000,
                        android.graphics.PorterDuff.Mode.MULTIPLY);
            findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightGrey));

            final Timer timer = new Timer();
            timer.schedule( new TimerTask() {
                int numCounts;
                int[] firstFSRMovingAverageInTimer = new int[30];
                int[] sidesFSRMovingAverageInTimer = new int[30];
                int firstSum;
                int sidesSumTimer;

                public void run() {
                    // do your work
                    firstSum = 0;
                    sidesSumTimer = 0;
                    Log.d("PlyGuy", "FSRs I'm reading: " + firstFSR + " " + secondFSR + " " + thirdFSR + " ");
                    numCounts++;
                    firstFSRMovingAverageInTimer[numCounts%30] = firstFSR;
                    sidesFSRMovingAverageInTimer[numCounts%30] = secondFSR + thirdFSR;
                    if(numCounts > 50 ) {
                        for(int i = 0; i < firstFSRMovingAverageInTimer.length; i++) {
                            firstSum += firstFSRMovingAverageInTimer[i];
                            sidesSumTimer += sidesFSRMovingAverageInTimer[i];
                            Log.d("PlyGuy", Integer.toString(sidesSumTimer/3));
                        }
                        sideSum = 0;
                        if(firstSum > 22000) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressSpinner.setVisibility(View.INVISIBLE);
                                    circle.setVisibility(View.VISIBLE);
                                    plyMessage.setVisibility(View.VISIBLE);
                                    statusTitle.setText("Apply Sock Ply!");
                                    forceValue.setText("Apply Sock Ply");
                                    plyMessage.setText("Add \n1 Ply");
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                    changedSocks.setVisibility(View.VISIBLE);
                                    upDown.setVisibility(View.VISIBLE);
                                    snooze.setVisibility(View.VISIBLE);
                                    stayAtOnePly = true;
                                    stayAtTwoPly = true;
                                    timer.cancel();
                                }
                            });
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressSpinner.setVisibility(View.INVISIBLE);
                                    checkSockStatus.setVisibility(View.VISIBLE);
                                    checkmark.setVisibility(View.VISIBLE);
                                    statusTitle.setText("All Good!");
                                    forceValue.setText("All Good");
                                    statusTitle.setBackgroundColor(getResources().getColor(R.color.colorDarkGreen));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightGreen));
                                    stayAtOnePly = false;
                                    stayAtTwoPly = false;
                                    removeOnePly = false;
                                    numGoodPressureCycles = 50;
                                    numBadPressureCycles = 0;
                                }
                            });
                        }
                        timer.cancel();
                    }
                }
            }, 0, 110);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // connection methods are best here in case program goes into the background etc

        //Get MAC address from DeviceListActivity
        Intent intent = getIntent();
        newAddress = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        // Set up a pointer to the remote device using its address.
        BluetoothDevice device = btAdapter.getRemoteDevice(newAddress);

        //Attempt to create a bluetooth socket for comms
        try {
            btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e1) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create Bluetooth socket", Toast.LENGTH_SHORT).show();
        }

        // Establish the connection.
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();        //If IO exception occurs attempt to close socket
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
            }
        }

        // Create a data stream so we can talk to the device
        try {
            outStream = btSocket.getOutputStream();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create bluetooth outstream", Toast.LENGTH_SHORT).show();
        }

        // Create a data stream so we can talk to the device
        try {
            inputStream = btSocket.getInputStream();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR - Could not create bluetooth inputstream", Toast.LENGTH_SHORT).show();
        }

        try {
            byte[] buffer = new byte[256];
            int bytes;
            int i = 0;

            while(i<2) {
                bytes = inputStream.read(buffer); //read bytes from input buffer
                String readMessage = new String(buffer, 0, bytes);
                Log.d("PlyGuy",readMessage);
                i++;
            }
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "ERROR - FAILED", Toast.LENGTH_SHORT).show();
            finish();
        }

        //When activity is resumed, attempt to send a piece of junk data ('x') so that it will fail if not connected
        // i.e don't wait for a user to press button to recognise connection failure
        // sendData("x");
    }

    @Override
    public void onPause() {
        super.onPause();
        //Pausing can be the end of an app if the device kills it or the user doesn't open it again
        //close all connections so resources are not wasted

        //Close BT socket to device
        try     {
            btSocket.close();
        } catch (IOException e2) {
            Toast.makeText(getBaseContext(), "ERROR - Failed to close Bluetooth socket", Toast.LENGTH_SHORT).show();
        }
    }
    //takes the UUID and creates a comms socket
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    //same as in device list activity
    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "ERROR - Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    // Timer functions for updating GUI

    private void UpdateGUI() {
        time++;
        //tv.setText(String.valueOf(i));
        myHandler.post(myRunnable);
    }

    final Runnable myRunnable = new Runnable() {
        String forceValueMessage = "All Good!";
        public void run() {
            String stored = "";
            try {
                String readMessage;
                byte[] buffer = new byte[256];
                int bytes;
                int i = 0;

                while(i<4){
                    if(inputStream.available() > 12) { // read if at least 13 digits are present
                        bytes = inputStream.read(buffer); //read bytes from input buffer
                        readMessage = new String(buffer, 0, bytes);
                        stored = stored + " " + readMessage;
                        Log.d("PlyGuy", readMessage);
                        if (!readMessage.isEmpty()) {
                            if(stayAtOnePly || stayAtTwoPly || removeOnePly) {
                                try {
                                    firstFSR = Integer.parseInt(readMessage.substring(0,4));
                                    secondFSR = Integer.parseInt(readMessage.substring(5, 9));
                                    thirdFSR = Integer.parseInt(readMessage.substring(10, 14));
                                }catch(Exception e){
                                    Log.d("PlyGuy", "Failed the checking");
                                }
                                continue;
                            }

                            try {
                                int bottomSum = 0;
                                sideSum = 0;
                                firstFSR = Integer.parseInt(readMessage.substring(0,4));
                                secondFSR = Integer.parseInt(readMessage.substring(5, 9));
                                thirdFSR = Integer.parseInt(readMessage.substring(10, 14));

                                Log.d("PlyGuy", Boolean.toString(stayAtOnePly));
                                forceValuesMovingAverageFirstFSR[newForceValueCount%10] = firstFSR;
                                forceValuesMovingAverageSecondFSR[newForceValueCount%10] = secondFSR;
                                forceValuesMovingAverageThirdFSR[newForceValueCount%10] = thirdFSR;
                                newForceValueCount++;

                                if(newForceValueCount == 1000) { newForceValueCount = 0; } // prevent overflow
                                for(int index = 0; index < forceValuesMovingAverageFirstFSR.length; index++) {
                                    bottomSum += forceValuesMovingAverageFirstFSR[index];
                                    sideSum += forceValuesMovingAverageSecondFSR[index] + forceValuesMovingAverageThirdFSR[index];
                                }
                                Log.d("PlyGuy", "bottomSum: " + String.valueOf(bottomSum) + " " + String.valueOf(sideSum));

                                if(bottomSum > 7000) {
                                    numGoodPressureCycles = 0;
                                    numBadPressureCycles++;
                                    if(numBadPressureCycles > 30) {
                                        NotificationCompat.Builder mBuilder =
                                                new NotificationCompat.Builder(ArduinoMain.this)
                                                        .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                                        .setContentTitle("PlyGuy")
                                                        .setContentText("Apply Sock Ply!");
                                        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                        mNotificationManager.notify(001, mBuilder.build());

                                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                        // Vibrate for 500 milliseconds
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
                                        }else{
                                            //deprecated in API 26
                                            v.vibrate(500);
                                        }

                                        Log.d("PlyGuy", "A little too much");
                                        forceValueMessage = "Apply Sock Ply!";
                                        statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                        findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                        plyMessage.setText("Add \n1 Ply");
                                        plyMessage.setVisibility(View.VISIBLE);
                                        checkmark.setVisibility(View.INVISIBLE);
                                        checkSockStatus.setVisibility(View.INVISIBLE);
                                        circle.setVisibility(View.VISIBLE);
                                        changedSocks.setVisibility(View.VISIBLE);
                                        upDown.setVisibility(View.VISIBLE);
                                        snooze.setVisibility(View.VISIBLE);
                                        stayAtOnePly = true;
                                    }
                                }
                                if(bottomSum > 9000 && numBadPressureCycles > 10) { // if it greatly exceeds threshold and already had some bad values
                                    Log.d("PlyGuy", "Too much");
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(ArduinoMain.this)
                                                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                                    .setContentTitle("PlyGuy")
                                                    .setContentText("Apply Sock Ply!");
                                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    mNotificationManager.notify(001, mBuilder.build());

                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    // Vibrate for 500 milliseconds
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
                                    }else{
                                        //deprecated in API 26
                                        v.vibrate(500);
                                    }
                                    forceValueMessage = "Apply Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                    plyMessage.setText("Add \n2 Ply");
                                    plyMessage.setVisibility(View.VISIBLE);
                                    checkmark.setVisibility(View.INVISIBLE);
                                    checkSockStatus.setVisibility(View.INVISIBLE);
                                    circle.setVisibility(View.VISIBLE);
                                    changedSocks.setVisibility(View.VISIBLE);
                                    upDown.setVisibility(View.VISIBLE);
                                    snooze.setVisibility(View.VISIBLE);
                                    numGoodPressureCycles = 0;
                                    stayAtTwoPly = true;
                                } else if(sideSum > 16000) {
                                    Log.d("PlyGuy", "Too little");
                                    NotificationCompat.Builder mBuilder =
                                            new NotificationCompat.Builder(ArduinoMain.this)
                                                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                                                    .setContentTitle("PlyGuy")
                                                    .setContentText("Remove Sock Ply!");
                                    NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                    mNotificationManager.notify(001, mBuilder.build());

                                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                    // Vibrate for 500 milliseconds
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        v.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE));
                                    }else{
                                        //deprecated in API 26
                                        v.vibrate(500);
                                    }
                                    numGoodPressureCycles = 0;
                                    forceValueMessage = "Remove Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                    plyMessage.setText("Remove \n1 Ply");
                                    plyMessage.setVisibility(View.VISIBLE);
                                    checkmark.setVisibility(View.INVISIBLE);
                                    checkSockStatus.setVisibility(View.INVISIBLE);
                                    circle.setVisibility(View.VISIBLE);
                                    changedSocks.setVisibility(View.VISIBLE);
                                    upDown.setVisibility(View.VISIBLE);
                                    snooze.setVisibility(View.VISIBLE);
                                    numGoodPressureCycles = 0;
                                    removeOnePly = true;
                                }
                                else if (bottomSum < 6000){ // pressure is gone; need consecutive tries until good
                                    numGoodPressureCycles++;
                                    numBadPressureCycles = 0;
                                    if(numGoodPressureCycles > 20) {
                                        statusTitle.setBackgroundColor(getResources().getColor(R.color.colorDarkGreen));
                                        findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightGreen));
                                        checkmark.setVisibility(View.VISIBLE);
                                        checkSockStatus.setVisibility(View.VISIBLE);
                                        circle.setVisibility(View.INVISIBLE);
                                        plyMessage.setVisibility(View.INVISIBLE);
                                        changedSocks.setVisibility(View.INVISIBLE);
                                        snooze.setVisibility(View.INVISIBLE);
                                        upDown.setVisibility(View.INVISIBLE);
                                        forceValueMessage = "All Good!";
                                    }
                                }
                                forceValue.setText(forceValueMessage);
                                statusTitle.setText(forceValueMessage);
                                if(numGoodPressureCycles > 30) {
                                    forceValue.setText("All Good!");
                                    statusTitle.setText("All Good!");
                                }
                            } catch(Exception e) {
                                Log.d("PlyGuy", "Invalid format");
                            }
                        }
                    }
                    i++;
                }
            } catch (IOException e) {
                // Toast.makeText(getBaseContext(), "ERROR - disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };
}