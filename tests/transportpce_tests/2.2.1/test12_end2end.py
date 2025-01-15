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

import base64
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEFulltesting(unittest.TestCase):

    processes = None
    cr_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-create",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"
        },
        "service-name": "service1",
        "common-id": "ASATT1234567",
        "connection-type": "service",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "XPDR-A1",
            "service-format": "Ethernet",
            "clli": "NodeA",
            "tx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "1/0/2-PLUG-CLIENT",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.3",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "1/0/2-PLUG-CLIENT",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.4",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "XPDR-C1",
            "service-format": "Ethernet",
            "clli": "NodeC",
                    "tx-direction": [{
                        "port": {
                            "port-device-name": "XPDR-C1-XPDR2",
                            "port-type": "router",
                            "port-name": "1/0/2-PLUG-CLIENT",
                            "port-rack": "000000.00",
                            "port-shelf": "00"
                        },
                        "lgx": {
                            "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                            "lgx-port-name": "LGX Back.29",
                            "lgx-port-rack": "000000.00",
                            "lgx-port-shelf": "00"
                        },
                        "index": 0
                    }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-C1-XPDR2",
                    "port-type": "router",
                    "port-name": "1/0/2-PLUG-CLIENT",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                    "lgx-port-name": "LGX Back.29",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "due-date": "2016-11-28T00:00:01Z",
        "operator-contact": "pw1234"
    }

    del_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-delete",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"},
        "service-delete-req-info": {
            "service-name": "TBD",
            "tail-retention": "no"}
    }

    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xpdrA_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_07_connect_xpdrC_xpdr1_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_xpdrC_xpdr1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_09_connect_xpdrA_N2_to_roadmA_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '2',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_10_connect_roadmA_PP2_to_xpdrA_N2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '2',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_11_connect_xpdrC_xpdr2_N1_to_roadmC_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_12_connect_roadmC_PP2_to_xpdrC_xpdr2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_13_add_omsAttributes_ROADMA_ROADMC(self):
        # Config ROADMA-ROADMC oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.4,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_14_add_omsAttributes_ROADMC_ROADMA(self):
        # Config ROADMC-ROADMA oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.4,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

