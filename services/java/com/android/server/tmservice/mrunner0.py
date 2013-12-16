#! /usr/bin/env monkeyrunner
from com.android.monkeyrunner import MonkeyRunner, MonkeyDevice
from time import ctime, time
import socket
import sys


def timestamp(func):
    def func_(*args, **kwargs):
        time0 = time()
        try:
            ret = func(*args, **kwargs)
        except Exception, e:
            print >> sys.stderr, "exception" + e
        print >> sys.stderr, "[DBG] %s ran for %.2f sec." % \
            (func, time() - time0)
        return ret
    return func_


def del_chars(device, fieldLength):
    """
    """
    # select all the chars
    device.press('KEYCODE_SHIFT_LEFT', MonkeyDevice.DOWN)
    for i in range(fieldLength):
        device.press('KEYCODE_DPAD_LEFT', MonkeyDevice.DOWN_AND_UP)
        #MonkeyRunner.sleep(0.1)
    device.press('KEYCODE_SHIFT_LEFT', MonkeyDevice.UP)

    # delete them
    device.press('KEYCODE_DEL', MonkeyDevice.DOWN_AND_UP)


@timestamp
def run_over(device, apk_name):
    """
    """
    _launch_app(device, apk_name)

    MonkeyRunner.sleep(3)

    _run(device)

    _back(device)
    _back(device)


@timestamp
def _run(device):
    device.touch(0xd8, 0x94, 'DOWN_AND_UP')
    del_chars(device, 24)
    device.type('sos15.cs.columbia.edu')

    device.touch(0x6a, 0xb8, 'DOWN_AND_UP')
    del_chars(device, 8)
    device.type('5000')

    device.touch(0x2b, 0xec, 'DOWN_AND_UP')


@timestamp
def _launch_app(device, apk_name):
    """
    """

    apk_path = device.shell("pm path %s " % (apk_name,))

    if apk_path.startswith("package:"):
        print "%s already installed" % (apk_name,)
    else:
        print "%s not installed" % (apk_name, )
        #device.installPackage("gpsapp.apk")

    while True:
        try:
            device.wake()
            break
        except:
            print "wake event failed, trying again"
            MonkeyRunner.sleep(1)

    MonkeyRunner.sleep(5)
    print "launching %s..." % (apk_name,)
    while True:
        try:
            device.startActivity(component=apk_name + "/" + apk_name + ".MainActivity")
            break
        except:
            print "StartActivity failed, trying again"
            MonkeyRunner.sleep(1)
    MonkeyRunner.sleep(5)


@timestamp
def _back(device):
    """
    """
    device.press('KEYCODE_BACK', 'DOWN_AND_UP')
    device.press('KEYCODE_BACK', 'DOWN_AND_UP')


if __name__ == "__main__":
    apk_name = "edu.columbia.gpsapp"
    device = MonkeyRunner.waitForConnection()

    if len(sys.argv) == 2:
        port = int(sys.argv[1])
    elif len(sys.argv) == 1:
        port = 10000
    else:
        assert(not "not here")

    host = ''
    backlog = 5
    size = 1024

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.bind((host, port))
    s.listen(backlog)

    print "Begin listen..."

    while True:
        client, address = s.accept()
        data = client.recv(size).strip()
        print "RECV:", type(data), data

        if data:
            if data in ("end", "exit"):
                break
            elif data == "run_over":
                run_over(device, apk_name)

            elif data == "launch":
                _launch_app(device, apk_name)

            elif data == "cleanup":
                _back(device)
                _back(device)
            elif data == "run":
                _run(device, apk_name)
            else:
                print "Unexpected input: " + data

            msg = ctime() + ":" + data
            print "Request:", address, msg
            client.send(msg)

        #if data.strip().startswith("disc"):
        client.close()
