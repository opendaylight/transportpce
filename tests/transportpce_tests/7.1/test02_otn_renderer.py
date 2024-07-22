#!/usr/bin/env python
##############################################################################
# Copyright (c) 2022 AT&T, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCE400GPortMappingTesting(unittest.TestCase):

    processes = None
    NETWORK2_CHECK_DICT = {"logical-connection-point": "XPDR2-NETWORK1",
                           'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                           'rate': '200',
                           'supporting-port': 'L1',
                           'port-oper-state': 'InService',
                           'supported-interface-capability': [
                               'org-openroadm-port-types:if-otsi-otsigroup'
                           ],
                           'port-direction': 'bidirectional',
                           'port-qual': 'switch-network',
                           'port-admin-state': 'InService',
                           'supported-operational-mode': [
                               'OR-W-200G-oFEC-31.6Gbd', 'OR-W-100G-oFEC-31.6Gbd'
                           ],
                           'xpdr-type': 'mpdr',
                           'lcp-hash-val': 'LY9PxYJqUbw='}
    NODE_VERSION = '7.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra2', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(2)

    def test_01_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-A2",
                                           ('xpdra2', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    # Check if the node appears in the ietf-network topology
    # this test has been removed, since it already exists in port-mapping
    # 1a) create a OTUC2 device renderer
    def test_02_service_path_create_otuc2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC2',
                'wave-number': '0',
                'modulation-format': 'dp-qpsk',
                'operation': 'create',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ', response['output']['result'])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'otu-interface-id': ['XPDR2-NETWORK1-OTUC2'],
             'och-interface-id': ['XPDR2-NETWORK1-OTSIGROUP-200G',
                                  'XPDR2-NETWORK1-755:768']},
            response['output']['node-interface'])

    def test_03_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-otucn"] = "XPDR2-NETWORK1-OTUC2"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_04_check_interface_otsi(self):
        # pylint: disable=line-too-long
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-755:768")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-755:768',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'type': 'org-openroadm-interfaces:otsi',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            "frequency": 196.0812,
            "otsi-rate": "org-openroadm-common-optical-channel-types:R200G-otsi",
            "fec": "org-openroadm-common-types:ofec",
            "transmit-power": -5,
            "provision-mode": "explicit",
            "modulation-format": "dp-qpsk"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic2.4", "iid": [1, 2]},
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])

    def test_05_check_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTSIGROUP-200G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSIGROUP-200G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R200G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_06_check_interface_otuc2(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTUC2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTUC2',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-OTSIGROUP-200G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 2,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # 1b) create a ODUC2 device renderer
    def test_07_otn_service_path_create_oduc2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC2',
                'operation': 'create',
                'service-rate': '200',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :XPDR-A2', response['output']['result'])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'odu-interface-id': ['XPDR2-NETWORK1-ODUC2']}, response['output']['node-interface'])

    def test_08_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-oducn"] = "XPDR2-NETWORK1-ODUC2"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_09_check_interface_oduc2(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC2")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODUC2',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC2',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}

        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'tx-sapi': 'LY9PxYJqUbw=',
                        'tx-dapi': 'LY9PxYJqUbw=',
                        'expected-sapi': 'LY9PxYJqUbw=',
                        'expected-dapi': 'LY9PxYJqUbw=',
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "oducn-n-rate": 2
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # 1c) create Ethernet device renderer
    def test_10_otn_service_path_create_100ge(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_Ethernet',
                'operation': 'create',
                'service-rate': '100',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'XPDR-A2', 'client-tp': 'XPDR2-CLIENT1', 'network-tp': 'XPDR2-NETWORK1'}],
                'ethernet-encoding': 'eth encode',
                'opucn-trib-slots': ['1.1', '1.20']
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :XPDR-A2', response['output']['result'])
        self.assertEqual('XPDR-A2', response['output']['node-interface'][0]['node-id'])
        self.assertIn('XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4',
                      response['output']['node-interface'][0]['connection-id'])
        self.assertIn('XPDR2-CLIENT1-ETHERNET-100G', response['output']['node-interface'][0]['eth-interface-id'])
        self.assertIn('XPDR2-NETWORK1-ODU4:service_Ethernet',
                      response['output']['node-interface'][0]['odu-interface-id'])
        self.assertIn('XPDR2-CLIENT1-ODU4:service_Ethernet',
                      response['output']['node-interface'][0]['odu-interface-id'])

    def test_11_check_interface_100ge_client(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-CLIENT1-ETHERNET-100G")
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
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
                             response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_12_check_interface_odu4_client(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-CLIENT1-ODU4:service_Ethernet")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-CLIENT1-ODU4:service_Ethernet',
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

    def test_13_check_interface_odu4_network(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-ODU4:service_Ethernet")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODU4:service_Ethernet',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-ODUC2',
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

    def test_14_check_odu_connection_xpdra2(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2",
            "odu-connection", "XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR2-NETWORK1-ODU4-x-XPDR2-CLIENT1-ODU4',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **response['odu-connection'][0]),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR2-NETWORK1-ODU4:service_Ethernet'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR2-CLIENT1-ODU4:service_Ethernet'},
                             response['odu-connection'][0]['source'])

    # 1d) Delete Ethernet device interfaces
    def test_15_otn_service_path_delete_100ge(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_Ethernet',
                'operation': 'delete',
                'service-rate': '100',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'XPDR-A2', 'client-tp': 'XPDR2-CLIENT1', 'network-tp': 'XPDR2-NETWORK1'}],
                'ethernet-encoding': 'eth encode',
                'trib-slot': ['1'],
                'trib-port-number': '1'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])

    def test_16_check_no_odu_connection(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2",
            "odu-connection", "XPDR2-CLIENT1-ODU4-x-XPDR2-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_17_check_no_interface_odu_network(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-ODU4:service_Ethernet")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_18_check_no_interface_odu_client(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-CLIENT1-ODU4:service_Ethernet")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_19_check_no_interface_100ge_client(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-CLIENT1-ETHERNET-100G")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    # 1e) Delete ODUC2 device interfaces
    def test_20_otn_service_path_delete_oduc2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC2',
                'operation': 'delete',
                'service-rate': '200',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        # Here you have remove the added oducn supporting port interface
        del self.NETWORK2_CHECK_DICT["supporting-oducn"]

    def test_21_check_no_interface_oduc2(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC2")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    # Check if port-mapping data is updated, where the supporting-oducn is deleted
    def test_21a_check_no_oduc2(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-oducn"])

    # 1f) Delete OTUC2 device interfaces

    def test_22_service_path_delete_otuc2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC2',
                'wave-number': '0',
                'modulation-format': 'dp-qpsk',
                'operation': 'delete',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        del self.NETWORK2_CHECK_DICT["supporting-otucn"]

    def test_23_check_no_interface_otuc2(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-OTUC2")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_24_check_no_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTSIGROUP-200G")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_25_check_no_interface_otsi(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-755:768")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_25a_check_no_otuc2(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    # 2a) create a OTUC3 device renderer
    def test_26_service_path_create_otuc3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC3',
                'wave-number': '0',
                'modulation-format': 'dp-qam8',
                'operation': 'create',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ', response['output']['result'])
        expected_subset_response = {
            'node-id': 'XPDR-A2',
            'otu-interface-id': ['XPDR2-NETWORK1-OTUC3']}
        expected_sorted_list = ['XPDR2-NETWORK1-755:768',
                                'XPDR2-NETWORK1-OTSIGROUP-300G']
        subset = {k: v for k, v in response['output']['node-interface'][0].items() if k in expected_subset_response}
        self.assertDictEqual(subset, expected_subset_response)
        self.assertEqual(sorted(response['output']['node-interface'][0]['och-interface-id']), expected_sorted_list)

    def test_27_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-otucn"] = "XPDR2-NETWORK1-OTUC3"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_28_check_interface_otsi(self):
        # pylint: disable=line-too-long
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-755:768")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR2-NETWORK1-755:768',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'type': 'org-openroadm-interfaces:otsi',
                        'supporting-port': 'L1'}
        input_dict_2 = {
            "frequency": 196.0812,
            "otsi-rate": "org-openroadm-common-optical-channel-types:R300G-otsi",
            "fec": "org-openroadm-common-types:ofec",
            "transmit-power": -5,
            "provision-mode": "explicit",
            "modulation-format": "dp-qam8"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic3.6", "iid": [1, 2, 3]},
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])

    def test_29_check_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTSIGROUP-300G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSIGROUP-300G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R300G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_30_check_interface_otuc3(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTUC3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTUC3',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-OTSIGROUP-300G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 3,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # 2b) create a ODUC3 device renderer
    def test_31_otn_service_path_create_oduc3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC3',
                'operation': 'create',
                'service-rate': '300',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :XPDR-A2', response['output']['result'])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'odu-interface-id': ['XPDR2-NETWORK1-ODUC3']}, response['output']['node-interface'])

    def test_32_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-oducn"] = "XPDR2-NETWORK1-ODUC3"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_33_check_interface_oduc3(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC3")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODUC3',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC3',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}

        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'tx-sapi': 'LY9PxYJqUbw=',
                        'tx-dapi': 'LY9PxYJqUbw=',
                        'expected-sapi': 'LY9PxYJqUbw=',
                        'expected-dapi': 'LY9PxYJqUbw=',
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "oducn-n-rate": 3
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # 2c) create Ethernet device renderer
    # No change in the ethernet device renderer so skipping those tests
    # 2d) Delete Ethernet device interfaces
    # No change in the ethernet device renderer so skipping those tests

    # 2e) Delete ODUC3 device interfaces
    def test_34_otn_service_path_delete_oduc3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC3',
                'operation': 'delete',
                'service-rate': '300',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        del self.NETWORK2_CHECK_DICT["supporting-oducn"]

    def test_35_check_no_interface_oduc3(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC3")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_35a_check_no_oduc3(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-oducn"])

    # 2f) Delete OTUC3 device interfaces
    def test_36_service_path_delete_otuc3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC3',
                'wave-number': '0',
                'modulation-format': 'dp-qam8',
                'operation': 'delete',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        del self.NETWORK2_CHECK_DICT["supporting-otucn"]

    def test_37_check_no_interface_otuc3(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR2-NETWORK1-OTUC3")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_38_check_no_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A1", "interface", "XPDR2-NETWORK1-OTSIGROUP-300G")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_39_check_no_interface_otsi(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR2-NETWORK1-755:768")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_39a_check_no_otuc3(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    # 3a) create a OTUC4 device renderer
    def test_40_service_path_create_otuc4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC4',
                'wave-number': '0',
                'modulation-format': 'dp-qam16',
                'operation': 'create',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ', response['output']['result'])
        expected_subset_response = {
            'node-id': 'XPDR-A2',
            'otu-interface-id': ['XPDR2-NETWORK1-OTUC4']}
        expected_sorted_list = ['XPDR2-NETWORK1-755:768',
                                'XPDR2-NETWORK1-OTSIGROUP-400G']
        subset = {k: v for k, v in response['output']['node-interface'][0].items() if k in expected_subset_response}
        self.assertDictEqual(subset, expected_subset_response)
        self.assertEqual(sorted(response['output']['node-interface'][0]['och-interface-id']), expected_sorted_list)

    def test_41_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-otucn"] = "XPDR2-NETWORK1-OTUC4"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_42_check_interface_otsi(self):
        # pylint: disable=line-too-long
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-755:768")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR2-NETWORK1-755:768',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
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
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic4.8", "iid": [1, 2, 3, 4]},
                             response['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])

    def test_43_check_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTSIGROUP-400G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-755:768',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R400G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             response['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_44_check_interface_otuc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR2-NETWORK1-OTUC4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR2-NETWORK1-OTUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR2-NETWORK1-OTSIGROUP-400G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 4,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # 3b) create a ODUC4 device renderer
    def test_45_otn_service_path_create_oduc3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC4',
                'operation': 'create',
                'service-rate': '400',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :XPDR-A2', response['output']['result'])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'odu-interface-id': ['XPDR2-NETWORK1-ODUC4']}, response['output']['node-interface'])

    def test_46_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-oducn"] = "XPDR2-NETWORK1-ODUC4"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response['mapping'])

    def test_47_check_interface_oduc4(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR2-NETWORK1-ODUC4',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/2/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}

        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'tx-sapi': 'LY9PxYJqUbw=',
                        'tx-dapi': 'LY9PxYJqUbw=',
                        'expected-sapi': 'LY9PxYJqUbw=',
                        'expected-dapi': 'LY9PxYJqUbw=',
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "oducn-n-rate": 4
                        }
        self.assertDictEqual(dict(input_dict_1, **response['interface'][0]),
                             response['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    # 3c) create Ethernet device renderer
    # No change in the ethernet device renderer so skipping those tests
    # 3d) Delete Ethernet device interfaces
    # No change in the ethernet device renderer so skipping those tests

    # 3e) Delete ODUC4 device interfaces
    def test_48_otn_service_path_delete_oduc4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODUC4',
                'operation': 'delete',
                'service-rate': '400',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'XPDR-A2', 'network-tp': 'XPDR2-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        del self.NETWORK2_CHECK_DICT["supporting-oducn"]

    def test_49_check_no_interface_oduc4(self):
        response = test_utils.check_node_attribute_request("XPDR-A2", "interface", "XPDR2-NETWORK1-ODUC4")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_49a_check_no_oduc4(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-oducn"])

    # 3f) Delete OTUC4 device interfaces
    def test_50_service_path_delete_otuc4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_OTUC4',
                'wave-number': '0',
                'modulation-format': 'dp-qam16',
                'operation': 'delete',
                'nodes': [{'node-id': 'XPDR-A2', 'dest-tp': 'XPDR2-NETWORK1'}],
                'center-freq': 196.1,
                'nmc-width': 75,
                'min-freq': 196.0375,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 755,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        del self.NETWORK2_CHECK_DICT["supporting-otucn"]

    def test_51_check_no_interface_otuc4(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR2-NETWORK1-OTUC4")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_52_check_no_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A1", "interface", "XPDR2-NETWORK1-OTSIGROUP-400G")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_53_check_no_interface_otsi(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR2-NETWORK1-755:768")
        self.assertIn(response['status_code'], (requests.codes.conflict, requests.codes.service_unavailable))

    def test_53a_check_no_otuc4(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR2-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    # Disconnect the XPDR
    def test_54_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_55_xpdr_device_disconnected(self):
        response = test_utils.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_56_xpdr_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == '__main__':
    unittest.main(verbosity=2)
