#!/usr/bin/env python

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest


class TransportPCEtesting(unittest.TestCase):

    testtools_process = None
    odl_process = None

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
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(60)

    @classmethod
    def tearDownClass(cls):
        cls.testtools_process.send_signal(signal.SIGINT)
        cls.testtools_process.wait()
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):
        time.sleep(1)

    def test_01_connect_device(self):
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
        time.sleep(20)

    def test_02_device_connected(self):
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
        time.sleep(10)

    def test_03_portmapping_SRG1_PP3_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP3-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C3', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP3-TXRX'},
            res['mapping'])

    def test_04_portmapping_SRG1_PP6_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP6-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C6', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP6-TXRX'},
            res['mapping'])

    def test_05_portmapping_DEG1_TTP_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/DEG1-TTP-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '2/0',
             'logical-connection-point': 'DEG1-TTP-TXRX'},
            res['mapping'])

    def test_06_portmapping_SRG1_PP9_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP9-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C9', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP9-TXRX'},
            res['mapping'])

    def test_07_portmapping_SRG1_PP16_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP16-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C16', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP16-TXRX'},
            res['mapping'])

    def test_08_portmapping_SRG1_PP4_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP4-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C4', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP4-TXRX'},
            res['mapping'])

    def test_09_portmapping_SRG1_PP2_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP2-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C2', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP2-TXRX'},
            res['mapping'])

    def test_10_portmapping_SRG1_PP14_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP14-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C14', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP14-TXRX'},
            res['mapping'])

    def test_11_portmapping_SRG1_PP11_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP11-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C11', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP11-TXRX'},
            res['mapping'])

    def test_12_portmapping_SRG1_PP7_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP7-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C7', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP7-TXRX'},
            res['mapping'])

    def test_13_portmapping_DEG2_TTP_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/DEG2-TTP-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'DEG2-TTP-TXRX'},
            res['mapping'])

    def test_14_portmapping_DEG2_TTP_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/DEG2-TTP-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'L1', 'supporting-circuit-pack-name': '3/0',
             'logical-connection-point': 'DEG2-TTP-TXRX'},
            res['mapping'])

    def test_15_portmapping_SRG1_PP12_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP12-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C12', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP12-TXRX'},
            res['mapping'])

    def test_16_portmapping_SRG1_PP8_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP8-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C8', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP8-TXRX'},
            res['mapping'])

    def test_17_portmapping_SRG1_PP5_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP5-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C5', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP5-TXRX'},
            res['mapping'])

    def test_18_portmapping_SRG1_PP13_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP13-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C13', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP13-TXRX'},
            res['mapping'])

    def test_19_portmapping_SRG1_PP15_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP15-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C15', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP15-TXRX'},
            res['mapping'])

    def test_20_portmapping_SRG1_PP10_TXRX(self):
        url = ("http://127.0.0.1:8181/restconf/config/portmapping:network/"
               "nodes/ROADMA/mapping/SRG1-PP10-TXRX")
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn(
            {'supporting-port': 'C10', 'supporting-circuit-pack-name': '4/0',
             'logical-connection-point': 'SRG1-PP10-TXRX'},
            res['mapping'])

    def test_21_cross_connection_DEG1_TTP_TXRX_SRG1_PP3_TXRX(self):
        url = "http://127.0.0.1:8181/restconf/operations/renderer:service-path"
        data = {"renderer:input": {
            "renderer:service-name": "service_32",
            "renderer:wave-number": "32",
            "renderer:operation": "create",
            "renderer:nodes": [
                {"renderer:node-id": "ROADMA",
                 "renderer:src-tp": "DEG1-TTP-TXRX",
                 "renderer:dest-tp": "SRG1-PP3-TXRX"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {
                'result':
                'Roadm-connection successfully created for nodes [ROADMA]'}})

    def test_22_cross_connection_SRG1_PP3_TXRX_DEG1_TTP_TXRX(self):
        url = "http://127.0.0.1:8181/restconf/operations/renderer:service-path"
        data = {"renderer:input": {
            "renderer:service-name": "service_32",
            "renderer:wave-number": "32",
            "renderer:operation": "create",
            "renderer:nodes": [
                {"renderer:node-id": "ROADMA",
                 "renderer:src-tp": "SRG1-PP3-TXRX",
                 "renderer:dest-tp": "DEG1-TTP-TXRX"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {
                'result':
                'Roadm-connection successfully created for nodes [ROADMA]'}})

    def test_23_delete_DEG1_TTP_TXRX_SRG1_PP3_TXRX(self):
        url = "http://127.0.0.1:8181/restconf/operations/renderer:service-path"
        data = {"renderer:input": {
            "renderer:service-name": "service_32",
            "renderer:wave-number": "32",
            "renderer:operation": "delete",
            "renderer:nodes": [
                {"renderer:node-id": "ROADMA",
                 "renderer:src-tp": "DEG1-TTP-TXRX",
                 "renderer:dest-tp": "SRG1-PP3-TXRX"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {'result': 'Request processed'}})

    def test_24_delete_SRG1_PP3_TXRX_DEG1_TTP_TXRX(self):
        url = "http://127.0.0.1:8181/restconf/operations/renderer:service-path"
        data = {"renderer:input": {
            "renderer:service-name": "service_32",
            "renderer:wave-number": "32",
            "renderer:operation": "delete",
            "renderer:nodes": [
                {"renderer:node-id": "ROADMA",
                 "renderer:src-tp": "SRG1-PP3-TXRX",
                 "renderer:dest-tp": "DEG1-TTP-TXRX"}]}}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "POST", url, data=json.dumps(data),
            headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json(), {
            'output': {'result': 'Request processed'}})


if __name__ == "__main__":
    unittest.main(verbosity=2)
