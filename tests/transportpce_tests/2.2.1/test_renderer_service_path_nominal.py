#!/usr/bin/env python

#############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
#############################################################################

import unittest
import requests
import time
import subprocess
import signal
import json
import os
import psutil
import shutil
from unittest.result import failfast
import test_utils


class TransportPCERendererTesting(unittest.TestCase):

    honeynode_process1 = None
    honeynode_process2 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

# START_IGNORE_XTESTING

    @classmethod
    def setUpClass(cls):
        print("starting honeynode1...")
        cls.honeynode_process1 = test_utils.start_xpdra_honeynode()
        time.sleep(20)

        print("starting honeynode2...")
        cls.honeynode_process2 = test_utils.start_roadma_honeynode()
        time.sleep(20)
        print("all honeynodes started")

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
        for child in psutil.Process(cls.honeynode_process2.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process2.send_signal(signal.SIGINT)
        cls.honeynode_process2.wait()

    def setUp(self):
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

# END_IGNORE_XTESTING

    def test_01_rdm_device_connected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "ROADM-A1",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17841",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_02_xpdr_device_connected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "XPDR-A1",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17840",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_03_rdm_portmapping(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional'},
            res['nodes'][0]['mapping'])

    def test_04_xpdr_portmapping(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-OCH'],
             'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional', 'connection-map-lcp': 'XPDR1-CLIENT1'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supported-interface-capability': ['org-openroadm-port-types:if-100GE'],
             'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'connection-map-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client'},
            res['nodes'][0]['mapping'])

    def test_05_service_path_create(self):
        url = "{}/operations/transportpce-device-renderer:service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "renderer:service-name": "service_test",
            "renderer:wave-number": "7",
            "renderer:modulation-format": "qpsk",
            "renderer:operation": "create",
            "renderer:nodes": [
                {"renderer:node-id": "ROADM-A1",
                 "renderer:src-tp": "SRG1-PP3-TXRX",
                 "renderer:dest-tp": "DEG1-TTP-TXRX"},
                {"renderer:node-id": "XPDR-A1",
                 "renderer:src-tp": "XPDR1-CLIENT1",
                 "renderer:dest-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes: ROADM-A1', res["output"]["result"])

    def test_06_service_path_create_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/DEG1-TTP-TXRX-nmc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset({'name': 'DEG1-TTP-TXRX-nmc-7', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': '1/0',
                                       'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                                       'supporting-port': 'L1'}, res['interface'][0])
        self.assertDictEqual(
            {u'frequency': 195.8, u'width': 40},
            res['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp'])

    def test_07_service_path_create_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/DEG1-TTP-TXRX-mc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset({'name': 'DEG1-TTP-TXRX-mc-7', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': '1/0',
                                       'type': 'org-openroadm-interfaces:mediaChannelTrailTerminationPoint',
                                       'supporting-port': 'L1'}, res['interface'][0])
        self.assertDictEqual(
            {u'min-freq': 195.775, u'max-freq': 195.825},
            res['interface'][0]['org-openroadm-media-channel-interfaces:mc-ttp'])

    def test_08_service_path_create_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/SRG1-PP3-TXRX-nmc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset({'name': 'SRG1-PP3-TXRX-nmc-7', 'administrative-state': 'inService',
                                       'supporting-circuit-pack-name': '3/0',
                                       'type': 'org-openroadm-interfaces:networkMediaChannelConnectionTerminationPoint',
                                       'supporting-port': 'C3'}, res['interface'][0])
        self.assertDictEqual(
            {u'frequency': 195.8, u'width': 40},
            res['interface'][0]['org-openroadm-network-media-channel-interfaces:nmc-ctp'])

    # -mc supporting interfaces must not be created for SRG, only degrees
    def test_09_service_path_create_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/SRG1-PP3-TXRX-mc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_10_service_path_create_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP3-TXRX-DEG1-TTP-TXRX-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
            {'connection-name': 'SRG1-PP3-TXRX-DEG1-TTP-TXRX-7',
             'opticalControlMode': 'off'},
            res['roadm-connections'][0])
        self.assertDictEqual(
            {'src-if': 'SRG1-PP3-TXRX-nmc-7'},
            res['roadm-connections'][0]['source'])
        self.assertDictEqual(
            {'dst-if': 'DEG1-TTP-TXRX-nmc-7'},
            res['roadm-connections'][0]['destination'])

    def test_11_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
            {'name': 'XPDR1-NETWORK1-7', 'administrative-state': 'inService',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'type': 'org-openroadm-interfaces:opticalChannel',
             'supporting-port': '1'},
            res['interface'][0])
        self.assertDictEqual(
            {u'rate': u'org-openroadm-common-types:R100G',
             u'transmit-power': -5,
             u'frequency': 195.8},
            res['interface'][0]['org-openroadm-optical-channel-interfaces:och'])

    def test_12_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
            {'name': 'XPDR1-NETWORK1-OTU', 'administrative-state': 'inService',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'type': 'org-openroadm-interfaces:otnOtu',
             'supporting-port': '1',
             'supporting-interface': 'XPDR1-NETWORK1-7'},
            res['interface'][0])
        self.assertDictEqual(
            {u'rate': u'org-openroadm-otn-common-types:OTU4',
             u'fec': u'scfec'},
            res['interface'][0]['org-openroadm-otn-otu-interfaces:otu'])

    def test_13_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
            {'name': 'XPDR1-NETWORK1-ODU', 'administrative-state': 'inService',
             'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'type': 'org-openroadm-interfaces:otnOdu',
             'supporting-port': '1',
             'supporting-interface': 'XPDR1-NETWORK1-OTU'},
            res['interface'][0])
        self.assertDictContainsSubset(
            {'rate': 'org-openroadm-otn-common-types:ODU4',
             u'monitoring-mode': u'terminated'},
            res['interface'][0]['org-openroadm-otn-odu-interfaces:odu'])
        self.assertDictEqual({u'exp-payload-type': u'07', u'payload-type': u'07'},
                             res['interface'][0]['org-openroadm-otn-odu-interfaces:odu']['opu'])

    def test_14_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertDictContainsSubset(
            {'name': 'XPDR1-CLIENT1-ETHERNET', 'administrative-state': 'inService',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'type': 'org-openroadm-interfaces:ethernetCsmacd',
             'supporting-port': 'C1'},
            res['interface'][0])
        self.assertDictEqual(
            {u'fec': u'off', u'speed': 100000},
            res['interface'][0]['org-openroadm-ethernet-interfaces:ethernet'])

    def test_15_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "circuit-packs/1%2F0%2F1-PLUG-NET"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('not-reserved-inuse', res['circuit-packs'][0]["equipment-state"])

    def test_16_service_path_create_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "circuit-packs/1%2F0%2F1-PLUG-CLIENT"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('not-reserved-inuse', res['circuit-packs'][0]["equipment-state"])

    def test_17_service_path_delete(self):
        url = "{}/operations/transportpce-device-renderer:service-path".format(self.restconf_baseurl)
        data = {"renderer:input": {
            "renderer:service-name": "service_test",
            "renderer:wave-number": "7",
            "renderer:operation": "delete",
            "renderer:nodes": [
                {"renderer:node-id": "ROADM-A1",
                 "renderer:src-tp": "SRG1-PP3-TXRX",
                 "renderer:dest-tp": "DEG1-TTP-TXRX"},
                {"renderer:node-id": "XPDR-A1",
                 "renderer:src-tp": "XPDR1-CLIENT1",
                 "renderer:dest-tp": "XPDR1-NETWORK1"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {'result': 'Request processed', 'success': True}})

    def test_18_service_path_delete_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/DEG1-TTP-TXRX-mc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_19_service_path_delete_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/DEG1-TTP-TXRX-nmc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
             "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_20_service_path_delete_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/SRG1-PP3-TXRX-mc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_21_service_path_delete_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/SRG1-PP3-TXRX-nmc-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_22_service_path_delete_rdm_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/ROADM-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "roadm-connections/SRG1-PP3-TXRX-DEG1-TTP-TXRX-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_23_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-7"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_24_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-OTU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_25_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-NETWORK1-ODU"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_26_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "interface/XPDR1-CLIENT1-ETHERNET"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type": "application", "error-tag": "data-missing",
                "error-message": "Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_27_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "circuit-packs/1%2F0%2F1-PLUG-NET"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual('not-reserved-available', res["circuit-packs"][0]['equipment-state'])

    def test_28_service_path_delete_xpdr_check(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/"
               "node/XPDR-A1/yang-ext:mount/org-openroadm-device:org-openroadm-device/"
               "circuit-packs/1%2F0%2F1-PLUG-CLIENT"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual('not-reserved-available', res["circuit-packs"][0]['equipment-state'])

    def test_29_rdm_device_disconnected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)

    def test_30_xpdr_device_disconnected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)


if __name__ == "__main__":
    unittest.main(verbosity=2, failfast=True)
