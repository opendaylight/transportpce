#!/usr/bin/env python

##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest
import logging
import test_utils


def extract_a_from_b(a, b):
    return dict([(i, b[i]) for i in b.keys() if i in a.keys()])


class TransportPCEtesting(unittest.TestCase):

    honeynode_process1 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def setUpClass(cls):
        print("starting honeynode1...")
        cls.honeynode_process1 = test_utils.start_spdra_honeynode()
        time.sleep(30)

        print("starting opendaylight...")
        cls.odl_process = test_utils.start_tpce()
        time.sleep(60)
        print("opendaylight started")

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()
        for child in psutil.Process(cls.honeynode_process1.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process1.send_signal(signal.SIGINT)
        cls.honeynode_process1.wait()

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "SPDR-SA1",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17845",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(10)
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_get_portmapping_CLIENT1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/SPDR-SA1/mapping/XPDR1-CLIENT1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': [
                'org-openroadm-port-types:if-10GE-ODU2e',
                'org-openroadm-port-types:if-10GE-ODU2',
                'org-openroadm-port-types:if-10GE'],
             'supporting-port': 'CP1-SFP4-P1',
             'supporting-circuit-pack-name': 'CP1-SFP4',
             'logical-connection-point': 'XPDR1-CLIENT1',
              'port-direction': 'bidirectional',
             'port-qual': 'xpdr-client',
              'lcp-hash-val': '8b3efff522736722500b5e68fb6e696e'},
            res['mapping'])

    def test_03_get_portmapping_NETWORK1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/SPDR-SA1/mapping/XPDR1-NETWORK1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {"logical-connection-point": "XPDR1-NETWORK1",
             "supporting-port": "CP1-CFP0-P1",
             "supported-interface-capability": [
                 "org-openroadm-port-types:if-OCH-OTU4-ODU4"
             ],
                "port-direction": "bidirectional",
                "port-qual": "xpdr-network",
                "supporting-circuit-pack-name": "CP1-CFP0",
                "xponder-type": "mpdr",
             'lcp-hash-val': '1021db8d2affe7386705c438c67ea21f'},
            res['mapping'])

    def test_04_service_path_create_OCH_OTU4(self):
        url = "{}/operations/transportpce-device-renderer:service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service_ODU4",
            "wave-number": "1",
            "modulation-format": "qpsk",
            "operation": "create",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "dest-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes: ', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'otu-interface-id': ['XPDR1-NETWORK1-OTU'],
             'och-interface-id': ['XPDR1-NETWORK1-1']}, res["output"]['node-interface'])

    def test_05_get_portmapping_NETWORK1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/SPDR-SA1/mapping/XPDR1-NETWORK1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {"logical-connection-point": "XPDR1-NETWORK1",
             "supporting-port": "CP1-CFP0-P1",
             "supported-interface-capability": [
                 "org-openroadm-port-types:if-OCH-OTU4-ODU4"
             ],
                "port-direction": "bidirectional",
                "port-qual": "xpdr-network",
                "supporting-circuit-pack-name": "CP1-CFP0",
                "xponder-type": "mpdr",
                "lcp-hash-val": "1021db8d2affe7386705c438c67ea21f"},
            res['mapping'])

    def test_06_check_interface_och(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        input_dict = {'name': 'XPDR1-NETWORK1-1',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-CFP0',
                      'type': 'org-openroadm-interfaces:opticalChannel',
                      'supporting-port': 'CP1-CFP0-P1'
                      }
        # assertDictContainsSubset is deprecated
        '''
        self.assertDictContainsSubset({'name': 'XPDR1-NETWORK1-1', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-CFP0',
                                       'type': 'org-openroadm-interfaces:opticalChannel',
                                       'supporting-port': 'CP1-CFP0-P1'}, res['interface'][0])
        '''
        self.assertDictEqual(input_dict,
                             extract_a_from_b(input_dict,
                                  res['interface'][0])
                             )
        self.assertDictEqual(
            {u'frequency': 196.1, u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_07_check_interface_OTU(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-NETWORK1-OTU',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-CFP0',
                      'supporting-interface': 'XPDR1-NETWORK1-1',
                      'type': 'org-openroadm-interfaces:otnOtu',
                      'supporting-port': 'CP1-CFP0-P1'}

        # assertDictContainsSubset is deprecated
        '''
        self.assertDictContainsSubset({'name': 'XPDR1-NETWORK1-OTU', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-1',
                                       'type': 'org-openroadm-interfaces:otnOtu',
                                       'supporting-port': 'CP1-CFP0-P1'}, res['interface'][0])
        '''
        self.assertDictEqual(input_dict, extract_a_from_b(input_dict,
                                                          res['interface'][0])
                             )

        self.assertDictEqual(
            {u'rate': u'org-openroadm-otn-common-types:OTU4',
             u'fec': u'scfec'},
            res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_08_otn_service_path_create_ODU4(self):
        url = "{}/operations/transportpce-device-renderer:otn-service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service_ODU4",
            "operation": "create",
            "service-rate": "100G",
            "service-type": "ODU",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "network-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'odu-interface-id': ['XPDR1-NETWORK1-ODU4']}, res["output"]['node-interface'])

    def test_09_get_portmapping_NETWORK1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/SPDR-SA1/mapping/XPDR1-NETWORK1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {"logical-connection-point": "XPDR1-NETWORK1",
            "supporting-port": "CP1-CFP0-P1",
            "supported-interface-capability": [
                "org-openroadm-port-types:if-OCH-OTU4-ODU4"
            ],
            "port-direction": "bidirectional",
            "port-qual": "xpdr-network",
            "supporting-circuit-pack-name": "CP1-CFP0",
            "xponder-type": "mpdr",
            "supporting-odu4": "XPDR1-NETWORK1-ODU4",
             "lcp-hash-val": "1021db8d2affe7386705c438c67ea21f"
             },
            res['mapping'])

    def test_10_check_interface_ODU4(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU4"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-CFP0-P1'}
        input_dict_2 = {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
                        'rate': 'org-openroadm-otn-common-types:ODU4'}

        self.assertDictEqual(input_dict_1, extract_a_from_b(input_dict_1,
                                                            res['interface'][0])
                             )
        self.assertDictEqual(input_dict_2,
                             extract_a_from_b(input_dict_2,
                                              res['interface'][0][
                                                'org-openroadm-otn-odu-interfaces:odu'])

                             )
        '''
        self.assertDictContainsSubset({'name': 'XPDR1-NETWORK1-ODU4', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-CFP0', 'supporting-interface': 'XPDR1-NETWORK1-OTU',
                                       'type': 'org-openroadm-interfaces:otnOdu',
                                       'supporting-port': 'CP1-CFP0-P1'}, res['interface'][0])
        self.assertDictContainsSubset(
            {'odu-function': 'org-openroadm-otn-common-types:ODU-TTP',
             'rate': 'org-openroadm-otn-common-types:ODU4'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        '''
        self.assertDictEqual(
            {u'payload-type': u'21', u'exp-payload-type': u'21'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_11_otn_service_path_create_10GE(self):
        url = "{}/operations/transportpce-device-renderer:otn-service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service1",
            "operation": "create",
            "service-rate": "10G",
            "service-type": "Ethernet",
            "ethernet-encoding": "eth encode",
            "trib-slot": ["1"],
            "trib-port-number": "1",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "client-tp": "XPDR1-CLIENT1",
                 "network-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Otn Service path was set up successfully for node :SPDR-SA1', res["output"]["result"])
        self.assertTrue(res["output"]["success"])
        self.assertIn(
            {'node-id': 'SPDR-SA1',
             'connection-id': ['XPDR1-CLIENT1-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1'],
             'odu-interface-id': ['XPDR1-NETWORK1-ODU2e-service1', 'XPDR1-CLIENT1-ODU2e-service1'],
             'eth-interface-id': ['XPDR1-CLIENT1-ETHERNET10G']}, res["output"]['node-interface'])

    def test_12_check_interface_10GE_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET10G"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict = {'name': 'XPDR1-CLIENT1-ETHERNET10G',
                      'administrative-state': 'inService',
                      'supporting-circuit-pack-name': 'CP1-SFP4',
                      'type': 'org-openroadm-interfaces:ethernetCsmacd',
                      'supporting-port': 'CP1-SFP4-P1'
                      }

        '''
        self.assertDictContainsSubset({'name': 'XPDR1-CLIENT1-ETHERNET10G', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-SFP4',
                                       'type': 'org-openroadm-interfaces:ethernetCsmacd',
                                       'supporting-port': 'CP1-SFP4-P1'}, res['interface'][0])
        '''
        self.assertDictEqual(input_dict, extract_a_from_b(input_dict,
                                                          res['interface'][0])
                             )
        self.assertDictEqual(
            {u'speed': 10000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_13_check_interface_ODU2E_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()

        input_dict_1 = {'name': 'XPDR1-CLIENT1-ODU2e-service1',
                        'administrative-state': 'inService',
                        'supporting-circuit-pack-name': 'CP1-SFP4',
                        'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                        'type': 'org-openroadm-interfaces:otnOdu',
                        'supporting-port': 'CP1-SFP4-P1'}
        input_dict_2 = {
          'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
          'rate': 'org-openroadm-otn-common-types:ODU2e',
          'monitoring-mode': 'terminated'}

        self.assertDictEqual(input_dict_1, extract_a_from_b(input_dict_1,
                                                          res['interface'][0])
                             )
        self.assertDictEqual(input_dict_2,
                             extract_a_from_b(input_dict_2, res['interface'][0][
                               'org-openroadm-otn-odu-interfaces:odu'])
                             )

        '''
        self.assertDictContainsSubset({'name': 'XPDR1-CLIENT1-ODU2e-service1', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-SFP4',
                                       'supporting-interface': 'XPDR1-CLIENT1-ETHERNET10G',
                                       'type': 'org-openroadm-interfaces:otnOdu',
                                       'supporting-port': 'CP1-SFP4-P1'}, res['interface'][0])
        self.assertDictContainsSubset({
            'odu-function': 'org-openroadm-otn-common-types:ODU-TTP-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'terminated'}, res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        '''
        self.assertDictEqual(
            {u'payload-type': u'03', u'exp-payload-type': u'03'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_check_interface_ODU2E_NETWORK(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
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

        self.assertDictEqual(input_dict_1, extract_a_from_b(input_dict_1,
                                                            res['interface'][0])
                             )

        self.assertDictEqual(input_dict_2,
                             extract_a_from_b(input_dict_2,
                                              res['interface'][0][
                                                'org-openroadm-otn-odu-interfaces:odu']
                                              ))

        self.assertDictEqual(input_dict_3,
                             extract_a_from_b(input_dict_3,
                                              res['interface'][0][
                                                'org-openroadm-otn-odu-interfaces:odu'][
                                                'parent-odu-allocation']))

        '''
        self.assertDictContainsSubset({'name': 'XPDR1-NETWORK1-ODU2e-service1', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': 'CP1-CFP0',
                                       'supporting-interface': 'XPDR1-NETWORK1-ODU4',
                                       'type': 'org-openroadm-interfaces:otnOdu',
                                       'supporting-port': 'CP1-CFP0-P1'}, res['interface'][0])
        self.assertDictContainsSubset({
            'odu-function': 'org-openroadm-otn-common-types:ODU-CTP',
            'rate': 'org-openroadm-otn-common-types:ODU2e',
            'monitoring-mode': 'monitored'}, res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictContainsSubset(
            {'trib-port-number': 1},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['parent-odu-allocation'])
        '''
        self.assertIn(1,
                      res['interface'][0][
                        'org-openroadm-otn-odu-interfaces:odu'][
                        'parent-odu-allocation']['trib-slots'])

    def test_15_check_ODU2E_connection(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "odu-connection/XPDR1-CLIENT1-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        input_dict_1 = {
          'connection-name':
            'XPDR1-CLIENT1-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1',
          'direction': 'bidirectional'
        }

        self.assertDictEqual(input_dict_1,
                             extract_a_from_b(input_dict_1,
                                              res['odu-connection'][0]))
        '''
        self.assertDictContainsSubset({
            'connection-name': 'XPDR1-CLIENT1-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1',
            'direction': 'bidirectional'},
            res['odu-connection'][0])
        '''
        self.assertDictEqual({u'dst-if': u'XPDR1-NETWORK1-ODU2e-service1'},
                             res['odu-connection'][0]['destination'])
        self.assertDictEqual({u'src-if': u'XPDR1-CLIENT1-ODU2e-service1'},
                             res['odu-connection'][0]['source'])

    def test_16_otn_service_path_delete_10GE(self):
        url = "{}/operations/transportpce-device-renderer:otn-service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service1",
            "operation": "delete",
            "service-rate": "10G",
            "service-type": "Ethernet",
            "ethernet-encoding": "eth encode",
            "trib-slot": ["1"],
            "trib-port-number": "1",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "client-tp": "XPDR1-CLIENT1",
                 "network-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_17_check_no_ODU2E_connection(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "odu-connection/XPDR1-CLIENT1-ODU2e-service1-x-XPDR1-NETWORK1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_18_check_no_interface_ODU2E_NETWORK(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_19_check_no_interface_ODU2E_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ODU2e-service1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_20_check_no_interface_10GE_CLIENT(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET10G"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_21_otn_service_path_delete_ODU4(self):
        url = "{}/operations/transportpce-device-renderer:otn-service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service_ODU4",
            "operation": "delete",
            "service-rate": "100G",
            "service-type": "ODU",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "network-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_22_check_no_interface_ODU4(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU4"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_23_service_path_delete_OCH_OTU4(self):
        url = "{}/operations/transportpce-device-renderer:service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "service-name": "service_OTU4",
            "wave-number": "1",
            "modulation-format": "qpsk",
            "operation": "delete",
            "nodes": [
                {"node-id": "SPDR-SA1",
                 "dest-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        time.sleep(3)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        self.assertTrue(res["output"]["success"])

    def test_24_check_no_interface_OTU4(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_25_check_no_interface_OCH(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/SPDR-SA1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)

    def test_26_disconnect_SPDR_SA1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)


if __name__ == "__main__":
    unittest.main(verbosity=2)
