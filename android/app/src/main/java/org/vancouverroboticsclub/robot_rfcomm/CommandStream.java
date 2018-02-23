package org.vancouverroboticsclub.robot_rfcomm;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * CommandStream is a helper class in order to store the binary representation of a
 * command (so it can get queued for later).
 *
 * Generally all it does is hold the bytes that need to be sent to the robot and the
 * bytes coming back as a reply thereof. It also provides convenience functions to
 * encode and decode numerical values in the byte stream. This makes it particularly
 * simple to directly exchange C-structs with the robot.
 *
 * CommandStream makes the assumption that the first byte to transfer to the robot is
 * the associated Command's id - and that it's value is not included by the checksum.
 */

public class CommandStream {

    private Command cmd;
    private byte[] resp;
    private int roff;
    private ByteArrayOutputStream os;
    private int checksum;

    CommandStream(Command c) {
        cmd = c;
        os = new ByteArrayOutputStream();
        os.write(cmd.nr);
        checksum = 0;
    }

    public Command command() {
        return cmd;
    }

    // All commands are expected to respond within 500ms.
    public int timeoutMS() {
        return 500;
    }

    public byte[] getRequest() {
        return os.toByteArray();
    }

    public boolean requiresResponse() {
        return 0 != cmd.rsize;
    }

    public boolean setResponse(byte[] response) {
        if (response.length == cmd.rsize || -1 == cmd.rsize) {
            resp = response;
            roff = 0;
            return true;
        } else if (-1 == cmd.nr) {
            return true;
        }
        return false;
    }

    public boolean writeByte(int value) {
        byte[] data = new byte[1];
        data[0] = (byte)value;
        return write(data);
    }
    public boolean writeShort(int value) {
        return write(ByteBuffer.allocate(2).order(targetOrder()).putShort((short)value).array());
    }
    public boolean writeInt(int value) {
        return write(ByteBuffer.allocate(4).order(targetOrder()).putInt(value).array());
    }
    public boolean writeFloat(float value) {
        return write(ByteBuffer.allocate(4).order(targetOrder()).putFloat(value).array());
    }
    public boolean writeChecksum() {
        // use big endian byte order for checksum, just do something different
        return write(ByteBuffer.allocate(2).order(ByteOrder.BIG_ENDIAN).putShort((short)checksum).array(), false);
    }
    public boolean write(byte[] data) {
        return write(data, true);
    }
    public boolean write(byte[] data, boolean updateChecksum) {
        try {
            if (updateChecksum) {
                for (byte b : data) {
                    checksum += (int) b & 0xFF;
                }
            }
            os.write(data);
        } catch (IOException e) {
            Log.e("CommandStream", "io error", e);
            return false;
        }
        return true;
    }

    public float readFloat() { return readFloat(-1); }
    public float readFloat(int offset) {
        if (-1 == offset) {
            offset = roff;
            roff += 4;
        }
        return ByteBuffer.wrap(resp, offset, 4).order(targetOrder()).getFloat();
    }
    public int readInt() { return readInt(-1); }
    public int readInt(int offset) {
        if (-1 == offset) {
            offset = roff;
            roff += 4;
        }
        return ByteBuffer.wrap(resp, offset, 4).order(targetOrder()).getInt();
    }
    public int readShort() { return readShort(-1); }
    public int readShort(int offset) {
        if (-1 == offset) {
            offset = roff;
            roff += 2;
        }
        return ByteBuffer.wrap(resp, offset, 2).order(targetOrder()).getShort();
    }
    public int readByte() { return readByte(-1); }
    public int readByte(int offset) {
        if (-1 == offset) {
            offset = roff;
            roff += 1;
        }
        return resp[offset];
    }
    public String readString() { return readString(-1); }
    public String readString(int length) { return readString(-1, length); }
    public String readString(int offset, int length) {
        if (-1 == offset) {
            offset = roff;
        }
        if (-1 == length) {
            length = 0;
            for (int i=offset; 0 != resp[i] && i < resp.length; ++i, ++length) ;
        }
        if (offset == roff) {
            roff += length;
            while (roff < resp.length && 0 == resp[roff]) {
                roff += 1;
            }
        }
        return new String(resp, offset, length);
    }

    // return the byte order of the target processor. This is important if the protocol
    // with the robot includes binary data (any of the int, float read/write functions
    // above). If the protocol is string based then the byte order has no impact.
    private ByteOrder targetOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }
}
