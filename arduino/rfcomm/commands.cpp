#include "commands.h"

#include "uart.h"
#include <string.h>

value_t value = { 0, 1 };
const uint8_t zero = 0;

// note that the assigned time and date end up being the last time
// commands.c was compiled.
const app_id_t AppId = { "newt-rfc", __DATE__, __TIME__ };

uint8_t rxBuffer[16];

static int applyValue() {
  uint16_t cs = 0;

  for (unsigned i=0; i<sizeof(value_t); ++i) {
    cs += 0x00FF & (uint16_t)(rxBuffer[i]);
  }

  if (((cs >> 8) & 0x00FF) == rxBuffer[sizeof(value_t)]
      && (cs & 0x00FF) == rxBuffer[sizeof(value_t) + 1]) {
    memcpy(&value, rxBuffer, sizeof(value_t));
  } else {
    return -1;
  }

  return 0;
}

int commandProcess(uint8_t cmd) {
  switch (cmd) {

    case CMD_PING:
      value.v += value.i * 0.001;
      uartTx(&zero, 1);
      break;

    case CMD_ID:
      uartTx((const uint8_t*)&AppId, sizeof(app_id_t));
      break;

    case CMD_VALUE_GET:
      uartTx((const uint8_t*)&value.v, sizeof(value.v));
      break;

    case CMD_VALUE_SET:
      uartRx(&rxBuffer[0], sizeof(value_t) + sizeof(uint16_t), applyValue);
      break;
  }
  return 0;
}

