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
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_rfc8040  # nopep8


class TransportPCEtesting(unittest.TestCase):

    processes = None
    CHECK_DICT1 = {
        'ROADM-A1-SRG1': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADM-A1-SRG3': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG3-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG3-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADM-A1-DEG1': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADM-A1-DEG2': {
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
        'ROADM-C1-SRG1': {
            'node_type': 'SRG',
            'checks_tp': [({'tp-id': 'SRG1-CP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-CP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'SRG1-PP1-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'SRG-TXRX-PP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADM-C1-DEG1': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG1-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG1-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        },
        'ROADM-C1-DEG2': {
            'node_type': 'DEGREE',
            'checks_tp': [({'tp-id': 'DEG2-TTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-TTP',
                            'org-openroadm-common-network:operational-state': 'inService'}),
                          ({'tp-id': 'DEG2-CTP-TXRX', 'org-openroadm-common-network:administrative-state': 'inService',
                            'org-openroadm-common-network:tp-type': 'DEGREE-TXRX-CTP',
                            'org-openroadm-common-network:operational-state': 'inService'})]
        }
    }
    NODE_VERSION = '2.2.1'

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
        time.sleep(5)

    def test_01_connect_ROADM_A1(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_getClliNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        logging.info(response)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_03_getOpenRoadmNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'ROADM-A1')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(response['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-common-network:node-type'], 'ROADM')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-network:model'], 'model2')

    def test_04_getLinks_OpenroadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 10)
        check_list = {'EXPRESS-LINK': ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX']
                      }
        for val in response['network'][0]['ietf-network-topology:link']:
            linkId = val['link-id']
            linkType = val['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for val in check_list.values():
            self.assertEqual(len(val), 0)

    def test_05_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 4)
        listNode = ['ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for val in response['network'][0]['node']:
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
            nodeType = val['org-openroadm-common-network:node-type']
            nodeId = val['node-id']
            self.assertIn(nodeId, self.CHECK_DICT1)
            self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
            if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
            for item in self.CHECK_DICT1[nodeId]['checks_tp']:
                self.assertIn(item, val['ietf-network-topology:termination-point'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_06_connect_XPDRA(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_07_getClliNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['network'][0]['node'][0]['node-id'], 'NodeA')
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_08_getOpenRoadmNetwork(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        nbNode = len(response['network'][0]['node'])
        self.assertEqual(nbNode, 2)
        for val in response['network'][0]['node']:
            self.assertEqual(val['supporting-node'][0]['network-ref'], 'clli-network')
            self.assertEqual(val['supporting-node'][0]['node-ref'], 'NodeA')
            nodeId = val['node-id']
            if nodeId == 'XPDR-A1':
                self.assertEqual(val['org-openroadm-common-network:node-type'], 'XPONDER')
            elif nodeId == 'ROADM-A1':
                self.assertEqual(val['org-openroadm-common-network:node-type'], 'ROADM')
            else:
                self.assertFalse(True)
                continue
            self.assertEqual(val['org-openroadm-network:model'], 'model2')

    def test_09_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 5)
        listNode = ['XPDR-A1-XPDR1', 'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for val in response['network'][0]['node']:
            nodeType = val['org-openroadm-common-network:node-type']
            nodeId = val['node-id']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDR-A1-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'}, val['supporting-node'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                client = 0
                network = 0
                for val2 in val['ietf-network-topology:termination-point']:
                    tpType = (val2['org-openroadm-common-network:tp-type'])
                    tpId = val2['tp-id']
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                    if tpId == 'XPDR1-NETWORK2':
                        self.assertEqual(val2['transportpce-topology:associated-connection-map-port'], 'XPDR1-CLIENT2')
                    if tpId == 'XPDR1-CLIENT2':
                        self.assertEqual(val2['transportpce-topology:associated-connection-map-port'], 'XPDR1-NETWORK2')
                self.assertEqual(client, 2)
                self.assertEqual(network, 2)
                listNode.remove(nodeId)
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
                for item in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(item, val['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
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
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 12)
        check_list = {'EXPRESS-LINK': ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX'],
                      'XPONDER-INPUT': ['ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1'],
                      'XPONDER-OUTPUT': ['XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX']
                      }
        for val in response['network'][0]['ietf-network-topology:link']:
            linkId = val['link-id']
            linkType = val['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for val in check_list.values():
            self.assertEqual(len(val), 0)

    def test_13_connect_ROADMC(self):
        response = test_utils_rfc8040.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

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
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listNode = ['NodeA', 'NodeC']
        for val in response['network'][0]['node']:
            nodeId = val['node-id']
            self.assertIn(nodeId, listNode)
            self.assertEqual(val['org-openroadm-clli-network:clli'], nodeId)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_17_getOpenRoadmNetwork(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        nbNode = len(response['network'][0]['node'])
        self.assertEqual(nbNode, 3)
        listNode = ['XPDR-A1', 'ROADM-A1', 'ROADM-C1']
        CHECK_LIST = {'XPDR-A1': {'node-ref': 'NodeA', 'node-type': 'XPONDER'},
                      'ROADM-A1': {'node-ref': 'NodeA', 'node-type': 'ROADM'},
                      'ROADM-C1': {'node-ref': 'NodeC', 'node-type': 'ROADM'}
                      }
        for val in response['network'][0]['node']:
            self.assertEqual(val['supporting-node'][0]['network-ref'], 'clli-network')
            nodeId = val['node-id']
            if nodeId in CHECK_LIST:
                self.assertEqual(val['supporting-node'][0]['node-ref'], CHECK_LIST[nodeId]['node-ref'])
                self.assertEqual(val['org-openroadm-common-network:node-type'], CHECK_LIST[nodeId]['node-type'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
                continue
            self.assertEqual(val['org-openroadm-network:model'], 'model2')
        self.assertEqual(len(listNode), 0)

    def test_18_getROADMLinkOpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 20)
        check_list = {'EXPRESS-LINK': ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                       'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX',
                                       'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                   'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                                    'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX'],
                      'ROADM-TO-ROADM': ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                                         'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX'],
                      'XPONDER-INPUT': ['ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1'],
                      'XPONDER-OUTPUT': ['XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX']
                      }
        for val in response['network'][0]['ietf-network-topology:link']:
            linkId = val['link-id']
            linkType = val['org-openroadm-common-network:link-type']
            self.assertIn(linkType, check_list)
            find = linkId in check_list[linkType]
            self.assertEqual(find, True)
            (check_list[linkType]).remove(linkId)
        for val in check_list.values():
            self.assertEqual(len(val), 0)

    def test_19_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 20)
        R2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX']
        for val in response['network'][0]['ietf-network-topology:link']:
            link_id = val['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = val['org-openroadm-network-topology:OMS-attributes']['span']['engineered-spanloss']
                # pylint: disable=line-too-long
                length = val['org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
                if (spanLoss is not None) & (length is not None):
                    find = True
                self.assertTrue(find)
                R2RLink.remove(link_id)
        self.assertEqual(len(R2RLink), 0)

    def test_20_getNodes_OpenRoadmTopology(self):
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 8)
        listNode = ['XPDR-A1-XPDR1',
                    'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2',
                    'ROADM-C1-SRG1', 'ROADM-C1-DEG1', 'ROADM-C1-DEG2']
        # Tests related to XPDRA nodes
        for val in response['network'][0]['node']:
            nodeType = val['org-openroadm-common-network:node-type']
            nodeId = val['node-id']
            if nodeId == 'XPDR-A1-XPDR1':
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'}, val['supporting-node'])
                self.assertEqual(nodeType, 'XPONDER')
                nbTps = len(val['ietf-network-topology:termination-point'])
                self.assertTrue(nbTps >= 4)
                client = 0
                network = 0
                for val2 in val['ietf-network-topology:termination-point']:
                    tpType = (val2['org-openroadm-common-network:tp-type'])
                    if tpType == 'XPONDER-CLIENT':
                        client += 1
                    elif tpType == 'XPONDER-NETWORK':
                        network += 1
                self.assertTrue(client == 2)
                self.assertTrue(network == 2)
                listNode.remove(nodeId)
        # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
                for item in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(item, val['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
                self.assertEqual(val['org-openroadm-common-network:node-type'], self.CHECK_DICT1[nodeId]['node_type'])
                listNode.remove(nodeId)
        # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT2:
                self.assertEqual(nodeType, self.CHECK_DICT2[nodeId]['node_type'])
                if self.CHECK_DICT2[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
                for item in self.CHECK_DICT2[nodeId]['checks_tp']:
                    self.assertIn(item, val['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeC'}, val['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-C1'}, val['supporting-node'])
                self.assertEqual(val['org-openroadm-common-network:node-type'], self.CHECK_DICT2[nodeId]['node_type'])
                listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_21_connect_ROADMB(self):
        response = test_utils.mount_device("ROADM-B1", ('roadmb', self.NODE_VERSION))
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
        # pylint: disable=redundant-unittest-assert
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listNode = ['NodeA', 'NodeB', 'NodeC']
        for val in response['network'][0]['node']:
            nodeId = val['node-id']
            self.assertIn(nodeId, listNode)
            self.assertEqual(val['org-openroadm-clli-network:clli'], nodeId)
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_27_verifyDegree(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        listR2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                       'ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX',
                       'ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX',
                       'ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX']
        for val in response['network'][0]['ietf-network-topology:link']:
            if val['org-openroadm-common-network:link-type'] == 'ROADM-TO-ROADM':
                link_id = val['link-id']
                find = link_id in listR2RLink
                self.assertEqual(find, True)
                listR2RLink.remove(link_id)
        self.assertEqual(len(listR2RLink), 0)

    def test_28_verifyOppositeLinkTopology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 30)
        for val in response['network'][0]['ietf-network-topology:link']:
            link_id = val['link-id']
            link_type = val['org-openroadm-common-network:link-type']
            link_src = val['source']['source-node']
            link_dest = val['destination']['dest-node']
            oppLink_id = val['org-openroadm-common-network:opposite-link']
            # Find the opposite link
            response_oppLink = test_utils.get_ordm_topo_request("ietf-network-topology:link/"+oppLink_id)
            self.assertEqual(response_oppLink.status_code, requests.codes.ok)
            res_oppLink = response_oppLink.json()
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]
                             ['org-openroadm-common-network:opposite-link'], link_id)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['source']['source-node'], link_dest)
            self.assertEqual(res_oppLink['ietf-network-topology:link'][0]['destination']['dest-node'], link_src)
            oppLink_type = res_oppLink['ietf-network-topology:link'][0]['org-openroadm-common-network:link-type']
            CHECK_DICT = {'ADD-LINK': 'DROP-LINK', 'DROP-LINK': 'ADD-LINK',
                          'EXPRESS-LINK': 'EXPRESS-LINK', 'ROADM-TO-ROADM': 'ROADM-TO-ROADM',
                          'XPONDER-INPUT': 'XPONDER-OUTPUT', 'XPONDER-OUTUT': 'XPONDER-INPUT'}
            if link_type in CHECK_DICT:
                self.assertEqual(oppLink_type, CHECK_DICT[link_type])

    def test_29_getLinkOmsAttributesOpenRoadmTopology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        R2RLink = ['ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                   'ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX',
                   'ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX',
                   'ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX']
        for val in response['network'][0]['ietf-network-topology:link']:
            link_id = val['link-id']
            if link_id in R2RLink:
                find = False
                spanLoss = val['org-openroadm-network-topology:OMS-attributes']['span']["engineered-spanloss"]
                # pylint: disable=line-too-long
                length = val['org-openroadm-network-topology:OMS-attributes']['span']['link-concatenation'][0]['SRLG-length']
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
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 5)
        listNode = ['XPDR-A1-XPDR1', 'ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for val in response['network'][0]['node']:
            nodeType = val['org-openroadm-common-network:node-type']
            nodeId = val['node-id']
            # Tests related to XPDRA nodes
            if nodeId == 'XPDR-A1-XPDR1':
                for val2 in val['ietf-network-topology:termination-point']:
                    tpid = val2['tp-id']
                    if tpid == 'XPDR1-CLIENT1':
                        self.assertEqual((val2['org-openroadm-common-network:tp-type']), 'XPONDER-CLIENT')
                    elif tpid == 'XPDR1-NETWORK1':
                        self.assertEqual((val2['org-openroadm-common-network:tp-type']), 'XPONDER-NETWORK')
                        # pylint: disable=line-too-long
                        self.assertEqual((val2['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id']),
                                         'ROADM-A1-SRG1--SRG1-PP1-TXRX')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'XPDR-A1'}, val['supporting-node'])
                listNode.remove(nodeId)
            # Tests related to ROADMA nodes
            elif nodeId in self.CHECK_DICT1:
                self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
                if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                    self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
                for item in self.CHECK_DICT1[nodeId]['checks_tp']:
                    self.assertIn(item, val['ietf-network-topology:termination-point'])
                self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
                listNode.remove(nodeId)
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)
        # Test related to SRG1 of ROADMC
        for val in response['network'][0]['node']:
            self.assertNotEqual(val['node-id'], 'ROADM-C1-SRG1')
            self.assertNotEqual(val['node-id'], 'ROADM-C1-DEG1')
            self.assertNotEqual(val['node-id'], 'ROADM-C1-DEG2')

    def test_33_getOpenRoadmNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        nbNode = len(response['network'][0]['node'])
        self.assertEqual(nbNode, 2)
        for val in response['network'][0]['node']:
            self.assertNotEqual(val['node-id'], 'ROADM-C1')
            self.assertNotEqual(val['node-id'], 'ROADM-B1')

    def test_34_getClliNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 1)
        self.assertNotEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeC')

    def test_35_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_36_getClliNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 1)
        self.assertEqual(response['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeA')

    def test_37_getOpenRoadmNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 1)
        self.assertNotEqual(response['network'][0]['node'][0]['node-id'], 'XPDR-A1')

    def test_38_getNodes_OpenRoadmTopology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 4)
        listNode = ['ROADM-A1-SRG1', 'ROADM-A1-SRG3', 'ROADM-A1-DEG1', 'ROADM-A1-DEG2']
        for val in response['network'][0]['node']:
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
            nodeType = val['org-openroadm-common-network:node-type']
            nodeId = val['node-id']
            self.assertIn(nodeId, self.CHECK_DICT1)
            self.assertEqual(nodeType, self.CHECK_DICT1[nodeId]['node_type'])
            if self.CHECK_DICT1[nodeId]['node_type'] == 'SRG':
                self.assertEqual(len(val['ietf-network-topology:termination-point']), 5)
            for item in self.CHECK_DICT1[nodeId]['checks_tp']:
                self.assertIn(item, val['ietf-network-topology:termination-point'])
            self.assertIn({'network-ref': 'clli-network', 'node-ref': 'NodeA'}, val['supporting-node'])
            self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'ROADM-A1'}, val['supporting-node'])
            listNode.remove(nodeId)
        self.assertEqual(len(listNode), 0)

    def test_39_disconnect_ROADM_XPDRA_link(self):
        # Link-1
        response = test_utils.del_link_request("XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        # Link-2
        response = test_utils.del_link_request("ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_40_getLinks_OpenRoadmTopology(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 16)
        check_list = {'EXPRESS-LINK': ['ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                       'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX'],
                      'ADD-LINK': ['ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                                   'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX'],
                      'DROP-LINK': ['ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                                    'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                                    'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX']
                      }
        roadmtoroadmLink = 0
        for val in response['network'][0]['ietf-network-topology:link']:
            linkId = val['link-id']
            linkType = val['org-openroadm-common-network:link-type']
            if linkType in check_list:
                find = linkId in check_list[linkType]
                self.assertEqual(find, True)
                (check_list[linkType]).remove(linkId)
            else:
                roadmtoroadmLink += 1
        for val in check_list.values():
            self.assertEqual(len(val), 0)
        self.assertEqual(roadmtoroadmLink, 6)
        for val in response['network'][0]['ietf-network-topology:link']:
            self.assertNotEqual(val['org-openroadm-common-network:link-type'], 'XPONDER-OUTPUT')
            self.assertNotEqual(val['org-openroadm-common-network:link-type'], 'XPONDER-INPUT')

    def test_41_disconnect_ROADMA(self):
        response = test_utils_rfc8040.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        # Delete in the clli-network
        response = test_utils.del_node_request("NodeA")
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_42_getClliNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('clli-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('node', response['network'][0])

    def test_43_getOpenRoadmNetwork(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-network', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('node', response['network'][0])

    def test_44_check_roadm2roadm_link_persistence(self):
        response = test_utils_rfc8040.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('node', response['network'][0])
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 6)


if __name__ == "__main__":
    unittest.main(verbosity=2)
