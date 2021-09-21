#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
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
    NODE_VERSION_221 = '2.2.1'
    NODE_VERSION_71 = '7.1'

    cr_serv_sample_data = {"input": {
        "sdnc-request-header": {
            "request-id": "request-1",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service1-OTUC4",
        "common-id": "commonId",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "400",
            "node-id": "XPDR-A2",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTUCn",
            "clli": "NodeSA",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "XPDR-A2-XPDR2",
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
                    "port-device-name": "XPDR-A2-XPDR2",
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
            "service-rate": "400",
            "node-id": "XPDR-C2",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTUCn",
            "clli": "NodeSC",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "XPDR-C2-XPDR2",
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
                    "port-device-name": "XPDR-C2-XPDR2",
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
        cls.processes = test_utils.start_sims([('xpdra2', cls.NODE_VERSION_71),
                                               ('roadma', cls.NODE_VERSION_221),
                                               ('roadmc', cls.NODE_VERSION_221),
                                               ('xpdrc2', cls.NODE_VERSION_71)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(5)

    def test_01_connect_xpdra2(self):
        response = test_utils.mount_device("XPDR-A2", ('xpdra2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrc2(self):
        response = test_utils.mount_device("XPDR-C2", ('xpdrc2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdma(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmc(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xprda2_2_N1_to_roadma_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A2", "2", "1",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadma_PP2_to_xpdra2_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A2", "2", "1",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_07_connect_xprdc2_2_N1_to_roadmc_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C2", "2", "1",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmc_PP2_to_xpdrc2_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C2", "2", "1",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_09_add_omsAttributes_roadma_roadmc(self):
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

    def test_10_add_omsAttributes_roadmc_roadma(self):
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

# test service-create for OCH-OTU4 service from xpdra2 to xpdrc2
    def test_11_check_otn_topology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 4, 'There should be 4 nodes')
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_12_create_OTUC4_service(self):
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_13_get_OTUC4_service1(self):
        response = test_utils.get_service_list_request(
            "services/service1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1-OTUC4')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        self.assertEqual(
            res['services'][0]['service-layer'], 'wdm')
        self.assertEqual(
            res['services'][0]['service-a-end']['service-rate'], 400)
        self.assertEqual(
            res['services'][0]['service-a-end']['otu-service-rate'], 'org-openroadm-otn-common-types:OTUCn')
        time.sleep(2)
        self.assertEqual(
            res['services'][0]['service-z-end']['otu-service-rate'], 'org-openroadm-otn-common-types:OTUCn')
        time.sleep(2)

    # Check correct configuration of devices
    def test_14_check_interface_och_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-755:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-755:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                                   'type': 'org-openroadm-interfaces:otsi',
                                   'supporting-port': 'L1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            dict({u'frequency': 196.0812, u'otsi-rate': u'org-openroadm-common-optical-channel-types:R400G-otsi',
             u'transmit-power': -5, u'modulation-format': 'dp-qam16'},
                  **res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
            res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])

    def test_15_check_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-OTSI-GROUP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]
                                  ['org-openroadm-otsi-group-interfaces:otsi-group']),
                             res['interface'][0]
                             ['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_16_check_interface_OTUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSI-GROUP',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTSI-GROUP',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {'tx-sapi': 'LY9PxYJqUbw=',
                        'expected-dapi': 'LY9PxYJqUbw=',
                        'rate': 'org-openroadm-otn-common-types:OTUCn',
                        'degthr-percentage': 100,
                        'degm-intervals': 2,
                        'otucn-n-rate': 4
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_17_check_interface_och_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-NETWORK1-755:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-755:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                                   'type': 'org-openroadm-interfaces:otsi',
                                   'supporting-port': 'L1'
                                   }, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(
            dict({u'frequency': 196.0812, u'otsi-rate': u'org-openroadm-common-optical-channel-types:R400G-otsi',
             u'transmit-power': -5, u'modulation-format': 'dp-qam16'},
                  **res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
            res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])

    def test_18_check_interface_OTSI_GROUP_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-NETWORK1-OTSI-GROUP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSI-GROUP',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]
                                  ['org-openroadm-otsi-group-interfaces:otsi-group']),
                             res['interface'][0]
                             ['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_19_check_interface_OTUC4_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-NETWORK1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTSI-GROUP',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {'tx-dapi': 'LY9PxYJqUbw=',
                        'expected-sapi': 'LY9PxYJqUbw=',
                        'tx-sapi': 'Nmbu2MNHvc4=',
                        'expected-dapi': 'Nmbu2MNHvc4=',
                        'rate': 'org-openroadm-otn-common-types:OTUCn',
                        'degthr-percentage': 100,
                        'degm-intervals': 2,
                        'otucn-n-rate': 4
                        }

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])

        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             res['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_20_check_no_interface_ODUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_21_check_openroadm_topo_xpdra2(self):
        response = test_utils.get_ordm_topo_request("node/XPDR-A2-XPDR2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        ele = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', ele['tp-id'])
        self.assertEqual({u'frequency': 196.08125,
                          u'width': 75},
                         ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
        time.sleep(3)

    def test_22_check_otn_topo_OTUC4_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 2)
        listLinkId = ['OTUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                      'OTUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1']
        for link in res['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(
                link['transportpce-topology:otn-link-type'], 'OTUC4')
            self.assertEqual(
                link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 400000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(
                link['org-openroadm-common-network:opposite-link'], listLinkId)

# test service-create for ODU4 service from xpdra2 to xpdrc2
    def test_23_create_ODUC4_service(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1-ODUC4"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "ODU"
        del self.cr_serv_sample_data["input"]["service-a-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODUCn"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "ODU"
        del self.cr_serv_sample_data["input"]["service-z-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODUCn"

        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_24_get_ODUC4_service1(self):
        response = test_utils.get_service_list_request(
            "services/service1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1-ODUC4')
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        self.assertEqual(
            res['services'][0]['service-layer'], 'wdm')
        self.assertEqual(
            res['services'][0]['service-a-end']['service-rate'], 400)
        self.assertEqual(
            res['services'][0]['service-a-end']['odu-service-rate'], 'org-openroadm-otn-common-types:ODUCn')
        self.assertEqual(
            res['services'][0]['service-z-end']['odu-service-rate'], 'org-openroadm-otn-common-types:ODUCn')
        time.sleep(2)

    def test_25_check_interface_ODUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'expected-dapi': 'Nmbu2MNHvc4=',
                        'expected-sapi': 'Nmbu2MNHvc4=',
                        'tx-dapi': 'Nmbu2MNHvc4=',
                        'tx-sapi': 'LY9PxYJqUbw='}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'22', u'exp-payload-type': u'22'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_26_check_interface_ODUC4_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'tx-sapi': 'Nmbu2MNHvc4=',
                        'tx-dapi': 'LY9PxYJqUbw=',
                        'expected-sapi': 'LY9PxYJqUbw=',
                        'expected-dapi': 'LY9PxYJqUbw='
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'22', u'exp-payload-type': u'22'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_27_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('OTUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'OTUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 400000)
            elif (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                             'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 400000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
                self.assertEqual(
                    link['transportpce-topology:otn-link-type'], 'ODUC4')
                self.assertEqual(
                    link['org-openroadm-common-network:link-type'], 'OTN-LINK')
                self.assertIn(link['org-openroadm-common-network:opposite-link'],
                              ['ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                               'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1'])
            else:
                self.fail("this link should not exist")

    def test_28_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if node['node-id'] == 'XPDR-A2-XPDR2' or 'XPDR-C2-XPDR2':
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 4)
                        self.assertEqual(xpdrTpPortConAt['odtu-tpn-pool'][0]['odtu-type'],
                                         'org-openroadm-otn-common-types:ODTU4.ts-Allocated')

# test service-create for 100GE service from xpdra2 to xpdrc2
    def test_29_create_100GE_service(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-100GE"
        self.cr_serv_sample_data["input"]["connection-type"] = "service"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-rate"] = "100"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "Ethernet"
        del self.cr_serv_sample_data["input"]["service-a-end"]["odu-service-rate"]
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-rate"] = "100"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "Ethernet"
        del self.cr_serv_sample_data["input"]["service-z-end"]["odu-service-rate"]
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR2-CLIENT1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_30_get_100GE_service(self):
        response = test_utils.get_service_list_request(
            "services/service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-100GE')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_31_check_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-CLIENT1-ETHERNET100G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ETHERNET100G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'C1'
                      }
        input_dict_2 = {u'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
                             res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_32_check_interface_ODU4_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-CLIENT1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4-service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT1-ETHERNET100G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'07', u'exp-payload-type': u'07'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_33_check_interface_ODU4_NETWORK_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4-service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-ODUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'not-terminated'}
        input_dict_3 = {u'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn('1.1', res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('1.20', res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])

    def test_34_check_ODU4_connection_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2",
            "odu-connection/XPDR2-CLIENT1-ODU4-service-100GE-x-XPDR2-NETWORK1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-service-100GE-x-XPDR2-NETWORK1-ODU4-service-100GE',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR2-NETWORK1-ODU4-service-100GE'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR2-CLIENT1-ODU4-service-100GE'},
                             res['odu-connection'][0]['source'])

    def test_35_check_interface_100GE_CLIENT_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-CLIENT1-ETHERNET100G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ETHERNET100G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'C1'
                      }
        input_dict_2 = {u'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_36_check_interface_ODU4_CLIENT_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-CLIENT1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4-service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT1-ETHERNET100G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'07', u'exp-payload-type': u'07'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_37_check_interface_ODU4_NETWORK_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2", "interface/XPDR2-NETWORK1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4-service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-ODUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'not-terminated'}

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
        self.assertIn('1.1',
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])
        self.assertIn('1.20',
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])

    def test_38_check_ODU4_connection_xpdrc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C2",
            "odu-connection/XPDR2-CLIENT1-ODU4-service-100GE-x-XPDR2-NETWORK1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-service-100GE-x-XPDR2-NETWORK1-ODU4-service-100GE',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR2-NETWORK1-ODU4-service-100GE'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR2-CLIENT1-ODU4-service-100GE'},
                             res['odu-connection'][0]['source'])

    def test_39_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 300000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_40_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if node['node-id'] == 'XPDR-A2-XPDR2' or 'XPDR-C2-XPDR2':
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 60)
                        tsPoolList = list(range(1, 20))
                        self.assertNotIn(
                            tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 3)
                        self.assertNotIn(
                            1, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_41_delete_100GE_service(self):
        response = test_utils.service_delete_request("service-100GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_42_check_service_list(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 2)
        time.sleep(2)

    def test_43_check_no_ODU4_connection_xpdra2(self):
        response = test_utils.check_netconf_node_request("XPDR-A2", "")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn(['odu-connection'][0], res['org-openroadm-device'])
        time.sleep(1)

    def test_44_check_no_interface_ODU4_NETWORK_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_45_check_no_interface_ODU4_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-CLIENT1-ODU4-service-100GE")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_46_check_no_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-CLIENT1-ETHERNET100G")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_47_check_otn_topo_links(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nb_links = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nb_links, 4)
        for link in res['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 400000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_48_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if (node['node-id'] == 'XPDR-A2-XPDR2' or 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 4)

    def test_49_delete_ODUC4_service(self):
        response = test_utils.service_delete_request("service1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_50_check_service_list(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(len(res['service-list']['services']), 1)
        time.sleep(2)

    def test_51_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_52_check_otn_topo_links(self):
        self.test_22_check_otn_topo_OTUC4_links()

    def test_53_check_otn_topo_tp(self):
        response = test_utils.get_otn_topo_request()
        res = response.json()
        for node in res['network'][0]['node']:
            if node['node-id'] == 'XPDR-A2-XPDR2' or 'XPDR-C2-XPDR2':
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        self.assertNotIn('org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes',
                             dict.keys(tp))

    def test_54_delete_OTUC4_service(self):
        response = test_utils.service_delete_request("service1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_55_get_no_service(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])
        time.sleep(1)

    def test_56_check_no_interface_OTUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_57_check_no_interface_OCH_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-755:768")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_58_check_no_interface_OTSI_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR2-NETWORK1-OTSI-GROUP")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_59_getLinks_OtnTopology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_60_check_openroadm_topo_xpdra2(self):
        response = test_utils.get_ordm_topo_request("node/XPDR-A2-XPDR2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        tp = res['node'][0]['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp[u'org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(3)

    def test_61_check_openroadm_topology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        self.assertEqual(22, len(links), 'Topology should contain 22 links')

    def test_62_connect_xprda2_2_N1_to_roadma_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A2", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_63_connect_roadma_PP2_to_xpdra2_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A2", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_64_connect_xprdc2_2_N1_to_roadmc_PP2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C2", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_65_connect_roadmc_PP2_to_xpdrc2_2_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C2", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

# test service-create for 400GE service from xpdra2 to xpdrc2
    def test_66_create_400GE_service(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service-400GE"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-rate"] = "400"
        self.cr_serv_sample_data["input"]["service-a-end"]["tx-direction"]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_sample_data["input"]["service-a-end"]["rx-direction"]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-rate"] = "400"
        self.cr_serv_sample_data["input"]["service-z-end"]["tx-direction"]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_sample_data["input"]["service-z-end"]["rx-direction"]["port"]["port-name"] = "XPDR1-CLIENT1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_67_get_400GE_service(self):
        response = test_utils.get_service_list_request(
            "services/service-400GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service-400GE')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_68_check_xc1_roadma(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-755:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP1-TXRX-nmc-755:768'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG2-TTP-TXRX-nmc-755:768'},
            res['roadm-connections'][0]['destination'])
        time.sleep(5)

    def test_69_check_topo_xpdra2(self):
        response = test_utils.get_ordm_topo_request("node/XPDR-A2-XPDR1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual({u'frequency': 196.08125,
                                  u'width': 75},
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength'])
            if ele['tp-id'] == 'XPDR1-CLIENT1':
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
        time.sleep(3)

    def test_70_check_topo_roadma_SRG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(3)

    def test_71_check_topo_roadma_DEG1(self):
        response = test_utils.get_ordm_topo_request("node/ROADM-A1-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        freq_map = base64.b64decode(
            res['node'][0]['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
        liste_tp = res['node'][0]['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                    freq_map = base64.b64decode(
                        ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                    freq_map_array = [int(x) for x in freq_map]
                    self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
        time.sleep(3)


    def test_72_check_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ETHERNET',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': '1/1/1-PLUG-CLIENT',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'C1'
                      }
        input_dict_2 = {u'speed': 400000}
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
                             res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_73_check_interface_OTSI_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-755:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-755:768',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        'type': 'org-openroadm-interfaces:otsi',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            "frequency": 196.0812,
            "otsi-rate": "org-openroadm-common-optical-channel-types:R400G-otsi",
            "fec": "org-openroadm-common-types:ofec",
            "transmit-power": -5,
            "provision-mode": "explicit",
            "modulation-format": "dp-qam16"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
                             res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic4.8", "iid": [1, 2, 3, 4]},
                             res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])

    def test_74_check_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTSI-GROUP")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             res['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_75_check_interface_OTUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-OTSI-GROUP',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"tx-sapi": "AIGiVAQ4gDil", "rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 4,
                        "degm-intervals": 2,
                        "expected-dapi": "AIGiVAQ4gDil"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_76_check_interface_ODUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-OTSI-GROUP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP",
                        "tim-detect-mode": "Disabled",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "oducn-n-rate": 4}
        input_dict_3 = {"exp-payload-type": "22", "payload-type": "22"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_77_delete_400GE_service(self):
        response = test_utils.service_delete_request("service-400GE")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_78_get_no_service(self):
        response = test_utils.get_service_list_request("")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])
        time.sleep(1)

    def test_79_check_no_interface_ODUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-ODUC4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_80_check_no_interface_OTUC4_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTUC4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_81_check_no_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTSI-GROUP")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_82_check_no_interface_OTSI_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-755:768")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_83_check_no_interface_400GE_CLIENT_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_84_disconnect_xponders_from_roadm(self):
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

    def test_85_disconnect_xpdra2(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_86_disconnect_xpdrc2(self):
        response = test_utils.unmount_device("XPDR-C2")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_87_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_88_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
