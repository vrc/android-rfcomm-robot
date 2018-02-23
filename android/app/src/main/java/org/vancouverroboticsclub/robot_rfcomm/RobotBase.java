package org.vancouverroboticsclub.robot_rfcomm;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;

/**
 * RobotBase implements the basic command communication with the robot.
 *
 * Implements the basic framework for a specific robot - which should
 * be a subclass of RobotBase. It provides the aggregation of received bytes
 * into a command and uses a FIFO in order to maintain the correlation between
 * request and response.
 *
 * The functions that need to be implemented by subclasses are
 *      /c processResponse()
 *      /c abortResponse()
 *
 * \c processResponse is only called once a complete message has been received.
 * Note that the reception buffer is cleared afterwards.
 *
 * \c abortResponse is called if reception of a response does not complete
 * in the specified time.
 * Note that the reception buffer is cleared afterwards.
 */

public class RobotBase
        implements Handler.Callback
{
    private static final int MSG_TIMEOUT = ConnectionThread.MSG_USER;

    protected final ConnectionThread io;
    protected final Handler self;
    protected byte[] response;
    protected CommandStream activeCmd;
    private LinkedList<CommandStream> cmdQueue;

    protected final static String LOGTAG = "Robot";
    private final static char[] hex = "0123456789ABCDEF".toCharArray();

    RobotBase(BluetoothDevice dev) {
        self = new Handler(this);
        io = new ConnectionThread(dev, self);
        io.start();
        cmdQueue = new LinkedList<>();
        activeCmd = new CommandStream(Command.NONE);
    }

    protected void processResponse() {
        // overwrite
    }
    protected void abortResponse() {
        // overwrite
    }

    public void disconnect() {
        io.cancel();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {

            case ConnectionThread.MSG_STARTED:
                Log.d(LOGTAG, "Robot connection active");
                break;

            case ConnectionThread.MSG_READ:
                int count = msg.arg1;
                byte[] obj = (byte[])msg.obj;
                Log.d(LOGTAG, "Received (" + activeCmd + "): " + count + "[" + obj.length + "] " + hexDump(obj, count));
                if (null == response) {
                    response = obj;
                } else {
                    byte[] rcvd = new byte[response.length + count];
                    System.arraycopy(response, 0, rcvd, 0, response.length);
                    System.arraycopy(obj, 0, rcvd, response.length, count);
                    response = rcvd;
                }

                if (activeCmd.setResponse(response)) {
                    self.removeMessages(MSG_TIMEOUT);
                    processResponse();
                    response = null;
                    scheduleNextCommand();
                }
                break;

            case ConnectionThread.MSG_WRITE:
                if (!activeCmd.requiresResponse()) {
                    scheduleNextCommand();
                } else {
                    self.sendEmptyMessageDelayed(MSG_TIMEOUT, activeCmd.timeoutMS());
                }
                break;

            case MSG_TIMEOUT:
                abortResponse();
                response = null;
                scheduleNextCommand();
                break;
        }
        return true;
    }

    protected String hexDump(byte[] bytes, int length) {
        if (length > 0) {
            char[] buf = new char[length * 3 + 1];
            buf[0] = '{';
            for (int i = 0; i < length; ++i) {
                buf[3 * i + 1] = hex[(bytes[i] >> 4) & 0x0F];
                buf[3 * i + 2] = hex[(bytes[i] >> 0) & 0x0F];
                buf[3 * i + 3] = ':';
            }
            buf[length*3] = '}';
            return new String(buf);
        }
        return "{}";
    }

    // Only useful if the robot has single byte commands, no header, no payload, no checksum,...
    protected void sendCommand(Command cmd) {
        sendCommand(new CommandStream(cmd));
    }

    protected void sendCommand(CommandStream cmd) {
        Log.d(LOGTAG, "sendCommand(" + cmd.command() + ")");
        cmdQueue.add(cmd);
        if (activeCmd.command() == Command.NONE) {
            scheduleNextCommand();
        }
    }

    private void scheduleNextCommand() {
        if (cmdQueue.isEmpty()) {
            activeCmd = new CommandStream(Command.NONE);
        } else {
            activeCmd = cmdQueue.removeFirst();
            io.write(activeCmd.getRequest());
        }
    }

    protected void clearMessageQueue() {
        cmdQueue.clear();
    }
}
