package org.vancouverroboticsclub.robot_rfcomm;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The UI is a simple activity showing all info and controls.
 *
 * The implementation is meant as a starting point for a real app and does make some assumptions:
 *  - the app is for a specific robot and has a hardcoded MAC address to connect to
 *  - the robot (it's BT module) is expected to be up and running when the app is started
 *  - there is no recovery mechanism implemented in case the communication with the robot should
 *    get out of sync - a restart of the app is required.
 *  - Most of this class, other than the MAC address \c handleMessage and the two UI sepcific button
 *    callbacks could be refactored, without modification, into a generic base class and re-used
 *    for multiple activities (or apps).
 *  - All communication with the robot is initiated by the UI, this activity.
 */
public class MainActivity
        extends AppCompatActivity
        implements Handler.Callback
{
    private final static String LOGTAG = "ROBOT";
    // actual value doesn't matter, but it needs to be unique and the same across restarts.
    private final static String TAG_RETAINED_FRAGMENT = "org.vancouverroboticsclub.robot_rfcomm.Robot";

    // This is the MAC address of the serial BT module, replace with the one you're using.
    private final static String ADDRESS = "00:06:66:45:08:0C";

    // These should not have to change regardless of your app.
    private BluetoothAdapter btAdapter;
    private BroadcastReceiver bcReceiver;

    private Handler handler;
    private Runnable periodic;
    private RetainedFragment fragment;

    // Helper function to setup the connection to the robot, starts bonding if necessary.
    // Returns \c true if connection to robot is established, \c false otherwise.
    private boolean setupRobot(int bondState) {
        switch (bondState) {
            case -1:
                Log.e(LOGTAG, "Bond state change msg doesn't include state");
                break;
            case BluetoothDevice.BOND_NONE:
                Log.d(LOGTAG, "Create a bond.");
                fragment.btDevice.createBond();
                break;
            case BluetoothDevice.BOND_BONDED:
                Log.d(LOGTAG, "Bonded, let's start");
                fragment.robot = new Robot(fragment.btDevice, handler);
                fragment.robot.getID();
                return true;
            case BluetoothDevice.BOND_BONDING:
                Log.d(LOGTAG, "Bonding already in progress");
                break;
        }

        if (null != fragment.robot) {
            fragment.robot.disconnect();
            fragment.robot = null;
        }

        return false;
    }

    // Called by Android when the App is started - more specifically when the activity is started.
    // This is the place to initialize the UI and connect to the robot, or recover the connection
    // if it was established previously (and stored in the retained fragment)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // There's no point in doing anything if BT isn't functional.
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (null == btAdapter || !btAdapter.isEnabled()) {
            Toast oops = Toast.makeText(getApplicationContext(), "Bluetooth not enabled, enable Bluetooth and try again!", Toast.LENGTH_LONG);
            oops.show();
            return;
        }

        // Restore connection to robot if previously established. If the fragment doesn't exist
        // create one and store it persistently
        fragment = (RetainedFragment)getFragmentManager().findFragmentByTag(TAG_RETAINED_FRAGMENT);
        if (null == fragment) {
            fragment = new RetainedFragment();
            getFragmentManager().beginTransaction().add(fragment, TAG_RETAINED_FRAGMENT).commit();
        }

        // We are interested in some broadcast messages that are being sent out by the BT adapter
        // and BT device on certain occasions.
        bcReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice dev = (BluetoothDevice)intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (dev.getAddress().equals(ADDRESS)) {
                        Log.d(LOGTAG, "Robot discovered");
                        // they say to cancel discovery before you do anything.
                        btAdapter.cancelDiscovery();
                        fragment.btDevice = dev;
                    } else {
                        Log.d(LOGTAG, "Discovered device " + dev.getAddress());
                    }
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    Log.d(LOGTAG, "Discovery done");
                    if (null != fragment.btDevice) {
                        fragment.btDevice.createBond();
                    }
                } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    setupRobot(intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(bcReceiver, filter);

        // We also need a handler for the messages sent by the robot.
        handler = new Handler(this);

        // Now that we have all the handlers and receivers it's time to make sure we have a
        // connection to the robot. If we don't yet, start the BT discovery process. If the device
        // is discovered make sure it's bonded.
        if (null == fragment.btDevice) {
            for (BluetoothDevice dev : btAdapter.getBondedDevices()) {
                if (dev.getAddress().equals(ADDRESS)) {
                    fragment.btDevice = dev;
                    Log.d(LOGTAG, "Robot already bonded");
                    break;
                }
                Log.d(LOGTAG, "already paired with " + dev.getAddress());
            }
        }
        if (null == fragment.btDevice) {
            btAdapter.startDiscovery();
        } else if (null == fragment.robot) {
            setupRobot(fragment.btDevice.getBondState());
        } else {
            // There is a valid connection to the robot -> set ourselves as the receiver for
            // messages and get the ID so we can update the title bar.
            fragment.robot.setHandler(handler);
            fragment.robot.getID();
        }

        // This runnable is optional and demonstrates a simple way of performing periodic tasks.
        // Note that this approach is not very time accurate but it's good enough to send a PING
        // to the robot and potentially update the UI.
        periodic = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(periodic, 10000);
                Log.d(LOGTAG, "- tick -");
                if (null != fragment.robot) {
                    fragment.robot.ping();
                }
            }
        };
        periodic.run();
    }

    // According to the documentation the BT discovery process is expensive enough that they
    // recommend cancelling it ASAP. If this activity is destroyed it is certainly obsolete.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != btAdapter && btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
    }

    // If the entire app is closed and not just sent to the background, or a different activity
    // is activated, then we should clear the persisted fragment. This releases all resources
    // associated with the fragment.
    @Override
    public void onPause() {
        super.onPause();
        if (isFinishing()) {
            getFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    // This is the callback invoked for the \c Handler created in \c onCreate. All messages sent by
    // the robot come through this function.
    // We need to process all those that have an impact on the UI.
    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            case Robot.MSG_ID: {
                Robot.Id id = (Robot.Id) msg.obj;
                Log.d(LOGTAG, "MSG_ID: " + id);
                String title = id.name + ": " + id.date + " " + id.time;
                setTitle(title);
            } break;

            case Robot.MSG_VALUE: {
                Robot.Value value = (Robot.Value)msg.obj;
                Log.d(LOGTAG, "MSG_VALUE: " + value);
                String v = Float.toString(value.value);
                ((TextView)findViewById(R.id.getValue)).setText(v);
            } break;

        }
        return true;
    }

    // Callback for the "Refersh" button
    public void onValueGet(View view)  {
        if (null != fragment.robot) {
            fragment.robot.getValue();
        } else {
            Log.d(LOGTAG, "onValueGet -> no connection to robot, ignore");
        }
    }

    // Callback for the "Set" button
    public void onValueSet(View view) {
        if (null != fragment.robot) {
            float value = Float.valueOf(((EditText) findViewById(R.id.setValueValue)).getText().toString());
            int increment = Integer.valueOf(((EditText) findViewById(R.id.setValueIncrement)).getText().toString());
            fragment.robot.setValue(value, increment);
        } else {
            Log.d(LOGTAG, "onValueSet -> no connection to robot, ignore");
        }
    }
}
