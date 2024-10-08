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

import time
import unittest
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCETopologyTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '1.2.1'
    CHECK_DICT1 = {
        'ROADMA01-SRG1': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADMA01-SRG3': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADMA01-DEG1': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADMA01-DEG2': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        }
    }
    CHECK_DICT2 = {
        'ROADMC01-SRG1': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADMC01-DEG1': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADMC01-DEG2': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        }
    }

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION),
                                               ('roadmb', cls.NODE_VERSION), ('roadmc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    def test_01_connect_ROADMA(self):
        response = test_utils.mount_device("ROADMA01", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_03_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'ROADMA01')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'ROADM')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:model'], '2')

    def test_04_getLinks_OpenroadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Tests related to links
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 10)
        check_list = {'EXPRESS-LINK': ['ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                       'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX']
                      }
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            linkType = link['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for link in check_list.values():
            self.assertEqual(len(link), 0)

    def test_05_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 5)
        listNode = ['ROADMA01-SRG1', 'ROADMA01-SRG3', 'ROADMA01-DEG1', 'ROADMA01-DEG2']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            nodeType = node['org-openroadm-common-network:node-type']
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
            self.assertIn(nodeId, self.CHECK_DICT1)
            self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
            if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
            for tp in self.CHECK_DICT1[nodeId]['checks_tp']:
                self.assertIn(tp, node['ietf-network-topology:termination-point'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
            listNode.remove(nodeId)

        self.assertEqual(len(listNode), 0)

    def test_06_connect_XPDRA(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_07_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_08_getOpenRoadmNetwork(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 3)
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertEqual(node['supporting-node'][0]['network-ref'], 'clli-network')
            self.assertEqual(node['supporting-node'][0]['node-ref'], 'NodeA')
            if nodeId == 'XPDRA01':
                self.assertEqual(node['org-openroadm-common-network:node-type'], 'XPONDER')
                self.assertEqual(node['org-openroadm-network:model'], '1')
            elif nodeId == 'ROADMA01':
                self.assertEqual(node['org-openroadm-common-network:node-type'], 'ROADM')
                self.assertEqual(node['org-openroadm-network:model'], '2')
            else:
                self.assertFalse(True)

    def test_09_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 6)
        listNode = ['XPDRA01-XPDR1', 'ROADMA01-SRG1', 'ROADMA01-SRG3', 'ROADMA01-DEG1', 'ROADMA01-DEG2']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            nodeType = node['org-openroadm-common-network:node-type']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDRA01-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA01'}, node['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                client = 0
                network = 0
                for tp in node['ietf-network-topology:termination-point']:
                    tpType = tp['org-openroadm-common-network:tp-type']
                    tpId = tp['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                    if tpId == 'XPDR1-NETWORK2':
                        self.assertEqual(
                            tp['org-openroadm-common-network:associated-connection-map-tp'], ['XPDR1-CLIENT3'])
                    elif tpId == 'XPDR1-CLIENT3':
                        self.assertEqual(
                            tp['org-openroadm-common-network:associated-connection-map-tp'], ['XPDR1-NETWORK2'])
                self.assertTrue(client == 4)
                self.assertTrue(network == 2)
                listNode.remove(nodeId)
            # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
                for tp in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(tp, node['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    # Connect the tail XPDRA to ROADMA and vice versa
    def test_10_connect_tail_xpdr_rdm(self):
        # Connect the tail: XPDRA to ROADMA
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDRA01', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADMA01', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_11_connect_tail_rdm_xpdr(self):
        # Connect the tail: ROADMA to XPDRA
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDRA01', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADMA01', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_12_getLinks_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 12)
        check_list = {'EXPRESS-LINK': ['ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                       'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX'],
                      'XPONDER-INPUT': ['ROADMA01-SRG1-SRG1-PP1-TXRXtoXPDRA01-XPDR1-XPDR1-NETWORK1'],
                      'XPONDER-OUTPUT': ['XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX']
                      }
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            linkType = link['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for link in check_list.values():
            self.assertEqual(len(link), 0)

    def test_13_connect_ROADMC(self):
        response = test_utils.mount_device("ROADMC01", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_14_omsAttributes_ROADMA_ROADMC(self):
        # Config ROADMA01-ROADMC01 oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_15_omsAttributes_ROADMC_ROADMA(self):
        # Config ROADMC01-ROADMA oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_16_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listNode = ['NodeA', 'NodeC', 'TAPI-SBI-ABS-NODE']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            self.assertIn(nodeId, listNode)
            self.assertEqual(node['org-openroadm-clli-network:clli'], nodeId)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_17_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 4)
        listNode = ['XPDRA01', 'ROADMA01', 'ROADMC01']
        CHECK_LIST = {'XPDRA01': {'node-ref': 'NodeA', 'node-type': 'XPONDER', 'model': '1'},
                      'ROADMA01': {'node-ref': 'NodeA', 'node-type': 'ROADM', 'model': '2'},
                      'ROADMC01': {'node-ref': 'NodeC', 'node-type': 'ROADM', 'model': '2'}
                      }
        for node in response['network'][0]['node']:
            self.assertEqual(node['supporting-node'][0]['network-ref'], 'clli-network')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertIn(nodeId, CHECK_LIST)
            self.assertEqual(node['supporting-node'][0]['node-ref'],
                             CHECK_LIST[nodeId]['node-ref'])
            self.assertEqual(node['org-openroadm-common-network:node-type'],
                             CHECK_LIST[nodeId]['node-type'])
            self.assertEqual(node['org-openroadm-network:model'],
                             CHECK_LIST[nodeId]['model'])
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_18_getROADMLinkOpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 20)
        check_list = {'EXPRESS-LINK': ['ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                       'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                       'ROADMC01-DEG2-DEG2-CTP-TXRXtoROADMC01-DEG1-DEG1-CTP-TXRX',
                                       'ROADMC01-DEG1-DEG1-CTP-TXRXtoROADMC01-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                   'ROADMC01-SRG1-SRG1-CP-TXRXtoROADMC01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMC01-SRG1-SRG1-CP-TXRXtoROADMC01-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX',
                                    'ROADMC01-DEG1-DEG1-CTP-TXRXtoROADMC01-SRG1-SRG1-CP-TXRX',
                                    'ROADMC01-DEG2-DEG2-CTP-TXRXtoROADMC01-SRG1-SRG1-CP-TXRX'],
                      'ROADM-TO-ROADM': ['ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX',
                                         'ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX'],
                      'XPONDER-INPUT': ['ROADMA01-SRG1-SRG1-PP1-TXRXtoXPDRA01-XPDR1-XPDR1-NETWORK1'],
                      'XPONDER-OUTPUT': ['XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX']
                      }
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            linkType = link['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for link in check_list.values():
            self.assertEqual(len(link), 0)

    def test_19_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 20)
        R2RLink = ['ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX',
                   'ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX']
        for link in response['network'][0]['ietf-network-topology:link']:
            link_id = link['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = link['org-openroadm-network-topology:OMS-attributes']['span']['engineered-spanloss']
                # pylint: disable=line-too-long
                length = link['org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
                if (spanLoss is not None) & (length is not None):
                    find = True
                self.assertTrue(find)
                R2RLink.remove(link_id)
        self.assertEqual(len(R2RLink), 0)

    def test_20_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 9)
        listNode = ['XPDRA01-XPDR1',
                    'ROADMA01-SRG1', 'ROADMA01-SRG3', 'ROADMA01-DEG1', 'ROADMA01-DEG2',
                    'ROADMC01-SRG1', 'ROADMC01-DEG1', 'ROADMC01-DEG2']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeType = node['org-openroadm-common-network:node-type']
            if nodeId == 'XPDRA01-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA01'}, node['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                self.assertEqual(len(node['ietf-network-topology:termination-point']), 6)
                client = 0
                network = 0
                for tp in node['ietf-network-topology:termination-point']:
                    tpType = tp['org-openroadm-common-network:tp-type']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                self.assertTrue(client == 4)
                self.assertTrue(network == 2)
                listNode.remove(nodeId)
            # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
                for tp in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(tp, node['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
                self.assertEqual(node['org-openroadm-common-network:node-type'], self.CHECK_DICT1[nodeId]['node_type'])
                listNode.remove(nodeId)
            # Tests related to ROADMC nodes
            elif nodeId in self.CHECK_DICT2:
                self.assertEqual(nodeType, self.CHECK_DICT2[nodeId]['node_type'])
                if self.CHECK_DICT2[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
                for tp in self.CHECK_DICT2[nodeId]['checks_tp']:
                    self.assertIn(tp, node['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeC'}, node['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMC01'}, node['supporting-node'])
                self.assertEqual(node['org-openroadm-common-network:node-type'], self.CHECK_DICT2[nodeId]['node_type'])
                listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_21_connect_ROADMB(self):
        response = test_utils.mount_device("ROADMB01", ('roadmb', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_22_omsAttributes_ROADMA_ROADMB(self):
        # Config ROADMA01-ROADMB01 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMA01-DEG2-DEG2-TTP-TXRXtoROADMB01-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_23_omsAttributes_ROADMB_ROADMA(self):
        # Config ROADMB01-ROADMA01 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMB01-DEG1-DEG1-TTP-TXRXtoROADMA01-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_24_omsAttributes_ROADMB_ROADMC(self):
        # Config ROADMB01-ROADMC01 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMB01-DEG2-DEG2-TTP-TXRXtoROADMC01-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_25_omsAttributes_ROADMC_ROADMB(self):
        # Config ROADMC01-ROADMB01 oms-attributes
        data = {"span": {
                "auto-spanloss": "true",
                "engineered-spanloss": 12.2,
                "link-concatenation": [{
                    "SRLG-Id": 0,
                    "fiber-type": "smf",
                    "SRLG-length": 100000,
                    "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request(
            "ROADMC01-DEG1-DEG1-TTP-TXRXtoROADMB01-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_26_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listNode = ['NodeA', 'NodeB', 'NodeC']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertIn(nodeId, listNode)
            self.assertEqual(node['org-openroadm-clli-network:clli'], nodeId)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_27_verifyDegree(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listR2RLink = ['ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX',
                       'ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX',
                       'ROADMA01-DEG2-DEG2-TTP-TXRXtoROADMB01-DEG1-DEG1-TTP-TXRX',
                       'ROADMC01-DEG1-DEG1-TTP-TXRXtoROADMB01-DEG2-DEG2-TTP-TXRX',
                       'ROADMB01-DEG1-DEG1-TTP-TXRXtoROADMA01-DEG2-DEG2-TTP-TXRX',
                       'ROADMB01-DEG2-DEG2-TTP-TXRXtoROADMC01-DEG1-DEG1-TTP-TXRX']
        for link in response['network'][0]['ietf-network-topology:link']:
            if link['org-openroadm-common-network:link-type'] == 'ROADM-TO-ROADM':
                link_id = link['link-id']
                find = link_id in listR2RLink
                self.assertEqual(find, True)
                listR2RLink.remove(link_id)
        self.assertEqual(len(listR2RLink), 0)

    def test_28_verifyOppositeLinkTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 34)
        for link in response['network'][0]['ietf-network-topology:link']:
            link_id = link['link-id']
            link_type = link['org-openroadm-common-network:link-type']
            link_src = link['source']['source-node']
            link_dest = link['destination']['dest-node']
            oppLink_id = link['org-openroadm-common-network:opposite-link']
            # Find the opposite link
            res_oppLink = test_utils.get_ietf_network_link_request('openroadm-topology', oppLink_id, 'config')
            self.assertEqual(res_oppLink['status_code'], requests.codes.ok)
            self.assertEqual(res_oppLink['link']['org-openroadm-common-network:opposite-link'], link_id)
            self.assertEqual(res_oppLink['link']['source']['source-node'], link_dest)
            self.assertEqual(res_oppLink['link']['destination']['dest-node'], link_src)
            oppLink_type = res_oppLink['link']['org-openroadm-common-network:link-type']
            CHECK_DICT = {'ADD-LINK': 'DROP-LINK', 'DROP-LINK': 'ADD-LINK',
                          'EXPRESS-LINK': 'EXPRESS-LINK', 'ROADM-TO-ROADM': 'ROADM-TO-ROADM',
                          'XPONDER-INPUT': 'XPONDER-OUTPUT', 'XPONDER-OUTUT': 'XPONDER-INPUT'}
            if link_type in CHECK_DICT:
                self.assertEqual(oppLink_type, CHECK_DICT[link_type])

    def test_29_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        R2RLink = ['ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX',
                   'ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX',
                   'ROADMA01-DEG2-DEG2-TTP-TXRXtoROADMB01-DEG1-DEG1-TTP-TXRX',
                   'ROADMC01-DEG1-DEG1-TTP-TXRXtoROADMB01-DEG2-DEG2-TTP-TXRX',
                   'ROADMB01-DEG1-DEG1-TTP-TXRXtoROADMA01-DEG2-DEG2-TTP-TXRX',
                   'ROADMB01-DEG2-DEG2-TTP-TXRXtoROADMC01-DEG1-DEG1-TTP-TXRX']
        for link in response['network'][0]['ietf-network-topology:link']:
            link_id = link['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = link['org-openroadm-network-topology:OMS-attributes']['span']['engineered-spanloss']
                # pylint: disable=line-too-long
                length = link['org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
                if (spanLoss is not None) & (length is not None):
                    find = True
                self.assertTrue(find)
                R2RLink.remove(link_id)
        self.assertEqual(len(R2RLink), 0)

    def test_30_disconnect_ROADMB(self):
        # Delete in the topology-netconf
        response = test_utils.unmount_device("ROADMB01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        # Delete in the clli-network
        response = test_utils.del_ietf_network_node_request('clli-network', 'NodeB', 'config')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_31_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADMC01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        # Delete in the clli-network
        response = test_utils.del_ietf_network_node_request('clli-network', 'NodeC', 'config')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_32_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 6)
        listNode = ['XPDRA01-XPDR1', 'ROADMA01-SRG1', 'ROADMA01-SRG3', 'ROADMA01-DEG1', 'ROADMA01-DEG2']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            nodeType = node['org-openroadm-common-network:node-type']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDRA01-XPDR1':
                for tp in node['ietf-network-topology:termination-point']:
                    tpid = tp['tp-id']
                    if tpid == 'XPDR1-CLIENT1':
                        self.assertEqual(tp['org-openroadm-common-network:tp-type'], 'XPONDER-CLIENT')
                    if tpid == 'XPDR1-NETWORK1':
                        self.assertEqual(tp['org-openroadm-common-network:tp-type'], 'XPONDER-NETWORK')
                        # pylint: disable=line-too-long
                        self.assertEqual(tp['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'],
                                         'ROADMA01-SRG1--SRG1-PP1-TXRX')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDRA01'}, node['supporting-node'])
                listNode.remove(nodeId)
            # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
                for tp in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(tp, node['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)
        # Test related to SRG1 of ROADMC
        for node in response['network'][0]['node']:
            self.assertNotEqual(node['node-id'], 'ROADMC01-SRG1')
            self.assertNotEqual(node['node-id'], 'ROADMC01-DEG1')
            self.assertNotEqual(node['node-id'], 'ROADMC01-DEG2')

    def test_33_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 3)
        for node in response['network'][0]['node']:
            self.assertNotEqual(node['node-id'], 'ROADMC01')
            self.assertNotEqual(node['node-id'], 'ROADMB01')

    def test_34_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 2)
        self.assertNotEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeC')

    def test_35_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_36_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 2)
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_37_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 2)
        self.assertNotEqual(response['network'][0]['node'][0]['node-id'], 'XPDRA01')

    def test_38_getNodes_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 5)
        listNode = ['ROADMA01-SRG1', 'ROADMA01-SRG3', 'ROADMA01-DEG1', 'ROADMA01-DEG2']
        for node in response['network'][0]['node']:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
            nodeType = node['org-openroadm-common-network:node-type']
            self.assertIn(nodeId, self.CHECK_DICT1)
            self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
            if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                self.assertEqual(len(node['ietf-network-topology:termination-point']), 17)
            for tp in self.CHECK_DICT1[nodeId]['checks_tp']:
                self.assertIn(tp, node['ietf-network-topology:termination-point'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, node['supporting-node'])
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADMA01'}, node['supporting-node'])
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_39_disconnect_ROADM_XPDRA_link(self):
        # Link-1
        response = test_utils.del_ietf_network_link_request(
            'openroadm-topology',
            'XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX',
            'config')
        # Link-2
        response2 = test_utils.del_ietf_network_link_request(
            'openroadm-topology',
            'ROADMA01-SRG1-SRG1-PP1-TXRXtoXPDRA01-XPDR1-XPDR1-NETWORK1',
            'config')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        self.assertIn(response2.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_40_getLinks_OpenRoadmTopology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 16)
        check_list = {'EXPRESS-LINK': ['ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                       'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG1-SRG1-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG2-DEG2-CTP-TXRX',
                                   'ROADMA01-SRG3-SRG3-CP-TXRXtoROADMA01-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG1-SRG1-CP-TXRX',
                                    'ROADMA01-DEG1-DEG1-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX',
                                    'ROADMA01-DEG2-DEG2-CTP-TXRXtoROADMA01-SRG3-SRG3-CP-TXRX']
                      }
        roadmtoroadmLink = 0
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            linkType = link['org-openroadm-common-network:link-type']
            if linkType in check_list:
                find = linkId in check_list[linkType]
                self.assertEqual(find, True)
                (check_list[linkType]).remove(linkId)
            else:
                roadmtoroadmLink += 1
        for link in check_list.values():
            self.assertEqual(len(link), 0)
        self.assertEqual(roadmtoroadmLink, 6)
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertNotEqual(link['org-openroadm-common-network:link-type'], 'XPONDER-OUTPUT')
            self.assertNotEqual(link['org-openroadm-common-network:link-type'], 'XPONDER-INPUT')

    def test_41_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADMA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        # Delete in the clli-network
        response = test_utils.del_ietf_network_node_request('clli-network', 'NodeA', 'config')
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_42_getClliNetwork(self):
        response = test_utils.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)

    def test_43_getOpenRoadmNetwork(self):
        response = test_utils.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)

    def test_44_check_roadm2roadm_link_persistence(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        # Only TAPI-SBI-ABS-NODE created at initialization shall remain in the topology
        self.assertEqual(len(response['network'][0]['node']), 1)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 6)


if __name__ == "__main__":
    unittest.main(verbosity=2)
