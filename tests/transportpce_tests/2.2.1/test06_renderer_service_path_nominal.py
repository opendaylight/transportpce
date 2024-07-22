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
# from unittest.result import failfast
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCERendererTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

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

    def test_01_rdm_device_connected(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_xpdr_device_connected(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_rdm_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            response['nodes'][0]['mapping'])

    def test_04_xpdr_portmapping(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {'logical-connection-point': 'XPDR1-NETWORK1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET', 'rate': '0',
             'port-admin-state': 'InService', 'supporting-port': '1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr',
             'port-qual': 'xpdr-network', 'lcp-hash-val': 'AMkDwQ7xTmRI'},
            response['nodes'][0]['mapping'])
        self.assertIn(
            {'logical-connection-point': 'XPDR1-CLIENT1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT', 'rate': '100',
             'port-admin-state': 'InService', 'supporting-port': 'C1',
             'port-oper-state': 'InService', 'connection-map-lcp': 'XPDR1-NETWORK1',
             'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'port-direction': 'bidirectional', 'xpdr-type': 'tpdr',
             'port-qual': 'xpdr-client', 'lcp-hash-val': 'AJUUr6I5fALj'},
            response['nodes'][0]['mapping'])

    def test_05_service_path_create(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'create',
                'nodes': [{'node-id': 'ROADM-A1', 'src-tp': 'SRG1-PP3-TXRX', 'dest-tp': 'DEG1-TTP-TXRX'},
                          {'node-id': 'XPDR-A1', 'src-tp': 'XPDR1-CLIENT1', 'dest-tp': 'XPDR1-NETWORK1'}],
                'center-freq': 195.8,
                'nmc-width': 40,
                'min-freq': 195.775,
                'max-freq': 195.825,
                'lower-spectral-slot-number': 713,
                'higher-spectral-slot-number': 720
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces created successfully for nodes: ROADM-A1', response['output']['result'])

    def test_06_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "DEG1-TTP-TXRX-nmc-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'DEG1-TTP-TXRX-nmc-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0',
                 'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                 'supporting-port': 'L1'
                 }, **response['interface'][0]), response['interface'][0]
        )
        nmcctp = response['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp']
        self.assertEqual(float(nmcctp['frequency']), 195.8)
        self.assertEqual(float(nmcctp['width']), 40)

    def test_07_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "DEG1-TTP-TXRX-mc-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'DEG1-TTP-TXRX-mc-7',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0',
                 'type': 'org-openroadm-interfaces:mediaChannelTrailTerminationPoint',
                 'supporting-port': 'L1'
                 }, **response['interface'][0]), response['interface'][0]
        )
        mcttp = response['interface'][0]['org-openroadm-media-channel-interfaces:mc-ttp']
        self.assertEqual(float(mcttp['min-freq']), 195.775)
        self.assertEqual(float(mcttp['max-freq']), 195.825)

    def test_08_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "SRG1-PP3-TXRX-nmc-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'SRG1-PP3-TXRX-nmc-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '3/0',
                 'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                 'supporting-port': 'C3'
                 }, **response['interface'][0]), response['interface'][0]
        )
        nmcctp = response['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp']
        self.assertEqual(float(nmcctp['frequency']), 195.8)
        self.assertEqual(float(nmcctp['width']), 40)

    # -mc supporting interfaces must not be created for SRG, only degrees
    def test_09_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "SRG1-PP3-TXRX-mc-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_10_service_path_create_rdm_check(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'connection-name': 'SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720',
                 'opticalControlMode': 'off'
                 }, **response['roadm-connections'][0]), response['roadm-connections'][0])
        self.assertDictEqual({'src-if': 'SRG1-PP3-TXRX-nmc-713:720'}, response['roadm-connections'][0]['source'])
        self.assertDictEqual({'dst-if': 'DEG1-TTP-TXRX-nmc-713:720'}, response['roadm-connections'][0]['destination'])

    def test_11_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-713:720")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:opticalChannel',
                 'supporting-port': '1'
                 }, **response['interface'][0]), response['interface'][0]
        )
        intf = response['interface'][0]['org-openroadm-optical-channel-interfaces:och']
        self.assertEqual(intf['rate'], 'org-openroadm-common-types:R100G')
        self.assertEqual(intf['modulation-format'], 'dp-qpsk')
        self.assertEqual(float(intf['frequency']), 195.8)
        self.assertEqual(float(intf['transmit-power']), -5)

    def test_12_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-OTU',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:otnOtu',
                 'supporting-port': '1',
                 'supporting-interface': 'XPDR1-NETWORK1-7'
                 }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            dict({'rate': 'org-openroadm-otn-common-types:OTU4', 'fec': 'scfec'},
                 **response['interface'][0]['org-openroadm-otn-otu-interfaces:otu']),
            response['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_13_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-ODU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the 2 following statements replace self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-ODU4',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:otnOdu',
                 'supporting-port': '1',
                 'supporting-interface': 'XPDR1-NETWORK1-OTU'
                 }, **response['interface'][0]), response['interface'][0])
        self.assertDictEqual(
            dict({'rate': 'org-openroadm-otn-common-types:ODU4', 'monitoring-mode': 'terminated'},
                 **response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
            response['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual({'exp-payload-type': '07', 'payload-type': '07'},
                             response['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response['status_code'], requests.codes.ok)
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-CLIENT1-ETHERNET',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
                 'type': 'org-openroadm-interfaces:ethernetCsmacd',
                 'supporting-port': 'C1'
                 }, **response['interface'][0]), response['interface'][0]
        )
        self.assertDictEqual(
            dict({'fec': 'off', 'speed': 100000},
                 **response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet']),
            response['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_15_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "circuit-packs", "1%2F0%2F1-PLUG-NET")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('not-reserved-inuse', response['circuit-packs'][0]['equipment-state'])
        # FIXME: https://jira.opendaylight.org/browse/TRNSPRTPCE-591

    def test_16_service_path_create_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "circuit-packs", "1%2F0%2F1-PLUG-CLIENT")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('not-reserved-inuse', response['circuit-packs'][0]['equipment-state'])
        # FIXME: https://jira.opendaylight.org/browse/TRNSPRTPCE-591

    def test_17_service_path_delete(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'service-path',
            {
                'service-name': 'service_test',
                'wave-number': '7',
                'modulation-format': 'dp-qpsk',
                'operation': 'delete',
                'nodes': [{'node-id': 'ROADM-A1', 'src-tp': 'SRG1-PP3-TXRX', 'dest-tp': 'DEG1-TTP-TXRX'},
                          {'node-id': 'XPDR-A1', 'src-tp': 'XPDR1-CLIENT1', 'dest-tp': 'XPDR1-NETWORK1'}],
                'center-freq': 195.8,
                'nmc-width': 40,
                'min-freq': 195.775,
                'max-freq': 195.825,
                'lower-spectral-slot-number': 713,
                'higher-spectral-slot-number': 720
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(response['output'], {'result': 'Request processed', 'success': True})

    def test_18_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "DEG1-TTP-TXRX-mc-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_19_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "DEG1-TTP-TXRX-nmc-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_20_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "SRG1-PP3-TXRX-mc-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_21_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request("ROADM-A1", "interface", "SRG1-PP3-TXRX-nmc-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_22_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "interface", "SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_23_service_path_delete_rdm_check(self):
        response = test_utils.check_node_attribute_request(
            "ROADM-A1", "roadm-connections", "SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_24_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-713:720")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_25_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-OTU")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_26_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-NETWORK1-ODU")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_27_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "interface", "XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_28_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "circuit-packs", "1%2F0%2F1-PLUG-NET")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual('not-reserved-available', response["circuit-packs"][0]['equipment-state'])

    def test_29_service_path_delete_xpdr_check(self):
        response = test_utils.check_node_attribute_request("XPDR-A1", "circuit-packs", "1%2F0%2F1-PLUG-CLIENT")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual('not-reserved-available', response["circuit-packs"][0]['equipment-state'])

    def test_30_rdm_device_disconnected(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_31_xpdr_device_disconnected(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2, failfast=False)
