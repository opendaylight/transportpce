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
        "service-name": "service1-openroadm",
        "common-id": "commonId",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "ROADM-A1",
            "service-format": "OTU",
            "clli": "clli_1",
            "frequency-slot": {
                "center-frequency": 193.1,
                "slot-width": 37.5
            },
            "tx-direction": [{
                "port": {
                    "port-device-name": "ROADM-A1-SRG3",
                    "port-type": "",
                    "port-name": "",
                    "port-rack": "",
                    "port-shelf": ""
                },
                "lgx": {
                    "lgx-device-name": "",
                    "lgx-port-name": "",
                    "lgx-port-rack": "",
                    "lgx-port-shelf": ""
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "ROADM-A1-SRG3",
                    "port-type": "",
                    "port-name": "",
                    "port-rack": "",
                    "port-shelf": ""
                },
                "lgx": {
                    "lgx-device-name": "",
                    "lgx-port-name": "",
                    "lgx-port-rack": "",
                    "lgx-port-shelf": ""
                },
                "index": 0
            }],
            "optic-type": "dwdm",
            "other-service-format-and-rate": "10G-ZR::10GbE"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "ROADM-C1",
            "service-format": "OTU",
            "clli": "clli_1",
            "tx-direction": [{
                "port": {
                    "port-device-name": "ROADM-C1-SRG1",
                    "port-type": "",
                    "port-name": "",
                    "port-rack": "",
                    "port-shelf": ""
                },
                "lgx": {
                    "lgx-device-name": "",
                    "lgx-port-name": "",
                    "lgx-port-rack": "",
                    "lgx-port-shelf": ""
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "ROADM-C1-SRG1",
                    "port-type": "",
                    "port-name": "",
                    "port-rack": "",
                    "port-shelf": ""
                },
                "lgx": {
                    "lgx-device-name": "",
                    "lgx-port-name": "",
                    "lgx-port-rack": "",
                    "lgx-port-shelf": ""
                },
                "index": 0
            }],
            "optic-type": "dwdm",
            "other-service-format-and-rate": "10G-ZR::10GbE"
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

    def test_11_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 13)

    def test_12_create_otu_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_13_get_otu_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-openroadm")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_14_create_other_service(self):
        self.cr_serv_input_data["service-a-end"]["service-format"] = "other"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "other"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_15_get_other_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-openroadm")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-openroadm')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    # Check correct configuration of devices
    def test_16_check_interface_roadma1_deg2_mc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'DEG2-TTP-TXRX-mc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                "name": "DEG2-TTP-TXRX-mc-282:287",
                "supporting-circuit-pack-name": "2/0",
                "operational-state": "inService",
                "administrative-state": "inService",
                "type": "org-openroadm-interfaces:mediaChannelTrailTerminationPoint",
                "circuit-id": "   TBD    ",
                "org-openroadm-media-channel-interfaces:mc-ttp": {
                    "max-freq": "193.11875",
                    "min-freq": "193.08125"
                },
                "supporting-interface": "OMS-DEG2-TTP-TXRX",
                "supporting-port": "L1",
                "description": "  TBD   "
            },
            response['interface'][0]
        )

    def test_17_check_interface_roadm_a1_deg2_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'DEG2-TTP-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                'name': 'DEG2-TTP-TXRX-nmc-282:287',
                'supporting-circuit-pack-name': '2/0',
                'operational-state': 'inService',
                'administrative-state': 'inService',
                'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                'circuit-id': '   TBD    ',
                'supporting-interface': 'DEG2-TTP-TXRX-mc-282:287',
                'supporting-port': 'L1',
                'description': '  TBD   ',
                'org-openroadm-network-media-channel-interfaces:nmc-ctp': {
                    'frequency': '193.1',
                    'width': '29.5'
                }
            },
            response['interface'][0]
        )

    def test_18_check_interface_roadm_a1_srg3_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'SRG3-PP1-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                "name": "SRG3-PP1-TXRX-nmc-282:287",
                "supporting-circuit-pack-name": "5/0",
                "operational-state": "inService",
                "administrative-state": "inService",
                "type": "org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint",
                "circuit-id": "   TBD    ",
                "supporting-port": "C1",
                "description": "  TBD   ",
                "org-openroadm-network-media-channel-interfaces:nmc-ctp": {
                    "frequency": "193.1",
                    "width": "29.5"
                }
            },
            response['interface'][0]
        )

    def test_19_check_interface_roadm_c1_deg1_mc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'DEG1-TTP-TXRX-mc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                'name': 'DEG1-TTP-TXRX-mc-282:287',
                'supporting-circuit-pack-name': '1/0',
                'operational-state': 'inService',
                'administrative-state': 'inService',
                'type': 'org-openroadm-interfaces:mediaChannelTrailTerminationPoint',
                'circuit-id': '   TBD    ',
                'org-openroadm-media-channel-interfaces:mc-ttp': {
                    'max-freq': '193.11875',
                    'min-freq': '193.08125'
                },
                'supporting-interface': 'OMS-DEG1-TTP-TXRX',
                'supporting-port': 'L1',
                'description': '  TBD   '
            },
            response['interface'][0]
        )

    def test_20_check_interface_roadm_c1_deg1_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'DEG1-TTP-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                'name': 'DEG1-TTP-TXRX-nmc-282:287',
                'supporting-circuit-pack-name': '1/0',
                'operational-state': 'inService',
                'administrative-state': 'inService',
                'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                'circuit-id': '   TBD    ',
                'supporting-interface': 'DEG1-TTP-TXRX-mc-282:287',
                'supporting-port': 'L1',
                'description': '  TBD   ',
                'org-openroadm-network-media-channel-interfaces:nmc-ctp': {
                    'frequency': '193.1',
                    'width': '29.5'
                }
            },
            response['interface'][0]
        )

    def test_21_check_interface_roadm_c1_srg1_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'SRG1-PP1-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            {
                'name': 'SRG1-PP1-TXRX-nmc-282:287',
                'supporting-circuit-pack-name': '3/0',
                'operational-state': 'inService',
                'administrative-state': 'inService',
                'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                'circuit-id': '   TBD    ',
                'supporting-port': 'C1',
                'description': '  TBD   ',
                'org-openroadm-network-media-channel-interfaces:nmc-ctp': {
                    'frequency': '193.1',
                    'width': '29.5'
                }
            },
            response['interface'][0]
        )

    def test_22_check_openroadm_topo_ROADM_A1_SRG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG3', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][3]['tp-id'], 'SRG3-CP-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][4]['tp-id'], 'SRG3-PP1-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG3-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] == 'SRG3-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] != 'SRG3-PP1-TXRX' and ele['tp-id'] != 'SRG3-CP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_23_check_openroadm_topo_ROADM_A1_DEG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][0]['tp-id'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'DEG2-CTP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] != 'DEG2-TTP-TXRX' and ele['tp-id'] != 'DEG2-CTP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_24_check_openroadm_topo_ROADM_C1_DEG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-C1-DEG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][0]['tp-id'], 'DEG1-CTP-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'DEG1-TTP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] != 'DEG1-CTP-TXRX' and ele['tp-id'] != 'DEG1-TTP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_25_check_openroadm_topo_ROADM_C1_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-C1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'SRG1-PP1-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][4]['tp-id'], 'SRG1-CP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] == 'SRG1-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 129, "Bits 282 to 287 should not be available")
            if ele['tp-id'] != 'SRG1-PP1-TXRX' and ele['tp-id'] != 'SRG1-CP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_26_check_openroadm_topology_links(self):
        #response = test_utils.get_ietf_network_request('otn-topology', 'config')
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 22)
        listLinkId = ['ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                      'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                      'ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                      'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX',
                      'ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX',
                      'ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX',
                      'ROADM-A1-SRG1-SRG1-PP1-TXRXtoSPDR-SA1-XPDR1-XPDR1-NETWORK1',
                      'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                      'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX',
                      'SPDR-SA1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX',
                      'ROADM-C1-SRG1-SRG1-PP1-TXRXtoSPDR-SC1-XPDR1-XPDR1-NETWORK1',
                      'SPDR-SC1-XPDR1-XPDR1-NETWORK1toROADM-C1-SRG1-SRG1-PP1-TXRX',
                      'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                      'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX',
                      'ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX',
                      'ROADM-A1-DEG2-DEG2-CTP-TXRXtoROADM-A1-SRG3-SRG3-CP-TXRX',
                      'ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                      'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG2-DEG2-CTP-TXRX',
                      'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-SRG1-SRG1-CP-TXRX',
                      'ROADM-C1-SRG1-SRG1-CP-TXRXtoROADM-C1-DEG1-DEG1-CTP-TXRX',
                      'ROADM-A1-DEG1-DEG1-CTP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX',
                      'ROADM-A1-SRG3-SRG3-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX']
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertIn(
                link['org-openroadm-common-network:opposite-link'], listLinkId)

    def test_27_delete_openroadm_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-openroadm"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_28_get_no_service(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_29_check_no_interface_roadm_a1_deg2_mc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'DEG2-TTP-TXRX-mc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_30_check_no_interface_roadm_a1_deg2_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'DEG2-TTP-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_31_check_no_interface_roadm_a1_srg3_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-A1', 'interface', 'SRG3-PP1-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_32_check_no_interface_roadm_c1_deg1_mc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'DEG1-TTP-TXRX-mc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_33_check_no_interface_roadm_c1_deg1_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'DEG1-TTP-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_34_check_no_interface_roadm_c1_srg1_nmc(self):
        response = test_utils.check_node_attribute_request(
            'ROADM-C1', 'interface', 'SRG1-PP1-TXRX-nmc-282:287')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_35_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG3', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG3-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] == 'SRG3-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] != 'SRG3-PP1-TXRX' and ele['tp-id'] != 'SRG3-CP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_36_check_openroadm_topo_ROADMA_DEG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][0]['tp-id'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'DEG2-CTP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG2-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] == 'DEG2-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")

    def test_37_check_openroadm_topo_ROADM_C1_DEG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-C1-DEG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][0]['tp-id'], 'DEG1-CTP-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'DEG1-TTP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'DEG1-CTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:ctp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] == 'DEG1-TTP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:tx-ttp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] != 'DEG1-CTP-TXRX' and ele['tp-id'] != 'DEG1-TTP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_38_check_openroadm_topo_ROADM_C1_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-C1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][1]['tp-id'], 'SRG1-PP1-TXRX')
        self.assertEqual(response['node']['ietf-network-topology:termination-point'][4]['tp-id'], 'SRG1-CP-TXRX')
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] == 'SRG1-CP-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:cp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[35], 255, "Bits 282 to 287 should be available")
            if ele['tp-id'] != 'SRG1-PP1-TXRX' and ele['tp-id'] != 'SRG1-CP-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))

    def test_39_getLinks_OtnTopology(self):
        #response = test_utils.get_ietf_network_request('otn-topology', 'config')
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 13)

    def test_40_check_openroadm_topo_spdra(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SA1-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        tp = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR1-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp['org-openroadm-network-topology:xpdr-network-attributes']))

    def test_41_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(22,
                         len(response['network'][0]['ietf-network-topology:link']),
                         'Topology should contain 22 links')

    def test_42_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_43_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_44_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_45_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
