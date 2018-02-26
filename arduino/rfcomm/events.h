/**
 * callout events and timers.
 * */
#ifndef INCLUDED_events_h
#define INCLUDED_events_h

#define EVENT_HEARTBEAT     ((void*)0)
#define EVENT_RX_TIMEOUT    ((void*)1)

extern unsigned long hbTimer; // timer for EVENT_HEARTBEAT
extern unsigned long rxTimer; // timer for EVENT_RX_TIMEOUT

void timerSet(unsigned long &timer, unsigned ms);
void timerStop(unsigned long &timer);
bool timerExpired(unsigned long &timer);
bool timerReset(unsigned long &timer, unsigned ms);

#endif
