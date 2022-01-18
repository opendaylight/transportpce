#!/usr/bin/env python
##############################################################################
# Copyright (c) 2021 AT&T, Inc. and others.  All rights reserved.
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
import test_utils_rfc8040  # nopep8


class TransportPCE400GPortMappingTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '7.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils_rfc8040.start_tpce()
        cls.processes = test_utils_rfc8040.start_sims([('xpdra2', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_xpdr_device_connection(self):
        response = test_utils_rfc8040.mount_device("XPDR-A2",
                                                   ('xpdra2', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils_rfc8040.CODE_SHOULD_BE_201)

    # Check if the node appears in the ietf-network topology
    def test_02_xpdr_device_connected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')
        time.sleep(10)

    # 1a) create a OTUC2 device renderer
    def test_03_service_path_create_otuc2(self):
        response = test_utils.service_path_request("create", "service_OTUC2", "0",
                                                   [{"node-id": "XPDR-A2", "dest-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 75, 196.0375, 196.125, 755,
                                                   768, "dp-qpsk")
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces created successfully for nodes: ', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'otu-interface-id': ['XPDR1-NETWORK1-OTUC2'],
             'och-interface-id': ['XPDR1-NETWORK1-OTSIG-200G']}, res["output"]['node-interface'])

    def test_04_get_portmapping_network1(self):
        response = test_utils.portmapping_request("XPDR-A2/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.NETWORK1_CHECK_DICT["supporting-otucn"] = "XPDR1-NETWORK1-OTUC2"
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            res['mapping'])

    def test_06_check_interface_otsi(self):
        response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-NETWORK1-755:768-200G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        input_dict_1 = {'name': 'XPDR1-NETWORK1-755:768-400G',
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
          "modulation-format": "dp-qpsk"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']),
                             res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi'])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic2.4", "iid": [1, 2]},
                             res['interface'][0]['org-openroadm-optical-tributary-signal-interfaces:otsi']['flexo'])


    def test_07_check_interface_otsig(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTSIG-200G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTSIG-200G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-755:768-200G',
                        'type': 'org-openroadm-interfaces:otsi-group',
                        'supporting-port': 'L1'}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R200G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group']),
                             res['interface'][0]['org-openroadm-otsi-group-interfaces:otsi-group'])

    def test_08_check_interface_otuc2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-OTUC2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTUC2',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        ['supporting-interface-list'][0]: 'XPDR1-NETWORK1-OTSIG-400G',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'L1'}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 2,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
                             res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    # 1b) create a ODUC2 device renderer
    def test_09_otn_service_path_create_oduc2(self):
        response = test_utils.otn_service_path_request("create", "service_ODUC4", "200", "ODU",
                                                       [{"node-id": "XPDR-A2", "network-tp": "XPDR1-NETWORK1"}])
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :XPDR-A2', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'XPDR-A2',
             'odu-interface-id': ['XPDR1-NETWORK1-ODUC2']}, res["output"]['node-interface'])

    def test_10_get_portmapping_network1(self):
      response = test_utils.portmapping_request("XPDR-A2/mapping/XPDR1-NETWORK1")
      self.assertEqual(response.status_code, requests.codes.ok)
      res = response.json()
      self.NETWORK1_CHECK_DICT["supporting-oducn"] = "XPDR1-NETWORK1-ODUC2"
      self.assertIn(
          self.NETWORK1_CHECK_DICT,
          res['mapping'])


    def test_10_check_interface_oduc2(self):
        response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-NETWORK1-ODUC2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODUC2',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR2-NETWORK1-OTUC2',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}

        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODUCn',
                        'tx-sapi': 'AIGiVAQ4gDil',
                        'tx-dapi': 'AIGiVAQ4gDil',
                        'expected-sapi': 'AIGiVAQ4gDil',
                        'expected-dapi': 'AIGiVAQ4gDil'
                        }
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {'payload-type': '22', 'exp-payload-type': '22'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])



    # 1c) create Ethernet device renderer
    def test_11_otn_service_path_create_100ge(self):
        response = test_utils.otn_service_path_request("create", "service_Ethernet", "100", "Ethernet",
                                                       [{"node-id": "XPDR1-A2", "client-tp": "XPDR1-CLIENT1",
                                                         "network-tp": "XPDR1-NETWORK1"}],
                                                       {"ethernet-encoding": "eth encode",
                                                        "trib-slot": ["1"], "trib-port-number": "1"})
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :XPDR1-A2', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertEqual('XPDR1-A2', res["output"]['node-interface'][0]['node-id'])
        self.assertIn('XPDR1-CLIENT1-ODU4-service_Ethernet-x-XPDR1-NETWORK1-ODU4-service_Ethernet',
                      res["output"]['node-interface'][0]['connection-id'])
        self.assertIn('XPDR1-CLIENT1-ETHERNET-100G', res["output"]['node-interface'][0]['eth-interface-id'])
        self.assertIn('XPDR1-NETWORK1-ODU4-service_Ethernet', res["output"]['node-interface'][0]['odu-interface-id'])
        self.assertIn('XPDR1-CLIENT1-ODU4-service_Ethernet', res["output"]['node-interface'][0]['odu-interface-id'])


    def test_12_check_interface_100ge_client(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-CLIENT1-ETHERNET-100G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ETHERNET-100G',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/1-PLUG-CLIENT',
                        'type': 'org-openroadm-interfaces:ethernetCsmacd',
                        'supporting-port': 'C1'
                        }
        input_dict_2 = {'speed': 100000}
        self.assertDictEqual(dict(input_dict_1, **res['interface'][0]),
                             res['interface'][0])
        self.assertDictEqual(dict(input_dict_2, **res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
                             res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_13_check_interface_odu4_client(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-CLIENT1-ODU4-service_Ethernet")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU4-service_Ethernet',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/1-PLUG-CLIENT',
                        'supporting-interface-list': 'XPDR1-CLIENT1-ETHERNET-100G',
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
            {'payload-type': '07', 'exp-payload-type': '07'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_interface_odu4_network(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2", "interface/XPDR1-NETWORK1-ODU4-service_Ethernet")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4-service_Ethernet',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': '1/1/2-PLUG-NET',
                        'supporting-interface-list': 'XPDR1-NETWORK1-ODUC2',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'L1'}
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
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        self.assertIn('1.1', res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
        ['opucn-trib-slots'])
        self.assertIn('1.20', res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation']
        ['opucn-trib-slots'])

    def test_15_check_odu_connection_xpdra2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2",
            "odu-connection/XPDR1-CLIENT1-ODU4-service_Ethernet-x-XPDR1-NETWORK1-ODU4-service_Ethernet")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
          'connection-name':
            'XPDR1-CLIENT1-ODU4-service_Ethernet-x-XPDR1-NETWORK1-ODU4-service_Ethernet',
          'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(input_dict_1, **res['odu-connection'][0]),
                             res['odu-connection'][0])
        self.assertDictEqual({'dst-if': 'XPDR1-NETWORK1-ODU4-service_Ethernet'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({'src-if': 'XPDR1-CLIENT1-ODU4-service_Ethernet'},
                             res['odu-connection'][0]['source'])

    # 1d) Delete Ethernet device interfaces
    def test_16_otn_service_path_delete_100ge(self):
        response = test_utils.otn_service_path_request("delete", "service_Ethernet", "100", "Ethernet",
                                                       [{"node-id": "XPDR-A2", "client-tp": "XPDR1-CLIENT1",
                                                         "network-tp": "XPDR1-NETWORK1"}],
                                                       {"ethernet-encoding": "eth encode",
                                                        "trib-slot": ["1"], "trib-port-number": "1"})
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_17_check_no_odu_connection(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A2",
            "odu-connection/XPDR1-CLIENT1-ODU4-service_Ethernet-x-XPDR1-NETWORK1-ODU4-service_Ethernet")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_18_check_no_interface_odu_network(self):
      response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-NETWORK1-ODU4-service_Ethernet")
      self.assertEqual(response.status_code, requests.codes.conflict)

    def test_19_check_no_interface_odu_client(self):
      response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-CLIENT1-ODU4-service_Ethernet")
      self.assertEqual(response.status_code, requests.codes.conflict)

    def test_20_check_no_interface_100ge_client(self):
      response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-CLIENT1-ETHERNET-100G")
      self.assertEqual(response.status_code, requests.codes.conflict)

    # 1e) Delete ODUC2 device interfaces
    def test_21_otn_service_path_delete_oduc2(self):
      response = test_utils.otn_service_path_request("delete", "service_ODUC2", "200", "ODU",
                                                     [{"node-id": "XPDR-A2", "network-tp": "XPDR1-NETWORK1"}])
      time.sleep(3)
      self.assertEqual(response.status_code, requests.codes.ok)
      res = response.json()
      self.assertIn('Request processed', res["output"]["result"])
      self.assertTrue(res["output"]["success"])

    def test_22_check_no_interface_oduc2(self):
      response = test_utils.check_netconf_node_request("XPDR-A2", "interface/XPDR1-NETWORK1-ODUC2")
      self.assertEqual(response.status_code, requests.codes.conflict)

    # 1f) Delete OTUC2 device interfaces
    def test_23_service_path_delete_otuc2(self):
        response = test_utils.service_path_request("delete", "service_OTUC2", "0",
                                                   [{"node-id": "XPDR-A2", "dest-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 75, 196.0375, 196.125, 755,
                                                   768, "dp-qpsk")
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_24_check_no_interface_otuc2(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-OTUC2")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_25_check_no_interface_otsig(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-OTSIG-200G")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_26_check_no_interface_otsi(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-755:768-200G")
        self.assertEqual(response.status_code, requests.codes.conflict)

    # 2a) create a OTUC3 device renderer
    # 2b) create a ODUC3 device renderer
    # 2c) create Ethernet device renderer
    # 2d) Delete Ethernet device interfaces
    # 2e) Delete ODUC3 device interfaces
    # 2f) Delete OTUC3 device interfaces

    # 3a) create a OTUC4 device renderer
    # 3b) create a ODUC4 device renderer
    # 3c) create Ethernet device renderer
    # 3d) Delete Ethernet device interfaces
    # 3e) Delete ODUC4 device interfaces
    # 3f) Delete OTUC4 device interfaces

    def test_80_xpdr_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_81_xpdr_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['connection-status']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['connection-status']['error-tag'], 'data-missing')
        self.assertEqual(response['connection-status']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')

    def test_82_xpdr_device_not_connected(self):
        response = test_utils_rfc8040.get_portmapping_node_info("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.conflict)
        self.assertIn(response['node-info']['error-type'], ('protocol', 'application'))
        self.assertEqual(response['node-info']['error-tag'], 'data-missing')
        self.assertEqual(response['node-info']['error-message'],
                         'Request could not be completed because the relevant data model content does not exist')


if __name__ == '__main__':
    unittest.main(verbosity=2)
