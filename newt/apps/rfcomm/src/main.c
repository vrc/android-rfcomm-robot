#include "commands.h"
#include "events.h"
#include "heartbeat.h"
#include "uart.h"

#include "bsp/bsp.h"
#include "hal/hal_gpio.h"
#include "os/os.h"
#include "sysinit/sysinit.h"

static unsigned err = 0;

static void monitor(struct os_event *event) {
  static int state = 0;
  static int edisp = 0;

  if (event && EVENT_RX_TIMEOUT == event->ev_arg) {
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
      os_callout_reset(&hbTimer, OS_TICKS_PER_SEC / 4);
      if (0 == (state & 0x01) && (state/2) < err) {
          hal_gpio_write(LED_BLINK_PIN, 1);
      } else {
          hal_gpio_write(LED_BLINK_PIN, 0);
      }
    } else {
      heartbeat(state, LED_BLINK_PIN);
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

static void monitorInit() {
  hal_gpio_init_out(LED_BLINK_PIN, 1);
}

int main(int argc, char **argv) {
  sysinit();

  monitorInit();
  uartInit(monitor);
  heartbeatInit(monitor);

  heartbeatStart();
  while (1) {
    os_eventq_run(os_eventq_dflt_get());
  }

  return 0;
}

