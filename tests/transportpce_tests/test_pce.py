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


class TransportPCEtesting(unittest.TestCase):

    odl_process = None
    simple_data = None
    complex_data = None
    restconf_baseurl = "http://127.0.0.1:8181/restconf"

    @classmethod
    def _get_file(cls):
        simple_topology_file = "sample_configs/NW-simple-topology.xml"
        if os.path.isfile(simple_topology_file):
            with open(simple_topology_file, 'r') as simple_file:
                cls.simple_data = simple_file.read();
        complex_topology_file = "sample_configs/NW-for-test-5-4.xml"
        if os.path.isfile(complex_topology_file):
            with open(complex_topology_file, 'r') as complex_file:
                cls.complex_data = complex_file.read();

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):  # a class method called before tests in an individual class run.
        cls._get_file()
        cls.__start_odl()
        time.sleep(90)

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):  # instruction executed before each test method
        time.sleep(1)

    # Load simple topology
    def test_01_load_simple_topology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        body = self.simple_data
        headers = {'content-type': 'application/xml',
        "Accept": "application/json"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Get existing nodeId
    def test_02_get_nodeId(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/node/XPONDER-1-2"
                .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'],
            'XPONDER-1-2')
        time.sleep(1)

    # Get existing linkId
    def test_03_get_linkId(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/link/XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['ietf-network-topology:link'][0]['link-id'],
            'XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX')
        time.sleep(1)

    # Path Computation success
    def test_04_path_computation(self):
        url = ("{}/operations/pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service-1",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-1"
                },
                "service-a-end": {
                    "node-id": "XPONDER-1-2",
                    "service-rate": "0",
                    "clli": "ORANGE1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-3-2",
                    "service-rate": "0",
                    "clli": "ORANGE1"
                }
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Delete topology
    def test_05_delete_simple_topology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/xml',
        "Accept": "application/json"}
        response = requests.request(
            "DELETE", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted topology
    def test_06_test_topology_simple_deleted(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/node/XPONDER-1-2"
                .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    # Load simple topology
    def test_07_load_complex_topology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        body = self.complex_data
        headers = {'content-type': 'application/xml',
        "Accept": "application/json"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 201)
        time.sleep(2)

    # Get existing nodeId
    def test_08_get_nodeId(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/node/XPONDER-3-2"
                .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'],
            'XPONDER-3-2')
        time.sleep(1)

    # Test failed path computation
    def test_09_fail_path_computation(self):
        url = ("{}/operations/pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-handler-header": {
                    "request-id": "request-1"
                }
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service Name is not set',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Test1 success path computation
    def test_10_success1_path_computation(self):
        url = ("{}/operations/pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service1",
                "resource-reserve": "true",
                "service-handler-header": {
                  "request-id": "request1"
                },
                "service-a-end": {
                  "service-format": "Ethernet",
                  "service-rate": "0",
                  "clli": "clli11",
                  "node-id": "XPONDER-2-2",
                  "tx-direction": {
                    "port": {
                      "port-device-name": "Some port-device-name",
                      "port-type": "Some port-type",
                      "port-name": "Some port-name",
                      "port-rack": "Some port-rack",
                      "port-shelf": "Some port-shelf",
                      "port-slot": "Some port-slot",
                      "port-sub-slot": "Some port-sub-slot"
                    }
                  },
                  "rx-direction": {
                    "port": {
                      "port-device-name": "Some port-device-name",
                      "port-type": "Some port-type",
                      "port-name": "Some port-name",
                      "port-rack": "Some port-rack",
                      "port-shelf": "Some port-shelf",
                      "port-slot": "Some port-slot",
                      "port-sub-slot": "Some port-sub-slot"
                    }
                  }
                },
                "service-z-end": {
                  "service-format": "Ethernet",
                  "service-rate": "0",
                  "clli": "clli11",
                  "node-id": "XPONDER-1-2",
                  "tx-direction": {
                    "port": {
                      "port-device-name": "Some port-device-name",
                      "port-type": "Some port-type",
                      "port-name": "Some port-name",
                      "port-rack": "Some port-rack",
                      "port-shelf": "Some port-shelf",
                      "port-slot": "Some port-slot",
                      "port-sub-slot": "Some port-sub-slot"
                    }
                  },
                  "rx-direction": {
                    "port": {
                      "port-device-name": "Some port-device-name",
                      "port-type": "Some port-type",
                      "port-name": "Some port-name",
                      "port-rack": "Some port-rack",
                      "port-shelf": "Some port-shelf",
                      "port-slot": "Some port-slot",
                      "port-sub-slot": "Some port-sub-slot"
                    }
                  }
                },
                "hard-constraints": {
                  "customer-code": [
                    "Some customer-code"
                  ],
                  "co-routing": {
                    "existing-service": [
                      "Some existing-service"
                    ]
                  }
                },
                "soft-constraints": {
                  "customer-code": [
                    "Some customer-code"
                  ],
                  "co-routing": {
                    "existing-service": [
                      "Some existing-service"
                    ]
                  }
                },
                "pce-metric": "hop-count",
                "locally-protected-links": "true"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
            res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Test2 success path computation with path description
    def test_11_success2_path_computation(self):
        url = ("{}/operations/pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service 1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request 1"
                },
                "service-a-end": {
                    "service-rate": "0",
                    "node-id": "XPONDER-1-2"
                },
                "service-z-end": {
                    "service-rate": "0",
                    "node-id": "XPONDER-3-2"
             },
             "pce-metric": "hop-count"
           }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
            res['output']['configuration-response-common']['response-message'])
        self.assertEqual(5 , res['output']['response-parameters']['path-description']
            ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(5 , res['output']['response-parameters']['path-description']
            ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(5)

    # Test3 success path computation with hard-constraints exclude
    def test_12_success3_path_computation(self):
        url = ("{}/operations/pce:path-computation-request"
              .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service 1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request 1"
                },
                "service-a-end": {
                    "service-rate": "0",
                    "node-id": "XPONDER-1-2"
                },
                "service-z-end": {
                    "service-rate": "0",
                    "node-id": "XPONDER-3-2"
                },
                "hard-constraints": {
                    "exclude_": {
                        "node-id": ["OpenROADM-2-1", "OpenROADM-2-2"]
                    }
                },
                "pce-metric": "hop-count"
            }
        }
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
            res['output']['configuration-response-common']['response-message'])
        self.assertEqual(9 , res['output']['response-parameters']['path-description']
            ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(9 , res['output']['response-parameters']['path-description']
            ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(5)

    # Delete complex topology
    def test_13_delete_complex_topology(self):
        url = ("{}/config/ietf-network:network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/xml',
        "Accept": "application/json"}
        response = requests.request(
            "DELETE", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted complex topology
    def test_14_test_topology_complex_deleted(self):
        url = ("{}/config/ietf-network:network/openroadm-topology/node/XPONDER-3-2"
                .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
        "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
