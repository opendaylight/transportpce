#!/usr/bin/env python

##############################################################################
# Copyright (c) 2019 Orange, Inc. and others.  All rights reserved.
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

    honeynode_process1 = None
    honeynode_process2 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def __start_honeynode1(cls):
        executable = ("./honeynode/2.2.1/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode1.log', 'w') as outfile:
                cls.honeynode_process1 = subprocess.Popen(
                    [executable, "17840", "sample_configs/openroadm/2.2.1/oper-XPDRA.xml"],
                    stdout=outfile)

    @classmethod
    def __start_honeynode2(cls):
        executable = ("./honeynode/2.2.1/honeynode-distribution/target/honeynode-distribution-1.18.01-hc"
                      "/honeynode-distribution-1.18.01/honeycomb-tpce")
        if os.path.isfile(executable):
            with open('honeynode2.log', 'w') as outfile:
                cls.honeynode_process2 = subprocess.Popen(
                    [executable, "17841", "sample_configs/openroadm/2.2.1/oper-ROADMA.xml"],
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
        cls.__start_honeynode1()
        time.sleep(20)
        cls.__start_honeynode2()
        time.sleep(20)
        cls.__start_odl()
        time.sleep(60)

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
        print ("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

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

    def test_02_rdm_device_connected(self):
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
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

    def test_03_rdm_portmapping_info(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1/node-info"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'rdm',
             u'node-ip-address': u'127.0.0.11',
             u'node-clli': u'NodeA',
             u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorA',
             u'node-model': u'model2'}},
            res)
        time.sleep(3)

    def test_04_rdm_portmapping_DEG1_TTP_TXRX(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1/mapping/DEG1-TTP-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '1/0',
             'logical-connection-point': 'DEG1-TTP-TXRX', 'port-direction': 'bidirectional'},
            res['mapping'])

    def test_05_rdm_portmapping_DEG2_TTP_TXRX_with_ots_oms(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1/mapping/DEG2-TTP-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG2-TTP-TXRX',
             'supporting-oms': 'OMS-DEG2-TTP-TXRX', 'supporting-ots': 'OTS-DEG2-TTP-TXRX',
             'port-direction': 'bidirectional'},
            res['mapping'])

    def test_06_rdm_portmapping_SRG1_PP3_TXRX(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1/mapping/SRG1-PP3-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'SRG1-PP3-TXRX', 'port-direction': 'bidirectional'},
            res['mapping'])

    def test_07_rdm_portmapping_SRG3_PP1_TXRX(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/ROADM-A1/mapping/SRG3-PP1-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C1', 'supporting-circuit-pack-name': '5/0',
             'logical-connection-point': 'SRG3-PP1-TXRX', 'port-direction': 'bidirectional'},
            res['mapping'])

    def test_08_xpdr_device_connected(self):
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

    def test_09_xpdr_device_connected(self):
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
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

    def test_10_xpdr_portmapping_info(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1/node-info"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            {u'node-info': {u'node-type': u'xpdr',
             u'node-ip-address': u'1.2.3.4',
             u'node-clli': u'NodeA',
             u'openroadm-version': u'2.2.1', u'node-vendor': u'vendorA',
             u'node-model': u'model2'}},
            res)
        time.sleep(3)

    def test_11_xpdr_portmapping_NETWORK1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1/mapping/XPDR1-NETWORK1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/1-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-network',
             'port-direction': 'bidirectional', 'associated-lcp': 'XPDR1-CLIENT1'},
            res['mapping'])

    def test_12_xpdr_portmapping_NETWORK2(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1/mapping/XPDR1-NETWORK2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': '1', 'supporting-circuit-pack-name': '1/0/2-PLUG-NET',
             'logical-connection-point': 'XPDR1-NETWORK2', 'port-direction': 'bidirectional',
             'associated-lcp': 'XPDR1-CLIENT2', 'port-qual': 'xpdr-network'},
            res['mapping'])

    def test_13_xpdr_portmapping_CLIENT1(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1/mapping/XPDR1-CLIENT1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C1',
             'supporting-circuit-pack-name': '1/0/1-PLUG-CLIENT',
             'logical-connection-point': 'XPDR1-CLIENT1', 'port-direction': 'bidirectional',
             'associated-lcp': 'XPDR1-NETWORK1', 'port-qual': 'xpdr-client'},
            res['mapping'])

    def test_14_xpdr_portmapping_CLIENT2(self):
        url = ("{}/config/transportpce-portmapping:network/"
               "nodes/XPDR-A1/mapping/XPDR1-CLIENT2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C1',
                 'supporting-circuit-pack-name': '1/0/2-PLUG-CLIENT',
                 'logical-connection-point': 'XPDR1-CLIENT2', 'port-direction': 'bidirectional',
                 'associated-lcp': 'XPDR1-NETWORK2', 'port-qual': 'xpdr-client'},
            res['mapping'])

    def test_15_xpdr_device_disconnected(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)

    def test_16_xpdr_device_disconnected(self):
        url = ("{}/operational/network-topology:network-topology/topology/"
               "topology-netconf/node/XPDR-A1".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_17_xpdr_device_disconnected(self):
        url = ("{}/config/transportpce-portmapping:network/nodes/XPDR-A1".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_18_rdm_device_disconnected(self):
        url = ("{}/config/network-topology:network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(20)

    def test_19_rdm_device_disconnected(self):
        url = ("{}/operational/network-topology:network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])

    def test_20_rdm_device_disconnected(self):
        url = ("{}/config/transportpce-portmapping:network/nodes/ROADM-A1".format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.not_found)
        res = response.json()
        self.assertIn(
            {"error-type":"application", "error-tag":"data-missing",
             "error-message":"Request could not be completed because the relevant data model content does not exist"},
            res['errors']['error'])


if __name__ == "__main__":
    unittest.main(verbosity=2)
