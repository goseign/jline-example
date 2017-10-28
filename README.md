# jline-example

[![Build Status](https://travis-ci.org/mslinn/jline-example.svg?branch=master)](https://travis-ci.org/mslinn/jline-example)
[![GitHub version](https://badge.fury.io/gh/mslinn%2Fjline-example.svg)](https://badge.fury.io/gh/mslinn%2Fjline-example)

Scala and Java examples of [jline3](https://github.com/jline/jline3).

## Running the Program
The `bin/run` Bash script assembles this project into a fat jar and runs it.
Sample usage, which runs the `Main` entry point in `src/main/scala/Main.java`:

```
$ bin/run
```

The `-j` option forces a rebuild of the fat jar.
Use it after modifying the source code.

```
$ bin/run -j
```

You can specify an arbitrary entry point.
This runs the Scala test program:
```
$ bin/run Main
```

This runs the Java test program:
```
$ bin/run JavaMain
```
