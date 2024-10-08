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


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.check_device_connection("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_02_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        logging.info(response)
        self.assertEqual(response['network'][0]['node'][1]['node-id'], 'NodeSA')
        self.assertEqual(response['network'][0]['node'][1]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_03_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'SPDR-SA1')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeSA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'XPONDER')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:model'], 'universal-switchponder')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:vendor'], 'vendorA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:ip'], '1.2.3.4')

    def test_04_getLinks_OpenroadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_05_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('node', response['network'][0])
        self.assertEqual(len(response['network'][0]['node']), 4)
        listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2', 'SPDR-SA1-XPDR3']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'}, node['supporting-node'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'}, node['supporting-node'])
            nodeType = node['org-openroadm-common-network:node-type']
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
            if nodeId in ('SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR3'):
                self.assertTrue(network == 1)
            else:
                # elif nodeId == 'SPDR-SA1-XPDR2':
                self.assertTrue(network == 4)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_06_getLinks_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_07_getNodes_OtnTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 4)
        listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2', 'SPDR-SA1-XPDR3']
        CHECK_LIST = {
            'SPDR-SA1-XPDR1': {
                'node-type': 'MUXPDR',
                'xpdr-number': 1,
                'port-types': ['org-openroadm-port-types:if-10GE-ODU2', 'org-openroadm-port-types:if-10GE-ODU2e',
                               'org-openroadm-port-types:if-10GE'],
                'otn-common-types': 'org-openroadm-otn-common-types:ODU2e',
                'network_nb': 1,
                'nbl_nb': 4,
                'tp-checklist': ['XPDR1-NETWORK1', 'XPDR1-CLIENT1'],
                'tp-unchecklist': ['XPDR1-CLIENT2']
            },
            'SPDR-SA1-XPDR2': {
                'node-type': 'SWITCH',
                'xpdr-number': 2,
                'port-types': 'org-openroadm-port-types:if-100GE-ODU4',
                'otn-common-types': 'org-openroadm-otn-common-types:ODU4',
                'network_nb': 4,
                'nbl_nb': 1,
                'tp-checklist': ['XPDR2-NETWORK4', 'XPDR2-CLIENT1', 'XPDR2-NETWORK3', 'XPDR2-CLIENT4',
                                 'XPDR2-CLIENT2', 'XPDR2-NETWORK2', 'XPDR2-CLIENT3', 'XPDR2-NETWORK1'],
                'tp-unchecklist': []
            }
        }
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            if nodeId in CHECK_LIST:
                self.assertEqual(node['org-openroadm-common-network:node-type'], CHECK_LIST[nodeId]['node-type'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              node['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': nodeId},
                              node['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'},
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
                        # pylint: disable=consider-using-f-string
                        print("tpId = {}".format(tpId))
                        print("tp= {}".format(tp))
                        # pylint: disable=line-too-long
                        for capa in tp['org-openroadm-otn-network-topology:tp-supported-interfaces']['supported-interface-capability']:
                            self.assertIn((capa['if-cap-type']), CHECK_LIST[nodeId]['port-types'])
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                        self.assertEqual((tp['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                          ['supported-interface-capability'][0]['if-cap-type']),
                                         'org-openroadm-port-types:if-OCH-OTU4-ODU4')
                        self.assertEqual((tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                                          ['rate']),
                                         'org-openroadm-otn-common-types:ODU4')
                        self.assertEqual((tp['supporting-termination-point'][0]['network-ref']), 'openroadm-topology')
                        self.assertEqual((tp['supporting-termination-point'][0]['node-ref']), nodeId)
                        self.assertEqual((tp['supporting-termination-point'][0]['tp-ref']), tpId)
                self.assertTrue(client == 4)
                self.assertTrue(network == CHECK_LIST[nodeId]['network_nb'])
                listNode.remove(nodeId)
                self.assertEqual(
                    len(node['org-openroadm-otn-network-topology:switching-pools']
                        ['odu-switching-pools'][0]['non-blocking-list']),
                    CHECK_LIST[nodeId]['nbl_nb'])
                # pylint: disable=line-too-long
                for nbl in node['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list']:
                    if nbl['nbl-number'] == 1:
                        if nodeId == 'SPDR-SA1-XPDR1':
                            self.assertEqual(nbl['available-interconnect-bandwidth'], 10)
                            self.assertEqual(nbl['interconnect-bandwidth-unit'], 1000000000)
                        for tp in CHECK_LIST[nodeId]['tp-checklist']:
                            self.assertIn(tp, nbl['tp-list'])
                        for tp in CHECK_LIST[nodeId]['tp-unchecklist']:
                            self.assertNotIn(tp, nbl['tp-list'])
            else:
                self.assertEqual('SPDR-SA1-XPDR3', nodeId)
                listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_08_disconnect_SPDR_SA1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_09_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 2)
        self.assertEqual(response['network'][0]['node'][1]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_10_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)

    def test_11_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)

    def test_12_getNodes_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
