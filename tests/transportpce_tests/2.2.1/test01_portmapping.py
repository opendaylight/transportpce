#!/usr/bin/env python

##############################################################################
# Copyright (c) 2019 Orange, Inc. and others.  All rights reserved.
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
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEPortMappingTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_rdm_device_connection(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_rdm_device_connected(self):
        response = test_utils.get_netconf_oper_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    def test_03_rdm_portmapping_info(self):
        response = test_utils.portmapping_request("ROADM-A1/node-info")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'rdm',
                            u'node-ip-address': u'127.0.0.11',
                            u'node-clli': u'NodeA',
                            u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorA',
                            u'node-model': u'model2'}},
            res)
        time.sleep(3)

    def test_04_rdm_portmapping_DEG1_TTP_TXRX(self):
        response = test_utils.portmapping_request("ROADM-A1/mapping/DEG1-TTP-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_05_rdm_portmapping_DEG2_TTP_TXRX_with_ots_oms(self):
        response = test_utils.portmapping_request("ROADM-A1/mapping/DEG2-TTP-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG2-TTP-TXRX',
             'supporting-oms': 'OMS-DEG2-TTP-TXRX', 'supporting-ots': 'OTS-DEG2-TTP-TXRX',
             'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_06_rdm_portmapping_SRG1_PP3_TXRX(self):
        response = test_utils.portmapping_request("ROADM-A1/mapping/SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_07_rdm_portmapping_SRG3_PP1_TXRX(self):
        response = test_utils.portmapping_request("ROADM-A1/mapping/SRG3-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C1', 'supporting-circuit-pack-name': '5/0',
             'logical-connection-point': 'SRG3-PP1-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_08_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_09_xpdr_device_connected(self):
        response = test_utils.get_netconf_oper_request("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    def test_10_xpdr_portmapping_info(self):
        response = test_utils.portmapping_request("XPDR-A1/node-info")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'xpdr',
                            u'node-ip-address': u'1.2.3.4',
                            u'node-clli': u'NodeA',
                            u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorA',
                            u'node-model': u'model2'}},
            res)
        time.sleep(3)

    def test_11_xpdr_portmapping_NETWORK1(self):
        response = test_utils.portmapping_request("XPDR-A1/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'lcp-hash-val': 'AMkDwQ7xTmRI',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_12_xpdr_portmapping_NETWORK2(self):
        response = test_utils.portmapping_request("XPDR-A1/mapping/XPDR1-NETWORK2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/2-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK2', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-CLIENT2', 'port-qual': 'xpdr-network',
             'lcp-hash-val': 'AMkDwQ7xTmRL',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_13_xpdr_portmapping_CLIENT1(self):
        response = test_utils.portmapping_request("XPDR-A1/mapping/XPDR1-CLIENT1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AJUUr6I5fALj',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_14_xpdr_portmapping_CLIENT2(self):
        response = test_utils.portmapping_request("XPDR-A1/mapping/XPDR1-CLIENT2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/2-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT2', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK2', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AJUUr6I5fALg',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['mapping'])

    def test_15_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_16_xpdr_device_disconnected(self):
        response = test_utils.get_netconf_oper_request("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_17_xpdr_device_not_connected(self):
        response = test_utils.portmapping_request("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_18_rdm_device_disconnection(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_19_rdm_device_disconnected(self):
        response = test_utils.get_netconf_oper_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_20_rdm_device_not_connected(self):
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])


if __name__ == "__main__":
    unittest.main(verbosity=2)
