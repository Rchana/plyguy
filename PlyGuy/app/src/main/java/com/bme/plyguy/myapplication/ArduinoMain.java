package com.bme.plyguy.myapplication;

import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Runnable;
import java.util.ArrayList;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.util.Log;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
// import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
// import android.view.View.OnKeyListener;
import android.widget.Button;
// import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

public class ArduinoMain extends Activity {

    //Declare buttons & editText
    Button functionOne, checkSockStatus;
    TextView forceValue;
    TextView weightValue;
    TextView statusTitle;
    int parsedData; // represents bluetooth serial data after parsing to int
    int[] forceValuesMovingAverage = new int[10];

    int newForceValueCount;

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

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // addKeyListener();

        //Initialising buttons in the view
        //mDetect = (Button) findViewById(R.id.mDetect);
        statusTitle = findViewById(R.id.statusTitle);
        functionOne = (Button) findViewById(R.id.functionOne);
        checkSockStatus = (Button) findViewById(R.id.checkSockStatus);
        forceValue = findViewById(R.id.forceValue);
        weightValue = findViewById(R.id.weightValue);

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
                if(parsedData > 1000) {
                    weightValue.setText("Last manually detected weight: " + ">> 10 kg");
                } else {
                    weightValue.setText("Last manually detected weight: " + String.valueOf(parsedData/100.0) + "kg");
                }
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
        String forceValueMessage = "All good!";
        int numGoodPressureCycles;
        public void run() {
            String stored = "";
            try {
                String readMessage;
                byte[] buffer = new byte[256];
                int bytes;
                int i = 0;

                while(i<4){
                    if(inputStream.available() > 3) { // read if at least 4 digits are present
                        bytes = inputStream.read(buffer); //read bytes from input buffer
                        readMessage = new String(buffer, 0, bytes);
                        stored = stored + " " + readMessage;
                        if (!readMessage.isEmpty()) {
                            try {
                                int sum = 0;
                                parsedData = Integer.parseInt(readMessage.substring(0,4));
                                Log.d("PlyGuy", "Value: " + parsedData);
                                forceValuesMovingAverage[newForceValueCount%10] = parsedData;
                                newForceValueCount++;
                                if(newForceValueCount == 1000) { newForceValueCount = 0; } // prevent overflow
                                for(int index = 0; index < forceValuesMovingAverage.length; index++) {
                                    sum += forceValuesMovingAverage[index];
                                }
                                Log.d("PlyGuy", "Sum: " + String.valueOf(sum));

                                if(sum > 9900) {
                                    Log.d("PlyGuy", "Too much");
                                    forceValueMessage = "Apply Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.VISIBLE);
                                    numGoodPressureCycles = 0;
                                } else if (sum < 9500){ // pressure is gone; need consecutive tries until good
                                    numGoodPressureCycles++;
                                    if(numGoodPressureCycles > 20) {
                                        statusTitle.setBackgroundColor(getResources().getColor(R.color.colorDarkGreen));
                                        findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightGreen));
                                        findViewById(R.id.checkmark).setVisibility(View.VISIBLE);
                                        findViewById(R.id.circle).setVisibility(View.INVISIBLE);
                                        findViewById(R.id.navigationGood).setVisibility(View.VISIBLE);
                                        findViewById(R.id.addPlyText).setVisibility(View.INVISIBLE);
                                        findViewById(R.id.removePlyText).setVisibility(View.INVISIBLE);
                                        findViewById(R.id.onePly).setVisibility(View.INVISIBLE);
                                        findViewById(R.id.threePly).setVisibility(View.INVISIBLE);
                                        findViewById(R.id.fivePly).setVisibility(View.INVISIBLE);
                                        forceValueMessage = "All Good!";
                                    }
                                }

                                //Laura's Additions
                                else if (sum > 9700 && sum < 9900) {
                                    forceValueMessage = "Apply Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightYellow);
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.VISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.INVISIBLE);
                                    numGoodPressureCycles = 0;
                                } else if () {
                                    forceValueMessage = "Apply Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightOrange);
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.VISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.INVISIBLE);
                                    numGoodPressureCycles = 0;
                                }
                                //For Side Sensors
                                if (sum > 9900) {
                                    forceValueMessage = "Remove Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightRed));
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.VISIBLE);
                                    numGoodPressureCycles = 0;
                                } else if (//medium pressure on sides) {
                                    forceValueMessage = "Remove Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightOrange));
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.VISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.INVISIBLE);
                                    numGoodPressureCycles = 0;
                                } else if (//small pressure on sides) {
                                    forceValueMessage = "Remove Sock Ply!";
                                    statusTitle.setBackgroundColor(Color.parseColor("#B70F0A"));
                                    findViewById(R.id.RL).setBackgroundColor(getResources().getColor(R.color.colorLightYellow));
                                    findViewById(R.id.checkmark).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.circle).setVisibility(View.VISIBLE);
                                    findViewById(R.id.navigationGood).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.addPlyText).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.removePlyText).setVisibility(View.VISIBLE);
                                    findViewById(R.id.onePly).setVisibility(View.VISIBLE);
                                    findViewById(R.id.threePly).setVisibility(View.INVISIBLE);
                                    findViewById(R.id.fivePly).setVisibility(View.INVISIBLE);
                                    numGoodPressureCycles = 0;
                                }


                            } catch(Exception e) {
                                Log.d("PlyGuy", "Invalid format");
                            }
                        }
                    }
                    i++;
                }
                forceValue.setText(forceValueMessage);
                statusTitle.setText(forceValueMessage);
            } catch (IOException e) {
                // Toast.makeText(getBaseContext(), "ERROR - disconnected", Toast.LENGTH_SHORT).show();
            }
        }
    };
}