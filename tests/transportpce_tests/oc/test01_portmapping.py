#!/usr/bin/env python
##############################################################################
# Copyright © 2024 NTT and others.  All rights reserved.
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
from . import test_utils_oc
sys.path.append('transportpce_tests/common')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportpceOCPortMappingTesting(unittest.TestCase):
    processes = None
    NODE_VERSION = 'oc'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('sample-openconfig-mpdr', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        test_utils_oc.del_metadata()
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_meta_data_insertion(self):
        response = test_utils_oc.metadata_input()
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_02_catlog_input_insertion(self):
        response = test_utils_oc.catlog_input()
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_03_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-OC",
                                           ('sample-openconfig-mpdr', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_04_xpdr_device_connected(self):
        response = test_utils.check_device_connection("XPDR-OC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_05_xpdr_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'xpdr',
             'node-ip-address': '127.0.0.1',
             'node-clli': '1',
             'openconfig-version': '1.9.0',
             'node-vendor': 'vendor1',
             'node-model': 'Chassis component',
             'sw-version': 'platform-OS-1.0'},
            response['node-info'])

    def test_06_mpdr_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "mapping", "XPDR1-NETWORK5")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {
                'logical-connection-point': 'XPDR1-NETWORK5',
                'port-qual': 'switch-network',
                'port-oper-state': 'ACTIVE',
                'xpdr-type': 'mpdr',
                'supporting-circuit-pack-name': 'cfp2-transceiver-1',
                'lcp-hash-val': 'AOVxBCXPOzbw',
                'supported-interface-capability': ['org-openroadm-port-types:if-OTUCn-ODUCn'],
                'port-direction': 'bidirectional',
                'port-admin-state': 'ENABLED',
                'supporting-port': 'line-cfp2-1'
            },
            response['mapping'])

    def test_07_mpdr_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT1',
             'port-qual': 'switch-client',
             'port-oper-state': 'ACTIVE',
             'xpdr-type': 'mpdr',
             'supporting-circuit-pack-name': 'qsfp-transceiver-1',
             'lcp-hash-val': 'ALoMFfw9DapP',
             'port-direction': 'bidirectional',
             'port-admin-state': 'ENABLED',
             'supporting-port': 'client-qsfp-1'
             },
            response['mapping']
        )

    def test_08_mpdr_switching_pool(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "switching-pool-lcp", "1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching-pool-lcp'][0]['switching-pool-type'])
        self.assertEqual(1,
                         len(response['switching-pool-lcp'][0]['non-blocking-list']))
        actual_lcp_list = response['switching-pool-lcp'][0]['non-blocking-list'][0]['lcp-list']
        sorted_actual_lcp_list = sorted(actual_lcp_list)
        expected_lcp_list = sorted(['XPDR1-CLIENT4', 'XPDR1-CLIENT3',
                                   'XPDR1-NETWORK5', 'XPDR1-CLIENT2', 'XPDR1-CLIENT1'])
        self.assertEqual(sorted_actual_lcp_list, expected_lcp_list)

    def test_09_check_mccapprofile(self):
        res = test_utils.get_portmapping_node_attr("XPDR-OC", "mc-capabilities", "XPDR-mcprofile")
        self.assertEqual(res['status_code'], requests.codes.ok)
        self.assertEqual(res['mc-capabilities'][0]['mc-node-name'], 'XPDR-mcprofile')
        self.assertEqual(float(res['mc-capabilities'][0]['center-freq-granularity']), 12.5)
        self.assertEqual(float(res['mc-capabilities'][0]['slot-width-granularity']), 25.0)

    def test_10_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-OC")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == '__main__':
    unittest.main(verbosity=2)
