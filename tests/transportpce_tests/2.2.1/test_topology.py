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
# pylint: disable=too-many-lines

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
        cls.processes = test_utils.start_sims(['xpdra', 'roadma', 'roadmb', 'roadmc'])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_ROADM_A1(self):
        response = test_utils.mount_device("ROADM-A1", 'roadma')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        logging.info(res)
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_03_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'ROADM-A1')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'ROADM')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'], 'model2')

    def test_04_getLinks_OpenroadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 10)
        expressLink = ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX']
        addLink = ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX']
        dropLink = ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX']
        for i in range(0, nbLink):
            linkId = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if (res['network'][0]['ietf-network-topology:link'][i]
                   ['org-openroadm-common-network:link-type'] == 'EXPRESS-LINK'):
                find = linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif (res['network'][0]['ietf-network-topology:link'][i]
                     ['org-openroadm-common-network:link-type'] == 'ADD-LINK'):
                find = linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif (res['network'][0]['ietf-network-topology:link'][i]
                     ['org-openroadm-common-network:link-type'] == 'DROP-LINK'):
                find = linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink), 0)
        self.assertEqual(len(addLink), 0)
        self.assertEqual(len(dropLink), 0)

    def test_05_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 4)
        listNode = ['ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for i in range(0, nbNode):
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                          res['network'][0]['node'][i]['supporting-node'])
            nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'ROADM-A1-SRG1':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG3':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG1':
                # Test related to DEG1
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG2':
                # Test related to DEG2
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    def test_06_connect_XPDRA(self):
        response = test_utils.mount_device("XPDR-A1", 'xpdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_07_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_08_getOpenRoadmNetwork(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 2)
        for i in range(0, nbNode):
            self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['network-ref'], 'clli-network')
            self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'], 'NodeA')
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'XPDR-A1':
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'XPONDER')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'], 'model2')
            elif nodeId == 'ROADM-A1':
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'], 'model2')
            else:
                self.assertFalse(True)

    def test_09_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 5)
        listNode = ['XPDR-A1-XPDR1', 'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for i in range(0, nbNode):
            nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
            nodeId = res['network'][0]['node'][i]['node-id']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDR-A1-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                    if tpId == 'XPDR1-NETWORK2':
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                         ['transportpce-topology:associated-connection-map-port'], 'XPDR1-CLIENT2')
                    if tpId == 'XPDR1-CLIENT2':
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                         ['transportpce-topology:associated-connection-map-port'], 'XPDR1-NETWORK2')

                self.assertTrue(client == 2)
                self.assertTrue(network == 2)
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG1':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG3':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG1':
                # Test related to DEG1
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG2':
                # Test related to DEG2
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    # Connect the tail XPDRA to ROADMA and vice versa
    def test_10_connect_tail_xpdr_rdm(self):
        # Connect the tail: XPDRA to ROADMA
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_11_connect_tail_rdm_xpdr(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_12_getLinks_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 12)
        expressLink = ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX']
        addLink = ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX']
        dropLink = ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX']
        XPDR_IN = ['ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1']
        XPDR_OUT = ['XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX']
        for i in range(0, nbLink):
            nodeType = res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-common-network:link-type']
            linkId = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if nodeType == 'EXPRESS-LINK':
                find = linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif nodeType == 'ADD-LINK':
                find = linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif nodeType == 'DROP-LINK':
                find = linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            elif nodeType == 'XPONDER-INPUT':
                find = linkId in XPDR_IN
                self.assertEqual(find, True)
                XPDR_IN.remove(linkId)
            elif nodeType == 'XPONDER-OUTPUT':
                find = linkId in XPDR_OUT
                self.assertEqual(find, True)
                XPDR_OUT.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink), 0)
        self.assertEqual(len(addLink), 0)
        self.assertEqual(len(dropLink), 0)
        self.assertEqual(len(XPDR_IN), 0)
        self.assertEqual(len(XPDR_OUT), 0)

    def test_13_connect_ROADMC(self):
        response = test_utils.mount_device("ROADM-C1", 'roadmc')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_14_omsAttributes_ROADMA_ROADMC(self):
        # Config ROADMA-ROADMC oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_15_omsAttributes_ROADMC_ROADMA(self):
        # Config ROADM-C1-ROADM-A1 oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}

        response = test_utils.add_oms_attr_request("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_16_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        listNode = ['NodeA', 'NodeC']
        for i in range(0, nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            find = nodeId in listNode
            self.assertEqual(find, True)
            if nodeId == 'NodeA':
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeA')
            else:
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeC')
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_17_getOpenRoadmNetwork(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 3)
        listNode = ['XPDR-A1', 'ROADM-A1', 'ROADM-C1']
        for i in range(0, nbNode):
            self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['network-ref'], 'clli-network')
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'XPDR-A1':
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'], 'NodeA')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'XPONDER')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'], 'model2')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1':
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'], 'NodeA')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'], 'model2')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-C1':
                self.assertEqual(res['network'][0]['node'][i]['supporting-node'][0]['node-ref'], 'NodeC')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'ROADM')
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-network:model'], 'model2')
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    def test_18_getROADMLinkOpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 20)
        expressLink = ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                       'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX',
                       'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX']
        addLink = ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                   'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX']
        dropLink = ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                    'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX',
                    'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX']
        R2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX']
        XPDR_IN = ['ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1']
        XPDR_OUT = ['XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX']
        for i in range(0, nbLink):
            nodeType = res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-common-network:link-type']
            linkId = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if nodeType == 'EXPRESS-LINK':
                find = linkId in expressLink
                self.assertEqual(find, True)
                expressLink.remove(linkId)
            elif nodeType == 'ADD-LINK':
                find = linkId in addLink
                self.assertEqual(find, True)
                addLink.remove(linkId)
            elif nodeType == 'DROP-LINK':
                find = linkId in dropLink
                self.assertEqual(find, True)
                dropLink.remove(linkId)
            elif nodeType == 'ROADM-TO-ROADM':
                find = linkId in R2RLink
                self.assertEqual(find, True)
                R2RLink.remove(linkId)
            elif nodeType == 'XPONDER-INPUT':
                find = linkId in XPDR_IN
                self.assertEqual(find, True)
                XPDR_IN.remove(linkId)
            elif nodeType == 'XPONDER-OUTPUT':
                find = linkId in XPDR_OUT
                self.assertEqual(find, True)
                XPDR_OUT.remove(linkId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(expressLink), 0)
        self.assertEqual(len(addLink), 0)
        self.assertEqual(len(dropLink), 0)
        self.assertEqual(len(R2RLink), 0)
        self.assertEqual(len(XPDR_IN), 0)
        self.assertEqual(len(XPDR_OUT), 0)

    def test_19_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 20)
        R2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX']
        for i in range(0, nbLink):
            link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = res['network'][0]['ietf-network-topology:link'][i][
                    'org-openroadm-network-topology:OMS-attributes']['span']["engineered-spanloss"]
                length = res['network'][0]['ietf-network-topology:link'][i][
                    'org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
                if (spanLoss is not None) & (length is not None):
                    find = True
                self.assertTrue(find)
                R2RLink.remove(link_id)
        self.assertEqual(len(R2RLink), 0)

    def test_20_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 8)
        listNode = ['XPDR-A1-XPDR1',
                    'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2',
                    'ROADM-C1-SRG1', 'ROADM-C1-DEG1', 'ROADM-C1-DEG2']
        # ************************Tests related to XPDRA nodes
        for i in range(0, nbNode):
            nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'XPDR-A1-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertTrue(nbTps >= 4)
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = (res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                 ['org-openroadm-common-network:tp-type'])
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                self.assertTrue(client == 2)
                self.assertTrue(network == 2)
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG1':
                # Test related to SRG1
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'SRG')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG3':
                # Test related to SRG1
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'SRG')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG1':
                # Test related to DEG1
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'DEGREE')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG2':
                # Test related to DEG2
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'DEGREE')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-C1-SRG1':
                # Test related to SRG1
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-C1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'SRG')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-C1-DEG1':
                # Test related to DEG1
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-C1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'DEGREE')
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-C1-DEG2':
                # Test related to DEG1
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-C1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-common-network:node-type'], 'DEGREE')
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    def test_21_connect_ROADMB(self):
        response = test_utils.mount_device("ROADM-B1", 'roadmb')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_22_omsAttributes_ROADMA_ROADMB(self):
        # Config ROADM-A1-ROADM-B1 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "spanloss-current": 12,
                "spanloss-base": 11.4,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_23_omsAttributes_ROADMB_ROADMA(self):
        # Config ROADM-B1-ROADM-A1 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "spanloss-current": 12,
                "spanloss-base": 11.4,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_24_omsAttributes_ROADMB_ROADMC(self):
        # Config ROADM-B1-ROADM-C1 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "spanloss-current": 12,
                "spanloss-base": 11.4,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_25_omsAttributes_ROADMC_ROADMB(self):
        # Config ROADM-C1-ROADM-B1 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_26_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        listNode = ['NodeA', 'NodeB', 'NodeC']
        for i in range(0, nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            find = nodeId in listNode
            self.assertEqual(find, True)
            if nodeId == 'NodeA':
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeA')
            elif nodeId == 'NodeB':
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeB')
            else:
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeC')
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_27_verifyDegree(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        listR2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX',
                       'ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX']
        for i in range(0, nbLink):
            if (res['network'][0]['ietf-network-topology:link'][i]
                   ['org-openroadm-common-network:link-type'] == 'ROADM-TO-ROADM'):
                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
                find = link_id in listR2RLink
                self.assertEqual(find, True)
                listR2RLink.remove(link_id)
        self.assertEqual(len(listR2RLink), 0)

    def test_28_verifyOppositeLinkTopology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 26)
        for i in range(0, nbLink):
            link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            link_type = res['network'][0]['ietf-network-topology:link'][i]['org-openroadm-common-network:link-type']
            link_src = res['network'][0]['ietf-network-topology:link'][i]['source']['source-node']
            link_dest = res['network'][0]['ietf-network-topology:link'][i]['destination']['dest-node']
            oppLink_id = (res['network'][0]['ietf-network-topology:link'][i]
                             ['org-openroadm-common-network:opposite-link'])
            # Find the opposite link
            response_oppLink = test_utils.get_ordm_topo_request("ietf-network-topology:link/"+oppLink_id)
            self.assertEqual(response_oppLink.status_code, requests.codes.ok)
            res_oppLink = response_oppLink.json()
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]
                             ['org-openroadm-common-network:opposite-link'], link_id)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['source']['source-node'], link_dest)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['destination']['dest-node'], link_src)
            oppLink_type = res_oppLink['ietf-network-topology:link'][0]['org-openroadm-common-network:link-type']
            if link_type == 'ADD-LINK':
                self.assertEqual(oppLink_type, 'DROP-LINK')
            elif link_type == 'DROP-LINK':
                self.assertEqual(oppLink_type, 'ADD-LINK')
            elif link_type == 'EXPRESS-LINK':
                self.assertEqual(oppLink_type, 'EXPRESS-LINK')
            elif link_type == 'ROADM-TO-ROADM':
                self.assertEqual(oppLink_type, 'ROADM-TO-ROADM')
            elif link_type == 'XPONDER-INPUT':
                self.assertEqual(oppLink_type, 'XPONDER-OUTPUT')
            elif link_type == 'XPONDER-OUTPUT':
                self.assertEqual(oppLink_type, 'XPONDER-INPUT')

    def test_29_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        R2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                   'ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX',
                   'ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX']
        for i in range(0, nbLink):
            link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = res['network'][0]['ietf-network-topology:link'][i][
                    'org-openroadm-network-topology:OMS-attributes']['span']["engineered-spanloss"]
                length = res['network'][0]['ietf-network-topology:link'][i][
                    'org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
                if (spanLoss is not None) & (length is not None):
                    find = True
                self.assertTrue(find)
                R2RLink.remove(link_id)
        self.assertEqual(len(R2RLink), 0)

    def test_30_disconnect_ROADMB(self):
        # Delete in the topology-netconf
        response = test_utils.unmount_device("ROADM-B1")
        # Delete in the clli-network
        response = test_utils.del_node_request("NodeB")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_31_disconnect_ROADMC(self):
        # Delete in the topology-netconf
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        # Delete in the clli-network
        response = test_utils.del_node_request("NodeC")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_32_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 5)
        listNode = ['XPDR-A1-XPDR1', 'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for i in range(0, nbNode):
            nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
            nodeId = res['network'][0]['node'][i]['node-id']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDR-A1-XPDR1':
                nbTp = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                for j in range(0, nbTp):
                    tpid = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if tpid == 'XPDR1-CLIENT1':
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                             ['org-openroadm-common-network:tp-type']), 'XPONDER-CLIENT')
                    if tpid == 'XPDR1-NETWORK1':
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                          ['org-openroadm-common-network:tp-type']), 'XPONDER-NETWORK')
                        self.assertEqual((res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]
                                          ['org-openroadm-network-topology:xpdr-network-attributes']
                                          ['tail-equipment-id']),
                                         'ROADM-A1-SRG1--SRG1-PP1-TXRX')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG1':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG3':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG1':
                # Test related to DEG1
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG2':
                # Test related to DEG2
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                              res['network'][0]['node'][i]['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)
        # Test related to SRG1 of ROADMC
        for i in range(0, nbNode):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'ROADM-C1-SRG1')
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'ROADM-C1-DEG1')
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'ROADM-C1-DEG2')

    def test_33_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 2)
        for i in range(0, nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'ROADM-C1')
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'ROADM-B1')

    def test_34_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 1)
        for i in range(0, nbNode-1):
            self.assertNotEqual(res['network'][0]['node'][i]['org-openroadm-clli-network:clli'], 'NodeC')

    def test_35_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_36_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 1)
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_37_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 1)
        for i in range(0, nbNode):
            self.assertNotEqual(res['network'][0]['node'][i]['node-id'], 'XPDR-A1')

    def test_38_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ordm_topo_request("")
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 4)
        listNode = ['ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for i in range(0, nbNode):
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'},
                          res['network'][0]['node'][i]['supporting-node'])
            nodeType = res['network'][0]['node'][i]['org-openroadm-common-network:node-type']
            nodeId = res['network'][0]['node'][i]['node-id']
            if nodeId == 'ROADM-A1-SRG1':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-SRG3':
                # Test related to SRG1
                self.assertEqual(nodeType, 'SRG')
                self.assertEqual(len(res['network'][0]['node'][i]['ietf-network-topology:termination-point']), 5)
                self.assertIn({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG1':
                # Test related to DEG1
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            elif nodeId == 'ROADM-A1-DEG2':
                # Test related to DEG2
                self.assertEqual(nodeType, 'DEGREE')
                self.assertIn({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                self.assertIn({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP'},
                              res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    def test_39_disconnect_ROADM_XPDRA_link(self):
        # Link-1
        response = test_utils.del_link_request("XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        # Link-2
        response = test_utils.del_link_request("ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_40_getLinks_OpenRoadmTopology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbLink, 16)
        expressLink = ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX']
        addLink = ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX']
        dropLink = ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX']
        roadmtoroadmLink = 0
        for i in range(0, nbLink):
            if (res['network'][0]['ietf-network-topology:link'][i]
                   ['org-openroadm-common-network:link-type'] == 'EXPRESS-LINK'):
                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
                find = link_id in expressLink
                self.assertEqual(find, True)
                expressLink.remove(link_id)
            elif (res['network'][0]['ietf-network-topology:link'][i]
                     ['org-openroadm-common-network:link-type'] == 'ADD-LINK'):
                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
                find = link_id in addLink
                self.assertEqual(find, True)
                addLink.remove(link_id)
            elif (res['network'][0]['ietf-network-topology:link'][i]
                     ['org-openroadm-common-network:link-type'] == 'DROP-LINK'):
                link_id = res['network'][0]['ietf-network-topology:link'][i]['link-id']
                find = link_id in dropLink
                self.assertEqual(find, True)
                dropLink.remove(link_id)
            else:
                roadmtoroadmLink += 1
        self.assertEqual(len(expressLink), 0)
        self.assertEqual(len(addLink), 0)
        self.assertEqual(len(dropLink), 0)
        self.assertEqual(roadmtoroadmLink, 6)
        for i in range(0, nbLink):
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]
                                ['org-openroadm-common-network:link-type'], 'XPONDER-OUTPUT')
            self.assertNotEqual(res['network'][0]['ietf-network-topology:link'][i]
                                ['org-openroadm-common-network:link-type'], 'XPONDER-INPUT')

    def test_41_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        # Delete in the clli-network
        response = test_utils.del_node_request("NodeA")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_42_getClliNetwork(self):
        response = test_utils.get_clli_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_43_getOpenRoadmNetwork(self):
        response = test_utils.get_ordm_net_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_44_check_roadm2roadm_link_persistence(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertNotIn('node', res['network'][0])
        self.assertEqual(nbLink, 6)


if __name__ == "__main__":
    unittest.main(verbosity=2)
