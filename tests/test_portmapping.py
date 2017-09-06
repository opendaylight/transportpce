#!/usr/bin/env python

import json
import os
import psutil
import requests
import shutil
import subprocess
import time
import unittest
import zipfile


class TransportPCEtesting(unittest.TestCase):

    @classmethod
    def __start_testtools(cls):
        executable = ("./netconf/netconf/tools/netconf-testtool/target/"
                      "netconf-testtool-1.3.0-SNAPSHOT-executable.jar")
        if os.path.isfile(executable):
            with open('testtools.log', 'w') as outfile:
                cls.testtools_process = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample-config-ROADM.xml"],
                    stdout=outfile)

    @classmethod
    def __start_odl(cls):
        zfile = "../karaf/target/transportpce-karaf-0.2.0-SNAPSHOT.zip"
        executable = "transportpce-karaf-0.2.0-SNAPSHOT/bin/karaf"
        if os.path.isfile(zfile):
            try:
                shutil.rmtree("transportpce-karaf-0.2.0-SNAPSHOT")
            except OSError:
                pass
            zipobj = zipfile.ZipFile(zfile)
            zipobj.extractall()
            with open('odl.log', 'w') as outfile:
                cls.odl_process = subprocess.Popen(
                    ["bash", executable], stdout=outfile)

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(30)

    @classmethod
    def tearDownClass(cls):
        cls.testtools_process.kill()
        for child in psutil.Process(cls.odl_process.pid).children():
            child.kill()
        cls.odl_process.kill()

    def setUp(self):
        time.sleep(10)

    def test_connect_device(self):
        url = ("http://127.0.0.1:8181/restconf/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMA")
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

    def test_device_connected(self):
        url = ("http://127.0.0.1:8181/restconf/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADMA")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_portmapping(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP3-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C6', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP6-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG1-TTP-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C9', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP9-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C16', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP16-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C4', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP4-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C2', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP2-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C14', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP14-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C11', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP11-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C7', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP7-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'DEG2-TTP-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'DEG2-TTP-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C12', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP12-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C8', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP8-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C5', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP5-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C13', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP13-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C15', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP15-TXRX'},
            res['nodes'][0]['mapping'])
        self.assertIn(
            {'supporting-port': 'C10', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP10-TXRX'},
            res['nodes'][0]['mapping'])


def test_suite():
    suite = unittest.TestSuite()
    suite.addTest(TransportPCEtesting('test_connect_device'))
    suite.addTest(TransportPCEtesting('test_device_connected'))
    suite.addTest(TransportPCEtesting('test_portmapping'))
    return suite

if __name__ == "__main__":
    RUNNER = unittest.TextTestRunner(verbosity=2)
    RUNNER.run(test_suite())
