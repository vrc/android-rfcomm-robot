/**
 * Low level uart interface for sending and receiving a command.
 * */
#ifndef INCLUDED_uart_h
#define INCLUDED_uart_h

#include <stdint.h>

typedef int (*uart_rx_complete_cb)();

void uartTx(const uint8_t *data, unsigned size);
void uartRx(uint8_t *buffer, unsigned size, uart_rx_complete_cb cb);

unsigned uartRxCount();
unsigned uartRxError();

void uartInit();
void uartLoop();

#endif
