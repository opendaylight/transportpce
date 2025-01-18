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
import logging
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_oc


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = 'oc'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('oc-mpdr', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    def test_01_meta_data_insertion(self):
        response = test_utils_oc.metadata_input()
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_02_catlog_input_insertion(self):
        response = test_utils_oc.catlog_input()
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_03_connect_mpdr(self):
        response = test_utils.mount_device("XPDR-OC",
                                           ('oc-mpdr', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(3)

        response = test_utils.check_device_connection("XPDR-OC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_04_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        logging.info(response)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], '1')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], '1')

    def test_05_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'XPDR-OC')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['node-ref'], '1')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'XPONDER')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:model'], 'Chassis component')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:vendor'], 'vendor1')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:ip'], '127.0.0.1')

    def test_06_getLinks_OpenroadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_07_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('node', response['network'][0])
        self.assertEqual(len(response['network'][0]['node']), 1)
        listNode = ['XPDR-OC-XPDR1']
        for node in response['network'][0]['node']:
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-OC'}, node['supporting-node'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': '1'}, node['supporting-node'])
            nodeType = node['org-openroadm-common-network:node-type']
            nodeId = node['node-id']
            if nodeId not in listNode:
                self.assertFalse(True)
                continue
            self.assertEqual(nodeType, 'XPONDER')
            client = 0
            network = 0
            for tp in node['ietf-network-topology:termination-point']:
                tpType = tp['org-openroadm-common-network:tp-type']
                if tpType == 'XPONDER-CLIENT':
                    client += 1
                elif tpType == 'XPONDER-NETWORK':
                    network += 1
            self.assertTrue(client == 0)
            self.assertTrue(network == 1)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_08_getLinks_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_07_getNodes_OtnTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 1)
        listNode = ['XPDR-OC-XPDR1']
        CHECK_LIST = {
            'XPDR-OC-XPDR1': {
                'node-type': 'MUXPDR',
                'xpdr-number': 1,
                'network_nb': 1,
                'nbl_nb': 4,
                'tp-checklist': ['XPDR1-NETWORK1', 'XPDR1-CLIENT1'],
                'tp-unchecklist': ['XPDR1-CLIENT2']
            }
        }
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            self.assertEqual(node['org-openroadm-common-network:node-type'], CHECK_LIST[nodeId]['node-type'])
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-OC'},
                          node['supporting-node'])
            self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': nodeId},
                          node['supporting-node'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': '1'},
                          node['supporting-node'])
            self.assertEqual(node['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'],
                             CHECK_LIST[nodeId]['xpdr-number'])
            client = 0
            network = 0
            for tp in node['ietf-network-topology:termination-point']:
                tpType = tp['org-openroadm-common-network:tp-type']
                tpId = tp['tp-id']
                if tpType == 'XPONDER-CLIENT':
                    client += 1
                elif tpType == 'XPONDER-NETWORK':
                    network += 1
                    self.assertEqual((tp['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                      ['supported-interface-capability'][0]['if-cap-type']),
                                     'org-openroadm-port-types:if-OTUCn-ODUCn')
                    self.assertEqual((tp['supporting-termination-point'][0]['network-ref']), 'openroadm-topology')
                    self.assertEqual((tp['supporting-termination-point'][0]['node-ref']), nodeId)
                    self.assertEqual((tp['supporting-termination-point'][0]['tp-ref']), tpId)
            self.assertTrue(client == 4)
            self.assertTrue(network == CHECK_LIST[nodeId]['network_nb'])
            self.assertEqual(
                len(node['org-openroadm-otn-network-topology:switching-pools']
                    ['odu-switching-pools'][0]['non-blocking-list']),
                CHECK_LIST[nodeId]['nbl_nb'])
            # pylint: disable=line-too-long
            for nbl in node['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list']:
                if nbl['nbl-number'] == 1:
                    self.assertEqual(nbl['available-interconnect-bandwidth'], 10)
                    self.assertEqual(nbl['interconnect-bandwidth-unit'], 1000000000)
                    for tp in CHECK_LIST[nodeId]['tp-checklist']:
                        self.assertIn(tp, nbl['tp-list'])
                    for tp in CHECK_LIST[nodeId]['tp-unchecklist']:
                        self.assertNotIn(tp, nbl['tp-list'])
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_08_disconnect_mpdr(self):
        response = test_utils.unmount_device("XPDR-OC")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_09_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 1)
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], '1')

    def test_10_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('node', response['network'][0])

    def test_11_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertNotIn('node', response['network'][0])

    def test_12_getNodes_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertNotIn('node', response['network'][0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
