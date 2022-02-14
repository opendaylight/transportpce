import requests
import sys
import time
sys.path.append('transportpce_tests/common/')
import test_utils

processes = None


WAITING = 25  # nominal value is 300

NODE_VERSION_221 = '2.2.1'
NODE_VERSION_71 = '7.1'


# processes = test_utils.start_tpce()
# start_sims appends to the process list
devices_to_mounted = {"CISCO-TPDR1-400G": ('CISCO-TPDR1-400G', NODE_VERSION_71),
                      "CISCO-TPDR2-400G": ('CISCO-TPDR2-400G', NODE_VERSION_71),
                      "FUJITSU-TPDR-400G": ('FUJITSU-TPDR-400G', NODE_VERSION_71),
                      "INFINERA-TPDR-400G": ('INFINERA-TPDR-400G', NODE_VERSION_71),
                      "CIENA-ROADMA": ('CIENA-ROADMA', NODE_VERSION_221),
                      "FUJITSU-ROADMA": ('FUJITSU-ROADMA', NODE_VERSION_221),
                      "FUJITSU-ROADMB": ('FUJITSU-ROADMB', NODE_VERSION_221),
                      "INFINERA-ROADMA": ('INFINERA-ROADMA', NODE_VERSION_221),
                      "CIENA-TPDR": ('CIENA-TPDR', NODE_VERSION_221),
                      "FUJITSU-TPDR": ('FUJITSU-TPDR', NODE_VERSION_221),
                      "ECI-SWPDR": ('ECI-SWPDR', NODE_VERSION_221),
                      "NOKIA-SWPDR": ('NOKIA-SWPDR', NODE_VERSION_221)
                      }
processes = test_utils.start_sims([('CISCO-TPDR1-400G', NODE_VERSION_71),
                                   ('CIENA-ROADMA', NODE_VERSION_221),
                                   ('FUJITSU-ROADMA', NODE_VERSION_221),
                                   ('FUJITSU-TPDR-400G', NODE_VERSION_71),
                                   ('CISCO-TPDR2-400G', NODE_VERSION_71),
                                   ('INFINERA-TPDR-400G', NODE_VERSION_71),
                                   ('FUJITSU-ROADMB', NODE_VERSION_221),
                                   ('INFINERA-ROADMA', NODE_VERSION_221),
                                   ('CIENA-TPDR', NODE_VERSION_221),
                                   ('FUJITSU-TPDR', NODE_VERSION_221),
                                   ('ECI-SWPDR', NODE_VERSION_221),
                                   ('NOKIA-SWPDR', NODE_VERSION_221)])

for de in devices_to_mounted:
    # Start the mounting process
    response = test_utils.mount_device(de, devices_to_mounted[de])
    print("Response for {}-{} device: {}".format(devices_to_mounted[de][0],
                                                 devices_to_mounted[de][1],
                                                 response.status_code))
    # If at least one is not mounted correctly
    if response.status_code not in [requests.codes.created, requests.codes.ok]:
        print("node {}-{} not mounted: {}".format(devices_to_mounted[de][0],
                                                  devices_to_mounted[de][1],
                                                  response.status_code))
        print("Code will exit now")
        # Shutdown the honeynode and tpce process
        for process in processes:
            test_utils.shutdown_process(process)
        print("all processes killed, abnormal exit")
        sys.exit(-1)

print("All devices mounted successfully")
# Keep the pragram running until the keyboard interrupt
try:
    while True:
        time.sleep(1)
        continue
except KeyboardInterrupt:
    print("Tearing down all the process that were started")
    # Tear down the process
    # Shutdown the honeynode and tpce process
    # Unmount the devices
    for de in devices_to_mounted:
        response = test_utils.unmount_device(de)
        print(response)
    for process in processes:
        test_utils.shutdown_process(process)
    print("all processes killed, exiting the program")
