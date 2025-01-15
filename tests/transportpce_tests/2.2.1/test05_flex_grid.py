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
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('roadmd', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(2)

    def test_01_rdm_device_connection(self):
        response = test_utils.mount_device("ROADM-D1", ('roadmd', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_rdm_device_connected(self):
        response = test_utils.check_device_connection("ROADM-D1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')
        time.sleep(10)

    def test_03_rdm_portmapping_info(self):
        response = test_utils.get_portmapping_node_attr("ROADM-D1", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            {'node-type': 'rdm',
             'node-ip-address': '127.0.0.14',
             'node-clli': 'NodeD',
             'openroadm-version': '2.2.1',
             'node-vendor': 'vendorD',
             'node-model': 'model2'},
            response['node-info'])
        time.sleep(3)

    def test_04_rdm_deg1_lcp(self):
        # pylint: disable=line-too-long
        response = test_utils.get_portmapping_node_attr("ROADM-D1", "mc-capabilities", "DEG1-TTP")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(response['mc-capabilities'],
                      [[{'mc-node-name': 'DEG1-TTP', 'max-slots': 384, 'center-freq-granularity': 6.25, 'min-slots': 3, 'slot-width-granularity': 12.5}],
                       [{'mc-node-name': 'DEG1-TTP', 'max-slots': 384, 'center-freq-granularity': '6.25', 'min-slots': 3, 'slot-width-granularity': '12.5'}]])
        time.sleep(3)

    def test_05_rdm_deg2_lcp(self):
        # pylint: disable=line-too-long
        response = test_utils.get_portmapping_node_attr("ROADM-D1", "mc-capabilities", "DEG2-TTP")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(response['mc-capabilities'],
                      [[{'mc-node-name': 'DEG2-TTP', 'max-slots': 384, 'center-freq-granularity': 6.25, 'min-slots': 3, 'slot-width-granularity': 12.5}],
                       [{'mc-node-name': 'DEG2-TTP', 'max-slots': 384, 'center-freq-granularity': '6.25', 'min-slots': 3, 'slot-width-granularity': '12.5'}]])
        time.sleep(3)

    def test_06_rdm_srg1_lcp(self):
        # pylint: disable=line-too-long
        response = test_utils.get_portmapping_node_attr("ROADM-D1", "mc-capabilities", "SRG1-PP")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(response['mc-capabilities'],
                      [[{'mc-node-name': 'SRG1-PP', 'max-slots': 384, 'center-freq-granularity': 6.25, 'min-slots': 3, 'slot-width-granularity': 12.5}],
                       [{'mc-node-name': 'SRG1-PP', 'max-slots': 384, 'center-freq-granularity': '6.25', 'min-slots': 3, 'slot-width-granularity': '12.5'}]])
        time.sleep(3)

    # Renderer interface creations
    def test_07_device_renderer(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'modulation-format': 'dp-qpsk',
                'operation': 'create',
                'service-name': 'testNMC-MC',
                'wave-number': '0',
                'center-freq': '196.05',
                'nmc-width': '80',
                'nodes': [
                    {
                        'node-id': 'ROADM-D1',
                        'src-tp': 'SRG1-PP1-TXRX',
                        'dest-tp': 'DEG1-TTP-TXRX'
                    }
                ],
                'min-freq': 196.00625,
                'max-freq': 196.09375,
                'lower-spectral-slot-number': 749,
                'higher-spectral-slot-number': 763
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ', response['output']['result'])
        time.sleep(10)

    # Get Degree MC interface and check
    def test_08_degree_mc_interface(self):
        response = test_utils.check_node_attribute_request("ROADM-D1", "interface", "DEG1-TTP-TXRX-mc-749:763")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            dict({"name": "DEG1-TTP-TXRX-mc-749:763",
                  "supporting-interface": "OMS-DEG1-TTP-TXRX",
                  "supporting-circuit-pack-name": "1/0",
                  "circuit-id": "TBD",
                  "description": "TBD",
                  "supporting-port": "L1",
                  "type": "org-openroadm-interfaces:mediaChannelTrailTerminationPoint"},
                 **response['interface'][0]), response['interface'][0])
        # Check the mc-ttp max and min-freq
        mcttp = response['interface'][0]['org-openroadm-media-channel-interfaces:mc-ttp']
        self.assertEqual(float(mcttp['min-freq']), 196.00625)
        self.assertEqual(float(mcttp['max-freq']), 196.09375)
        time.sleep(3)

    # get DEG-NMC interface and check
    def test_09_degree_nmc_interface(self):
        response = test_utils.check_node_attribute_request("ROADM-D1", "interface", "DEG1-TTP-TXRX-nmc-749:763")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            dict({"name": "DEG1-TTP-TXRX-nmc-749:763",
                  "supporting-interface": "DEG1-TTP-TXRX-mc-749:763",
                  "supporting-circuit-pack-name": "1/0",
                  "circuit-id": "TBD",
                  "description": "TBD",
                  "supporting-port": "L1",
                  "type": "org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint"},
                 **response['interface'][0]), response['interface'][0])
        nmcctp = response['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp']
        self.assertEqual(float(nmcctp['frequency']), 196.05)
        self.assertEqual(float(nmcctp['width']), 80)
        time.sleep(3)

    # get SRG-NMC interface
    def test_10_srg_nmc_interface(self):
        response = test_utils.check_node_attribute_request("ROADM-D1", "interface", "SRG1-PP1-TXRX-nmc-749:763")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            dict({"name": "SRG1-PP1-TXRX-nmc-749:763",
                  "supporting-circuit-pack-name": "3/0",
                  "circuit-id": "TBD",
                  "description": "TBD",
                  "supporting-port": "C1",
                  "type": "org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint"},
                 **response['interface'][0]), response['interface'][0])
        nmcctp = response['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp']
        self.assertEqual(float(nmcctp['frequency']), 196.05)
        self.assertEqual(float(nmcctp['width']), 80)
        time.sleep(3)

    # Create ROADM-connection
    def test_11_roadm_connection(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-D1", "roadm-connections", "SRG1-PP1-TXRX-DEG1-TTP-TXRX-749:763")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("SRG1-PP1-TXRX-DEG1-TTP-TXRX-749:763",
                         response['roadm-connections'][0]['connection-name'])
        self.assertEqual("SRG1-PP1-TXRX-nmc-749:763",
                         response['roadm-connections'][0]['source']['src-if'])
        self.assertEqual("DEG1-TTP-TXRX-nmc-749:763",
                         response['roadm-connections'][0]['destination']['dst-if'])
        time.sleep(3)

    # Delete ROADM connections and interfaces

    # delete ROADM connection
    def test_12_delete_roadm_connection(self):
        response = test_utils.del_node_attribute_request(
            "ROADM-D1", "roadm-connections", "SRG1-PP1-TXRX-DEG1-TTP-TXRX-749:763")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        time.sleep(3)

    # Delete NMC SRG interface
    def test_13_delete_srg_interface(self):
        response = test_utils.del_node_attribute_request("ROADM-D1", "interface", "SRG1-PP1-TXRX-nmc-749:763")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        time.sleep(3)

    # Delete NMC Degree interface
    def test_14_delete_degree_interface(self):
        response = test_utils.del_node_attribute_request("ROADM-D1", "interface", "DEG1-TTP-TXRX-nmc-749:763")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        time.sleep(3)

    # Delete MC Degree interface
    def test_15_delete_degree_interface(self):
        response = test_utils.del_node_attribute_request("ROADM-D1", "interface", "DEG1-TTP-TXRX-mc-749:763")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        time.sleep(3)

    def test_16_disconnect_ROADM_D1(self):
        response = test_utils.unmount_device("ROADM-D1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
