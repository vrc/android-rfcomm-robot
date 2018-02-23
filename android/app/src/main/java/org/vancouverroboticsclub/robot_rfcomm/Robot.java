package org.vancouverroboticsclub.robot_rfcomm;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Implementation of a simple protocol with a robot.
 *
 * This robot's protocol consists of 4 possible commands, a ping which just responds
 * with the same ping back, a message to retrieve the robots name and date/time when
 * programmed, and 2 messages to set and retrieve a float value.
 *
 * There is no logic in this example, all it does is package the robot's responses and
 * forward them to the provided handler (in this case the UI).
 *
 * There is a slight wrinkle in the robot's implementation, each time it receives a PING
 * command it increments the float value by a tiny bit. This can be used to verify that
 * the float value is correctly interpreted on both ends of the rfcomm socket.
 */

public class Robot extends RobotBase {

    private Handler handler;

    // These are the messages this class sends to the assigned \c Handler.
    // Note that these IDs are app internal and have no relationship to IDs used by the
    // communication protocol over the actual BT connection.
    public final static int MSG_ID    = 0x00;
    public final static int MSG_VALUE = 0x01;

    Robot(BluetoothDevice dev, Handler hndlr) {
        super(dev);
        handler = hndlr;
    }

    public void setHandler(Handler hndlr) {
        handler = hndlr;
    }

    // object for \c MSG_ID
    public class Id {
        public final String name;
        public final String date;
        public final String time;
        Id(String n, String d, String t) {
            name = n;
            date = d;
            time = t;
        }
    }

    // object for \c MSG_VALUE
    public class Value {
        public final float value;
        Value(float v) {
            value = v;
        }
    }

    // Robot specific API to kick off requests
    public void ping() {
        sendCommand(Command.PING);
    }
    public void getID() {
        sendCommand(Command.ID);
    }
    public void getValue() {
        sendCommand(Command.VALUE_GET);
    }
    public void setValue(float value, int pingIncrement) {
        CommandStream cmd = new CommandStream(Command.VALUE_SET);
        cmd.writeFloat(value);
        cmd.writeInt(pingIncrement);
        cmd.writeChecksum();
        sendCommand(cmd);
    }

    // Extract message and forward data to UI
    @Override
    protected void processResponse() {
        Message msg = null;
        switch (activeCmd.command()) {
            case NONE: {
                Log.d(LOGTAG, "Got response of size " + response.length + " but no msg is active");
            } break;

            case PING: {
                int sync = activeCmd.readByte();
                if (0 != sync) {
                    Log.e(LOGTAG, "Ping returned " + sync + " (expected 0)");
                }
            } break;

            case ID: {
                String name = activeCmd.readString();
                String date = activeCmd.readString();
                String time = activeCmd.readString();
                Log.d(LOGTAG, "ID: " + name + " " + date + " " + time);

                Id obj = new Id(name, date, time);
                msg = handler.obtainMessage(MSG_ID, obj);
            } break;

            case VALUE_GET: {
                float value = activeCmd.readFloat();
                Log.d(LOGTAG, "VALUE: " + value);

                Value obj = new Value(value);
                msg = handler.obtainMessage(MSG_VALUE, obj);
            } break;
        }
        if (null != msg) {
            msg.sendToTarget();
        }
    }

    // Error handling for this robot is simple, log error and start over.
    // You probably wanna do something smarter, the logs aren't really visible
    // if the device is not connected to Android Studio
    @Override
    protected void abortResponse() {
        int received = (null == response) ? 0 : response.length;
        Log.e(LOGTAG, "Error in receiving response to " + activeCmd.command() + ", received " + received + " of " + activeCmd.command().rsize + " bytes");
        clearMessageQueue();
        getID();
    }
}
