#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
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
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    cr_serv_sample_data = {"input": {
        "sdnc-request-header": {
            "request-id": "request-1",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service-OCH-OTU4-AB",
        "common-id": "commonId",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "SPDR-SA1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSA",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "XPDR2-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "XPDR2-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "SPDR-SB1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSB",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "SPDR-SB1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "XPDR2-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "SPDR-SB1-XPDR2",
                    "port-type": "fixed",
                    "port-name": "XPDR2-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "due-date": "2018-06-15T00:00:01Z",
        "operator-contact": "pw1234"
    }
    }

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('spdrb', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmb', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_spdrA(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_spdrB(self):
        response = test_utils.mount_device("SPDR-SB1", ('spdrb', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_spdrC(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_rdmB(self):
        response = test_utils.mount_device("ROADM-B1", ('roadmb', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_06_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_07_connect_sprdA_2_N1_to_roadmA_PP3(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SA1", "2", "1",
                                                          "ROADM-A1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmA_PP3_to_spdrA_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SA1", "2", "1",
                                                          "ROADM-A1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_09_connect_sprdC_2_N1_to_roadmC_PP3(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SC1", "2", "1",
                                                          "ROADM-C1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_10_connect_roadmC_PP3_to_spdrC_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SC1", "2", "1",
                                                          "ROADM-C1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_11_connect_sprdB_2_N1_to_roadmB_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SB1", "2", "1",
                                                          "ROADM-B1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_12_connect_roadmB_PP1_to_spdrB_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SB1", "2", "1",
                                                          "ROADM-B1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_13_connect_sprdB_2_N2_to_roadmB_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SB1", "2", "2",
                                                          "ROADM-B1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_14_connect_roadmB_PP2_to_spdrB_2_N2(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SB1", "2", "2",
                                                          "ROADM-B1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_15_add_omsAttributes_ROADMA_ROADMB(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_16_add_omsAttributes_ROADMB_ROADMA(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_17_add_omsAttributes_ROADMB_ROADMC(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_18_add_omsAttributes_ROADMC_ROADMB(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_19_create_OTS_ROADMA_DEG1(self):
        response = test_utils.create_ots_oms_request("ROADM-A1", "DEG1-TTP-TXRX")
        time.sleep(10)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-A1',
                      res["output"]["result"])

    def test_20_create_OTS_ROADMB_DEG1(self):
        response = test_utils.create_ots_oms_request("ROADM-B1", "DEG1-TTP-TXRX")
        time.sleep(10)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-B1',
                      res["output"]["result"])

    def test_21_create_OTS_ROADMB_DEG2(self):
        response = test_utils.create_ots_oms_request("ROADM-B1", "DEG2-TTP-TXRX")
        time.sleep(10)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-B1',
                      res["output"]["result"])

    def test_22_create_OTS_ROADMC_DEG2(self):
        response = test_utils.create_ots_oms_request("ROADM-C1", "DEG2-TTP-TXRX")
        time.sleep(10)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-C1',
                      res["output"]["result"])

    def test_23_calculate_span_loss_base_all(self):
        url = "{}/operations/transportpce-olm:calculate-spanloss-base"
        data = {
            "input": {
                "src-type": "all"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success',
                      res["output"]["result"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX"
        }, res["output"]["spans"])
        time.sleep(5)

    def test_24_check_otn_topology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 9, 'There should be 9 nodes')
        self.assertNotIn('ietf-network-topology:link', res['network'][0],
                         'otn-topology should have no link')

# test service-create for OCH-OTU4 service from spdrA to spdrB
    def test_25_create_OCH_OTU4_service_AB(self):
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_26_get_OCH_OTU4_service_AB(self):
        response = test_utils.get_service_list_request(
            "services/service-OCH-OTU4-AB")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-OCH-OTU4-AB')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

# Check correct configuration of devices
    def test_27_check_interface_och_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_28_check_interface_OTU4_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-sapi': 'exT821pFtOc=',
                        'expected-dapi': 'exT821pFtOc=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_29_check_interface_och_spdrB(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_30_check_interface_OTU4_spdrB(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-dapi': 'exT821pFtOc=',
                        'expected-sapi': 'exT821pFtOc=',
                        'tx-sapi': 'HPQZi9Cb3Aw=',
                        'expected-dapi': 'HPQZi9Cb3Aw=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_31_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_32_check_openroadm_topo_spdra(self):
        response = test_utils.get_ordm_topo_request("node/SPDR-SA1-XPDR2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        ele = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', ele['tp-id'])
        self.assertEqual({u'frequency': 196.1,
                          u'width': 40},
                         ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
        self.assertEqual('ROADM-A1-SRG1--SRG1-PP3-TXRX',
                         ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
        time.sleep(3)

    def test_33_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP3-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(3)

    def test_33_check_openroadm_topo_ROADMA_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-DEG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        time.sleep(3)

    def test_34_check_otn_topo_otu4_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 2)
        listLinkId = ['OTU4-SPDR-SA1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK1toSPDR-SA1-XPDR2-XPDR2-NETWORK1']
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(
                link['transportpce-topology:otn-link-type'], 'OTU4')
            self.assertEqual(
                link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(
                link['org-openroadm-common-network:opposite-link'], listLinkId)

# test service-create for OCH-OTU4 service from spdrB to spdrC
    def test_35_create_OCH_OTU4_service_BC(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-OCH-OTU4-BC"
        self.cr_serv_sample_data["input"]["service-a-end"]["node-id"] = "SPDR-SB1"
        self.cr_serv_sample_data["input"]["service-a-end"]["clli"] = "NodeSB"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SB1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-NETWORK2"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SB1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-NETWORK2"
        self.cr_serv_sample_data["input"]["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_sample_data["input"]["service-z-end"]["clli"] = "NodeSC"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"

        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_36_get_OCH_OTU4_service_BC(self):
        response = test_utils.get_service_list_request(
            "services/service-OCH-OTU4-BC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-OCH-OTU4-BC')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

# Check correct configuration of devices
    def test_37_check_interface_och_spdrB(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK2-753:760")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-753:760',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP6-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.05, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_38_check_interface_OTU4_spdrB(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK2-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK2-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP6-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-753:760',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP6-CFP-P1'
                        }
        input_dict_2 = {'tx-sapi': 'HPQZi9Cb3A8=',
                        'expected-dapi': 'HPQZi9Cb3A8=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_39_check_interface_och_spdrC(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1", "interface/XPDR2-NETWORK1-753:760")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.05, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_40_check_interface_OTU4_spdrC(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1", "interface/XPDR2-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-753:760',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-dapi': 'HPQZi9Cb3A8=',
                        'expected-sapi': 'HPQZi9Cb3A8=',
                        'tx-sapi': 'ALx70DYYfGTx',
                        'expected-dapi': 'ALx70DYYfGTx',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_41_check_no_interface_ODU4_spdrB(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_42_check_openroadm_topo_spdrB(self):
        response = test_utils.get_ordm_topo_request("node/SPDR-SB1-XPDR2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR2-NETWORK1':
                self.assertEqual({u'frequency': 196.1,
                                  u'width': 40},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
                self.assertEqual('ROADM-B1-SRG1--SRG1-PP1-TXRX',
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
            elif ele['tp-id'] == 'XPDR2-NETWORK2':
                self.assertEqual({u'frequency': 196.05,
                                  u'width': 40},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
                self.assertEqual('ROADM-B1-SRG1--SRG1-PP2-TXRX',
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
            else:
                print("ele = {}".format(ele))
                self.assertNotIn('org-openroadm-network-topology:xpdr-network-attributes', dict.keys(ele))
        time.sleep(3)

    def test_43_check_openroadm_topo_ROADMB_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-B1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        self.assertEqual(freq_map_array[93], 255, "Lambda 3 should be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
            if ele['tp-id'] == 'SRG1-PP3-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(3)

    def test_44_check_openroadm_topo_ROADMB_DEG2(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-B1-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[94], 0, "Lambda 1 should not be available")
        time.sleep(3)

    def test_45_check_otn_topo_otu4_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        listLinkId = ['OTU4-SPDR-SA1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK1toSPDR-SA1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK2toSPDR-SC1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SC1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK2']
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(
                link['transportpce-topology:otn-link-type'], 'OTU4')
            self.assertEqual(
                link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(
                link['org-openroadm-common-network:opposite-link'], listLinkId)

# test service-create for 100GE service from spdrA to spdrC via spdrB
    def test_46_create_100GE_service_ABC(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-100GE-ABC"
        self.cr_serv_sample_data["input"]["connection-type"] = "service"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_sample_data["input"]["service-a-end"]["node-id"] = "SPDR-SA1"
        self.cr_serv_sample_data["input"]["service-a-end"]["clli"] = "NodeSA"
        del self.cr_serv_sample_data["input"]["service-a-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_sample_data["input"]["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_sample_data["input"]["service-z-end"]["clli"] = "NodeSC"
        del self.cr_serv_sample_data["input"]["service-z-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"

        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_47_get_100GE_service_ABC(self):
        response = test_utils.get_service_list_request(
            "services/service-100GE-ABC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-100GE-ABC')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_48_check_interface_100GE_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR2-CLIENT1-ETHERNET',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP2-QSFP1',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP2-QSFP1-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 100000,
             u'fec': 'off'},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_49_check_interface_ODU4_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-CLIENT1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP2-QSFP1',
                        'supporting-interface': 'XPDR2-CLIENT1-ETHERNET',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP2-QSFP1-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated',
            'expected-dapi': 'AItaZ6nmyaKJ',
            'expected-sapi': 'AKFnJJaijWiz',
            'tx-dapi': 'AKFnJJaijWiz',
            'tx-sapi': 'AItaZ6nmyaKJ'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_50_check_interface_ODU4_NETWORK_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP5-CFP-P1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertNotIn('opu',
                         dict.keys(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_51_check_ODU4_connection_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1",
            "odu-connection/XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR2-NETWORK1-ODU4'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR2-CLIENT1-ODU4'},
                             res['odu-connection'][0]['source'])

    def test_52_check_interface_100GE_CLIENT_spdrc(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1", "interface/XPDR2-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR2-CLIENT1-ETHERNET',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP2-QSFP1',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP2-QSFP1-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 100000,
             u'fec': 'off'},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_53_check_interface_ODU4_CLIENT_spdrc(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1", "interface/XPDR2-CLIENT1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP2-QSFP1',
                        'supporting-interface': 'XPDR2-CLIENT1-ETHERNET',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP2-QSFP1-P1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated',
            'expected-dapi': 'AKFnJJaijWiz',
            'expected-sapi': 'AItaZ6nmyaKJ',
            'tx-dapi': 'AItaZ6nmyaKJ',
            'tx-sapi': 'AKFnJJaijWiz'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_54_check_interface_ODU4_NETWORK_spdrc(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP5-CFP-P1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertNotIn('opu',
                         dict.keys(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_55_check_ODU4_connection_spdrc(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1",
            "odu-connection/XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR2-NETWORK1-ODU4'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR2-CLIENT1-ODU4'},
                             res['odu-connection'][0]['source'])

    def test_56_check_interface_ODU4_NETWORK1_spdrb(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP5-CFP-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertNotIn('opu',
                         dict.keys(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_57_check_interface_ODU4_NETWORK2_spdrb(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1", "interface/XPDR2-NETWORK2-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP6-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP6-CFP-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertNotIn('opu',
                         dict.keys(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_58_check_ODU4_connection_spdrb(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SB1",
            "odu-connection/XPDR2-NETWORK1-ODU4-x-XPDR2-NETWORK2-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR2-NETWORK1-ODU4-x-XPDR2-NETWORK2-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR2-NETWORK2-ODU4'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR2-NETWORK1-ODU4'},
                             res['odu-connection'][0]['source'])

    def test_59_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
            self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_60_delete_service_100GE_ABC(self):
        response = test_utils.service_delete_request("service-100GE-ABC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_61_check_service_list(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 2)
        time.sleep(2)

    def test_62_check_no_ODU4_connection_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn(['odu-connection'][0], res['org-openroadm-device'])
        time.sleep(1)

    def test_63_check_no_interface_ODU4_NETWORK_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_64_check_no_interface_ODU4_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-CLIENT1-ODU4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_65_check_no_interface_100GE_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_66_check_otn_topo_links(self):
        self.test_45_check_otn_topo_otu4_links()

    def test_67_delete_OCH_OTU4_service_AB(self):
        response = test_utils.service_delete_request("service-OCH-OTU4-AB")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_68_delete_OCH_OTU4_service_BC(self):
        response = test_utils.service_delete_request("service-OCH-OTU4-BC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_69_get_no_service(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])
        time.sleep(1)

    def test_70_check_no_interface_OTU4_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_71_check_no_interface_OCH_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1", "interface/XPDR2-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_72_getLinks_OtnTopology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_73_check_openroadm_topo_spdra(self):
        response = test_utils.get_ordm_topo_request("node/SPDR-SA1-XPDR2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        tp = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp[u'org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(3)

    def test_74_check_openroadm_topo_ROADMB_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-B1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        self.assertEqual(freq_map_array[94], 255, "Lambda 1 should be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[94], 255, "Lambda 1 should be available")
        time.sleep(3)

    def test_75_check_openroadm_topo_ROADMB_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-B1-DEG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        time.sleep(3)

    def test_76_check_openroadm_topo_ROADMB_DEG2(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-B1-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        time.sleep(3)

    def test_77_disconnect_xponders_from_roadm(self):
        url = "{}/config/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        for link in links:
            if ((link["org-openroadm-common-network:link-type"] == "XPONDER-OUTPUT" or
                    link["org-openroadm-common-network:link-type"] == "XPONDER-INPUT")
                and ('SPDR-SB1' in link['link-id'] or 'ROADM-B1' in link['link-id'])):
                link_name = link["link-id"]
                response = test_utils.delete_request(url+link_name)
                self.assertEqual(response.status_code, requests.codes.ok)

    def test_78_disconnect_spdrB(self):
        response = test_utils.unmount_device("SPDR-SB1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_79_disconnect_roadmB(self):
        response = test_utils.unmount_device("ROADM-B1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_80_remove_roadm_to_roadm_links(self):
        url = "{}/config/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        for link in links:
            if (link["org-openroadm-common-network:link-type"] == "ROADM-TO-ROADM"
                and 'ROADM-B1' in link['link-id']):
                link_name = link["link-id"]
                response = test_utils.delete_request(url+link_name)
                self.assertEqual(response.status_code, requests.codes.ok)

    def test_81_add_omsAttributes_ROADMA_ROADMC(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_82_add_omsAttributes_ROADMC_ROADMA(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_83_create_OCH_OTU4_service_AC(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-OCH-OTU4-AC"
        self.cr_serv_sample_data["input"]["connection-type"] = "infrastructure"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-rate"] = "100"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "OTU"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_sample_data["input"]["service-a-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-rate"] = "100"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "OTU"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_sample_data["input"]["service-z-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_84_get_OCH_OTU4_service_AC(self):
        response = test_utils.get_service_list_request(
            "services/service-OCH-OTU4-AC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-OCH-OTU4-AC')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

# test service-create for 100GE service from spdrA to spdrC via spdrB
    def test_85_create_100GE_service_AC(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-100GE-AC"
        self.cr_serv_sample_data["input"]["connection-type"] = "service"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_sample_data["input"]["service-a-end"]["node-id"] = "SPDR-SA1"
        self.cr_serv_sample_data["input"]["service-a-end"]["clli"] = "NodeSA"
        del self.cr_serv_sample_data["input"]["service-a-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_sample_data["input"]["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_sample_data["input"]["service-z-end"]["clli"] = "NodeSC"
        del self.cr_serv_sample_data["input"]["service-z-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"

        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_86_get_100GE_service_AC(self):
        response = test_utils.get_service_list_request("services/service-100GE-AC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-100GE-AC')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_87_check_configuration_spdra(self):
        self.test_48_check_interface_100GE_CLIENT_spdra()
        self.test_49_check_interface_ODU4_CLIENT_spdra()
        self.test_50_check_interface_ODU4_NETWORK_spdra()
        self.test_51_check_ODU4_connection_spdra()


    def test_88_check_configuration_spdrc(self):
        self.test_52_check_interface_100GE_CLIENT_spdrc()
        self.test_53_check_interface_ODU4_CLIENT_spdrc()
        self.test_54_check_interface_ODU4_NETWORK_spdrc()
        self.test_55_check_ODU4_connection_spdrc()

    def test_89_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 2)
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_90_delete_100GE_service_AC(self):
        response = test_utils.service_delete_request("service-100GE-AC")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_91_check_service_list(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 1)
        time.sleep(2)

    def test_92_check_configuration_spdra(self):
        self.test_62_check_no_ODU4_connection_spdra()
        self.test_63_check_no_interface_ODU4_NETWORK_spdra()
        self.test_64_check_no_interface_ODU4_CLIENT_spdra()
        self.test_65_check_no_interface_100GE_CLIENT_spdra()

    def test_93_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 2)
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_94_disconnect_xponders_from_roadm(self):
        url = "{}/config/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        for link in links:
            if (link["org-openroadm-common-network:link-type"] == "XPONDER-OUTPUT" or
                    link["org-openroadm-common-network:link-type"] == "XPONDER-INPUT"):
                link_name = link["link-id"]
                response = test_utils.delete_request(url+link_name)
                self.assertEqual(response.status_code, requests.codes.ok)

    def test_95_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_96_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_97_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_98_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
