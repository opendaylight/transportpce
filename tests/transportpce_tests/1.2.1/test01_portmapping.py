#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEPortMappingTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '1.2.1'

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
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_rdm_device_connection(self):
        response = test_utils.mount_device("ROADMA01", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_rdm_device_connected(self):
        response = test_utils.check_device_connection("ROADMA01")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')
        time.sleep(10)

    def test_03_rdm_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'rdm',
             'node-ip-address': '127.0.0.12',
             'node-clli': 'NodeA',
             'openroadm-version': '1.2.1',
             'node-vendor': 'vendorA',
             'node-model': '2'},
            response['node-info'])
        time.sleep(3)

    def test_04_rdm_portmapping_DEG1_TTP_TXRX(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", "mapping", "DEG1-TTP-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_05_rdm_portmapping_SRG1_PP7_TXRX(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", "mapping", "SRG1-PP7-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'C7', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP7-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_06_rdm_portmapping_SRG3_PP1_TXRX(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", "mapping", "SRG3-PP1-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'C1', 'supporting-circuit-pack-name': '5/0',
             'logical-connection-point': 'SRG3-PP1-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_07_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_08_xpdr_device_connected(self):
        response = test_utils.check_device_connection("XPDRA01")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')
        time.sleep(10)

    def test_09_xpdr_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'xpdr',
             'node-ip-address': '127.0.0.10',
             'node-clli': 'NodeA',
             'openroadm-version': '1.2.1',
             'node-vendor': 'vendorA',
             'node-model': '1'},
            response['node-info'])
        time.sleep(3)

    def test_10_xpdr_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-NETWORK1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': '1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr', 'port-qual': 'xpdr-network',
             'lcp-hash-val': 'OSvMgUyP+mE='},
            response['mapping'])

    def test_11_xpdr_portmapping_NETWORK2(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-NETWORK2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-NETWORK2',
             'supporting-circuit-pack-name': '1/0/2-PLUG-NET', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': '1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-CLIENT3',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr', 'port-qual': 'xpdr-network',
             'lcp-hash-val': 'OSvMgUyP+mI='},
            response['mapping'])

    def test_12_xpdr_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT1',
             'supporting-circuit-pack-name': '1/0/C1-PLUG-CLIENT', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': 'C1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-NETWORK1',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AO9UFkY/TLYw'},
            response['mapping'])

    def test_13_xpdr_portmapping_CLIENT2(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-CLIENT2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT2',
             'supporting-circuit-pack-name': '1/0/C2-PLUG-CLIENT', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': 'C2',
             'port-oper-state': 'InService', 'port-direction': 'bidirectional',
             'xpdr-type': 'tpdr', 'port-qual': 'xpdr-client', 'lcp-hash-val': 'AO9UFkY/TLYz'},
            response['mapping'])

    def test_14_xpdr_portmapping_CLIENT3(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-CLIENT3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT3',
             'supporting-circuit-pack-name': '1/0/C3-PLUG-CLIENT', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': 'C3',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-NETWORK2',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr',
             'port-qual': 'xpdr-client', 'lcp-hash-val': 'AO9UFkY/TLYy'},
            response['mapping'])

    def test_15_xpdr_portmapping_CLIENT4(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "mapping", "XPDR1-CLIENT4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT4',
             'supporting-circuit-pack-name': '1/0/C4-PLUG-CLIENT', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': 'C4',
             'port-oper-state': 'InService', 'port-direction': 'bidirectional',
             'xpdr-type': 'tpdr', 'port-qual': 'xpdr-client', 'lcp-hash-val': 'AO9UFkY/TLY1'},
            response['mapping'])

    def test_16_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_17_xpdr_device_disconnected(self):
        response = test_utils.check_device_connection("XPDRA01")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_18_xpdr_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_19_rdm_device_disconnection(self):
        response = test_utils.unmount_device("ROADMA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_20_rdm_device_disconnected(self):
        response = test_utils.check_device_connection("ROADMA01")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_21_rdm_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == "__main__":
    unittest.main(verbosity=2)
