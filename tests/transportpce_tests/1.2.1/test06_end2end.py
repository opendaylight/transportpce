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
import time
import unittest
import requests
import sys
sys.path.append('transportpce_tests/common/')
import test_utils

class TransportPCEFulltesting(unittest.TestCase):
    cr_serv_sample_data = {"input": {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-create",
            "request-system-id": "appname",
            "notification-url":
                "http://localhost:8585/NotificationServer/notify"
        },
        "service-name": "service1",
        "common-id": "ASATT1234567",
        "connection-type": "service",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "XPDRA01",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJP8",
            "tx-direction": {
                "port": {
                    "port-device-name":
                        "ROUTER_SNJSCAMCJP8_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name":
                        "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.3",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name":
                        "ROUTER_SNJSCAMCJP8_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name":
                        "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.4",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "XPDRC01",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJT4",
            "tx-direction": {
                "port": {
                    "port-device-name":
                        "ROUTER_SNJSCAMCJT4_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name":
                        "LGX Panel_SNJSCAMCJT4_000000.00_00",
                    "lgx-port-name": "LGX Back.29",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name":
                        "ROUTER_SNJSCAMCJT4_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name":
                        "LGX Panel_SNJSCAMCJT4_000000.00_00",
                    "lgx-port-name": "LGX Back.30",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "due-date": "2016-11-28T00:00:01Z",
        "operator-contact": "pw1234"
    }
    }
    processes = None
    WAITING = 20
    NODE_VERSION = '1.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma-full', cls.NODE_VERSION),
                                               ('roadmc-full', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        print("execution of {}".format(self.id().split(".")[-1]))

    #  connect netconf devices
    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDRC01", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADMA01", ('roadma-full', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADMC01", ('roadmc-full', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xprdA_N1_to_roadmA_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRA01", "1", "1",
                                                          "ROADMA01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRA01", "1", "1",
                                                          "ROADMA01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_07_connect_xprdC_N1_to_roadmC_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRC01", "1", "1",
                                                          "ROADMC01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_xpdrC_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRC01", "1", "1",
                                                          "ROADMC01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_09_add_omsAttributes_ROADMA_ROADMC(self):
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
        response = test_utils.add_oms_attr_request("ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_10_add_omsAttributes_ROADMC_ROADMA(self):
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
        response = test_utils.add_oms_attr_request("ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    # test service-create for Eth service from xpdr to xpdr
    def test_11_create_eth_service1(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(self.WAITING)

    def test_12_get_eth_service1(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_13_check_xc1_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768',
                'wavelength-number': 1,
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP1-TXRX-761:768'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG1-TTP-TXRX-761:768'},
            res['roadm-connections'][0]['destination'])
        time.sleep(5)

    def test_14_check_xc1_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADMC01", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768',
                'wavelength-number': 1,
                'opticalControlMode': 'gainLoss',
                'target-output-power': 2.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP1-TXRX-761:768'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG2-TTP-TXRX-761:768'},
            res['roadm-connections'][0]['destination'])
        time.sleep(5)

    def test_15_check_topo_XPDRA(self):
        response = test_utils.get_ordm_topo_request("node/XPDRA01-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual({u'frequency': 196.1,
                                  u'width': 40},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-CLIENT2' or ele['tp-id'] == 'XPDR1-CLIENT1':
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
            if ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertNotIn('org-openroadm-network-topology:xpdr-network-attributes', dict.keys(ele))
        time.sleep(3)

    def test_16_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(3)

    def test_17_check_topo_ROADMA_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-DEG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                    freq_map = base64.b64decode(
                        ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                    freq_map_array = [int(x) for x in freq_map]
                    self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        time.sleep(3)

    def test_18_connect_xprdA_N2_to_roadmA_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRA01", "1", "2",
                                                          "ROADMA01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_19_connect_roadmA_PP2_to_xpdrA_N2(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRA01", "1", "2",
                                                          "ROADMA01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_20_connect_xprdC_N2_to_roadmC_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRC01", "1", "2",
                                                          "ROADMC01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_21_connect_roadmC_PP2_to_xpdrC_N2(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRC01", "1", "2",
                                                          "ROADMC01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_22_create_eth_service2(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service2"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(self.WAITING)

    def test_23_get_eth_service2(self):
        response = test_utils.get_service_list_request("services/service2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service2')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_24_check_xc2_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/DEG1-TTP-TXRX-SRG1-PP2-TXRX-753:760")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'DEG1-TTP-TXRX-SRG1-PP2-TXRX-753:760',
                'wavelength-number': 2,
                'opticalControlMode': 'power'
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'DEG1-TTP-TXRX-753:760'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'SRG1-PP2-TXRX-753:760'},
            res['roadm-connections'][0]['destination'])

    def test_25_check_topo_XPDRA(self):
        response = test_utils.get_ordm_topo_request("node/XPDRA01-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual({u'frequency': 196.1,
                                  u'width': 40},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-NETWORK2':
                self.assertEqual({u'frequency': 196.05,
                                  u'width': 40},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-CLIENT1' or ele['tp-id'] == 'XPDR1-CLIENT2':
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
        time.sleep(10)

    def test_26_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[94], 255, "Lambda 2 should be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
            if ele['tp-id'] == 'SRG1-PP3-TXRX':
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(10)

    def test_27_check_topo_ROADMA_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-DEG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
                self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        time.sleep(10)

    #     creation service test on a non-available resource
    def test_28_create_eth_service3(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service3"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        self.assertIn('200', res['output']['configuration-response-common'][
            'response-code'])
        time.sleep(self.WAITING)

    # add a test that check the openroadm-service-list still only
    # contains 2 elements

    def test_29_delete_eth_service3(self):
        response = test_utils.service_delete_request("service3")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service \'service3\' does not exist in datastore',
                      res['output']['configuration-response-common'][
                          'response-message'])
        self.assertIn('500', res['output']['configuration-response-common'][
            'response-code'])
        time.sleep(20)

    def test_30_delete_eth_service1(self):
        response = test_utils.service_delete_request("service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(20)

    def test_31_delete_eth_service2(self):
        response = test_utils.service_delete_request("service2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(20)

    def test_32_check_no_xc_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "")
        res = response.json()
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertNotIn('roadm-connections',
                         dict.keys(res['org-openroadm-device']))
        time.sleep(2)

    def test_33_check_topo_XPDRA(self):
        response = test_utils.get_ordm_topo_request("node/XPDRA01-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ((ele[u'org-openroadm-common-network:tp-type'] ==
                 'XPONDER-CLIENT')
                    and (ele['tp-id'] == 'XPDR1-CLIENT1' or ele[
                        'tp-id'] == 'XPDR1-CLIENT3')):
                self.assertNotIn(
                    'org-openroadm-network-topology:xpdr-client-attributes',
                    dict.keys(ele))
            elif (ele[u'org-openroadm-common-network:tp-type'] ==
                  'XPONDER-NETWORK'):
                self.assertIn(u'tail-equipment-id', dict.keys(
                    ele[u'org-openroadm-network-topology:'
                        u'xpdr-network-attributes']))
                self.assertNotIn('wavelength', dict.keys(
                    ele[u'org-openroadm-network-topology:'
                        u'xpdr-network-attributes']))
        time.sleep(10)

    def test_34_check_topo_ROADMA_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should  be available")
        self.assertEqual(freq_map_array[94], 255, "Lambda 2 should  be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX' or ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should  be available")
                self.assertEqual(freq_map_array[94], 255, "Lambda 2 should  be available")
            elif ele['tp-id'] == 'SRG1-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should  be available")
                self.assertEqual(freq_map_array[94], 255, "Lambda 2 should  be available")
            else:
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(10)

    def test_35_check_topo_ROADMA_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-DEG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        self.assertEqual(freq_map_array[94], 255, "Lambda 2 should be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[94], 255, "Lambda 2 should be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
                self.assertEqual(freq_map_array[94], 255, "Lambda 2 should be available")
        time.sleep(10)

    # test service-create for Optical Channel (OC) service from srg-pp to srg-pp
    def test_36_create_oc_service1(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1"
        self.cr_serv_sample_data["input"]["connection-type"] = "roadm-line"
        self.cr_serv_sample_data["input"]["service-a-end"]["node-id"] = "ROADMA01"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "OC"
        self.cr_serv_sample_data["input"]["service-z-end"]["node-id"] = "ROADMC01"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "OC"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(self.WAITING)

    def test_37_get_oc_service1(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_38_check_xc1_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768',
                'wavelength-number': 1,
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP1-TXRX-761:768'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG1-TTP-TXRX-761:768'},
            res['roadm-connections'][0]['destination'])
        time.sleep(7)

    def test_39_check_xc1_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADMC01", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768',
                'wavelength-number': 1,
                'opticalControlMode': 'gainLoss',
                'target-output-power': 2.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP1-TXRX-761:768'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG2-TTP-TXRX-761:768'},
            res['roadm-connections'][0]['destination'])
        time.sleep(7)

    def test_40_create_oc_service2(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service2"
        self.cr_serv_sample_data["input"]["connection-type"] = "roadm-line"
        self.cr_serv_sample_data["input"]["service-a-end"]["node-id"] = "ROADMA01"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "OC"
        self.cr_serv_sample_data["input"]["service-z-end"]["node-id"] = "ROADMC01"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "OC"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(self.WAITING)

    def test_41_get_oc_service2(self):
        response = test_utils.get_service_list_request("services/service2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'],
            'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service2')
        self.assertEqual(
            res['services'][0]['connection-type'], 'roadm-line')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_42_check_xc2_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/SRG1-PP2-TXRX-DEG1-TTP-TXRX-753:760")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP2-TXRX-DEG1-TTP-TXRX-753:760',
                'wavelength-number': 2,
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP2-TXRX-753:760'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG1-TTP-TXRX-753:760'},
            res['roadm-connections'][0]['destination'])
        time.sleep(2)

    def test_43_check_topo_ROADMA(self):
        self.test_26_check_topo_ROADMA_SRG1()
        self.test_27_check_topo_ROADMA_DEG1()
        time.sleep(3)

    def test_44_delete_oc_service1(self):
        response = test_utils.service_delete_request("service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(20)

    def test_45_delete_oc_service2(self):
        response = test_utils.service_delete_request("service2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common'][
                          'response-message'])
        time.sleep(20)

    def test_46_get_no_oc_services(self):
        print("start test")
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {
                "error-type": "application",
                "error-tag": "data-missing",
                "error-message":
                    "Request could not be completed because the relevant data "
                    "model content does not exist"
            },
            res['errors']['error'])
        time.sleep(1)

    def test_47_get_no_xc_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn(['roadm-connections'][0], res['org-openroadm-device'])
        time.sleep(1)

    def test_48_check_topo_ROADMA(self):
        self.test_34_check_topo_ROADMA_SRG1()
        self.test_35_check_topo_ROADMA_DEG1()

    def test_49_loop_create_eth_service(self):
        for i in range(1, 6):
            print("iteration number {}".format(i))
            print("eth service creation")
            self.test_11_create_eth_service1()
            print("check xc in ROADMA01")
            self.test_13_check_xc1_ROADMA()
            print("check xc in ROADMC01")
            self.test_14_check_xc1_ROADMC()
            print("eth service deletion\n")
            self.test_30_delete_eth_service1()

    def test_50_loop_create_oc_service(self):
        response = test_utils.get_service_list_request("services/service1")
        if response.status_code != 404:
            response = test_utils.service_delete_request("service1")
            time.sleep(5)

        for i in range(1, 6):
            print("iteration number {}".format(i))
            print("oc service creation")
            self.test_36_create_oc_service1()
            print("check xc in ROADMA01")
            self.test_38_check_xc1_ROADMA()
            print("check xc in ROADMC01")
            self.test_39_check_xc1_ROADMC()
            print("oc service deletion\n")
            self.test_44_delete_oc_service1()

    def test_51_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_52_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDRC01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_53_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADMA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_54_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADMC01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
