#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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


class TransportPCEPortMappingTesting(unittest.TestCase):

    testtools_process1 = None
    testtools_process2 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def __start_testtools(cls):
        executable = ("./netconf/netconf/tools/netconf-testtool/target/"
                      "netconf-testtool-1.3.1-executable.jar")
        if os.path.isfile(executable):
            with open('testtools1.log', 'w') as outfile:
                cls.testtools_process1 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/ord_1.2.1/sample-config-ROADM.xml",
                     "--starting-port", "17830"],
                    stdout=outfile)
            with open('testtools2.log', 'w') as outfile:
                cls.testtools_process2 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/ord_1.2.1/sample-config-XPDR.xml",
                     "--starting-port", "17831"],
                    stdout=outfile)

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(60)

    @classmethod
    def tearDownClass(cls):
        cls.testtools_process1.send_signal(signal.SIGINT)
        cls.testtools_process1.wait()
        cls.testtools_process2.send_signal(signal.SIGINT)
        cls.testtools_process2.wait()
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):
        print ("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_restconfAPI(self):
        url = ("{}/operational/network-topology:network-topology/topology/"
        "topology-netconf/node/controller-config".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request("GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['node'] [0] ['netconf-node-topology:connection-status'],
                         'connected')

    def test_02_restconfAPI(self):
        url = ("{}/config/portmapping:network/nodes/controller-config"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist "},
            res['errors']['error'])

    def test_03_rdm_device_connected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMA"
              .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "ROADMA",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17830",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_04_rdm_device_connected(self):
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    def test_05_rdm_portmapping_DEG1_TTP_TXRX(self):
        url = ("{}/config/portmapping:network/"
               "nodes/ROADMA/mapping/DEG1-TTP-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG1-TTP-TXRX'},
            res['mapping'])

    def test_06_rdm_portmapping_SRG1_PP7_TXRX(self):
        url = ("{}/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP7-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C7', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP7-TXRX'},
            res['mapping'])

    def test_07_rdm_portmapping_SRG3_PP1_TXRX(self):
        url = ("{}/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG3-PP1-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C1', 'supporting-circuit-pack-name': '5/0',
             'logical-connection-point': 'SRG3-PP1-TXRX'},
            res['mapping'])

    def test_08_xpdr_device_connected(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRA"
              .format(self.restconf_baseurl))
        data = {"node": [{
            "node-id": "XPDRA",
            "netconf-node-topology:username": "admin",
            "netconf-node-topology:password": "admin",
            "netconf-node-topology:host": "127.0.0.1",
            "netconf-node-topology:port": "17831",
            "netconf-node-topology:tcp-only": "false",
            "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "PUT", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(20)

    def test_09_xpdr_device_connected(self):
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')
        time.sleep(10)

    def test_10_xpdr_portmapping_NETWORK1(self):
        url = ("{}/config/portmapping:network/"
               "nodes/XPDRA/mapping/XPDR-LINE1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR-LINE1'},
            res['mapping'])

    def test_11_xpdr_portmapping_CLIENT1(self):
        url = ("{}/config/portmapping:network/"
               "nodes/XPDRA/mapping/XPDR-CLNT1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': '1',
             'supporting-circuit-pack-name': '1/0/2-PLUG-CLIENT',
             'logical-connection-point': 'XPDR-CLNT1'},
            res['mapping'])

    def test_12_xpdr_device_disconnected(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/XPDRA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)

    def test_13_xpdr_device_disconnected(self):
        url = ("{}/operational/network-topology:network-topology/topology/"
               "topology-netconf/node/XPDRA".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist "},
            res['errors']['error'])

    def test_14_xpdr_device_disconnected(self):
        url = ("{}/config/portmapping:network/"
               "nodes/XPDRA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist "},
            res['errors']['error'])

    def test_15_rdm_device_disconnected(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)

    def test_16_rdm_device_disconnected(self):
        url = ("{}/operational/network-topology:network-topology/topology/"
               "topology-netconf/node/ROADMA".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist "},
            res['errors']['error'])

    def test_17_rdm_device_disconnected(self):
        url = ("{}/config/portmapping:network/nodes/ROADMA"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist "},
            res['errors']['error'])


if __name__ == "__main__":
    unittest.main(verbosity=2)
