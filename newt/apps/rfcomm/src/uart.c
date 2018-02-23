#include "uart.h"

#include "commands.h"
#include "events.h"
#include <assert.h>
#include <os/os.h>
#include <uart/uart.h>

static struct uart_dev *uart;

static const uint8_t *txPtr;
static uint8_t       *rxPtr;
static unsigned  txCnt;
static unsigned  rxCnt;
static unsigned  rxErr = 0;
static uart_rx_complete_cb rxCb;


// ----------------------- API ---------------------------

unsigned uartRxCount() {
  return rxCnt;
}
unsigned uartRxError() {
  return rxErr;
}


// ----------------------- TX ---------------------------

void uartTx(const uint8_t *data, unsigned size) {
  txPtr = data;
  txCnt = size;
  if (txCnt) {
    uart_start_tx(uart);
  }
}

static int uartTxCharacter(void *arg) {
  if (txCnt) {
    uint8_t byte = *txPtr;
    --txCnt;
    ++txPtr;
    return byte;
  }
  return -1;
}


// ----------------------- RX ---------------------------

void uartRx(uint8_t *buffer, unsigned size, uart_rx_complete_cb cb) {
  rxPtr = buffer;
  rxCnt = size;
  rxErr = 0;
  rxCb = cb;
  if (size) {
    os_callout_reset(&rxTimer, OS_TICKS_PER_SEC / 4);
  }
}

static int uartRxCharacter(void *arg, uint8_t byte) {
  if (rxCnt) {
    *rxPtr = byte;
    ++rxPtr;
    --rxCnt;
    if (!rxCnt) {
      os_callout_stop(&rxTimer);
      if (rxCb) {
        rxErr = rxCb();
        if (rxErr) {
          os_callout_reset(&rxTimer, 0);
        }
      }
    }
  } else {
    commandProcess(byte);
  }
  return 0;
}

// ----------------------- initialize ---------------------------

void uartInit(os_event_fn callback) {

  struct uart_conf uartConf = {
    .uc_speed = 38400,
    .uc_databits = 8,
    .uc_stopbits = 1,
    .uc_parity = UART_PARITY_NONE,
    .uc_flow_ctl = UART_FLOW_CTL_NONE,
    .uc_tx_char = uartTxCharacter,
    .uc_rx_char = uartRxCharacter,
    .uc_tx_done = 0,
    .uc_cb_arg = 0

  };
  uart = (struct uart_dev*)os_dev_open("uart0", OS_TIMEOUT_NEVER, &uartConf);
  assert(uart);

  os_callout_init(&rxTimer, os_eventq_dflt_get(), callback, EVENT_RX_TIMEOUT);
}

