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
from common import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims(['spdra'])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", 'spdra')
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
            self.assertEqual(nbNode, 2)
            listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2']
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
                if nodeId == 'SPDR-SA1-XPDR1':
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
        self.assertEqual(nbNode, 2)
        listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2']
        for i in range(0, nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'SPDR-SA1-XPDR1':
                nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
                self.assertEqual(nodeType, 'MUXPDR')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': 'SPDR-SA1-XPDR1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]
                                 ['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'], 1)
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                             ['supported-interface-capability'][0]['if-cap-type']),
                                         'org-openroadm-port-types:if-10GE-ODU2e')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                                             ['rate']),
                                         'org-openroadm-otn-common-types:ODU2e')
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
                                             ['supporting-termination-point'][0]['node-ref']), 'SPDR-SA1-XPDR1')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['supporting-termination-point'][0]['tp-ref']), 'XPDR1-NETWORK1')
                self.assertTrue(client == 4)
                self.assertTrue(network == 1)
                listNode.remove(nodeId)
                nbNbl = len(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                            ['odu-switching-pools'][0]['non-blocking-list'])
                self.assertEqual(nbNbl, 4)
                for k in range(0, nbNbl):
                    nbl = (res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                              ['odu-switching-pools'][0]['non-blocking-list'][k])
                    if nbl['nbl-number'] == 1:
                        self.assertEqual(nbl['available-interconnect-bandwidth'], 10)
                        self.assertEqual(nbl['interconnect-bandwidth-unit'], 1000000000)
                        self.assertIn('XPDR1-NETWORK1', nbl['tp-list'])
                        self.assertIn('XPDR1-CLIENT1', nbl['tp-list'])
                        self.assertNotIn('XPDR1-CLIENT2', nbl['tp-list'])
            elif nodeId == 'SPDR-SA1-XPDR2':
                nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
                self.assertEqual(nodeType, 'SWITCH')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': 'SPDR-SA1-XPDR2'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeSA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]
                                 ['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'], 2)
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                          ['org-openroadm-otn-network-topology:tp-supported-interfaces']
                                          ['supported-interface-capability'][0]['if-cap-type']),
                                         'org-openroadm-port-types:if-100GE-ODU4')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                                             ['rate']),
                                         'org-openroadm-otn-common-types:ODU4')
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
                                             ['supporting-termination-point'][0]['node-ref']), 'SPDR-SA1-XPDR2')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['supporting-termination-point'][0]['tp-ref']), tpId)
                self.assertTrue(client == 4)
                self.assertTrue(network == 4)
                listNode.remove(nodeId)
                nbNbl = len(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                            ['odu-switching-pools'][0]['non-blocking-list'])
                self.assertEqual(nbNbl, 1)
                nbl = (res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']
                          ['odu-switching-pools'][0]['non-blocking-list'][0])
                self.assertIn('XPDR2-NETWORK1', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT1', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK2', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT2', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK3', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT3', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK4', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT4', nbl['tp-list'])
            else:
                self.assertFalse(True)
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
