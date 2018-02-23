/**
 * If there is no error it's nice to have some feedback that the firmware
 * is still running.
 * */
#ifndef INCLUDED_heartbeat_h
#define INCLUDED_heartbeat_h

#include <os/os_eventq.h>

void heartbeat(int state, int led);
void heartbeatInit(os_event_fn callback);
void heartbeatStart();

#endif
