package org.vancouverroboticsclub.robot_rfcomm;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Implementation of the thread to service the rfcomm socket.
 *
 * The thread requires a Handler for construction which it uses to send
 * one of 3 messages to:
 *    MSG_STARTED ... sent once the service thread is up and running.
 *                    can be used to start communication.
 *                    No further information is provided by the message.
 *    MSG_READ    ... sent whenever some bytes were received over the socket.
 *                    Note that the thread has no concept of a message and
 *                    therefore cannot determine if a message is complete or
 *                    more bytes need to be received. These are responsibilities
 *                    of the receiving Handler.
 *                       arg1 ... # bytes in the buffer
 *                       arg2 ... -1
 *                       obj  ... byte[] with the received bytes
 *    MSG_WRITE   ... sent when a message has been sent over the socket. Used to
 *                    initiate sending the next message.
 *                        arg1 ... -1
 *                        arg2 ... -1
 *                        obj  ... byte[] the original message
 *    MSG_USER    ... this message is never sent but if the handler wants to use
 *                    additional messages their IDs can be based on MSG_USER and
 *                    bigger.
 */

public class ConnectionThread extends Thread {
    public final static int MSG_STARTED = 0;
    public final static int MSG_READ = 1;
    public final static int MSG_WRITE = 2;
    public final static int MSG_USER = 10;

    // This is the public, well known SPP uuid, and it seems the RN-42 requires this UUID.
    // Using any other uuid fails to connect.
    private final static UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final static String LOGTAG = "THREAD";

    private final BluetoothDevice device;
    private final BluetoothSocket socket;
    private final InputStream is;
    private final OutputStream os;

    private final Handler handler;

    private byte[] buffer;

    public ConnectionThread(BluetoothDevice dev, Handler hndlr) {
        device = dev;
        handler = hndlr;

        BluetoothSocket s = null;
        InputStream i = null;
        OutputStream o = null;
        try {
            s = device.createRfcommSocketToServiceRecord(MY_UUID);
            try {
                s.connect();

                try {
                    i = s.getInputStream();
                } catch (IOException e) {
                    Log.e(LOGTAG, "Input stream failed", e);
                }

                try {
                    o = s.getOutputStream();
                } catch (IOException e) {
                    Log.e(LOGTAG, "Output stream failed", e);
                }

            } catch (IOException e) {
                Log.e(LOGTAG, "Connection to socket faile", e);
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Socket creation failed.", e);
        }
        socket = s;

        // assuming everything went OK there should be an input and output
        // stream - clean up if that's not the case.
        is = i;
        os = o;
        if ((null == is || null == os) && null != socket) {
            cancel();
        }
    }

    @Override
    public void run() {
        buffer = new byte[1024];

        {
            Message msg = handler.obtainMessage(MSG_STARTED, 0, 0, null);
            msg.sendToTarget();
        }

        while (true) {
            try {
                int count = is.read(buffer);
                byte[] data = new byte[count];
                System.arraycopy(buffer, 0, data, 0, count);
                Message msg = handler.obtainMessage(MSG_READ, count, -1, data);
                msg.sendToTarget();
            } catch (IOException e) {
                Log.e(LOGTAG, "Reading message", e);
                break;
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            if (2 == bytes.length) {
                Log.d(LOGTAG, "write(" + bytes.length + ": " + bytes[0] + " " + bytes[1] + ")");
            }
            os.write(bytes);
            Message msg = handler.obtainMessage(MSG_WRITE, -1, -1, bytes);
            msg.sendToTarget();
        } catch (IOException e) {
            Log.e(LOGTAG, "Writing message", e);
        }
    }

    public void cancel() {
        if (null != socket) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(LOGTAG, "Closing socket failed", e);
            }
        }
    }
}
