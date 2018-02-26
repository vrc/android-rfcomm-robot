# rfcomm

Skeleton application to communicate between a robot running on
an [Arduino](https://www.arduino.cc/) and an Android app.

An [ATmega32u4 Breakout Board](https://www.adafruit.com/product/296) was used
which probably explains the LED pin assignment and the use of ``Serial1``
instead of ``Serial``. Adapting those two constants should allow the use of
any Arduino supported board.

The Bluetooth module is connected to the Arduino pins ``D0`` and ``D1`` and
configured for 38400 baud.
