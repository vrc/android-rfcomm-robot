package org.vancouverroboticsclub.robot_rfcomm;

/**
 * Definition of the robot specific commands.
 *
 * The commands, their IDs and the length of their responses should match
 * the actual robot. For the sample implementations look for commands.h
 *
 * The NONE command is internally used by the framework, it's id is arbitrary
 * but the response size should be 0.
 */

public enum Command {
    NONE(         -1,  0),
    PING(       0x00,  1),
    ID(         0x01, 30),
    VALUE_GET(  0x02,  4),
    VALUE_SET(  0x82,  0);

    public final int nr;
    public final int rsize;

    Command(int cmdId, int responseSize) {
        nr = cmdId;
        rsize = responseSize;
    }
}
