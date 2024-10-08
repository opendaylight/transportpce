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
        "service-name": "service-OCH-OTU4-AB",
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "SPDR-SB1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSB",
            "tx-direction": [{
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
        time.sleep(2)

    def test_001_connect_spdrA(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_002_connect_spdrB(self):
        response = test_utils.mount_device("SPDR-SB1", ('spdrb', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_003_connect_spdrC(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_004_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_005_connect_rdmB(self):
        response = test_utils.mount_device("ROADM-B1", ('roadmb', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_006_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_007_connect_sprdA_2_N1_to_roadmA_PP3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_008_connect_roadmA_PP3_to_spdrA_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_009_connect_sprdC_2_N1_to_roadmC_PP3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_010_connect_roadmC_PP3_to_spdrC_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_011_connect_sprdB_2_N1_to_roadmB_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SB1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-B1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_012_connect_roadmB_PP1_to_spdrB_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SB1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-B1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_013_connect_sprdB_2_N2_to_roadmB_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SB1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-B1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_014_connect_roadmB_PP2_to_spdrB_2_N2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SB1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-B1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_015_add_omsAttributes_ROADMA_ROADMB(self):
        # Config ROADMA-ROADMB oms-attributes
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
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_016_add_omsAttributes_ROADMB_ROADMA(self):
        # Config ROADMB-ROADMA oms-attributes
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
        response = test_utils.add_oms_attr_request("ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_017_add_omsAttributes_ROADMB_ROADMC(self):
        # Config ROADMB-ROADMC oms-attributes
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
        response = test_utils.add_oms_attr_request("ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_018_add_omsAttributes_ROADMC_ROADMB(self):
        # Config ROADMC-ROADMB oms-attributes
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
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX",
                                                   data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_019_create_OTS_ROADMA_DEG1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-A1',
                'logical-connection-point': 'DEG1-TTP-TXRX'
            })
        # time.sleep(10)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-A1',
                      response["output"]["result"])

    def test_020_create_OTS_ROADMB_DEG1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-B1',
                'logical-connection-point': 'DEG1-TTP-TXRX'
            })
        # time.sleep(10)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-B1',
                      response["output"]["result"])

    def test_021_create_OTS_ROADMB_DEG2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-B1',
                'logical-connection-point': 'DEG2-TTP-TXRX'
            })
        # time.sleep(10)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-B1',
                      response["output"]["result"])

    def test_022_create_OTS_ROADMC_DEG2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-C1',
                'logical-connection-point': 'DEG2-TTP-TXRX'
            })
        # time.sleep(10)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-C1',
                      response["output"]["result"])

    def test_023_calculate_span_loss_base_all(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-olm', 'calculate-spanloss-base',
            {
                'src-type': 'all'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Success', response["output"]["result"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        time.sleep(5)

    def test_024_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 10, 'There should be 10 nodes')
        self.assertNotIn('ietf-network-topology:link', response['network'][0],
                         'otn-topology should have no link')

# test service-create for OCH-OTU4 service from spdrA to spdrB
    def test_025_create_OCH_OTU4_service_AB(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_026_get_OCH_OTU4_service_AB(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-OCH-OTU4-AB")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-OCH-OTU4-AB')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# Check correct configuration of devices
    def test_027_check_interface_och_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-761:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
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

    def test_028_check_interface_OTU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-sapi': 'AOQxIv+6nCD+',
                        'expected-dapi': 'AOQxIv+6nCD+',
                        'tx-dapi': 'X+8cRNi+HbE=',
                        'expected-sapi': 'X+8cRNi+HbE=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK1-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_029_check_interface_och_spdrB(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK1-761:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-761:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
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

    def test_030_check_interface_OTU4_spdrB(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-dapi': 'AOQxIv+6nCD+',
                        'expected-sapi': 'AOQxIv+6nCD+',
                        'tx-sapi': 'X+8cRNi+HbE=',
                        'expected-dapi': 'X+8cRNi+HbE=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_031_check_no_interface_ODU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['interface'], (
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

    def test_032_check_openroadm_topo_spdra(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SA1-XPDR2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        ele = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', ele['tp-id'])
        self.assertEqual(
            196.1,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
        self.assertEqual(
            40,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
        self.assertEqual('ROADM-A1-SRG1--SRG1-PP3-TXRX',
                         ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
        time.sleep(1)

    def test_033_check_openroadm_topo_ROADMA_SRG(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP3-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(1)

    def test_034_check_openroadm_topo_ROADMA_DEG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_035_check_otn_topo_otu4_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 2)
        listLinkId = ['OTU4-SPDR-SA1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK1toSPDR-SA1-XPDR2-XPDR2-NETWORK1']
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


# test service-create for OCH-OTU4 service from spdrB to spdrC

    def test_036_create_OCH_OTU4_service_BC(self):
        self.cr_serv_input_data["service-name"] = "service-OCH-OTU4-BC"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "SPDR-SB1"
        self.cr_serv_input_data["service-a-end"]["clli"] = "NodeSB"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SB1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SB1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK2"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_input_data["service-z-end"]["clli"] = "NodeSC"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_037_get_OCH_OTU4_service_BC(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-OCH-OTU4-BC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-OCH-OTU4-BC')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# Check correct configuration of devices
    def test_038_check_interface_och_spdrB(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK2-753:760')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-753:760',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP6-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP1-CFP0-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])

        self.assertEqual('org-openroadm-common-types:R100G',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['rate'])
        self.assertEqual('dp-qpsk',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['modulation-format'])
        self.assertEqual(196.05,
                         float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['frequency']))
        self.assertEqual(
            -5,
            float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['transmit-power']))

    def test_039_check_interface_OTU4_spdrB(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK2-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK2-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP6-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-753:760',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP6-CFP-P1'
                        }
        input_dict_2 = {'tx-sapi': 'X+8cRNi+HbI=',
                        'expected-dapi': 'X+8cRNi+HbI=',
                        'tx-dapi': 'ALvne1QI5jo4',
                        'expected-sapi': 'ALvne1QI5jo4',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_040_check_interface_och_spdrC(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-753:760')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-753:760',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': 'CP5-CFP',
                                   'type': 'org-openroadm-interfaces:opticalChannel',
                                   'supporting-port': 'CP5-CFP-P1'
                                   }, **response['interface'][0]),
                             response['interface'][0])

        self.assertEqual('org-openroadm-common-types:R100G',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['rate'])
        self.assertEqual('dp-qpsk',
                         response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['modulation-format'])
        self.assertEqual(196.05,
                         float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['frequency']))
        self.assertEqual(
            -5,
            float(response['interface'][0]['org-openroadm-optical-channel-interfaces:och']['transmit-power']))

    def test_041_check_interface_OTU4_spdrC(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-753:760',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-dapi': 'X+8cRNi+HbI=',
                        'expected-sapi': 'X+8cRNi+HbI=',
                        'tx-sapi': 'ALvne1QI5jo4',
                        'expected-dapi': 'ALvne1QI5jo4',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK2-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_042_check_no_interface_ODU4_spdrB(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['interface'], (
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

    def test_043_check_openroadm_topo_spdrB(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SB1-XPDR2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_tp = response['node']['ietf-network-topology:termination-point']
        # pylint: disable=consider-using-f-string
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR2-NETWORK1':
                self.assertEqual(
                    196.1,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
                self.assertEqual(
                    40,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
                self.assertEqual('ROADM-B1-SRG1--SRG1-PP1-TXRX',
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
            elif ele['tp-id'] == 'XPDR2-NETWORK2':
                self.assertEqual(
                    196.05,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
                self.assertEqual(
                    40,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
                self.assertEqual('ROADM-B1-SRG1--SRG1-PP2-TXRX',
                                 ele['org-openroadm-network-topology:xpdr-network-attributes']['tail-equipment-id'])
            else:
                self.assertNotIn('org-openroadm-network-topology:xpdr-network-attributes', dict.keys(ele))
        time.sleep(1)

    def test_044_check_openroadm_topo_ROADMB_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-B1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Lambda 1 should not be available")
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        self.assertEqual(freq_map_array[93], 255, "Lambda 3 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_045_check_openroadm_topo_ROADMB_DEG2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-B1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[94], 0, "Lambda 2 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_046_check_otn_topo_otu4_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        listLinkId = ['OTU4-SPDR-SA1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK1toSPDR-SA1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SB1-XPDR2-XPDR2-NETWORK2toSPDR-SC1-XPDR2-XPDR2-NETWORK1',
                      'OTU4-SPDR-SC1-XPDR2-XPDR2-NETWORK1toSPDR-SB1-XPDR2-XPDR2-NETWORK2']
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


# test service-create for 100GE service from spdrA to spdrC via spdrB


    def test_047_create_100GE_service_ABC(self):
        self.cr_serv_input_data["service-name"] = "service-100GE-ABC"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "SPDR-SA1"
        self.cr_serv_input_data["service-a-end"]["clli"] = "NodeSA"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_input_data["service-z-end"]["clli"] = "NodeSC"
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_048_get_100GE_service_ABC(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-100GE-ABC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-100GE-ABC')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_049_check_interface_100GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-CLIENT1-ETHERNET')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR2-CLIENT1-ETHERNET',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP2-QSFP1',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP2-QSFP1-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(100000, response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])
        self.assertEqual('off', response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['fec'])

    def test_050_check_interface_ODU4_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-CLIENT1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP2-QSFP1',
                        'supporting-interface': 'XPDR2-CLIENT1-ETHERNET',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP2-QSFP1-P1'}
        # SAPI/DAPI are added in the Otu4 renderer
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated',
            'expected-dapi': 'AItaZ6nmyaKJ',
            'expected-sapi': 'AKFnJJaijWiz',
            'tx-dapi': 'AKFnJJaijWiz',
            'tx-sapi': 'AItaZ6nmyaKJ'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

        response2 = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-CLIENT1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['expected-dapi'])

    def test_051_check_interface_ODU4_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertNotIn('opu',
                         dict.keys(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_052_check_ODU4_connection_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'odu-connection', 'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4'},
                             response['odu-connection'][0]['source'])

    def test_053_check_interface_100GE_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-CLIENT1-ETHERNET')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR2-CLIENT1-ETHERNET',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP2-QSFP1',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP2-QSFP1-P1'
                      }
        self.assertDictEqual(dict(input_dict, **response['interface'][0]),
                             response['interface'][0])
        self.assertEqual(100000, response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])
        self.assertEqual('off', response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['fec'])

    def test_054_check_interface_ODU4_CLIENT_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-CLIENT1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

        response2 = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-CLIENT1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'],
                         response2['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['expected-dapi'])

    def test_055_check_interface_ODU4_NETWORK_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertNotIn('opu',
                         dict.keys(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_056_check_ODU4_connection_spdrc(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'odu-connection', 'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4'},
                             response['odu-connection'][0]['source'])

    def test_057_check_interface_ODU4_NETWORK1_spdrb(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP5-CFP-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertNotIn('opu',
                         dict.keys(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_058_check_interface_ODU4_NETWORK2_spdrb(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'interface', 'XPDR2-NETWORK2-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK2-ODU4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP6-CFP',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP6-CFP-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'monitored'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertNotIn('opu',
                         dict.keys(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']))

    def test_059_check_ODU4_connection_spdrb(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SB1', 'odu-connection', 'XPDR2-NETWORK1-ODU4-x-XPDR2-NETWORK2-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-NETWORK1-ODU4-x-XPDR2-NETWORK2-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK2-ODU4'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-NETWORK1-ODU4'},
                             response['odu-connection'][0]['source'])

    def test_060_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_061_delete_service_100GE_ABC(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-100GE-ABC"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_062_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 2)
        time.sleep(1)

    def test_063_check_no_ODU4_connection_spdra(self):
        response = test_utils.check_node_request("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn(['odu-connection'][0], response['org-openroadm-device'])
        time.sleep(1)

    def test_064_check_no_interface_ODU4_NETWORK_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_065_check_no_interface_ODU4_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-CLIENT1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_066_check_no_interface_100GE_CLIENT_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-CLIENT1-ETHERNET')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_067_check_otn_topo_links(self):
        self.test_046_check_otn_topo_otu4_links()

    def test_068_delete_OCH_OTU4_service_AB(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-OCH-OTU4-AB"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_069_delete_OCH_OTU4_service_BC(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-OCH-OTU4-BC"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_070_get_no_service(self):
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

    def test_071_check_no_interface_OTU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_072_check_no_interface_OCH_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-761:768')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_073_getLinks_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_074_check_openroadm_topo_spdra(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'SPDR-SA1-XPDR2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        tp = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp['org-openroadm-network-topology:xpdr-network-attributes']))
        time.sleep(1)

    def test_075_check_openroadm_topo_ROADMB_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-B1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        self.assertEqual(freq_map_array[94], 255, "Lambda 1 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_076_check_openroadm_topo_ROADMB_DEG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-B1-DEG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 255, "Lambda 1 should be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_077_check_openroadm_topo_ROADMB_DEG2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-B1-DEG2', 'config')
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
        time.sleep(1)

    def test_078_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if (link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT')
                    and ('SPDR-SB1' in link['link-id'] or 'ROADM-B1' in link['link-id'])):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_079_disconnect_spdrB(self):
        response = test_utils.unmount_device("SPDR-SB1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_080_disconnect_roadmB(self):
        response = test_utils.unmount_device("ROADM-B1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_081_remove_roadm_to_roadm_links(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if (link["org-openroadm-common-network:link-type"] == "ROADM-TO-ROADM"
                    and 'ROADM-B1' in link['link-id']):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_082_add_omsAttributes_ROADMA_ROADMC(self):
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

    def test_083_add_omsAttributes_ROADMC_ROADMA(self):
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

    def test_084_create_OCH_OTU4_service_AC(self):
        self.cr_serv_input_data["service-name"] = "service-OCH-OTU4-AC"
        self.cr_serv_input_data["connection-type"] = "infrastructure"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "OTU"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_input_data["service-a-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "OTU"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-NETWORK1"
        self.cr_serv_input_data["service-z-end"]["otu-service-rate"] = "org-openroadm-otn-common-types:OTU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_085_get_OCH_OTU4_service_AC(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-OCH-OTU4-AC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-OCH-OTU4-AC')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# test service-create for 100GE service from spdrA to spdrC via spdrB
    def test_086_create_100GE_service_AC(self):
        self.cr_serv_input_data["service-name"] = "service-100GE-AC"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-a-end"]["node-id"] = "SPDR-SA1"
        self.cr_serv_input_data["service-a-end"]["clli"] = "NodeSA"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["node-id"] = "SPDR-SC1"
        self.cr_serv_input_data["service-z-end"]["clli"] = "NodeSC"
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"

        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_087_get_100GE_service_AC(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-100GE-AC")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-100GE-AC')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_088_check_interface_OTU4_spdra(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-sapi': 'AOQxIv+6nCD+',
                        'expected-dapi': 'AOQxIv+6nCD+',
                        'tx-dapi': 'ALvne1QI5jo4',
                        'expected-sapi': 'ALvne1QI5jo4',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_089_check_interface_OTU4_spdrC(self):
        response = test_utils.check_node_attribute_request(
            'SPDR-SC1', 'interface', 'XPDR2-NETWORK1-OTU')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP5-CFP',
                        'supporting-interface': 'XPDR2-NETWORK1-753:760',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP5-CFP-P1'
                        }
        input_dict_2 = {'tx-dapi': 'AOQxIv+6nCD+',
                        'expected-sapi': 'AOQxIv+6nCD+',
                        'tx-sapi': 'ALvne1QI5jo4',
                        'expected-dapi': 'ALvne1QI5jo4',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

        response2 = test_utils.check_node_attribute2_request(
            'SPDR-SA1', 'interface', 'XPDR2-NETWORK1-OTU', 'org-openroadm-otn-otu-interfaces:otu')
        self.assertEqual(response2['status_code'], requests.codes.ok)
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-dapi'])
        self.assertEqual(input_dict_2['tx-sapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['tx-sapi'])
        self.assertEqual(input_dict_2['tx-dapi'], response2['org-openroadm-otn-otu-interfaces:otu']['expected-dapi'])

    def test_090_check_configuration_spdra(self):
        self.test_049_check_interface_100GE_CLIENT_spdra()
        self.test_050_check_interface_ODU4_CLIENT_spdra()
        self.test_051_check_interface_ODU4_NETWORK_spdra()
        self.test_052_check_ODU4_connection_spdra()

    def test_091_check_configuration_spdrc(self):
        self.test_053_check_interface_100GE_CLIENT_spdrc()
        self.test_054_check_interface_ODU4_CLIENT_spdrc()
        self.test_055_check_interface_ODU4_NETWORK_spdrc()
        self.test_056_check_ODU4_connection_spdrc()

    def test_092_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 2)
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 0)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_093_delete_100GE_service_AC(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-100GE-AC"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_094_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 1)
        time.sleep(1)

    def test_095_check_configuration_spdra(self):
        self.test_063_check_no_ODU4_connection_spdra()
        self.test_064_check_no_interface_ODU4_NETWORK_spdra()
        self.test_065_check_no_interface_ODU4_CLIENT_spdra()
        self.test_066_check_no_interface_100GE_CLIENT_spdra()

    def test_096_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 2)
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertEqual(
                link['org-openroadm-otn-network-topology:available-bandwidth'], 100000)
            self.assertEqual(
                link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_097_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_098_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_099_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_100_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_101_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
