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
#from unittest.result import failfast
import requests
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


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
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['nodes'][0]['mapping'])

    def test_04_xpdr_portmapping(self):
        response = test_utils.portmapping_request("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional', 'connection-map-lcp': 'XPDR1-CLIENT1',
             'lcp-hash-val': 'AMkDwQ7xTmRI',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client',
             'lcp-hash-val': 'AJUUr6I5fALj',
             'port-admin-state': 'InService', 'port-oper-state': 'InService'},
            res['nodes'][0]['mapping'])

    def test_05_service_path_create(self):
        response = test_utils.service_path_request("create", "service_test", "7",
                                                   [{"renderer:node-id": "ROADM-A1",
                                                     "renderer:src-tp": "SRG1-PP3-TXRX",
                                                     "renderer:dest-tp": "DEG1-TTP-TXRX"},
                                                    {"renderer:node-id": "XPDR-A1",
                                                     "renderer:src-tp": "XPDR1-CLIENT1",
                                                     "renderer:dest-tp": "XPDR1-NETWORK1"}],
                                                   195.8, 40, 195.775, 195.825, 713,
                                                   720)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes: ROADM-A1', res["output"]["result"])

    def test_06_service_path_create_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/DEG1-TTP-TXRX-nmc-713:720")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'DEG1-TTP-TXRX-nmc-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0',
                 'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                 'supporting-port': 'L1'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            {u'frequency': 195.8, u'width': 40},
            res['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp'])

    def test_07_service_path_create_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/DEG1-TTP-TXRX-mc-713:720")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'DEG1-TTP-TXRX-mc-7',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0',
                 'type': 'org-openroadm-interfaces:mediaChannelTrailTerminationPoint',
                 'supporting-port': 'L1'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            {u'min-freq': 195.775, u'max-freq': 195.825},
            res['interface'][0]['org-openroadm-media-channel-interfaces:mc-ttp'])

    def test_08_service_path_create_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/SRG1-PP3-TXRX-nmc-713:720")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'SRG1-PP3-TXRX-nmc-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '3/0',
                 'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                 'supporting-port': 'C3'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            {u'frequency': 195.8, u'width': 40},
            res['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp'])

    # -mc supporting interfaces must not be created for SRG, only degrees
    def test_09_service_path_create_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/SRG1-PP3-TXRX-mc-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_10_service_path_create_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "roadm-connections/SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'connection-name': 'SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720',
                 'opticalControlMode': 'off'
                 }, **res['roadm-connections'][0]),
            res['roadm-connections'][0]
        )
        self.assertDictEqual(
            {'src-if': 'SRG1-PP3-TXRX-nmc-713:720'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG1-TTP-TXRX-nmc-713:720'},
            res['roadm-connections'][0]['destination'])

    def test_11_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-713:720")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-713:720',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:opticalChannel',
                 'supporting-port': '1'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            {u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5,
             u'modulation-format': 'dp-qpsk',
             u'frequency': 195.8},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_12_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-OTU',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:otnOtu',
                 'supporting-port': '1',
                 'supporting-interface': 'XPDR1-NETWORK1-7'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        input_dict_2 = {'tx-sapi': 'AMkDwQ7xTmRI',
                        'expected-dapi': 'AMkDwQ7xTmRI',
                        'rate': 'org-openroadm-otn-common-types:OTU4',
                        'fec': 'scfec'}
        self.assertDictEqual(input_dict_2,
                             res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_13_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-ODU")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the 2 following statements replace self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-NETWORK1-ODU',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
                 'type': 'org-openroadm-interfaces:otnOdu',
                 'supporting-port': '1',
                 'supporting-interface': 'XPDR1-NETWORK1-OTU'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            dict({
                 'rate': 'org-openroadm-otn-common-types:ODU4',
                 u'monitoring-mode': u'terminated'
                 }, **res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']),
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']
        )
        self.assertDictEqual({u'exp-payload-type': u'07', u'payload-type': u'07'},
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # the following statement replaces self.assertDictContainsSubset deprecated in python 3.2
        self.assertDictEqual(
            dict({
                 'name': 'XPDR1-CLIENT1-ETHERNET',
                 'administrative-state': 'inService',
                 'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
                 'type': 'org-openroadm-interfaces:ethernetCsmacd',
                 'supporting-port': 'C1'
                 }, **res['interface'][0]),
            res['interface'][0]
        )
        self.assertDictEqual(
            {u'fec': u'off', u'speed': 100000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_15_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "circuit-packs/1%2F0%2F1-PLUG-NET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('not-reserved-inuse', res['circuit-packs'][0]["equipment-state"])

    def test_16_service_path_create_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "circuit-packs/1%2F0%2F1-PLUG-CLIENT")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('not-reserved-inuse', res['circuit-packs'][0]["equipment-state"])

    def test_17_service_path_delete(self):
        response = test_utils.service_path_request("delete", "service_test", "7",
                                                   [{"renderer:node-id": "ROADM-A1",
                                                     "renderer:src-tp": "SRG1-PP3-TXRX",
                                                     "renderer:dest-tp": "DEG1-TTP-TXRX"},
                                                    {"renderer:node-id": "XPDR-A1",
                                                     "renderer:src-tp": "XPDR1-CLIENT1",
                                                     "renderer:dest-tp": "XPDR1-NETWORK1"}],
                                                   195.8, 40, 195.775, 195.825, 713,
                                                   720)
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {'result': 'Request processed', 'success': True}})

    def test_18_service_path_delete_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/DEG1-TTP-TXRX-mc-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_19_service_path_delete_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/DEG1-TTP-TXRX-nmc-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_20_service_path_delete_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/SRG1-PP3-TXRX-mc-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_21_service_path_delete_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/SRG1-PP3-TXRX-nmc-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_22_service_path_delete_rdm_check(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "interface/SRG1-PP3-TXRX-DEG1-TTP-TXRX-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_23_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-713:720")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_24_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-OTU")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_25_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-NETWORK1-ODU")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_26_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "interface/XPDR1-CLIENT1-ETHERNET")
        self.assertEqual(response.status_code, requests.codes.conflict)
        res = response.json()
        self.assertIn({
            "error-type": "application",
            "error-tag": "data-missing",
            "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_27_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "circuit-packs/1%2F0%2F1-PLUG-NET")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual('not-reserved-available', res["circuit-packs"][0]['equipment-state'])

    def test_28_service_path_delete_xpdr_check(self):
        response = test_utils.check_netconf_node_request("XPDR-A1", "circuit-packs/1%2F0%2F1-PLUG-CLIENT")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual('not-reserved-available', res["circuit-packs"][0]['equipment-state'])

    def test_29_rdm_device_disconnected(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_30_xpdr_device_disconnected(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2, failfast=False)
