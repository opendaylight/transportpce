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
sys.path.append('transportpce_tests/common')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


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
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-A2",
                                           ('xpdra2', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    # Check if the node appears in the ietf-network topology
    def test_02_xpdr_device_connected(self):
        response = test_utils.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    # Check node info in the port-mappings
    def test_03_xpdr_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'xpdr',
             'node-ip-address': '1.2.3.4',
             'node-clli': 'NodeA',
             'openroadm-version': '7.1',
             'node-vendor': 'vendorA',
             'node-model': 'model'},
            response['node-info'])

    # Check the if-capabilities and the other details for network
    def test_04_tpdr_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {
                'logical-connection-point': 'XPDR1-NETWORK1',
                'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                'rate': '200',
                'supporting-port': 'L1',
                'port-oper-state': 'InService',
                'supported-interface-capability': ['org-openroadm-port-types:if-otsi-otsigroup'],
                'port-direction': 'bidirectional',
                'port-qual': 'xpdr-network',
                'port-admin-state': 'InService',
                'connection-map-lcp': 'XPDR1-CLIENT1',
                'supported-operational-mode': ['OR-W-200G-oFEC-31.6Gbd', 'OR-W-100G-oFEC-31.6Gbd'],
                'xpdr-type': 'tpdr',
                'lcp-hash-val': 'AIGiVAQ4gDil'
            },
            response['mapping'])

    def test_05_tpdr_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT1',
             'supporting-circuit-pack-name': '1/1/1-PLUG-CLIENT',
             'rate': '400', 'supporting-port': 'C1',
             'port-oper-state': 'InService',
             'supported-interface-capability': ['org-openroadm-port-types:if-400GE'],
             'port-direction': 'bidirectional',
             'port-qual': 'xpdr-client',
             'port-admin-state': 'InService',
             'connection-map-lcp': 'XPDR1-NETWORK1',
             'xpdr-type': 'tpdr',
             'lcp-hash-val': 'AODABTVSOHH0'
             },
            response['mapping']
        )

    # Check the port-mapping for the switch-client and switch-network port-quals
    def test_06_mpdr_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR2-NETWORK1',
             'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
             'rate': '200',
             'supporting-port': 'L1',
             'port-oper-state': 'InService',
             'supported-interface-capability': ['org-openroadm-port-types:if-otsi-otsigroup'],
             'port-direction': 'bidirectional',
             'port-qual': 'switch-network',
             'port-admin-state': 'InService',
             'supported-operational-mode': ['OR-W-200G-oFEC-31.6Gbd', 'OR-W-100G-oFEC-31.6Gbd'],
             'xpdr-type': 'mpdr',
             'lcp-hash-val': 'LY9PxYJqUbw='
             },
            response['mapping'])

    def test_07_mpdr_portmapping_CLIENT1(self):
        res = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-CLIENT1")
        self.assertEqual(res['status_code'], requests.codes.ok)
        self.assertIn('org-openroadm-port-types:if-100GE-ODU4',
                      res['mapping'][0]['supported-interface-capability'])
        self.assertIn('org-openroadm-port-types:if-OCH-OTU4-ODU4',
                      res['mapping'][0]['supported-interface-capability'])
        self.assertEqual('C1', res['mapping'][0]['supporting-port'])
        self.assertEqual('1/2/1/1-PLUG-CLIENT', res['mapping'][0]['supporting-circuit-pack-name'])
        self.assertEqual('XPDR2-CLIENT1', res['mapping'][0]['logical-connection-point'])
        self.assertEqual('bidirectional', res['mapping'][0]['port-direction'])
        self.assertEqual('switch-client', res['mapping'][0]['port-qual'])
        self.assertEqual('AK+Cna4EclRH', res['mapping'][0]['lcp-hash-val'])
        self.assertEqual('InService', res['mapping'][0]['port-admin-state'])
        self.assertEqual('InService', res['mapping'][0]['port-oper-state'])
        self.assertEqual('mpdr', res['mapping'][0]['xpdr-type'])
        self.assertEqual(1.1, float(res['mapping'][0]['mpdr-restrictions']['min-trib-slot']))
        self.assertEqual(1.2, float(res['mapping'][0]['mpdr-restrictions']['max-trib-slot']))

    # Added test to check mc-capability-profile for a transponder
    def test_08_check_mccapprofile(self):
        res = test_utils.get_portmapping_node_attr("XPDR-A2", "mc-capabilities", "XPDR-mcprofile")
        self.assertEqual(res['status_code'], requests.codes.ok)
        self.assertEqual(res['mc-capabilities'][0]['mc-node-name'], 'XPDR-mcprofile')
        self.assertEqual(float(res['mc-capabilities'][0]['center-freq-granularity']), 3.125)
        self.assertEqual(float(res['mc-capabilities'][0]['slot-width-granularity']), 6.25)

    def test_09_mpdr_switching_pool(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "switching-pool-lcp", "1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching-pool-lcp'][0]['switching-pool-type'])
        self.assertEqual(2,
                         len(response['switching-pool-lcp'][0]['non-blocking-list']))
        self.assertIn(
            {'nbl-number': 2,
             'interconnect-bandwidth': 0,
             'lcp-list': ['XPDR2-NETWORK1', 'XPDR2-CLIENT2']},
            response['switching-pool-lcp'][0]['non-blocking-list'])

    def test_10_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_11_xpdr_device_disconnected(self):
        response = test_utils.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_12_xpdr_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == '__main__':
    unittest.main(verbosity=2)
