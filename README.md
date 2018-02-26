# android-rfcomm-robot

A basic Android app to communicate with a robot equipped with a Bluetooth
serial module.

There are two implementations of example "robots", one built on Arduino and
the other built on Mynewt. The two samples are basically identical
implementations, differing only in the OS specific APIs for the
serial interface, GPIO and timer handling.

Both, the Android app and the two sample robots are meant as starting points
for you own robot.
