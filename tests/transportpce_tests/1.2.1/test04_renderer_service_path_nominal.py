#!/usr/bin/env python

#############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
#############################################################################

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


class TransportPCERendererTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '1.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION), ('roadma', cls.NODE_VERSION)])

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

    def test_01_rdm_device_connected(self):
        response = test_utils.mount_device("ROADMA01", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_xpdr_device_connected(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_rdm_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADMA01", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C7', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP7-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['nodes'][0]['mapping'])

    def test_04_xpdr_portmapping(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-NETWORK1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': '1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr',
             'port-qual': 'xpdr-network', 'lcp-hash-val': 'OSvMgUyP+mE='},
            response['nodes'][0]['mapping'])
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT1',
             'supporting-circuit-pack-name': '1/0/C1-PLUG-CLIENT', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': 'C1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-NETWORK1',
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr',
             'port-qual': 'xpdr-client', 'lcp-hash-val': 'AO9UFkY/TLYw'},
            response['nodes'][0]['mapping'])

    def test_05_service_path_create(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'create',
                'nodes': [{'node-id': 'ROADMA01', 'src-tp': 'SRG1-PP7-TXRX', 'dest-tp': 'DEG1-TTP-TXRX'},
                          {'node-id': 'XPDRA01', 'src-tp': 'XPDR1-CLIENT1', 'dest-tp': 'XPDR1-NETWORK1'}],
                'center-freq': 195.8,
                'nmc-width': 40,
                'min-freq': 195.775,
                'max-freq': 195.825,
                'lower-spectral-slot-number': 713,
                'higher-spectral-slot-number': 720
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ROADMA01', response['output']['result'])

    def test_06_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADMA01", "interface", "DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'DEG1-TTP-TXRX-713:720',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '2/0',
                'type': 'org-openroadm-interfaces:opticalChannel',
                'supporting-port': 'L1'
            }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            {'wavelength-number': 7},
            response['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_07_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADMA01", "interface", "SRG1-PP7-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'SRG1-PP7-TXRX-713:720',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '4/0',
                'type': 'org-openroadm-interfaces:opticalChannel',
                'supporting-port': 'C7'
            }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            {'wavelength-number': 7},
            response['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_08_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request(
            "ROADMA01", "roadm-connections", "SRG1-PP7-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'connection-number': 'SRG1-PP7-TXRX-DEG1-TTP-TXRX-713:720',
                'wavelength-number': 7,
                'opticalControlMode': 'off'
            }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP7-TXRX-713:720'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG1-TTP-TXRX-713:720'}, response['roadm-connections'][0]['destination'])

    def test_09_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'XPDR1-NETWORK1-713:720',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                'type': 'org-openroadm-interfaces:opticalChannel',
                'supporting-port': '1'
            }, **response['interface'][0]), response['interface'][0])
        intf = response['interface'][0]['org-openroadm-optical-channel-interfaces:och']
        self.assertEqual(intf['rate'], 'org-openroadm-optical-channel-interfaces:R100G')
        self.assertEqual(intf['modulation-format'], 'dp-qpsk')
        self.assertEqual(intf['wavelength-number'], 7)
        self.assertEqual(float(intf['transmit-power']), -5)

    def test_10_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'XPDR1-NETWORK1-OTU',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                'type': 'org-openroadm-interfaces:otnOtu',
                'supporting-port': '1',
                'supporting-interface': 'XPDR1-NETWORK1-713:720'
            }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            {'rate': 'org-openroadm-otn-otu-interfaces:OTU4', 'fec': 'scfec'},
            response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_11_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-ODU")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the 2 following statements replace self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'XPDR1-NETWORK1-ODU',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                'type': 'org-openroadm-interfaces:otnOdu',
                'supporting-port': '1',
                'supporting-interface': 'XPDR1-NETWORK1-OTU'
            }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            dict({
                'rate': 'org-openroadm-otn-odu-interfaces:ODU4',
                'monitoring-mode': 'terminated'
            }, **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
        )
        self.assertDictEqual({'exp-payload-type': '07', 'payload-type': '07'},
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_12_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                'name': 'XPDR1-CLIENT1-ETHERNET',
                'administrative-state': 'inService',
                'supporting-circuit-pack-name': '1/0/C1-PLUG-CLIENT',
                'type': 'org-openroadm-interfaces:ethernetCsmacd',
                'supporting-port': 'C1'
            }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            {'speed': 100000,
             'mtu': 9000,
             'auto-negotiation': 'enabled',
             'duplex': 'full',
             'fec': 'off'},
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_13_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "circuit-packs", "1%2F0%2F1-PLUG-NET")
        # FIXME: https://jira.opendaylight.org/browse/TRNSPRTPCE-591
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('not-reserved-inuse', response['circuit-packs'][0]["equipment-state"])

    def test_14_service_path_delete(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'delete',
                'nodes': [{'node-id': 'ROADMA01', 'src-tp': 'SRG1-PP7-TXRX', 'dest-tp': 'DEG1-TTP-TXRX'},
                          {'node-id': 'XPDRA01', 'src-tp': 'XPDR1-CLIENT1', 'dest-tp': 'XPDR1-NETWORK1'}],
                'center-freq': 195.8,
                'nmc-width': 40,
                'min-freq': 195.775,
                'max-freq': 195.825,
                'lower-spectral-slot-number': 713,
                'higher-spectral-slot-number': 720
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(response['output'], {'result': 'Request processed', 'success': True})

    def test_15_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADMA01", "interface", "DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_16_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADMA01", "interface", "SRG1-PP7-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_17_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request(
            "ROADMA01", "roadm-connections", "SRG1-PP7-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_18_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_19_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_20_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-NETWORK1-ODU")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_21_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "interface", "XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_22_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDRA01", "circuit-packs", "1%2F0%2F1-PLUG-NET")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual('not-reserved-available', response["circuit-packs"][0]['equipment-state'])

    def test_23_rdm_device_disconnected(self):
        response = test_utils.unmount_device("ROADMA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_24_xpdr_device_disconnected(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2, failfast=False)
