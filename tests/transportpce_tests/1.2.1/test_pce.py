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
import test_utils


class TransportPCEtesting(unittest.TestCase):

    odl_process = None
    simple_topo_bi_dir_data = None
    simple_topo_uni_dir_data = None
    complex_topo_uni_dir_data = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def _get_file(cls):
        topo_bi_dir_file = "sample_configs/honeynode-topo.xml"
        if os.path.isfile(topo_bi_dir_file):
            with open(topo_bi_dir_file, 'r') as topo_bi_dir:
                cls.simple_topo_bi_dir_data = topo_bi_dir.read()
        topo_uni_dir_file = "sample_configs/NW-simple-topology.xml"
        if os.path.isfile(topo_uni_dir_file):
            with open(topo_uni_dir_file, 'r') as topo_uni_dir:
                cls.simple_topo_uni_dir_data = topo_uni_dir.read()
        topo_uni_dir_complex_file = "sample_configs/NW-for-test-5-4.xml"
        if os.path.isfile(topo_uni_dir_complex_file):
            with open(topo_uni_dir_complex_file, 'r') as topo_uni_dir_complex:
                cls.complex_topo_uni_dir_data = topo_uni_dir_complex.read()

    @classmethod
    def setUpClass(cls):  # a class method called before tests in an individual class run.
        cls._get_file()
        print("starting opendaylight...")
        cls.odl_process = test_utils.start_tpce()
        time.sleep(90)
        print("opendaylight started")

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):  # instruction executed before each test method
        time.sleep(1)

     # Load simple bidirectional topology
    def test_01_load_simple_topology_bi(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        body = self.simple_topo_bi_dir_data
        headers = {'content-type': 'application/xml',
                   "Accept": "application/xml"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Get existing nodeId
    def test_02_get_nodeId(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/ROADMA01-SRG1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
                   "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'], 'ROADMA01-SRG1')
        time.sleep(1)

    # Get existing linkId
    def test_03_get_linkId(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/link/XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
                   "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['ietf-network-topology:link'][0]['link-id'],
            'XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX')
        time.sleep(1)

    # Path Computation success
    def test_04_path_computation_xpdr_bi(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service-1",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-1"
                },
                "service-a-end": {
                    "node-id": "XPDRA01",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "nodeA"
                },
                "service-z-end": {
                    "node-id": "XPDRC01",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "nodeC"
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

    # Path Computation success
    def test_05_path_computation_rdm_bi(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service-1",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-1"
                },
                "service-a-end": {
                    "node-id": "ROADMA01",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "NodeA"
                },
                "service-z-end": {
                    "node-id": "ROADMC01",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "NodeC"
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
    def test_06_delete_simple_topology_bi(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/xml',
                   "Accept": "application/json"}
        response = requests.request(
            "DELETE", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted topology
    def test_07_test_topology_simple_bi_deleted(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/ROADMA01-SRG1"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
                   "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    # Load simple bidirectional topology
    def test_08_load_simple_topology_uni(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        body = self.simple_topo_uni_dir_data
        headers = {'content-type': 'application/xml',
                   "Accept": "application/xml"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 201)
        time.sleep(2)

    # Get existing nodeId
    def test_09_get_nodeId(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/XPONDER-1-2"
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
    def test_10_get_linkId(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/link/XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX"
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
    def test_11_path_computation_xpdr_uni(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
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
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "ORANGE1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-3-2",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "ORANGE3"
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

    # Path Computation success
    def test_12_path_computation_rdm_uni(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request1"
                },
                "service-a-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "cll21",
                    "node-id": "OpenROADM-2-1"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "ncli22",
                    "node-id": "OpenROADM-2-2"
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
        # ZtoA path test
        atozList = len(res['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'])
        ztoaList = len(res['output']['response-parameters']['path-description']['zToA-direction']['zToA'])
        self.assertEqual(atozList, 15)
        self.assertEqual(ztoaList, 15)
        for i in range(0, 15):
            atoz = res['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
            ztoa = res['output']['response-parameters']['path-description']['zToA-direction']['zToA'][i]
            if (atoz['id'] == '14'):
                self.assertEqual(atoz['resource']['tp-id'], 'SRG1-PP1-TX')
            if (ztoa['id'] == '0'):
                self.assertEqual(ztoa['resource']['tp-id'], 'SRG1-PP1-RX')
        time.sleep(5)

    # Delete topology
    def test_13_delete_simple_topology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/xml',
                   "Accept": "application/json"}
        response = requests.request(
            "DELETE", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted topology
    def test_14_test_topology_simple_deleted(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/XPONDER-1-2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
                   "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)

    # Load simple topology
    def test_15_load_complex_topology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        body = self.complex_topo_uni_dir_data
        headers = {'content-type': 'application/xml',
                   "Accept": "application/json"}
        response = requests.request(
            "PUT", url, data=body, headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 201)
        time.sleep(2)

    # Get existing nodeId
    def test_16_get_nodeId(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/XPONDER-3-2"
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
    def test_17_fail_path_computation(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
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
    def test_18_success1_path_computation(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request1"
                },
                "service-a-end": {
                    "service-format": "Ethernet",
                    "service-rate": "100",
                    "clli": "ORANGE2",
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
                    "service-rate": "100",
                    "clli": "ORANGE1",
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
    def test_19_success2_path_computation(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service 1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request 1"
                },
                "service-a-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "node-id": "XPONDER-1-2",
                    "clli": "ORANGE1"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "node-id": "XPONDER-3-2",
                    "clli": "ORANGE3"
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
        self.assertEqual(5, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(5, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(5)

    # Test3 success path computation with hard-constraints exclude
    def test_20_success3_path_computation(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(self.restconf_baseurl))
        body = {"input": {
                "service-name": "service 1",
                "resource-reserve": "true",
                "service-handler-header": {
                    "request-id": "request 1"
                },
                "service-a-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "node-id": "XPONDER-1-2",
                    "clli": "ORANGE1"
                },
                "service-z-end": {
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "node-id": "XPONDER-3-2",
                    "clli": "ORANGE3"
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
        self.assertEqual(9, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(9, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(5)

    # Delete complex topology
    def test_21_delete_complex_topology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/xml',
                   "Accept": "application/json"}
        response = requests.request(
            "DELETE", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted complex topology
    def test_22_test_topology_complex_deleted(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology/node/XPONDER-3-2"
               .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json',
                   "Accept": "application/json"}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, 404)
        time.sleep(1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
