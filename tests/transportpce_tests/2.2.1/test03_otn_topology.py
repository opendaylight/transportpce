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
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


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
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.get_netconf_oper_request("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        logging.info(res)
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'NodeSA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_03_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'SPDR-SA1')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeSA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'XPONDER')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'], 'universal-switchponder')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:vendor'], 'vendorA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:ip'], '1.2.3.4')

    def test_04_getLinks_OpenroadmTopology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_05_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertIn('node', res['network'][0])
        if 'node' in res['network'][0]:
            nbNode = len(res['network'][0]['node'])
            self.assertEqual(nbNode, 3)
            listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2', 'SPDR-SA1-XPDR3']
            for i in range(0, nbNode):
                nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
                nodeId = res['network'][0]['node'][i]['node-id']
                if nodeId not in listNode:
                    self.assertFalse(True)
                    continue
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                self.assertTrue(client == 0)
                if nodeId == 'SPDR-SA1-XPDR1' or nodeId == 'SPDR-SA1-XPDR3':
                    self.assertTrue(network == 1)
                else:
                    # elif nodeId == 'SPDR-SA1-XPDR2':
                    self.assertTrue(network == 4)
                listNode.remove(nodeId)
            self.assertEqual(len(listNode), 0)

    def test_06_getLinks_OtnTopology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_07_getNodes_OtnTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_otn_topo_request()
        res = response.json()
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 3)
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
        for i in range(0, nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId in CHECK_LIST:
                nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
                self.assertEqual(nodeType, CHECK_LIST[nodeId]['node-type'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': nodeId},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]
                                    ['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'],
                                 CHECK_LIST[nodeId]['xpdr-number'])
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                        print("tpId = {}".format(tpId))
                        print("tp= {}".format(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]))
                        nbIfCapType = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                 ['supported-interface-capability'][0])
                        for k in range(0, nbIfCapType):
                            self.assertIn((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                             ['supported-interface-capability'][0]['if-cap-type']),
                                           CHECK_LIST[nodeId]['port-types'])
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                             ['supported-interface-capability'][0]['if-cap-type']),
                                         'org-openroadm-port-types:if-OCH-OTU4-ODU4')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                                             ['rate']),
                                         'org-openroadm-otn-common-types:ODU4')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['supporting-termination-point'][0]['network-ref']), 'openroadm-topology')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['supporting-termination-point'][0]['node-ref']), nodeId)
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['supporting-termination-point'][0]['tp-ref']), tpId)
                self.assertTrue(client == 4)
                self.assertTrue(network == CHECK_LIST[nodeId]['network_nb'])
                listNode.remove(nodeId)
                nbNbl = len(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                            ['odu-switching-pools'][0]['non-blocking-list'])
                self.assertEqual(nbNbl, CHECK_LIST[nodeId]['nbl_nb'])
                for k in range(0, nbNbl):
                    nbl = (res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                              ['odu-switching-pools'][0]['non-blocking-list'][k])
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
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_09_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 1)
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_10_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_11_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_12_getNodes_OtnTopology(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        self.assertNotIn('node', res['network'][0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