# test service-create for Eth service from xpdr to xpdr
    def test_15_create_eth_service2(self):
        self.cr_serv_input_data["service-name"] = "service2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_16_get_eth_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service2')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_17_check_xc1_ROADMA(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP1-TXRX-DEG2-TTP-TXRX-1:8")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-1:8',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP1-TXRX-nmc-1:8'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG2-TTP-TXRX-nmc-1:8'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_18_check_xc1_ROADMC(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-C1", "roadm-connections", "SRG1-PP2-TXRX-DEG1-TTP-TXRX-1:8")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP2-TXRX-DEG1-TTP-TXRX-1:8',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP2-TXRX-nmc-1:8'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG1-TTP-TXRX-nmc-1:8'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_19_check_topo_XPDRA(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual(
                    191.35,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
                self.assertEqual(
                    40.0,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
            elif ele['tp-id'] in ('XPDR1-CLIENT1', 'XPDR1-CLIENT2'):
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
            elif ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertIn('org-openroadm-network-topology:xpdr-network-attributes', dict.keys(ele))
        time.sleep(1)

    def test_20_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Index 1 should not be available")
            elif ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(1)

    def test_21_check_topo_ROADMA_DEG2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Index 1 should not be available")
            elif ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Index 1 should not be available")
        time.sleep(1)

    def test_22_create_eth_service1(self):
        self.cr_serv_input_data["service-name"] = "service1"
        del self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"]
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_23_get_eth_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service1')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_24_check_xc1_ROADMA(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "DEG2-TTP-TXRX-SRG1-PP2-TXRX-9:16")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'DEG2-TTP-TXRX-SRG1-PP2-TXRX-9:16',
                'opticalControlMode': 'power'
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'DEG2-TTP-TXRX-nmc-9:16'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'SRG1-PP2-TXRX-nmc-9:16'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_25_check_topo_XPDRA(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertEqual(
                    191.4,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
                self.assertEqual(
                    40.0,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
            elif ele['tp-id'] in ('XPDR1-CLIENT1'):
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
        time.sleep(1)

    def test_26_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[1], 255, "Lambda 2 should be available")
            elif ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
            elif ele['tp-id'] == 'SRG1-PP3-TXRX':
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(1)

    def test_27_check_topo_ROADMA_DEG2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
            elif ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
        time.sleep(1)

#     creation service test on a non-available resource
    def test_28_create_eth_service3(self):
        self.cr_serv_input_data["service-name"] = "service3"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        self.assertIn('200', response['output']['configuration-response-common']['response-code'])
        time.sleep(self.WAITING)

# add a test that check the openroadm-service-list still only contains 2 elements
    def test_29_delete_eth_service3(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service3"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Service \'service3\' does not exist in datastore',
                      response['output']['configuration-response-common']['response-message'])
        self.assertIn('500', response['output']['configuration-response-common']['response-code'])
        time.sleep(3)

    def test_30_delete_eth_service1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_31_delete_eth_service2(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_32_check_no_xc_ROADMA(self):
        response = test_utils.check_node_request("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('roadm-connections',
                         dict.keys(response['org-openroadm-device']))
        time.sleep(2)

    def test_33_check_topo_XPDRA(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['org-openroadm-common-network:tp-type'] == 'XPONDER-CLIENT':
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
            elif ele['org-openroadm-common-network:tp-type'] == 'XPONDER-NETWORK':
                self.assertIn('tail-equipment-id',
                              dict.keys(ele['org-openroadm-network-topology:xpdr-network-attributes']))
                self.assertNotIn('wavelength',
                                 dict.keys(ele['org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(1)

    def test_34_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 255, "Index 1 should  be available")
        self.assertEqual(freq_map_array[1], 255, "Index 2 should  be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] in ('SRG1-PP1-TXRX', 'SRG1-PP2-TXRX'):
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Index 1 should  be available")
                self.assertEqual(freq_map_array[1], 255, "Index 2 should  be available")
            elif ele['tp-id'] == 'SRG1-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Index 1 should  be available")
                self.assertEqual(freq_map_array[1], 255, "Index 2 should  be available")
            else:
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(1)

    def test_35_check_topo_ROADMA_DEG2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 255, "Lambda 1 should be available")
        self.assertEqual(freq_map_array[1], 255, "Lambda 2 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[1], 255, "Lambda 2 should be available")
            elif ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[1], 255, "Lambda 2 should be available")
        time.sleep(1)

# test service-create for Optical Channel (OC) service from srg-pp to srg-pp
    def test_36_create_oc_service1(self):
        self.cr_serv_input_data["service-name"] = "service1"
        self.cr_serv_input_data["connection-type"] = "roadm-line"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "ROADM-A1"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "OC"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "ROADM-C1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "OC"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_37_get_oc_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service1')
        self.assertEqual(response['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_38_check_xc1_ROADMA(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP1-TXRX-DEG2-TTP-TXRX-1:8")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-1:8',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP1-TXRX-nmc-1:8'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG2-TTP-TXRX-nmc-1:8'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_39_check_xc1_ROADMC(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-C1", "roadm-connections", "SRG1-PP1-TXRX-DEG1-TTP-TXRX-1:8")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP1-TXRX-DEG1-TTP-TXRX-1:8',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP1-TXRX-nmc-1:8'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG1-TTP-TXRX-nmc-1:8'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_40_create_oc_service2(self):
        self.cr_serv_input_data["service-name"] = "service2"
        self.cr_serv_input_data["connection-type"] = "roadm-line"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "ROADM-A1"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "OC"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "ROADM-C1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "OC"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_41_get_oc_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service2')
        self.assertEqual(response['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_42_check_xc2_ROADMA(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP2-TXRX-DEG2-TTP-TXRX-9:16")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP2-TXRX-DEG2-TTP-TXRX-9:16',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP2-TXRX-nmc-9:16'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG2-TTP-TXRX-nmc-9:16'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_43_check_topo_ROADMA(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[1], 255, "Lambda 2 should be available")
            elif ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[0], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[1], 0, "Lambda 2 should not be available")
            elif ele['tp-id'] == 'SRG1-PP3-TXRX':
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(1)
        self.test_27_check_topo_ROADMA_DEG2()
        time.sleep(1)

    def test_44_delete_oc_service1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_45_delete_oc_service2(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_46_get_no_oc_services(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['service-list'], (
            {
                "error-type": "protocol",
                "error-tag": "data-missing",
                "error-message":
                    "Request could not be completed because the relevant data "
                    "model content does not exist"
            }, {
                "error-type": "application",
                "error-tag": "data-missing",
                "error-message":
                    "Request could not be completed because the relevant data "
                    "model content does not exist"
            }))
        time.sleep(1)

    def test_47_get_no_xc_ROADMA(self):
        response = test_utils.check_node_request("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('roadm-connections', dict.keys(response['org-openroadm-device']))
        time.sleep(1)

    def test_48_check_topo_ROADMA(self):
        self.test_34_check_topo_ROADMA_SRG1()
        self.test_35_check_topo_ROADMA_DEG2()

    def test_49_loop_create_oc_service(self):
        for i in range(1, 3):
            # pylint: disable=consider-using-f-string
            print("iteration number {}".format(i))
            print("oc service creation")
            self.test_36_create_oc_service1()
            print("check xc in ROADM-A1")
            self.test_38_check_xc1_ROADMA()
            print("check xc in ROADM-C1")
            self.test_39_check_xc1_ROADMC()
            print("oc service deletion\n")
            self.test_44_delete_oc_service1()

    def test_50_loop_create_eth_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        if response['status_code'] != 404:
            self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
            response = test_utils.transportpce_api_rpc_request(
                'org-openroadm-service', 'service-delete',
                self.del_serv_input_data)
            time.sleep(5)
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "XPDR-A1"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "XPDR-C1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        for i in range(1, 3):
            # pylint: disable=consider-using-f-string
            print("iteration number {}".format(i))
            print("eth service creation")
            self.test_15_create_eth_service2()
            print("check xc in ROADM-A1")
            self.test_17_check_xc1_ROADMA()
            print("eth service deletion\n")
            self.test_31_delete_eth_service2()

    def test_51_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_52_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_53_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_54_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
