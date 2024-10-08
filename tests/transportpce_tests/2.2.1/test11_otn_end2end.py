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
# pylint: disable=too-many-lines

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


class TransportPCEtesting(unittest.TestCase):

    processes = None
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    cr_serv_input_data = {
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
            "tx-direction": [{
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "SPDR-SC1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSC",
            "tx-direction": [{
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "due-date": "2018-06-15T00:00:01Z",
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

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    def test_01_connect_spdrA(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_spdrC(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_sprdA_1_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_06_connect_roadmA_PP1_to_spdrA_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_07_connect_sprdC_1_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_08_connect_roadmC_PP1_to_spdrC_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

# test service-create for OCH-OTU4 service from spdr to spdr
    def test_11_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 7)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_12_create_OCH_OTU4_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_13_get_OCH_OTU4_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-OCH-OTU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-OCH-OTU4')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    # Check correct configuration of devices
    def test_14_check_interface_och_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-761:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual('org-openroadm-common-types:R100G',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['rate'])
        self.assertEqual('dp-qpsk',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['modulation-format'])
        self.assertEqual(196.1,
                         float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['frequency']))
        self.assertEqual(
            -5,
            float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['transmit-power']))

    def test_15_check_interface_OTU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-sapi': 'H/OelLynehI=',
                        'expected-dapi': 'H/OelLynehI=',
                        'tx-dapi': 'AMf1n5hK6Xkk',
                        'expected-sapi': 'AMf1n5hK6Xkk',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_16_check_interface_och_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-NETWORK1-761:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR1-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP1-CFP0',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual('org-openroadm-common-types:R100G',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['rate'])
        self.assertEqual('dp-qpsk',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['modulation-format'])
        self.assertEqual(196.1,
                         float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['frequency']))
        self.assertEqual(
            -5,
            float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['transmit-power']))

    def test_17_check_interface_OTU4_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }
        input_dict_2 = {'tx-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'H/OelLynehI=',
                        'tx-sapi': 'AMf1n5hK6Xkk',
                        'expected-dapi': 'AMf1n5hK6Xkk',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_18_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_19_check_openroadm_topo_spdra(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SA1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        ele = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR1-NETWORK1', ele['tp-id'])
        self.assertEqual(
            196.1,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
        self.assertEqual(
            40,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))

    def test_20_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_21_check_openroadm_topo_ROADMA_DEG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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

    def test_22_check_otn_topo_otu4_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 2)
        listLinkId = ['OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                      'OTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1']
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(
                link['transportpce-networkutils:otn-link-type'], 'OTU4')
            self.assertEqual(
                link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(
                link['org-openroadm-common-network:opposite-link'], listLinkId)

# test service-create for ODU4 service from spdr to spdr
    def test_23_create_ODU4_service(self):
        self.cr_serv_input_data["service-name"] = "service1-ODU4"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "ODU"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "ODU"
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"

        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_24_get_ODU4_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-ODU4')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_25_check_interface_ODU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'monitoring-mode': 'terminated',
                        'expected-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'AMf1n5hK6Xkk',
                        'tx-dapi': 'AMf1n5hK6Xkk',
                        'tx-sapi': 'H/OelLynehI='}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_26_check_interface_ODU4_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4',
                        'monitoring-mode': 'terminated',
                        'tx-sapi': 'AMf1n5hK6Xkk',
                        'tx-dapi': 'H/OelLynehI=',
                        'expected-sapi': 'H/OelLynehI=',
                        'expected-dapi': 'AMf1n5hK6Xkk'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_27_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                           'OTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)
            elif (linkId in ('ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                             'ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
                self.assertEqual(
                    link['transportpce-networkutils:otn-link-type'], 'ODTU4')
                self.assertEqual(
                    link['org-openroadm-common-network:link-type'], 'OTN-LINK')
                self.assertIn(link['org-openroadm-common-network:opposite-link'],
                              ['ODTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                               'ODTU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1'])
            else:
                self.fail("this link should not exist")

    def test_28_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR1', 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR1-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 80)
                        self.assertEqual(xpdrTpPortConAt['odtu-tpn-pool'][0]['odtu-type'],
                                         'org-openroadm-otn-common-types:ODTU4.ts-Allocated')

# test service-create for 10GE service from spdr to spdr
    def test_29_create_10GE_service(self):
        self.cr_serv_input_data["service-name"] = "service1-10GE"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "10"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        del self.cr_serv_input_data["service-a-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["ethernet-encoding"] = "10GBASE-R"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "10"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        del self.cr_serv_input_data["service-z-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["ethernet-encoding"] = "10GBASE-R"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_30_get_10GE_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-10GE")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-10GE')
        self.assertEqual(
            response['services'][0]['connection-type'], 'service')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_31_check_interface_10GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-CLIENT1-ETHERNET10G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(
            10000,
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])

    def test_32_check_interface_ODU2E_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-CLIENT1-ODU2e:service1-10GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e:service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '03', 'exp-payload-type': '03'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_33_check_interface_ODU2E_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-ODU2e:service1-10GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e:service1-10GE',
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn(1, response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['trib-slots'])

    def test_34_check_ODU2E_connection_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'odu-connection', 'XPDR1-CLIENT1-ODU2e-x-XPDR1-NETWORK1-ODU2e')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-x-XPDR1-NETWORK1-ODU2e',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU2e:service1-10GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT1-ODU2e:service1-10GE'},
                             response['odu-connection'][0]['source'])

    def test_35_check_interface_10GE_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-CLIENT1-ETHERNET10G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(
            10000,
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])

    def test_36_check_interface_ODU2E_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-CLIENT1-ODU2e:service1-10GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e:service1-10GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '03', 'exp-payload-type': '03'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_37_check_interface_ODU2E_NETWORK_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR1-NETWORK1-ODU2e:service1-10GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e:service1-10GE',
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_38_check_ODU2E_connection_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'odu-connection', 'XPDR1-CLIENT1-ODU2e-x-XPDR1-NETWORK1-ODU2e')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT1-ODU2e-x-XPDR1-NETWORK1-ODU2e',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU2e:service1-10GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT1-ODU2e:service1-10GE'},
                             response['odu-connection'][0]['source'])

    def test_39_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                           'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 90000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 10000)

    def test_40_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR1', 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR1-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 72)
                        tsPoolList = list(range(1, 9))
                        self.assertNotIn(
                            tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 79)
                        self.assertNotIn(
                            1, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_41_delete_10GE_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-10GE"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_42_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 2)

    def test_43_check_no_ODU2e_connection_spdra(self):
        response = test_utils.check_node_request("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn(['odu-connection'][0], response['org-openroadm-device'])

    def test_44_check_no_interface_ODU2E_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-ODU2e-service1')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_45_check_no_interface_ODU2E_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-CLIENT1-ODU2e-service1')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_46_check_no_interface_10GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-CLIENT1-ETHERNET10G')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_47_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                           'ODU4-SPDR-SC1-XPDR1-XPDR1-NETWORK1toSPDR-SA1-XPDR1-XPDR1-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_48_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR1', 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR1-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 80)

    def test_49_delete_ODU4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-ODU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_50_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 1)

    def test_51_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_52_check_otn_topo_links(self):
        self.test_22_check_otn_topo_otu4_links()

    def test_53_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR1', 'SPDR-SC1-XPDR1'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR1-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertNotIn('ts-pool', dict.keys(xpdrTpPortConAt))
                        self.assertNotIn(
                            'odtu-tpn-pool', dict.keys(xpdrTpPortConAt))

    def test_54_delete_OCH_OTU4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-OCH-OTU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_55_get_no_service(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_56_check_no_interface_OTU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_57_check_no_interface_OCH_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR1-NETWORK1-1')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_58_getLinks_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_59_check_openroadm_topo_spdra(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SA1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        tp = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR1-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp['org-openroadm-network-topology:xpdr-network-attributes']))

    def test_60_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        time.sleep(3)

    def test_61_check_openroadm_topo_ROADMA_DEG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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

    def test_62_connect_sprdA_3_N1_to_roadmA_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_63_connect_roadmA_PP2_to_spdrA_3_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_64_connect_sprdC_3_N1_to_roadmC_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_65_connect_roadmC_PP2_to_spdrC_3_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)

    def test_66_create_OCH_OTU4_service_2(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["service-name"] = "service2-OCH-OTU4"
        self.cr_serv_input_data["connection-type"] = "infrastructure"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "OTU"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "OTU"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_67_get_OCH_OTU4_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service2-OCH-OTU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service2-OCH-OTU4')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_68_create_ODU4_service_2(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["service-name"] = "service2-ODU4"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "ODU"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["service-format"] = "ODU"
        self.cr_serv_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-NETWORK1"
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_69_get_ODU4_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service2-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service2-ODU4')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_70_create_1GE_service(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["service-name"] = "service1-1GE"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "1"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        del self.cr_serv_input_data["service-a-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-CLIENT1"
        del self.cr_serv_input_data["service-z-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR3-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR3-CLIENT1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_71_get_1GE_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-1GE")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-1GE')
        self.assertEqual(
            response['services'][0]['connection-type'], 'service')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_72_check_interface_1GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-CLIENT1-ETHERNET1G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR3-CLIENT1-ETHERNET1G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(
            1000,
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])

    def test_73_check_interface_ODU0_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-CLIENT1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR3-CLIENT1-ODU0:service1-1GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP3-SFP1',
                        'supporting-interface': 'XPDR3-CLIENT1-ETHERNET1G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP3-SFP1-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU0',
            'monitoring-mode': 'terminated'}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_74_check_interface_ODU0_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-NETWORK1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR3-NETWORK1-ODU0:service1-1GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP3-CFP0',
                        'supporting-interface': 'XPDR3-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP3-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU0',
            'monitoring-mode': 'monitored'}
        input_dict_3 = {'trib-port-number': 1}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn(1, response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['trib-slots'])

    def test_75_check_ODU0_connection_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'odu-connection', 'XPDR3-CLIENT1-ODU0-x-XPDR3-NETWORK1-ODU0')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR3-CLIENT1-ODU0-x-XPDR3-NETWORK1-ODU0',
            'direction': 'bidirectional'
        }
        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR3-NETWORK1-ODU0:service1-1GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR3-CLIENT1-ODU0:service1-1GE'},
                             response['odu-connection'][0]['source'])

    def test_76_check_interface_1GE_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR3-CLIENT1-ETHERNET1G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR3-CLIENT1-ETHERNET1G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP3-SFP1',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP3-SFP1-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(
            1000,
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])

    def test_77_check_interface_ODU0_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR3-CLIENT1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR3-CLIENT1-ODU0:service1-1GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP3-SFP1',
                        'supporting-interface': 'XPDR3-CLIENT1-ETHERNET1G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP3-SFP1-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU0',
            'monitoring-mode': 'terminated'}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_78_check_interface_ODU0_NETWORK_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR3-NETWORK1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR3-NETWORK1-ODU0:service1-1GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP3-CFP0',
                        'supporting-interface': 'XPDR3-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP3-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU0',
            'monitoring-mode': 'monitored'}
        input_dict_3 = {'trib-port-number': 1}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_79_check_ODU0_connection_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'odu-connection', 'XPDR3-CLIENT1-ODU0-x-XPDR3-NETWORK1-ODU0')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR3-CLIENT1-ODU0-x-XPDR3-NETWORK1-ODU0',
            'direction': 'bidirectional'
        }
        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR3-NETWORK1-ODU0:service1-1GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR3-CLIENT1-ODU0:service1-1GE'},
                             response['odu-connection'][0]['source'])

    def test_80_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODU4-SPDR-SA1-XPDR3-XPDR3-NETWORK1toSPDR-SC1-XPDR3-XPDR3-NETWORK1',
                           'ODU4-SPDR-SC1-XPDR3-XPDR3-NETWORK1toSPDR-SA1-XPDR3-XPDR3-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 99000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 1000)

    def test_81_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR3', 'SPDR-SC1-XPDR3'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR3-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 79)
                        tsPoolList = list(range(1, 2))
                        self.assertNotIn(
                            tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 79)
                        self.assertNotIn(
                            1, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_82_delete_1GE_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-1GE"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_83_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 2)

    def test_84_check_no_ODU0_connection_spdra(self):
        response = test_utils.check_node_request("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn(['odu-connection'][0], response['org-openroadm-device'])

    def test_85_check_no_interface_ODU0_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-NETWORK1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_86_check_no_interface_ODU0_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-CLIENT1-ODU0:service1-1GE')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_87_check_no_interface_10GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR3-CLIENT1-ETHERNET1G')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_88_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODU4-SPDR-SA1-XPDR3-XPDR3-NETWORK1toSPDR-SC1-XPDR3-XPDR3-NETWORK1',
                           'ODU4-SPDR-SC1-XPDR3-XPDR3-NETWORK1toSPDR-SA1-XPDR3-XPDR3-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_89_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('SPDR-SA1-XPDR3', 'SPDR-SC1-XPDR3'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR3-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 80)

    def test_90_delete_ODU4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service2-ODU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_91_delete_OCH_OTU4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service2-OCH-OTU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_92_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_93_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(18,
                         len(response['network'][0]['ietf-network-topology:link']),
                         'Topology should contain 18 links')

    def test_94_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_95_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_96_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_97_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
