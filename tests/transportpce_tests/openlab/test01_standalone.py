#!/usr/bin/env python
##############################################################################
# Copyright (c) 2021 AT&T, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
import os
sys.path.append('transportpce_tests/common')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_openlab_utils

class OpenlabStandaloneTesting(unittest.TestCase):
    
    processes = None
    device = {
            "NAME": '',
            "VENDOR": '',
            "NODETYPE": '',
            "USERNAME": '',
            "PASSWORD": '',
            "HOST": '',
            "PORT": '',
            "OR_VERSION": ''
        } # TODO add TYPE when needed
    # NODE_VERSION = os.getenv('OR_VERSION')
    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    # retrieve values from TeamDynamics
    def test_00_retrive_from_teamDynamics(self):
        response = test_openlab_utils.get_devices_from_TD(os.getenv("VENDOR"))
        if response is not False:
            _device = test_openlab_utils.get_device(response, os.getenv("NODEID"))
            OpenlabStandaloneTesting.device = _device
        self.assertNotEqual(OpenlabStandaloneTesting.device,{
            "NAME": '',
            "VENDOR": '',
            "NODETYPE": '',
            "USERNAME": '',
            "PASSWORD": '',
            "HOST": '',
            "PORT": '',
            "OR_VERSION": ''
        })
           
    # check if device ip can be pinged
    def test_01_device_reachability(self):
        response = test_openlab_utils.check_ping(os.getenv("HOST"), OpenlabStandaloneTesting.device)
        self.assertEqual(response, "Network Active")
 
    # Check if device ip and netconf port can be reached by username and password
    def test_02_device_netconf_connection(self):
        connection, disconnection = test_openlab_utils.check_device_netconf_connection(OpenlabStandaloneTesting.device, os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),120)
        self.assertEqual(connection, True)
        self.assertEqual(disconnection, True)
        time.sleep(2)

    # Check if device netconf capability. TODO: check if all required capabilities exist 
    def test_03_device_netconf_capability(self):
        numCap= test_openlab_utils.check_device_netconf_capabilities(os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),120)
        self.assertGreater(numCap, 0, "no capabilities")
        time.sleep(2)

    # Check what streams device supports
    def test_04_device_netconf_stream(self):
        netconf_container = test_openlab_utils.check_device_container('netconf',os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),120)
        streamExist = test_openlab_utils.check_device_netconf_container(netconf_container)
        self.assertEqual(streamExist, True)
        time.sleep(2)

    # Check if device info tag through netconf
    def test_05_device_info_container(self):
        running_info_container = test_openlab_utils.check_device_openroadm_container('info',os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),120)
        vendorFound, modelFound, serialIdFound = test_openlab_utils.check_device_info_container(running_info_container)
        self.assertEqual(vendorFound, True)
        self.assertEqual(modelFound, True)
        self.assertEqual(serialIdFound, True)
        time.sleep(2)

    # # Check if device netconf get matches with yang model
    # def test_06_device_netconf_get_yang(self):
    #     operational_get = test_openlab_utils.check_device_openroadm_container('',os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),120)
    #     response = test_openlab_utils.check_device_netconf_get_yang(operational_get) 
    #     # TODO: not completed. see details inside the function
    #     self.assertEqual(response, True)
    #     time.sleep(2)

    # Check if device openroadm get, container by container. **MAIN**
    def test_07_device_get(self):
        operational_get = test_openlab_utils.check_device_openroadm_container('',os.getenv("HOST"),os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"),600)
        response = test_openlab_utils.check_device_get(operational_get)
        self.assertEqual(response, True)
        time.sleep(2)

    # Check if the device mount has error
    def test_08_check_device_mount_error(self):
        noError, response = test_openlab_utils.check_mount_device_error(os.getenv("NODEID"), 
                                                                        os.getenv("HOST"), os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"))
        self.assertEqual(noError,True,'error found during mounting')
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)
        #unmount device to make sure next mount will have 201 code (remount gets 204 no content code.)
        response = test_utils.unmount_device(os.getenv("NODEID"))
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


    # Check if the device can be mounted to TPCE
    def test_20_device_connection(self):
        response = test_openlab_utils.mount_device(os.getenv("NODEID"), os.getenv("HOST"), os.getenv("PORT"),os.getenv("USERNAME"),os.getenv("PASSWORD"))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    # search backward and find which stream is subscribed
    # TODO: bug in this test case
    def test_21_notification_subscription(self):
        response = test_openlab_utils.check_notification_subscription(os.getenv("NODEID"))
        self.assertIn(response,('OPENROADM stream subscribed','NETCONF stream subscribed'))

    # Check if the node appears in the ietf-network topology
    def test_22_device_connected(self):
        response = test_utils.check_device_connection(os.getenv("NODEID"))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    # Check node info in the port-mappings
    def test_23_device_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr(os.getenv("NODEID"), "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node-info']['node-type'], os.getenv('NODETYPE').lower())
        self.assertEqual(response['node-info']['openroadm-version'], os.getenv('OR_VERSION'))
    
    # Check device portmapping
    def test_24_device_portmapping(self):
        portmapping_response = test_utils.get_portmapping_node_attr(os.getenv("NODEID"), None, None)
        self.assertEqual(portmapping_response['status_code'], requests.codes.ok)
        response = test_openlab_utils.parsePortmapping(portmapping_response['nodes'][0]) # it is a list with only one element
        self.assertIsInstance(response, dict)
        OpenlabStandaloneTesting.portmapping = response
    #TODO: need to check connection map based on the port mapping for rdm
    #                    internal links based on port mapping for xpdr
    
    # interface creation test, also test pm and alarm within
    ################
    def test_30_device_rendering(self):
        response = test_openlab_utils.interface_test(OpenlabStandaloneTesting.portmapping)
        self.assertEqual(response[0], True, "createCheck")
        self.assertEqual(response[1], True, "configCheck")
        self.assertEqual(response[2], True, "pmCheck")
        self.assertEqual(response[3], True, "alarmCheck")
        self.assertEqual(response[4], True, "deleteCheck")
    
    # # Added test to check mc-capability-profile for a transponder
    # def test_08_check_mccapprofile(self):
    #     res = test_utils_rfc8040.get_portmapping_node_attr("XPDR-A2", "mc-capabilities", "XPDR-mcprofile")
    #     self.assertEqual(res['status_code'], requests.codes.ok)
    #     self.assertEqual(res['mc-capabilities'][0]['mc-node-name'], 'XPDR-mcprofile')
    #     self.assertEqual(str(res['mc-capabilities'][0]['center-freq-granularity']), '3.125')
    #     self.assertEqual(str(res['mc-capabilities'][0]['slot-width-granularity']), '6.25')
    #
    # def test_09_mpdr_switching_pool(self):
    #     response = test_utils_rfc8040.get_portmapping_node_attr("XPDR-A2", "switching-pool-lcp", "1")
    #     self.assertEqual(response['status_code'], requests.codes.ok)
    #     self.assertEqual("blocking",
    #                      response['switching-pool-lcp'][0]['switching-pool-type'])
    #     self.assertEqual(2,
    #                      len(response['switching-pool-lcp'][0]['non-blocking-list']))
    #     self.assertIn(
    #         {'nbl-number': 2,
    #          'interconnect-bandwidth': 0,
    #          'lcp-list': ['XPDR2-NETWORK1', 'XPDR2-CLIENT2']},
    #         response['switching-pool-lcp'][0]['non-blocking-list'])

    def test_40_device_disconnection(self):
        response = test_utils.unmount_device(os.getenv("NODEID"))
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_41_device_disconnected(self):
        response = test_utils.check_device_connection(os.getenv("NODEID"))
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_42_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr(os.getenv("NODEID"), "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == '__main__':
    unittest.main(verbosity=2)
