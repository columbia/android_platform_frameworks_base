==How to run experiments==

We have number of different entities we need to consider to run experiment.
* MonkeyRunner
* Emulator
  - Tm*Service
  - Target APP
* Logcat listener

Here we have detailed steps to follow to bootstrap an experiment instance.

1. Bring up preliminary daemons
   a. Monkey runner script - ./mrunner0.py <port number>  # default 50000
   b. Echo server - ~/bin/echo.py <port number>  # GPSApp will ping to this port. Now, it is set to 5000

2. Listen to logcat channel and gether relevant event entries. Redirect the output to a file for later analysis.
   $adb logcat -s TMLog -s dalvikvmtm > log.txt

3. Run emulator from VNC
   $emulator &

4. Remap port open from TM*Service to the host
   $telnet localhost 5554
    redir add tcp:50000:50000
    exit

5. telnet to TM*Service port and issue command
   $telnet localhost 50000
   run_over
   disc

Now, we can build and run TMLocationAnalyzer for analysis.

1. Move into the tmservice directory.
   $ cd ${TDROID_HOME}/frameworks/base/services/java/com/android/server/tmservice

2. Build sources
   $ make all

3. Run it with log.txt
   $/ usr/bin/java -classpath ${TDROID_HOME}/frameworks/base/services/java/ com.android.server.tmservice.TMLocationAnalyzer log.txt

4. Run prepared test-cases.
   $ make test
