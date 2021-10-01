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
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None
    NETWORK1_CHECK_DICT = {"logical-connection-point": "XPDR1-NETWORK1",
                           "supporting-port": "CP1-CFP0-P1",
                           "supported-interface-capability": [
                               "org-openroadm-port-types:if-OCH-OTU4-ODU4"
                           ],
                           "port-direction": "bidirectional",
                           "port-qual": "xpdr-network",
                           "supporting-circuit-pack-name": "CP1-CFP0",
                           "xponder-type": "mpdr",
                           'lcp-hash-val': 'Swfw02qXGyI=',
                           'port-admin-state': 'InService',
                           'port-oper-state': 'InService'}
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
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

        response = test_utils.get_netconf_oper_request("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_get_portmapping_CLIENT4(self):
        response = test_utils.portmapping_request("SPDR-SA1/mapping/XPDR1-CLIENT4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res_mapping = (response.json())['mapping'][0]
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
        response = test_utils.portmapping_request("SPDR-SA1/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            res['mapping'])

    def test_04_service_path_create_OCH_OTU4(self):
        response = test_utils.service_path_request("create", "service_OCH_OTU4", "1",
                                                   [{"node-id": "SPDR-SA1", "dest-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 40, 196.075, 196.125, 761,
                                                   768)
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes: ', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'otu-interface-id': ['XPDR1-NETWORK1-OTU'],
             'och-interface-id': ['XPDR1-NETWORK1-761:768']}, res["output"]['node-interface'])

    def test_05_get_portmapping_NETWORK1(self):
        response = test_utils.portmapping_request("SPDR-SA1/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.NETWORK1_CHECK_DICT["supporting-otu4"] = "XPDR1-NETWORK1-OTU"
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            res['mapping'])

    def test_06_check_interface_och(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        self.assertDictEqual(dict(res['interface'][0], **{'name': 'XPDR1-NETWORK1-761:768',
                                                          'administrative-state': 'inService',
                                                          'supporting-circuit-pack-name': 'CP1-CFP0',
                                                          'type': 'org-openroadm-interfaces:opticalChannel',
                                                          'supporting-port': 'CP1-CFP0-P1'
                                                          }),
                             res['interface'][0])

        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5, u'modulation-format': 'dp-qpsk'},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_07_check_interface_OTU(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-OTU',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-761:768',
                        'type': 'org-openroadm-interfaces:otnOtu',
                        'supporting-port': 'CP1-CFP0-P1'
                        }

        input_dict_2 = {'tx-dapi': 'Swfw02qXGyI=',
                        'expected-sapi': 'Swfw02qXGyI=',
                        'tx-sapi': 'Swfw02qXGyI=',
                        'expected-dapi': 'Swfw02qXGyI=',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'
                        }

        self.assertDictEqual(dict(res['interface'][0], **input_dict_1),
                             res['interface'][0])

        self.assertDictEqual(input_dict_2,
                             res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_08_otn_service_path_create_ODU4(self):
        response = test_utils.otn_service_path_request("create", "service_ODU4", "100", "ODU",
                                                       [{"node-id": "SPDR-SA1", "network-tp": "XPDR1-NETWORK1"}])
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'odu-interface-id': ['XPDR1-NETWORK1-ODU4']}, res["output"]['node-interface'])

    def test_09_get_portmapping_NETWORK1(self):
        response = test_utils.portmapping_request("SPDR-SA1/mapping/XPDR1-NETWORK1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.NETWORK1_CHECK_DICT["supporting-odu4"] = "XPDR1-NETWORK1-ODU4"
        self.NETWORK1_CHECK_DICT["supporting-otu4"] = "XPDR1-NETWORK1-OTU"
        self.assertIn(
            self.NETWORK1_CHECK_DICT,
            res['mapping'])

    def test_10_check_interface_ODU4(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4'}

        self.assertDictEqual(dict(res['interface'][0], **input_dict_1),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2
                                  ),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
                             )
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_11_otn_service_path_create_10GE(self):
        response = test_utils.otn_service_path_request("create", "service1", "10", "Ethernet",
                                                       [{"node-id": "SPDR-SA1", "client-tp": "XPDR1-CLIENT4",
                                                           "network-tp": "XPDR1-NETWORK1"}],
                                                       {"ethernet-encoding": "eth encode",
                                                        "trib-slot": ["1"], "trib-port-number": "1"})
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertEqual('SPDR-SA1', res["output"]['node-interface'][0]['node-id'])
        self.assertIn('XPDR1-CLIENT4-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1',
                      res["output"]['node-interface'][0]['connection-id'])
        self.assertIn('XPDR1-CLIENT4-ETHERNET10G', res["output"]['node-interface'][0]['eth-interface-id'])
        self.assertIn('XPDR1-NETWORK1-ODU2e-service1', res["output"]['node-interface'][0]['odu-interface-id'])
        self.assertIn('XPDR1-CLIENT4-ODU2e-service1', res["output"]['node-interface'][0]['odu-interface-id'])

    def test_12_check_interface_10GE_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT4-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT4-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }
        self.assertDictEqual(dict(res['interface'][0], **input_dict),
                             res['interface'][0])
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_13_check_interface_ODU2E_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT4-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        input_dict_1 = {'name': 'XPDR1-CLIENT4-ODU2e-service1',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT4-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}

        self.assertDictEqual(dict(res['interface'][0], **input_dict_1),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_interface_ODU2E_NETWORK(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU2e-service1', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0',
                        'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}

        input_dict_3 = {'trib-port-number': 1}

        self.assertDictEqual(dict(res['interface'][0], **input_dict_1),
                             res['interface'][0])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'],
                                  **input_dict_2),
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual(dict(res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'], **input_dict_3
        ),
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'][
            'parent-odu-allocation'])
        self.assertIn(1,
                      res['interface'][0][
                          'org-openroadm-otn-odu-interfaces:odu'][
                          'parent-odu-allocation']['trib-slots'])

    def test_15_check_ODU2E_connection(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1",
            "odu-connection/XPDR1-CLIENT4-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
            'connection-name':
            'XPDR1-CLIENT4-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1',
            'direction': 'bidirectional'
        }

        self.assertDictEqual(dict(res['odu-connection'][0], **input_dict_1),
                             res['odu-connection'][0])
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-service1'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT4-ODU2e-service1'},
                             res['odu-connection'][0]['source'])

    def test_16_otn_service_path_delete_10GE(self):
        response = test_utils.otn_service_path_request("delete", "service1", "10", "Ethernet",
                                                       [{"node-id": "SPDR-SA1", "client-tp": "XPDR1-CLIENT4",
                                                           "network-tp": "XPDR1-NETWORK1"}],
                                                       {"ethernet-encoding": "eth encode",
                                                        "trib-slot": ["1"], "trib-port-number": "1"})
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_17_check_no_ODU2E_connection(self):
        response = test_utils.check_netconf_node_request(
            "SPDR-SA1",
            "odu-connection/XPDR1-CLIENT4-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_18_check_no_interface_ODU2E_NETWORK(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_19_check_no_interface_ODU2E_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT4-ODU2e-service1")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_20_check_no_interface_10GE_CLIENT(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-CLIENT4-ETHERNET10G")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_21_otn_service_path_delete_ODU4(self):
        response = test_utils.otn_service_path_request("delete", "service_ODU4", "100", "ODU",
                                                       [{"node-id": "SPDR-SA1", "network-tp": "XPDR1-NETWORK1"}])
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_22_check_no_interface_ODU4(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-ODU4")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_23_service_path_delete_OCH_OTU4(self):
        response = test_utils.service_path_request("delete", "service_OCH_OTU4", "1",
                                                   [{"node-id": "SPDR-SA1", "dest-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 40, 196.075, 196.125, 761,
                                                   768)
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_24_check_no_interface_OTU4(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_25_check_no_interface_OCH(self):
        response = test_utils.check_netconf_node_request("SPDR-SA1", "interface/XPDR1-NETWORK1-1")
        self.assertEqual(response.status_code, requests.codes.conflict)

    def test_26_disconnect_SPDR_SA1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
