#include "events.h"
#include "heartbeat.h"
#include "uart.h"

#define LED_PIN 7 // builtin LED for the Adafruit ATmega32U4 Breakout Board

static unsigned err = 0;

void monitor(void *event) {
  static unsigned state = 0;
  static unsigned edisp = 0;
  
  if (EVENT_RX_TIMEOUT == event) {
    err = uartRxError();
    if (!err) {
      err = uartRxCount();
    }
    uartRx(0, 0, 0); // abort reception and clear error
    state = 0;
    edisp = 10;      // blink error 10 times, then clear error report
  } else {
    if (err) {
      // use the HB LED to blink the error code
      // if err > 7 all you'll see is a 2Hz blinking of the LED,
      // but then, most ppl get bored after counting to 3 ....
      timerSet(hbTimer, 250);
      if (0 == (state & 0x01) && (state/2) < err) {
        digitalWrite(LED_PIN, 1);
      } else {
        digitalWrite(LED_PIN, 0);
      }
    } else {
      heartbeat(state, LED_PIN);
    }

    if (++state > 15) {
      state = 0;
      if (edisp) {
        --edisp;
        if (!edisp) {
          err = 0;
        }
      }
    }
  }
}

void monitorInit() {
  pinMode(LED_PIN, OUTPUT);
}

void setup() {
  monitorInit();
  uartInit();
  heartbeatInit();

  heartbeatStart();
}

void loop() {
  if (timerExpired(rxTimer)) {
    monitor(EVENT_RX_TIMEOUT);
  }

 if (timerReset(hbTimer, 100)) {
    monitor(EVENT_HEARTBEAT);
  }

  uartLoop();
}

