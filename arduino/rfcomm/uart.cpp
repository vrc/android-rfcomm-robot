#include "uart.h"

#include "commands.h"
#include "events.h"
#include <Arduino.h>

#define SERIAL_DEVICE Serial1

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
}

static void uartTxCharacter() {
  if (txCnt) {
    SERIAL_DEVICE.write(*txPtr);
    --txCnt;
    ++txPtr;
  }
}


// ----------------------- RX ---------------------------

void uartRx(uint8_t *buffer, unsigned size, uart_rx_complete_cb cb) {
  rxPtr = buffer;
  rxCnt = size;
  rxErr = 0;
  rxCb = cb;
  if (size) {
    timerSet(rxTimer, 250);
  } else {
    timerStop(rxTimer);
  }
}

static int uartRxCharacter(uint8_t byte) {
  if (rxCnt) {
    *rxPtr = byte;
    ++rxPtr;
    --rxCnt;
    if (!rxCnt) {
      timerStop(rxTimer);
      if (rxCb) {
        rxErr = rxCb();
        if (rxErr) {
          timerSet(rxTimer, 0);
        }
      }
    }
  } else {
    commandProcess(byte);
  }
  return 0;
}


void uartLoop() {
  uartTxCharacter();
  if (SERIAL_DEVICE.available()) {
    uartRxCharacter(SERIAL_DEVICE.read());
  }
}
// ----------------------- initialize ---------------------------

void uartInit() {
  SERIAL_DEVICE.begin(38400);
  txCnt = 0;
  uartRx(0, 0, 0);
  while (!SERIAL_DEVICE) ;
}

