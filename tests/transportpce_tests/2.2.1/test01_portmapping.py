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
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils_rfc8040  # nopep8


class TransportPCEPortMappingTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils_rfc8040.start_tpce()
        cls.processes = test_utils_rfc8040.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION),
                                                       ('spdra', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_rdm_device_connection(self):
        response = test_utils_rfc8040.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_02_rdm_device_connected(self):
        response = test_utils_rfc8040.check_device_connection("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_03_rdm_portmapping_info(self):
        response = test_utils_rfc8040.get_portmapping_node_info("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'rdm',
             'node-ip-address': '127.0.0.11',
             'node-clli': 'NodeA',
             'openroadm-version': '2.2.1',
             'node-vendor': 'vendorA',
             'node-model': 'model2'},
            response['node-info'])

    def test_04_rdm_portmapping_DEG1_TTP_TXRX(self):
        response = test_utils_rfc8040.portmapping_request("ROADM-A1", "DEG1-TTP-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_05_rdm_portmapping_DEG2_TTP_TXRX_with_ots_oms(self):
        response = test_utils_rfc8040.portmapping_request("ROADM-A1", "DEG2-TTP-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG2-TTP-TXRX',
             'supporting-oms': 'OMS-DEG2-TTP-TXRX', 'supporting-ots': 'OTS-DEG2-TTP-TXRX',
             'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_06_rdm_portmapping_SRG1_PP3_TXRX(self):
        response = test_utils_rfc8040.portmapping_request("ROADM-A1", "SRG1-PP3-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_07_rdm_portmapping_SRG3_PP1_TXRX(self):
        response = test_utils_rfc8040.portmapping_request("ROADM-A1", "SRG3-PP1-TXRX")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'C1', 'supporting-circuit-pack-name': '5/0',
             'logical-connection-point': 'SRG3-PP1-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_08_xpdr_device_connection(self):
        response = test_utils_rfc8040.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_09_xpdr_device_connected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_10_xpdr_portmapping_info(self):
        response = test_utils_rfc8040.get_portmapping_node_info("XPDR-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'xpdr',
             'node-ip-address': '1.2.3.4',
             'node-clli': 'NodeA',
             'openroadm-version': '2.2.1',
             'node-vendor': 'vendorA',
             'node-model': 'model2'},
            response['node-info'])

    def test_11_xpdr_portmapping_NETWORK1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A1", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'lcp-hash-val': 'AMkDwQ7xTmRI', 'xponder-type': 'tpdr',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_12_xpdr_portmapping_XPDR2_NETWORK1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A1", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/2-PLUG-NET',
             'logical-connection-point': 'XPDR2-NETWORK1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR2-CLIENT1', 'port-qual': 'xpdr-network',
             'lcp-hash-val': 'Bpl3hMYP3fE=', 'xponder-type': 'tpdr',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_13_xpdr_portmapping_XPDR1_CLIENT1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A1", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AJUUr6I5fALj', 'xponder-type': 'tpdr',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_14_xpdr_portmapping_XPDR2_CLIENT1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A1", "XPDR2-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/2-PLUG-CLIENT',
             'logical-connection-point': 'XPDR2-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR2-NETWORK1', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'ayWKnqXgcyA=', 'xponder-type': 'tpdr',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['mapping'])

    def test_15_spdr_device_connection(self):
        response = test_utils_rfc8040.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_16_spdr_device_connected(self):
        response = test_utils_rfc8040.check_device_connection("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_17_spdr_portmapping_info(self):
        response = test_utils_rfc8040.get_portmapping_node_info("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'xpdr',
             'node-ip-address': '1.2.3.4',
             'node-clli': 'NodeSA',
             'openroadm-version': '2.2.1',
             'node-vendor': 'vendorA',
             'node-model': 'universal-switchponder'},
            response['node-info'])

    def test_18_spdr_switching_pool_1(self):
        response = test_utils_rfc8040.portmapping_switching_pool_request("SPDR-SA1", "1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching_pool'][0]['switching-pool-type'])
        self.assertEqual(4,
                         len(response['switching_pool'][0]['non-blocking-list']))
        self.assertIn(
            {'nbl-number': 11,
             'interconnect-bandwidth': 0,
             'interconnect-bandwidth-unit': 1000000000,
             'lcp-list': ['XPDR1-NETWORK1', 'XPDR1-CLIENT1']},
            response['switching_pool'][0]['non-blocking-list'])

    def test_19_spdr_switching_pool_2(self):
        response = test_utils_rfc8040.portmapping_switching_pool_request("SPDR-SA1", "2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("non-blocking",
                         response['switching_pool'][0]['switching-pool-type'])
        self.assertEqual(1,
                         len(response['switching_pool'][0]['non-blocking-list']))
        self.assertIn(
            {'nbl-number': 2,
             'interconnect-bandwidth': 0,
             'interconnect-bandwidth-unit': 1000000000,
             'lcp-list': ["XPDR2-CLIENT3", "XPDR2-CLIENT4", "XPDR2-NETWORK4",
                          "XPDR2-NETWORK1", "XPDR2-NETWORK3", "XPDR2-NETWORK2",
                          "XPDR2-CLIENT1", "XPDR2-CLIENT2"]},
            response['switching_pool'][0]['non-blocking-list'])

    def test_20_spdr_switching_pool_3(self):
        response = test_utils_rfc8040.portmapping_switching_pool_request("SPDR-SA1", "3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching_pool'][0]['switching-pool-type'])
        self.assertEqual(4,
                         len(response['switching_pool'][0]['non-blocking-list']))
        self.assertIn(
            {'nbl-number': 83,
             'interconnect-bandwidth': 0,
             'interconnect-bandwidth-unit': 1000000000,
             'lcp-list': ['XPDR3-NETWORK1', 'XPDR3-CLIENT3']},
            response['switching_pool'][0]['non-blocking-list'])

    def test_21_spdr_portmapping_mappings(self):
        response = test_utils_rfc8040.get_portmapping("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(18, len(response['nodes'][0]['mapping']))

    def test_22_spdr_portmapping_XPDR1_CLIENT1(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR1-CLIENT1",
             "supporting-circuit-pack-name": "CP1-SFP1",
             "supported-interface-capability": ["org-openroadm-port-types:if-10GE-ODU2e",
                                                "org-openroadm-port-types:if-10GE-ODU2"],
             "supporting-port": "CP1-SFP1-P1",
             "lcp-hash-val": "FqlcrxV7p30=",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "mpdr",
             "port-qual": "xpdr-client",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_23_spdr_portmapping_XPDR1_NETWORK1(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR1-NETWORK1",
             "supporting-circuit-pack-name": "CP1-CFP0",
             "supported-interface-capability": ["org-openroadm-port-types:if-OCH-OTU4-ODU4"],
             "supporting-port": "CP1-CFP0-P1",
             "lcp-hash-val": "Swfw02qXGyI=",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "mpdr",
             "port-qual": "xpdr-network",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_24_spdr_portmapping_XPDR2_CLIENT2(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR2-CLIENT2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR2-CLIENT2",
             "supporting-circuit-pack-name": "CP2-QSFP2",
             "supported-interface-capability": ["org-openroadm-port-types:if-100GE-ODU4",
                                                "org-openroadm-port-types:if-100GE"],
             "supporting-port": "CP2-QSFP2-P1",
             "lcp-hash-val": "AN/WSSRXne3t",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "switch",
             "port-qual": "switch-client",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_25_spdr_portmapping_XPDR2_NETWORK2(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR2-NETWORK2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR2-NETWORK2",
             "supporting-circuit-pack-name": "CP6-CFP",
             "supported-interface-capability": ["org-openroadm-port-types:if-OCH-OTU4-ODU4"],
             "supporting-port": "CP6-CFP-P1",
             "lcp-hash-val": "exT821pFtOQ=",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "switch",
             "port-qual": "switch-network",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_26_spdr_portmapping_XPDR3_CLIENT3(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR3-CLIENT3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR3-CLIENT3",
             "supporting-circuit-pack-name": "CP3-SFP3",
             "supported-interface-capability": ["org-openroadm-port-types:if-1GE-ODU0"],
             "supporting-port": "CP3-SFP3-P1",
             "lcp-hash-val": "AKsQ/HRQdtdN",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "mpdr",
             "port-qual": "xpdr-client",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_27_spdr_portmapping_XPDR3_NETWORK1(self):
        response = test_utils_rfc8040.portmapping_request("SPDR-SA1", "XPDR3-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {"logical-connection-point": "XPDR3-NETWORK1",
             "supporting-circuit-pack-name": "CP3-CFP0",
             "supported-interface-capability": ["org-openroadm-port-types:if-OCH-OTU4-ODU4"],
             "supporting-port": "CP3-CFP0-P1",
             "lcp-hash-val": "ANnxoi7K8q30",
             "port-direction": "bidirectional",
             "port-admin-state": "InService",
             "xponder-type": "mpdr",
             "port-qual": "xpdr-network",
             "port-oper-state": "InService"},
            response['mapping'])

    def test_28_spdr_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_29_xpdr_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_30_xpdr_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_31_xpdr_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-A1")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_32_xpdr_device_not_connected(self):
        response = test_utils_rfc8040.get_portmapping_node_info("XPDR-A1")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_33_rdm_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_34_rdm_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_35_rdm_device_not_connected(self):
        response = test_utils_rfc8040.get_portmapping_node_info("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == "__main__":
    unittest.main(verbosity=2)
