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
import sys
sys.path.append('transportpce_tests/common')
import test_utils


class TransportPCE400GPortMappingTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '7.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra2', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-A2",
                                           ('xpdra2', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    # Check if the node appears in the ietf-network topology
    def test_02_xpdr_device_connected(self):
        response = test_utils.get_netconf_oper_request("XPDR-A2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    # Check node info in the port-mappings
    def test_03_xpdr_portmapping_info(self):
        response = test_utils.portmapping_request("XPDR-A2/node-info")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'xpdr',
                            u'node-ip-address': u'1.2.3.4',
                            u'node-clli': u'NodeA',
                            u'openroadm-version': u'7.1.0',
                            u'node-vendor': u'vendorA',
                            u'node-model': u'model2'}},
            res)
        time.sleep(3)

    # Check the if-capabilities and the other details for network
    def test_04_xpdr_portmapping_NETWORK1(self):
        response = test_utils.portmapping_request("XPDR-A2/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability':
              ['org-openroadm-port-types:if-otsi-otsigroup'],
             'supporting-port': 'L1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1',
             'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-CLIENT1',
             'lcp-hash-val': 'AIGiVAQ4gDil',
             'port-admin-state': 'InService',
             'port-oper-state': 'InService',
             'xponder-type': 'tpdr'
             },
            res['mapping'])

    def test_05_xpdr_portmapping_CLIENT1(self):
        response = test_utils.portmapping_request("XPDR-A2/mapping/XPDR1-CLIENT1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-400GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AODABTVSOHH0',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_06_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_07_xpdr_device_disconnected(self):
        response = test_utils.get_netconf_oper_request("XPDR-A2")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the "
                              "relevant data model content does not exist"},
            res['errors']['error'])

    def test_08_xpdr_device_not_connected(self):
        response = test_utils.portmapping_request("XPDR-A2")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the "
                              "relevant data model content does not exist"},
            res['errors']['error'])


if __name__ == '__main__':
  unittest.main(verbosity=2)
