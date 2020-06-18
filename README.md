# ncinfrastructure

Infrastructure for hosting pwn challenges over TCP.

Usage:
```shell
$ ncinfrastructure <binary> <address:port>
```

Please note that, due to the nature of this software, most programs using Termios/ncurses (e.g. vim, nano, etc.) will have issues running.
This is a problem that cannot be fixed without making a special client for using the server, which is against the whole point of this program.