#include "heartbeat.h"

#include "events.h"
#include <hal/hal_gpio.h>

void heartbeat(int state, int led) {
  // for some reason I find 3 beats nicer than 2 beats
  os_callout_reset(&hbTimer, OS_TICKS_PER_SEC / 7);
  switch (state) {
    case 0:
    case 2:
    case 4:
      hal_gpio_write(led, 1);
      break;
    case 1:
    case 3:
    case 5:
      hal_gpio_write(led, 0);
      break;
  }
}

void heartbeatInit(os_event_fn callback) {
  os_callout_init(&hbTimer, os_eventq_dflt_get(), callback, EVENT_HEARTBEAT);
}

void heartbeatStart() {
  os_callout_reset(&hbTimer, OS_TICKS_PER_SEC / 7);
}
