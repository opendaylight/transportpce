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
    NETWORK1_CHECK_DICT = {'logical-connection-point': 'XPDR1-NETWORK1',
                           'supporting-circuit-pack-name': 'CP1-CFP0',
                           'rate': '100',
                           'port-admin-state': 'InService',
                           'supporting-port': 'CP1-CFP0-P1',
                           'port-oper-state': 'InService',
                           'supported-interface-capability': [
                               'org-openroadm-port-types:if-OCH-OTU4-ODU4'
                           ],
                           'port-direction': 'bidirectional',
                           'xpdr-type': 'mpdr', 'port-qual': 'xpdr-network',
                           'lcp-hash-val': 'Swfw02qXGyI='}
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.check_device_connection("SPDR-SA1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_02_get_portmapping_CLIENT4(self):
        response = test_utils.get_portmapping_node_attr("SPDR-SA1", "mapping", "XPDR1-CLIENT4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        res_mapping = response['mapping'][0]
        self.assertEqual('CP1-SFP4-P1', res_mapping['supporting-port'])
        self.assertEqual('CP1-SFP4', res_mapping['supporting-circuit-pack-name'])
        self.assertEqual('XPDR1-CLIENT4', res_mapping['logical-connection-point'])
        self.assertEqual('bidirectional', res_mapping['port-direction'])
        self.assertEqual('xpdr-client', res_mapping['port-qual'])
        self.assertEqual('FqlcrxV7p3g=', res_mapping['lcp-hash-val'])
        self.assertEqual('InService', res_mapping['port-admin-state'])
        self.assertEqual('InService', res_mapping['port-oper-state'])
        self.assertIn('org-openroadm-port-types:if-10GE-ODU2e', res_mapping['supported-interface-capability'])
        self.assertIn('org-openroadm-port-types:if-10GE-ODU2', res_mapping['supported-interface-capability'])
        self.assertIn('org-openroadm-port-types:if-10GE', res_mapping['supported-interface-capability'])

    def test_03_get_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("SPDR-SA1", "mapping", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            response['mapping'])

    def test_04_service_path_create_OCH_OTU4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'create',
                'nodes': [{"node-id": "SPDR-SA1", "dest-tp": "XPDR1-NETWORK1"}],
                'center-freq': 196.1,
                'nmc-width': 40,
                'min-freq': 196.075,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 761,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ', response['output']['result'])
        self.assertTrue(response['output']['success'])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'otu-interface-id': ['XPDR1-NETWORK1-OTU'],
             'och-interface-id': ['XPDR1-NETWORK1-761:768']}, response['output']['node-interface'])

    def test_05_get_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("SPDR-SA1", "mapping", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK1_CHECK_DICT["supporting-otu4"] = "XPDR1-NETWORK1-OTU"
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            response['mapping'])

    def test_06_check_interface_och(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-761:768")
        self.assertEqual(response['status_code'], requests.codes.ok)

        self.assertDictEqual(dict(response['interface'][0], **{'name': 'XPDR1-NETWORK1-761:768',
                                                               'administrative-state': 'inService',
                                                               'supporting-circuit-pack-name': 'CP1-CFP0',
                                                               'type': 'org-openroadm-interfaces:opticalChannel',
                                                               'supporting-port': 'CP1-CFP0-P1'
                                                               }),
                             response['interface'][0])
        intf = response['interface'][0]['org-openroadm-optical-channel-interfaces:och']
        self.assertEqual(intf['rate'], 'org-openroadm-common-types:R100G')
        self.assertEqual(intf['modulation-format'], 'dp-qpsk')
        self.assertEqual(float(intf['frequency']), 196.1)
        self.assertEqual(float(intf['transmit-power']), -5)

    def test_07_check_interface_OTU(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }

        input_dict_2 = {'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(response['interface'][0], **input_dict_1),
                             response['interface'][0])

        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'], **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_08_otn_service_path_create_ODU4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODU4',
                'operation': 'create',
                'service-rate': '100',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'SPDR-SA1', 'network-tp': 'XPDR1-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', response['output']['result'])
        self.assertTrue(response['output']['success'])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'odu-interface-id': ['XPDR1-NETWORK1-ODU4']}, response['output']['node-interface'])

    def test_09_get_portmapping_NETWORK1(self):
        response = test_utils.get_portmapping_node_attr("SPDR-SA1", "mapping", "XPDR1-NETWORK1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.NETWORK1_CHECK_DICT["supporting-odu4"] = "XPDR1-NETWORK1-ODU4"
        self.NETWORK1_CHECK_DICT["supporting-otu4"] = "XPDR1-NETWORK1-OTU"
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            response['mapping'])

    def test_10_check_interface_ODU4(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4'}

        self.assertDictEqual(dict(response['interface'][0], **input_dict_1),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2
                                  ),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {'payload-type': '21', 'exp-payload-type': '21'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_11_otn_service_path_create_10GE(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service1',
                'operation': 'create',
                'service-rate': '10',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'SPDR-SA1', 'client-tp': 'XPDR1-CLIENT4', 'network-tp': 'XPDR1-NETWORK1'}],
                'ethernet-encoding': 'eth encode',
                'trib-slot': ['1'],
                'trib-port-number': '1'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', response['output']['result'])
        self.assertTrue(response['output']['success'])
        self.assertEqual('SPDR-SA1', response['output']['node-interface'][0]['node-id'])
        self.assertIn('XPDR1-CLIENT4-ODU2e-x-XPDR1-NETWORK1-ODU2e',
                      response['output']['node-interface'][0]['connection-id'])
        self.assertIn('XPDR1-CLIENT4-ETHERNET10G', response['output']['node-interface'][0]['eth-interface-id'])
        self.assertIn('XPDR1-NETWORK1-ODU2e:service1', response['output']['node-interface'][0]['odu-interface-id'])
        self.assertIn('XPDR1-CLIENT4-ODU2e:service1', response['output']['node-interface'][0]['odu-interface-id'])

    def test_12_check_interface_10GE_CLIENT(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-CLIENT4-ETHERNET10G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict = {'name': 'XPDR1-CLIENT4-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(response['interface'][0], **input_dict),
                             response['interface'][0])
        self.assertEqual(10000, response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']['speed'])

    def test_13_check_interface_ODU2E_CLIENT(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-CLIENT4-ODU2e:service1")
        self.assertEqual(response['status_code'], requests.codes.ok)

        input_dict_1 = {'name': 'XPDR1-CLIENT4-ODU2e:service1',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT4-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(response['interface'][0], **input_dict_1),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '03', 'exp-payload-type': '03'},
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_interface_ODU2E_NETWORK(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU2e:service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e:service1', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(response['interface'][0], **input_dict_1),
                             response['interface'][0])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'], **input_dict_3
        ),
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      response['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_15_check_ODU2E_connection(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "odu-connection", "XPDR1-CLIENT4-ODU2e-x-XPDR1-NETWORK1-ODU2e")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT4-ODU2e-x-XPDR1-NETWORK1-ODU2e',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(response['odu-connection'][0], **input_dict_1),
                             response['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU2e:service1'},
                             response['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT4-ODU2e:service1'},
                             response['odu-connection'][0]['source'])

    def test_16_otn_service_path_delete_10GE(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service1',
                'operation': 'delete',
                'service-rate': '10',
                'service-format': 'Ethernet',
                'nodes': [{'node-id': 'SPDR-SA1', 'client-tp': 'XPDR1-CLIENT4', 'network-tp': 'XPDR1-NETWORK1'}],
                'ethernet-encoding': 'eth encode',
                'trib-slot': ['1'],
                'trib-port-number': '1'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        self.assertTrue(response['output']['success'])

    def test_17_check_no_ODU2E_connection(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "odu-connection", "XPDR1-CLIENT4-ODU2e-x-XPDR1-NETWORK1-ODU2e")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_18_check_no_interface_ODU2E_NETWORK(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU2e:service1")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_19_check_no_interface_ODU2E_CLIENT(self):
        response = test_utils.check_node_attribute_request(
            "SPDR-SA1", "interface", "XPDR1-CLIENT4-ODU2e:service1")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_20_check_no_interface_10GE_CLIENT(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-CLIENT4-ETHERNET10G")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_21_otn_service_path_delete_ODU4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'otn-service-path',
            {
                'service-name': 'service_ODU4',
                'operation': 'delete',
                'service-rate': '100',
                'service-format': 'ODU',
                'nodes': [{'node-id': 'SPDR-SA1', 'network-tp': 'XPDR1-NETWORK1'}]
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        self.assertTrue(response['output']['success'])

    def test_22_check_no_interface_ODU4(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_23_service_path_delete_OCH_OTU4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'delete',
                'nodes': [{"node-id": "SPDR-SA1", "dest-tp": "XPDR1-NETWORK1"}],
                'center-freq': 196.1,
                'nmc-width': 40,
                'min-freq': 196.075,
                'max-freq': 196.125,
                'lower-spectral-slot-number': 761,
                'higher-spectral-slot-number': 768
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Request processed', response['output']['result'])
        self.assertTrue(response['output']['success'])

    def test_24_check_no_interface_OTU4(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_25_check_no_interface_OCH(self):
        response = test_utils.check_node_attribute_request("SPDR-SA1", "interface", "XPDR1-NETWORK1-1")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_26_disconnect_SPDR_SA1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
