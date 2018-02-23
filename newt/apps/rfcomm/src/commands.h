/**
 * The commands the robot understands and the data structures used for their
 * communication.
 * */
#ifndef INCLUDED_commands_h_
#define INCLUDED_commands_h_

#include <stdint.h>

#define CMD_PING        0x00  // respond by sending a 0x00 byte back
#define CMD_ID          0x01  // respond by sending app_id_t back
#define CMD_VALUE_GET   0x02  // respond by sending a float value back
#define CMD_VALUE_SET   0x82  // set value, no response what so ever

// CMD_ID response
typedef struct {
  const char name[9];
  const char date[12];
  const char time[9];
} app_id_t;

// CMD_VALUE_SET payload
typedef struct {
  float v;
  int   i;
} value_t;

int commandProcess(uint8_t cmd);

#endif
