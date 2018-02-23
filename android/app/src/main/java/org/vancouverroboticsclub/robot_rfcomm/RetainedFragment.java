package org.vancouverroboticsclub.robot_rfcomm;

import android.app.Fragment;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

/**
 * Persistent state across (activity) restarts.
 *
 * Device discovery and establishing a BT connection are expensive operations
 * so it makes sense to retain them over activity restarts.
 */


public class RetainedFragment extends Fragment {
    public BluetoothDevice btDevice;
    public Robot robot;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setRetainInstance(true);
    }
}


