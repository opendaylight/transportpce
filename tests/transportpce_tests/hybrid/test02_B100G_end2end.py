#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=invalid-name
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
    NODE_VERSION_221 = '2.2.1'
    NODE_VERSION_71 = '7.1'

    cr_serv_input_data = {
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
            "tx-direction": [{
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "400",
            "node-id": "XPDR-C2",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTUCn",
            "clli": "NodeSC",
            "tx-direction": [{
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
                },
                "index": 0
            }],
            "rx-direction": [{
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
        time.sleep(1)

    def test_001_connect_xpdra2(self):
        response = test_utils.mount_device("XPDR-A2", ('xpdra2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_002_connect_xpdrc2(self):
        response = test_utils.mount_device("XPDR-C2", ('xpdrc2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_003_connect_rdma(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_004_connect_rdmc(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_005_connect_xpdra2_2_N1_to_roadma_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_006_connect_roadma_PP2_to_xpdra2_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_007_connect_xpdrc2_2_N1_to_roadmc_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_008_connect_roadmc_PP2_to_xpdrc2_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_009_add_omsAttributes_roadma_roadmc(self):
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

    def test_010_add_omsAttributes_roadmc_roadma(self):
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
    def test_011_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 8, 'There should be 8 nodes')
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_012_create_OTUC4_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_013_get_OTUC4_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-OTUC4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service1-OTUC4')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        self.assertEqual(response['services'][0]['service-layer'], 'wdm')
        self.assertEqual(response['services'][0]['service-a-end']['service-rate'], 400)
        self.assertEqual(response['services'][0]['service-a-end']['otu-service-rate'],
                         'org-openroadm-otn-common-types:OTUCn')
        self.assertEqual(response['services'][0]['service-z-end']['otu-service-rate'],
                         'org-openroadm-otn-common-types:OTUCn')

    # Check correct configuration of devices
    def test_014_check_interface_otsi_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-755:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-755:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                                   'type': 'org-openroadm-interfaces:otsi',
                                   'supporting-port': 'L1'
                                   }, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict({'frequency': 196.0812, 'otsi-rate': 'org-openroadm-common-optical-channel-types:R400G-otsi',
                  'transmit-power': -5, 'modulation-format': 'dp-qam16'},
                 **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
            response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])

    def test_015_check_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-OTSIGROUP-400G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]
                                  ['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]
                             ['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_016_check_interface_OTUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-OTUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {'tx-sapi': 'G54UFNImtOE=',
                        'expected-dapi': 'G54UFNImtOE=',
                        'tx-dapi': 'J/FIUzQc+4M=',
                        'expected-sapi': 'J/FIUzQc+4M=',
                        'rate': 'org-openroadm-otn-common-types:OTUCn',
                        'degthr-percentage': 100,
                        'degm-intervals': 2,
                        'otucn-n-rate': 4
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_017_check_interface_otsi_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-755:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'name': 'XPDR2-NETWORK1-755:768',
                                   'administrative-state': 'inService',
                                   'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                                   'type': 'org-openroadm-interfaces:otsi',
                                   'supporting-port': 'L1'
                                   }, **response['interface'][0]),
                             response['interface'][0])

        self.assertDictEqual(
            dict({'frequency': 196.0812, 'otsi-rate': 'org-openroadm-common-optical-channel-types:R400G-otsi',
                  'transmit-power': -5, 'modulation-format': 'dp-qam16'},
                 **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
            response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])

    def test_018_check_interface_OTSI_GROUP_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-OTSIGROUP-400G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]
                                  ['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]
                             ['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_019_check_interface_OTUC4_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-OTUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'
                        }
        input_dict_2 = {'tx-dapi': 'G54UFNImtOE=',
                        'expected-sapi': 'G54UFNImtOE=',
                        'tx-sapi': 'J/FIUzQc+4M=',
                        'expected-dapi': 'J/FIUzQc+4M=',
                        'rate': 'org-openroadm-otn-common-types:OTUCn',
                        'degthr-percentage': 100,
                        'degm-intervals': 2,
                        'otucn-n-rate': 4
                        }

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])

        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]
                             ['org-openroadm-otn-otu-interfaces:otu'])

    def test_020_check_no_interface_ODUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_021_check_openroadm_topo_xpdra2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A2-XPDR2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        ele = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', ele['tp-id'])
        self.assertEqual(
            196.08125,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
        self.assertEqual(
            75.0,
            float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))

    def test_022_check_otn_topo_OTUC4_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 2)
        listLinkId = ['OTUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                      'OTUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1']
        for link in response['network'][0]['ietf-network-topology:link']:
            self.assertIn(link['link-id'], listLinkId)
            self.assertEqual(link['transportpce-networkutils:otn-link-type'], 'OTUC4')
            self.assertEqual(link['org-openroadm-common-network:link-type'], 'OTN-LINK')
            self.assertEqual(link['org-openroadm-otn-network-topology:available-bandwidth'], 400000)
            self.assertEqual(link['org-openroadm-otn-network-topology:used-bandwidth'], 0)
            self.assertIn(link['org-openroadm-common-network:opposite-link'], listLinkId)

    # test service-create for ODU4 service from xpdra2 to xpdrc2
    def test_023_create_ODUC4_service(self):
        self.cr_serv_input_data["service-name"] = "service1-ODUC4"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "ODU"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODUCn"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "ODU"
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODUCn"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_024_get_ODUC4_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-ODUC4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['service-name'], 'service1-ODUC4')
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        self.assertEqual(response['services'][0]['service-layer'], 'wdm')
        self.assertEqual(response['services'][0]['service-a-end']['service-rate'], 400)
        self.assertEqual(response['services'][0]['service-a-end']['odu-service-rate'],
                         'org-openroadm-otn-common-types:ODUCn')
        self.assertEqual(response['services'][0]['service-z-end']['odu-service-rate'],
                         'org-openroadm-otn-common-types:ODUCn')

    def test_025_check_interface_ODUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_026_check_interface_ODUC4_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
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
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_027_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
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
                    link['transportpce-networkutils:otn-link-type'], 'ODUC4')
                self.assertEqual(
                    link['org-openroadm-common-network:link-type'], 'OTN-LINK')
                self.assertIn(link['org-openroadm-common-network:opposite-link'],
                              ['ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                               'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1'])
            else:
                self.fail("this link should not exist")

    def test_028_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 4)
                        self.assertEqual(xpdrTpPortConAt['odtu-tpn-pool'][0]['odtu-type'],
                                         'org-openroadm-otn-common-types:ODTU4.ts-Allocated')

    # test service-create for 100GE service 1 from xpdra2 to xpdrc2
    def test_029_create_100GE_service_1(self):
        self.cr_serv_input_data["service-name"] = "service-100GE"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        del self.cr_serv_input_data["service-a-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        del self.cr_serv_input_data["service-z-end"]["odu-service-rate"]
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_030_get_100GE_service_1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service-100GE")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-100GE')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')

    def test_031_check_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ETHERNET-100G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ETHERNET-100G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_032_check_interface_ODU4_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4:service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT1-ETHERNET-100G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_033_check_interface_ODU4_NETWORK_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4:service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-ODUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'not-terminated'}
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
        self.assertIn('1.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('1.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])

    def test_034_check_ODU4_connection_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'odu-connection', 'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4:service-100GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4:service-100GE'},
                             response['odu-connection'][0]['source'])

    def test_035_check_interface_100GE_CLIENT_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-CLIENT1-ETHERNET-100G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ETHERNET-100G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_036_check_interface_ODU4_CLIENT_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-CLIENT1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4:service-100GE',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT1-ETHERNET-100G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_037_check_interface_ODU4_NETWORK_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4:service-100GE',
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
        self.assertIn('1.1',
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])
        self.assertIn('1.20',
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])

    def test_038_check_ODU4_connection_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'odu-connection', 'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4:service-100GE'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4:service-100GE'},
                             response['odu-connection'][0]['source'])

    def test_039_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 300000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_040_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
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

    # test service-create for 100GE service 2 from xpdra2 to xpdrc2
    def test_041_create_100GE_service_2(self):
        self.cr_serv_input_data["service-name"] = "service-100GE2"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_042_get_100GE_service_2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-100GE2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-100GE2')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_043_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 4)

    def test_044_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 200000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 200000)

    def test_045_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 40)
                        tsPoolList = list(range(1, 40))
                        self.assertNotIn(
                            tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 2)
                        self.assertNotIn(
                            2, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_046_delete_100GE_service_2(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-100GE2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_047_delete_100GE_service_1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-100GE"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_048_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 2)

    def test_049_check_no_ODU4_connection_xpdra2(self):
        response = test_utils.check_node_request("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn(['odu-connection'][0], response['org-openroadm-device'])

    def test_050_check_no_interface_ODU4_NETWORK_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_051_check_no_interface_ODU4_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ODU4:service-100GE')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_052_check_no_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ETHERNET-100G')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_053_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 400000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 0)

    def test_054_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 80)
                        self.assertEqual(
                            len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 4)

    # test service-create for 100GE service 3 from xpdra2 client1 to xpdrc2 client2
    def test_055_create_100GE_service_3(self):
        self.cr_serv_input_data["service-name"] = "service-100GE3"
        self.cr_serv_input_data["connection-type"] = "service"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "100"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR2-CLIENT2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_056_get_100GE_service_3(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-100GE3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-100GE3')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_057_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 3)

    def test_058_check_interface_100GE_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ETHERNET-100G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ETHERNET-100G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_059_check_interface_ODU4_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-CLIENT1-ODU4:service-100GE3')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4:service-100GE3',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT1-ETHERNET-100GE3',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_060_check_interface_ODU4_NETWORK_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODU4:service-100GE3')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4:service-100GE3',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-ODUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'not-terminated'}
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
        self.assertIn('1.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('1.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])

    def test_061_check_ODU4_connection_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'odu-connection', 'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4:service-100GE3'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4:service-100GE3'},
                             response['odu-connection'][0]['source'])

    def test_062_check_interface_100GE_CLIENT_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-CLIENT2-ETHERNET-100G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT2-ETHERNET-100G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/2-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_063_check_interface_ODU4_CLIENT_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-CLIENT2-ODU4:service-100GE3')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4:service-100GE3',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/1/2-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR2-CLIENT2-ETHERNET-100GE3',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'C1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU4',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '07', 'exp-payload-type': '07'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_064_check_interface_ODU4_NETWORK_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'interface', 'XPDR2-NETWORK1-ODU4:service-100GE3')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4:service-100GE3',
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
        self.assertIn('1.1',
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])
        self.assertIn('1.20',
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['opucn-trib-slots'])

    def test_065_check_ODU4_connection_xpdrc2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-C2', 'odu-connection', 'XPDR2-CLIENT2-ODU4-x-XPDR2-NETWORK1-ODU4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-CLIENT2-ODU4-x-XPDR2-NETWORK1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4:service-100GE3'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT2-ODU4:service-100GE3'},
                             response['odu-connection'][0]['source'])

    def test_066_check_otn_topo_links(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 4)
        for link in response['network'][0]['ietf-network-topology:link']:
            linkId = link['link-id']
            if (linkId in ('ODUC4-XPDR-A2-XPDR2-XPDR2-NETWORK1toXPDR-C2-XPDR2-XPDR2-NETWORK1',
                           'ODUC4-XPDR-C2-XPDR2-XPDR2-NETWORK1toXPDR-A2-XPDR2-XPDR2-NETWORK1')):
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:available-bandwidth'], 300000)
                self.assertEqual(
                    link['org-openroadm-otn-network-topology:used-bandwidth'], 100000)

    def test_067_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        xpdrTpPortConAt = tp['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']
                        self.assertEqual(len(xpdrTpPortConAt['ts-pool']), 60)
                        tsPoolList = list(range(1, 20))
                        self.assertNotIn(tsPoolList, xpdrTpPortConAt['ts-pool'])
                        self.assertEqual(len(xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool']), 3)
                        self.assertNotIn(1, xpdrTpPortConAt['odtu-tpn-pool'][0]['tpn-pool'])

    def test_068_delete_100GE_service_3(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-100GE3"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_069_delete_ODUC4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-ODUC4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_070_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 1)

    def test_071_check_no_interface_ODU4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_072_check_otn_topo_links(self):
        self.test_022_check_otn_topo_OTUC4_links()

    def test_073_check_otn_topo_tp(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        for node in response['network'][0]['node']:
            if node['node-id'] in ('XPDR-A2-XPDR2', 'XPDR-C2-XPDR2'):
                tpList = node['ietf-network-topology:termination-point']
                for tp in tpList:
                    if tp['tp-id'] == 'XPDR2-NETWORK1':
                        self.assertNotIn('org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes',
                                         dict.keys(tp))

    def test_074_delete_OTUC4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-OTUC4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_075_get_no_service(self):
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

    def test_076_check_no_interface_OTUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-OTUC4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_077_check_no_interface_OTSI_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-755:768')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_078_check_no_interface_OTSIG_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR2-NETWORK1-OTSIGROUP-400G')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_079_getLinks_OtnTopology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_080_check_openroadm_topo_xpdra2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A2-XPDR2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        tp = response['node']['ietf-network-topology:termination-point'][0]
        self.assertEqual('XPDR2-NETWORK1', tp['tp-id'])
        self.assertNotIn('wavelength', dict.keys(
            tp['org-openroadm-network-topology:xpdr-network-attributes']))

    def test_081_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        self.assertEqual(22, len(links), 'Topology should contain 22 links')

    def test_082_connect_xpdra2_1_N1_to_roadma_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_083_connect_roadma_PP2_to_xpdra2_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_084_connect_xpdrc2_1_N1_to_roadmc_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_085_connect_roadmc_PP2_to_xpdrc2_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    # test service-create for 400GE service from xpdra2 to xpdrc2
    def test_086_create_400GE_service(self):
        self.cr_serv_input_data["service-name"] = "service-400GE"
        self.cr_serv_input_data["service-a-end"]["service-rate"] = "400"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["service-rate"] = "400"
        del self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-device-name"]
        del self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"]
        del self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-device-name"]
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_087_get_400GE_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-400GE")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-400GE')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_088_check_xc1_roadma(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP1-TXRX-DEG2-TTP-TXRX-755:768")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(
            dict({
                'connection-name': 'SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768',
                'opticalControlMode': 'gainLoss',
                'target-output-power': -3.0
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP1-TXRX-nmc-755:768'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG2-TTP-TXRX-nmc-755:768'}, response['roadm-connections'][0]['destination'])
        time.sleep(1)

    def test_089_check_topo_xpdra2(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPDR-A2-XPDR1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'XPDR1-NETWORK1':
                self.assertEqual(
                    196.08125,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['frequency']))
                self.assertEqual(
                    75.0,
                    float(ele['org-openroadm-network-topology:xpdr-network-attributes']['wavelength']['width']))
            if ele['tp-id'] == 'XPDR1-CLIENT1':
                self.assertNotIn('org-openroadm-network-topology:xpdr-client-attributes', dict.keys(ele))
        time.sleep(1)

    def test_090_check_topo_roadma_SRG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:srg-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
        for ele in liste_tp:
            if ele['tp-id'] == 'SRG1-PP1-TXRX':
                freq_map = base64.b64decode(
                    ele['org-openroadm-network-topology:pp-attributes']['avail-freq-maps'][0]['freq-map'])
                freq_map_array = [int(x) for x in freq_map]
                self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
            if ele['tp-id'] == 'SRG1-PP2-TXRX':
                self.assertNotIn('avail-freq-maps', dict.keys(ele))
        time.sleep(1)

    def test_091_check_topo_roadma_DEG1(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADM-A1-DEG2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        freq_map = base64.b64decode(
            response['node']['org-openroadm-network-topology:degree-attributes']['avail-freq-maps'][0]['freq-map'])
        freq_map_array = [int(x) for x in freq_map]
        self.assertEqual(freq_map_array[95], 0, "Index 1 should not be available")
        liste_tp = response['node']['ietf-network-topology:termination-point']
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
        time.sleep(1)

    def test_092_check_interface_400GE_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-CLIENT1-ETHERNET')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ETHERNET',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/1-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 400000}
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_093_check_interface_OTSI_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-755:768')
        self.assertEqual(response['status_code'], requests.codes.ok)
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

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(
            dict(input_dict_2, **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
            response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual(
            {"foic-type": "org-openroadm-common-optical-channel-types:foic4.8", "iid": [1, 2, 3, 4]},
            response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])

    def test_094_check_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-OTSIGROUP-400G')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTSIGROUP-400G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_095_check_interface_OTUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-OTUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-OTSIGROUP-400G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"tx-sapi": "ANeUjNzWtDLV",
                        "expected-dapi": "ANeUjNzWtDLV",
                        'tx-dapi': 'AKsqPmWceByv',
                        'expected-sapi': 'AKsqPmWceByv',
                        "rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 4,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_096_check_interface_ODUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-OTUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP",
                        "tim-detect-mode": "Disabled",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "oducn-n-rate": 4}
        input_dict_3 = {"exp-payload-type": "22", "payload-type": "22"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])
        self.assertEqual('XPDR1-NETWORK1-OTUC4', response['interface'][0]['supporting-interface-list'][0])

    def test_097_check_interface_ODUFLEX_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-ODUFLEX')
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODUFLEX',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-ODUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1',
                        'circuit-id': 'TBD',
                        'description': 'TBD'}
        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP-CTP",
                        "tim-detect-mode": "Disabled",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "rate": "org-openroadm-otn-common-types:ODUflex-cbr",
                        "oduflex-cbr-service": "org-openroadm-otn-common-types:ODUflex-cbr-400G"
                        }
        input_dict_3 = {"exp-payload-type": "32", "payload-type": "32"}
        input_dict_4 = {'trib-port-number': 1}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])
        self.assertDictEqual(dict(input_dict_4,
                                  **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
                                      'parent-odu-allocation']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn('1.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('1.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('2.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('2.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('3.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('3.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('4.1', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertIn('4.20', response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
                      ['opucn-trib-slots'])
        self.assertEqual('XPDR1-NETWORK1-ODUC4', response['interface'][0]['supporting-interface-list'][0])

    def test_098_delete_400GE_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service-400GE"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_099_get_no_service(self):
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

    def test_100_check_no_interface_ODUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-ODUC4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_101_check_no_interface_OTUC4_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-OTUC4')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_102_check_no_interface_OTSI_GROUP_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-OTSIGROUP-400G')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_103_check_no_interface_OTSI_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-NETWORK1-755:768')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_104_check_no_interface_400GE_CLIENT_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            'XPDR-A2', 'interface', 'XPDR1-CLIENT1-ETHERNET')
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_105_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_106_disconnect_xpdra2(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_107_disconnect_xpdrc2(self):
        response = test_utils.unmount_device("XPDR-C2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_108_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_109_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
