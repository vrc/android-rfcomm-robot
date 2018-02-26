#include "events.h"

#include <Arduino.h>

unsigned long hbTimer;
unsigned long rxTimer;

void timerSet(unsigned long &timeout, unsigned ms) {
  timeout = millis() + ms;
}

void timerStop(unsigned long &timeout) {
  timeout = -1L;
}

bool timerReset(unsigned long &timeout, unsigned ms) {
  unsigned long now = millis();
  if (now > timeout) {
    unsigned long next = timeout + ms;
    if (now > next) {
      timeout = now + ms;
    } else {
      timeout = next;
    }
    return true;
  }
  return false;
}

bool timerExpired(unsigned long &timeout) {
  return millis() > timeout;
}
