#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import unittest
import time
import requests
from common import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None
    WAITING = 20  # nominal value is 300

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims(['spdra', 'roadma', 'roadmc', 'spdrc'])

    @classmethod
    def tearDownClass(cls):
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_spdrA(self):
        response = test_utils.mount_device("SPDR-SA1", 'spdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_spdrC(self):
        response = test_utils.mount_device("SPDR-SC1", 'spdrc')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", 'roadma')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", 'roadmc')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_sprdA_N1_to_roadmA_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_spdrA_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_07_connect_sprdC_N1_to_roadmC_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_spdrC_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
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
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
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
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

# test service-create for OCH-OTU4 service from spdr to spdr
    def test_11_check_otn_topology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 4)
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_12_create_OCH_OTU4_service(self):
        url = "{}/operations/org-openroadm-service:service-create"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "request-1",
                    "rpc-action": "service-create",
                    "request-system-id": "appname"
                },
                "service-name": "service1-OCH-OTU4",
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
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                    "node-id": "SPDR-SC1",
                    "service-format": "OTU",
                    "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
                    "clli": "NodeSC",
                    "subrate-eth-sla": {
                        "subrate-eth-sla": {
                            "committed-info-rate": "100000",
                            "committed-burst-size": "64"
                        }
                    },
                    "tx-direction": {
                        "port": {
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_13_get_OCH_OTU4_service1(self):
        url = "{}/operational/org-openroadm-service:service-list/services/service1-OCH-OTU4"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1-OCH-OTU4')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    # Check correct configuration of devices
    def test_14_check_interface_och_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_15_check_interface_OTU4_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-sapi': 'Swfw02qXGyI=',
                        'expected-dapi': 'Swfw02qXGyI=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_16_check_interface_och_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-1',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_17_check_interface_OTU4_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-1',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': 'Swfw02qXGyI=',
                        'expected-sapi': 'Swfw02qXGyI=',
                        'tx-sapi': 'fuYZwEO660g=',
                        'expected-dapi': 'fuYZwEO660g=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_18_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_19_check_openroadm_topo_spdra(self):
        response = test_utils.get_ordm_topo_request("node/SPDR-SA1-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        ele = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR1-NETWORK1', ele['tp-id'])
        self.assertEqual({u'frequency': 196.1,
                          u'width': 40},
                         ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
        time.sleep(3)

    def test_20_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1},
                         res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                self.assertIn({u'index': 1, u'frequency': 196.1,
                               u'width': 40},
                              ele['org-openroadm-network-topology:pp-attributes']['used-wavelength'])
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('used-wavelength', dict.keys(ele))
        time.sleep(3)

    def test_21_check_openroadm_topo_ROADMA_DEG(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn({u'index': 1},
                         res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                self.assertIn({u'index': 1, u'frequency': 196.1,
                               u'width': 40},
                              ele['org-openroadm-network-topology:ctp-attributes']['used-wavelengths'])
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                self.assertIn({u'index': 1, u'frequency': 196.1,
                               u'width': 40},
                              ele['org-openroadm-network-topology:tx-ttp-attributes']['used-wavelengths'])
        time.sleep(3)

    def test_22_check_otn_topo_otu4_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 2)
        listLinkId = ['OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                      'OTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1']
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(link['transportpce-topology:otn-link-type'], 'OTU4')
            self.assertEqual(link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(link['org-openroadm-common-network:opposite-link'], listLinkId)

# test service-create for ODU4 service from spdr to spdr
    def test_23_create_ODU4_service(self):
        url = "{}/operations/org-openroadm-service:service-create"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "request-1",
                    "rpc-action": "service-create",
                    "request-system-id": "appname"
                },
                "service-name": "service1-ODU4",
                "common-id": "commonId",
                "connection-type": "infrastructure",
                "service-a-end": {
                    "service-rate": "100",
                    "node-id": "SPDR-SA1",
                    "service-format": "ODU",
                    "odu-service-rate": "org-openroadm-otn-common-types:ODU4",
                    "clli": "NodeSA",
                    "subrate-eth-sla": {
                        "subrate-eth-sla": {
                            "committed-info-rate": "100000",
                            "committed-burst-size": "64"
                        }
                    },
                    "tx-direction": {
                        "port": {
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                    "node-id": "SPDR-SC1",
                    "service-format": "ODU",
                    "odu-service-rate": "org-openroadm-otn-common-types:ODU4",
                    "clli": "NodeSC",
                    "subrate-eth-sla": {
                        "subrate-eth-sla": {
                            "committed-info-rate": "100000",
                            "committed-burst-size": "64"
                        }
                    },
                    "tx-direction": {
                        "port": {
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-NETWORK1",
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
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_24_get_ODU4_service1(self):
        url = "{}/operational/org-openroadm-service:service-list/services/service1-ODU4"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1-ODU4')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_25_check_interface_ODU4_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'expected-dapi': 'Swfw02qXGyI=',
                        'expected-sapi': 'fuYZwEO660g=',
                        'tx-dapi': 'fuYZwEO660g=',
                        'tx-sapi': 'Swfw02qXGyI='}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_26_check_interface_ODU4_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'tx-sapi': 'fuYZwEO660g=',
                        'tx-dapi': 'Swfw02qXGyI=',
                        'expected-sapi': 'Swfw02qXGyI=',
                        'expected-dapi': 'fuYZwEO660g='
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_27_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId == 'OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1' or
                    linkId == 'OTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'):
                self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
                self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)
            elif (linkId == 'ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1' or
                  linkId == 'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'):
                self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
                self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
                self.assertEqual(link['transportpce-topology:otn-link-type'], 'ODTU4')
                self.assertEqual(link['org-openroadm-common-network:link-type'], 'OTN-LINK')
                self.assertIn(link['org-openroadm-common-network:opposite-link'],
                              ['ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                               'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'])
            else:
                self.fail("this link should not exist")

    def test_28_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if (node['node-id'] == 'SPDR-SA1-XPDR1' or 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if (tp['tp-id'] == 'XPDR1-NETWORK1'):
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 80)
                        self.assertEqual(xpdrTpPortConAt['odtu-tpn-pool'][0]['odtu-type'],
                                         'org-openroadm-otn-common-types:ODTU4.ts-Allocated')

# test service-create for 10GE service from spdr to spdr
    def test_29_create_10GE_service(self):
        url = "{}/operations/org-openroadm-service:service-create"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "request-1",
                    "rpc-action": "service-create",
                    "request-system-id": "appname"
                },
                "service-name": "service1-10GE",
                "common-id": "commonId",
                "connection-type": "service",
                "service-a-end": {
                    "service-rate": "10",
                    "node-id": "SPDR-SA1",
                    "service-format": "Ethernet",
                    "clli": "NodeSA",
                    "subrate-eth-sla": {
                        "subrate-eth-sla": {
                            "committed-info-rate": "100000",
                            "committed-burst-size": "64"
                        }
                    },
                    "tx-direction": {
                        "port": {
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-CLIENT1",
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
                            "port-device-name": "SPDR-SA1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-CLIENT1",
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
                    "service-rate": "10",
                    "node-id": "SPDR-SC1",
                    "service-format": "Ethernet",
                    "clli": "NodeSC",
                    "subrate-eth-sla": {
                        "subrate-eth-sla": {
                            "committed-info-rate": "100000",
                            "committed-burst-size": "64"
                        }
                    },
                    "tx-direction": {
                        "port": {
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-CLIENT1",
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
                            "port-device-name": "SPDR-SC1-XPDR1",
                            "port-type": "fixed",
                            "port-name": "XPDR1-CLIENT1",
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
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_30_get_10GE_service1(self):
        url = "{}/operational/org-openroadm-service:service-list/services/service1-10GE"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1-10GE')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_31_check_interface_10GE_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_32_check_interface_ODU2E_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_33_check_interface_ODU2E_NETWORK_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}
        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn(1, res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['trib-slots'])

    def test_34_check_ODU2E_connection_spdra(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1",
            "odu-connection/XPDR1-CLIENT1-ODU2e-service1-10GE-x-XPDR1-NETWORK1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-service1-10GE-x-XPDR1-NETWORK1-ODU2e-service1-10GE',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-service1-10GE'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT1-ODU2e-service1-10GE'},
                             res['odu-connection'][0]['source'])

    def test_35_check_interface_10GE_CLIENT_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_36_check_interface_ODU2E_CLIENT_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-CLIENT1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_37_check_interface_ODU2E_NETWORK_spdrc(self):
        response = test_utils.check_netconf_node_request("SPDR-SC1", "interface/XPDR1-NETWORK1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_38_check_ODU2E_connection_spdrc(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SC1",
            "odu-connection/XPDR1-CLIENT1-ODU2e-service1-10GE-x-XPDR1-NETWORK1-ODU2e-service1-10GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-service1-10GE-x-XPDR1-NETWORK1-ODU2e-service1-10GE',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-service1-10GE'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT1-ODU2e-service1-10GE'},
                             res['odu-connection'][0]['source'])

    def test_39_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId == 'ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1' or
                    linkId == 'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'):
                self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 90000)
                self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 10000)

    def test_40_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if (node['node-id'] == 'SPDR-SA1-XPDR1' or 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if (tp['tp-id'] == 'XPDR1-NETWORK1'):
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 72)
                        tsPoolList = [i for i in range(1, 9)]
                        self.assertNotIn(tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 79)
                        self.assertNotIn(1, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_41_delete_10GE_service(self):
        url = "{}/operations/org-openroadm-service:service-delete"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service1-10GE",
                    "tail-retention": "no"
                }
                }
                }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_42_check_service_list(self):
        url = "{}/operational/org-openroadm-service:service-list"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 2)
        time.sleep(2)

    def test_43_check_no_ODU2e_connection_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn(['odu-connection'][0], res['org-openroadm-device'])
        time.sleep(1)

    def test_44_check_no_interface_ODU2E_NETWORK_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_45_check_no_interface_ODU2E_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_46_check_no_interface_10GE_CLIENT_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT1-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_47_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId == 'ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1' or
                    linkId == 'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'):
                self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
                self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_48_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if (node['node-id'] == 'SPDR-SA1-XPDR1' or 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if (tp['tp-id'] == 'XPDR1-NETWORK1'):
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 80)

    def test_49_delete_ODU4_service(self):
        url = "{}/operations/org-openroadm-service:service-delete"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service1-ODU4",
                    "tail-retention": "no"
                }
                }
                }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_50_check_service_list(self):
        url = "{}/operational/org-openroadm-service:service-list"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 1)
        time.sleep(2)

    def test_51_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_52_check_otn_topo_links(self):
        self.test_22_check_otn_topo_otu4_links()

    def test_53_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if (node['node-id'] == 'SPDR-SA1-XPDR1' or 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if (tp['tp-id'] == 'XPDR1-NETWORK1'):
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertNotIn('ts-pool', dict.keys(xpdrTpPortConAt))
                        self.assertNotIn('odtu-tpn-pool', dict.keys(xpdrTpPortConAt))

    def test_54_delete_OCH_OTU4_service(self):
        url = "{}/operations/org-openroadm-service:service-delete"
        data = {"input": {
                "sdnc-request-header": {
                    "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
                    "rpc-action": "service-delete",
                    "request-system-id": "appname",
                    "notification-url": "http://localhost:8585/NotificationServer/notify"
                },
                "service-delete-req-info": {
                    "service-name": "service1-OCH-OTU4",
                    "tail-retention": "no"
                }
                }
                }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_55_get_no_service(self):
        url = "{}/operational/org-openroadm-service:service-list"
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])
        time.sleep(1)

    def test_56_check_no_interface_OTU4_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_57_check_no_interface_OCH_spdra(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-1")
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_58_getLinks_OtnTopology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_59_check_openroadm_topo_spdra(self):
        response = test_utils.get_ordm_topo_request("node/SPDR-SA1-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        tp = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR1-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp[u'org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(3)

    def test_60_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({u'index': 1},
                      res['node'][0][u'org-openroadm-network-topology:srg-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                self.assertNotIn('org-openroadm-network-topology:pp-attributes', dict.keys(ele))
        time.sleep(3)

    def test_61_check_openroadm_topo_ROADMA_DEG(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({u'index': 1},
                      res['node'][0][u'org-openroadm-network-topology:degree-attributes']['available-wavelengths'])
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                self.assertNotIn('org-openroadm-network-topology:ctp-attributes', dict.keys(ele))
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                self.assertNotIn('org-openroadm-network-topology:tx-ttp-attributes', dict.keys(ele))
        time.sleep(3)

    def test_62_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_63_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_64_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_65_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
