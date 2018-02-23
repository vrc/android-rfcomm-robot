# rfcomm

Skeleton application to communicate between a robot running on
[Apache-Mynewt](https://mynewt.apache.org/) and an Android app.

Any board or processor supported by mynewt with a spare serial port connection
to any of the Bluetooth serial modules should work. The module used for this
project was setup for 38400 baud and connected to UART1 of the BSP. Those should
be the only hardware specific aspects of the source code.

A LED is assumed to be available and used for status reporting. If you don't
have a spare LED you want to disable the functionality in `monitor`. And if
you have another serial port available you might want to enable console and
get useful status reporting.

Speaking of - the `console` in combination with an ascii based protocol would
definitely be the simplest way to implement communication - but where's the fun in that....
