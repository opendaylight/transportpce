#!/usr/bin/env python
##############################################################################
# Copyright © 2024 NTT and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_oc  # nopep8


class TransportpceOCRendererTesting(unittest.TestCase):
    processes = None
    NODE_VERSION = 'oc'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('oc-mpdr', cls.NODE_VERSION)])

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
                                           ('oc-mpdr', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_04_xpdr_device_connected(self):
        response = test_utils.check_device_connection("XPDR-OC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_05_mpdr_portmapping_NETWORK5(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "mapping", "XPDR1-NETWORK5")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {
                'logical-connection-point': 'XPDR1-NETWORK5',
                'port-qual': 'switch-network',
                'port-oper-state': 'ACTIVE',
                "rate": "400",
                'xpdr-type': 'mpdr',
                'openconfig-info': {
                    'supported-optical-channels': ['cfp2-opt-1-1']
                },
                'supporting-circuit-pack-name': 'cfp2-transceiver-1',
                'lcp-hash-val': 'AOVxBCXPOzbw',
                'supported-interface-capability': ['org-openroadm-port-types:if-OTUCn-ODUCn'],
                'port-direction': 'bidirectional',
                'port-admin-state': 'ENABLED',
                'supporting-port': 'line-cfp2-1'
            },
            response['mapping'])

    def test_06_service_path_create_network(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OC_network',
                'operation': 'create',
                'operational-mode': 4308,
                'target-output-power': 0,
                'center-freq': 194.1,
                'nodes': [{'node-id': 'XPDR-OC', 'dest-tp': 'XPDR1-NETWORK5'}]
            }
        )
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Components configured successfully for nodes: XPDR-OC', response['output']['result'])
        self.assertTrue(response['output']['success'])
        self.assertIn({'node-id': 'XPDR-OC', 'port-id': ['line-cfp2-1']},
                      response['output']['node-interface'])

    def test_07_service_path_delete_network(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OC_network',
                'operation': 'delete',
                'nodes': [{'node-id': 'XPDR-OC', 'dest-tp': 'XPDR1-NETWORK5'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        self.assertTrue(response['output']['success'])

    def test_08_get_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-OC", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        expected = {
            'logical-connection-point': 'XPDR1-CLIENT1',
            'port-qual': 'switch-client',
            'port-oper-state': 'ACTIVE',
            'rate': '100',
            'xpdr-type': 'mpdr',
            'openconfig-info': {
                'supported-optical-channels': ['qsfp-opt-1-1', 'qsfp-opt-1-2', 'qsfp-opt-1-3', 'qsfp-opt-1-4'],
                'supported-interfaces': ['logical-channel-23300101', 'logical-channel-24300101']
            },
            'supporting-circuit-pack-name': 'qsfp-transceiver-1',
            'lcp-hash-val': 'ALoMFfw9DapP',
            'supported-interface-capability': ['org-openroadm-port-types:if-100GE-ODU4'],
            'port-direction': 'bidirectional',
            'port-admin-state': 'ENABLED',
            'supporting-port': 'client-qsfp-1'
        }
        expected_sorted = test_utils_oc.recursive_sort(expected)
        response_sorted = [
            test_utils_oc.recursive_sort(item) for item in response['mapping']
        ]
        self.assertIn(expected_sorted, response_sorted)

    def test_09_service_path_create_client(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_OC_client',
                'operation': 'create',
                'service-rate': '100',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'XPDR-OC', 'client-tp': 'XPDR1-CLIENT1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :XPDR-OC', response['output']['result'])
        self.assertTrue(response['output']['success'])
        expected = {
            'node-id': 'XPDR-OC',
            'optical-channel-id': [
                'qsfp-opt-1-1',
                'qsfp-opt-1-2',
                'qsfp-opt-1-3',
                'qsfp-opt-1-4'
            ],
            'port-id': [
                'client-qsfp-1'
            ],
            'interface-id': [
                'logical-channel-24300101',
                'logical-channel-23300101'
            ]
        }
        expected_sorted = test_utils_oc.recursive_sort(expected)
        response_sorted = [
            test_utils_oc.recursive_sort(item) for item in response['output']['node-interface']
        ]
        self.assertIn(expected_sorted, response_sorted)

    def test_10_service_path_delete_client(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_OC_client',
                'operation': 'delete',
                'service-rate': '100',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'XPDR-OC', 'client-tp': 'XPDR1-CLIENT1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('successfully disabled entities on node XPDR-OC!', response['output']['result'])
        self.assertTrue(response['output']['success'])

    def test_11_disconnect(self):
        response = test_utils.unmount_device("XPDR-OC")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
