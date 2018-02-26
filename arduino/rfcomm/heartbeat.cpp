#include "heartbeat.h"

#include "events.h"
#include <Arduino.h>

void heartbeat(int state, int led) {
  // for some reason I find 3 beats nicer than 2 beats
  timerSet(hbTimer, 120);
  switch (state) {
    case 0:
    case 2:
    case 4:
      digitalWrite(led, 1);
      break;
    case 1:
    case 3:
    case 5:
      digitalWrite(led, 0);
      break;
  }
}

void heartbeatInit() {
  timerStop(hbTimer);
}

void heartbeatStart() {
  timerSet(hbTimer, 120);
}
