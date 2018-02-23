/**
 * callout events and timers.
 * */
#ifndef INCLUDED_events_h
#define INCLUDED_events_h

#include <os/os.h>

#define EVENT_HEARTBEAT     ((void*)0)
#define EVENT_RX_TIMEOUT    ((void*)1)

struct os_callout hbTimer; // timer for EVENT_HEARTBEAT
struct os_callout rxTimer; // timer for EVENT_RX_TIMEOUT

#endif
