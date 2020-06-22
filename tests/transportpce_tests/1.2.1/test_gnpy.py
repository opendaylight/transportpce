#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import unittest
import json
import os
import time
import requests
from common import test_utils


class TransportGNPYtesting(unittest.TestCase):

    @classmethod
    def __init_logfile(cls):
        if os.path.isfile("./transportpce_tests/gnpy.log"):
            os.remove("transportpce_tests/gnpy.log")

    processes = None

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()

    @classmethod
    def tearDownClass(cls):
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

    # Mount the different topologies
    def test_01_connect_clliNetwork(self):
        url = ("{}/config/ietf-network:networks/network/clli-network"
               .format(test_utils.RESTCONF_BASE_URL))
        topo_cllinet_file = "sample_configs/gnpy/clliNetwork.json"
        if os.path.isfile(topo_cllinet_file):
            with open(topo_cllinet_file, 'r') as clli_net:
                body = clli_net.read()
        response = requests.request(
            "PUT", url, data=body, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_02_connect_openroadmNetwork(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
               .format(test_utils.RESTCONF_BASE_URL))
        topo_ordnet_file = "sample_configs/gnpy/openroadmNetwork.json"
        if os.path.isfile(topo_ordnet_file):
            with open(topo_ordnet_file, 'r') as ord_net:
                body = ord_net.read()
        response = requests.request(
            "PUT", url, data=body, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_03_connect_openroadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(test_utils.RESTCONF_BASE_URL))
        topo_ordtopo_file = "sample_configs/gnpy/openroadmTopology.json"
        if os.path.isfile(topo_ordtopo_file):
            with open(topo_ordtopo_file, 'r') as ord_topo:
                body = ord_topo.read()
        response = requests.request(
            "PUT", url, data=body, headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    # Path computed by PCE is feasible according to Gnpy
    def test_04_path_computation_FeasibleWithPCE(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(test_utils.RESTCONF_BASE_URL))
        body = {
            "input": {
                "service-name": "service-1",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-1"
                },
                "service-a-end": {
                    "node-id": "XPONDER-1",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-5",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node5"
                }
            }
        }
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '200')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'Path is calculated by PCE')
        self.assertEqual(res['output']['gnpy-response'][0]['path-dir'],
                         'A-to-Z')
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'], True)
        self.assertEqual(res['output']['gnpy-response'][1]['path-dir'],
                         'Z-to-A')
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'], True)
        time.sleep(5)

    # Path computed by PCE is not feasible by GNPy and GNPy cannot find
    # another one (low SNR)
    def test_05_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(test_utils.RESTCONF_BASE_URL))
        body = {
            "input": {
                "service-name": "service-2",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-2"
                },
                "service-a-end": {
                    "node-id": "XPONDER-1",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-5",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node5"
                },
                "hard-constraints": {
                    "include_": {
                        "ordered-hops": [
                            {
                                "hop-number": "0",
                                "hop-type": {
                                    "node-id": "OpenROADM-2"
                                }
                            },
                            {
                                "hop-number": "1",
                                "hop-type": {
                                    "node-id": "OpenROADM-3"
                                }
                            },
                            {
                                "hop-number": "2",
                                "hop-type": {
                                    "node-id": "OpenROADM-4"
                                }
                            }
                        ]
                    }
                }
            }
        }
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '500')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'No path available by PCE and GNPy ')
        self.assertEqual(res['output']['gnpy-response'][0]['path-dir'],
                         'A-to-Z')
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'],
                         False)
        self.assertEqual(res['output']['gnpy-response'][1]['path-dir'],
                         'Z-to-A')
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'],
                         False)
        time.sleep(5)

    # #PCE cannot find a path while GNPy finds a feasible one
    def test_06_path_computation_NotFoundByPCE_FoundByGNPy(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(test_utils.RESTCONF_BASE_URL))
        body = {
            "input": {
                "service-name": "service-3",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-3"
                },
                "service-a-end": {
                    "node-id": "XPONDER-1",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-4",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node5"
                },
                "hard-constraints": {
                    "include_": {
                        "ordered-hops": [
                            {
                                "hop-number": "0",
                                "hop-type": {
                                    "node-id": "OpenROADM-2"
                                }
                            },
                            {
                                "hop-number": "1",
                                "hop-type": {
                                    "node-id": "OpenROADM-3"
                                }
                            }
                        ]
                    }
                }
            }
        }
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '200')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'Path is calculated by GNPy')
        self.assertEqual(res['output']['gnpy-response'][0]['path-dir'],
                         'A-to-Z')
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'], True)
        self.assertEqual(res['output']['gnpy-response'][1]['path-dir'],
                         'Z-to-A')
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'], True)
        time.sleep(5)

    # Not found path by PCE and GNPy cannot find another one
    def test_07_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        url = ("{}/operations/transportpce-pce:path-computation-request"
               .format(test_utils.RESTCONF_BASE_URL))
        body = {
            "input": {
                "service-name": "service-4",
                "resource-reserve": "true",
                "pce-metric": "hop-count",
                "service-handler-header": {
                    "request-id": "request-4"
                },
                "service-a-end": {
                    "node-id": "XPONDER-1",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node1"
                },
                "service-z-end": {
                    "node-id": "XPONDER-4",
                    "service-rate": "100",
                    "service-format": "Ethernet",
                    "clli": "Node5"
                },
                "hard-constraints": {
                    "include_": {
                        "ordered-hops": [
                            {
                                "hop-number": "0",
                                "hop-type": {
                                    "node-id": "OpenROADM-2"
                                }
                            },
                            {
                                "hop-number": "1",
                                "hop-type": {
                                    "node-id": "OpenROADM-3"
                                }
                            },
                            {
                                "hop-number": "2",
                                "hop-type": {
                                    "node-id": "OpenROADM-4"
                                }
                            },
                            {
                                "hop-number": "3",
                                "hop-type": {
                                    "node-id": "OpenROADM-3"
                                }
                            }
                        ]
                    }
                }
            }
        }
        response = requests.request(
            "POST", url, data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '500')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'No path available by PCE and GNPy ')
        time.sleep(5)

    # Disconnect the different topologies
    def test_08_disconnect_openroadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
               .format(test_utils.RESTCONF_BASE_URL))
        data = {}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_09_disconnect_openroadmNetwork(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
               .format(test_utils.RESTCONF_BASE_URL))
        data = {}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_10_disconnect_clliNetwork(self):
        url = ("{}/config/ietf-network:networks/network/clli-network"
               .format(test_utils.RESTCONF_BASE_URL))
        data = {}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=test_utils.TYPE_APPLICATION_JSON,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    unittest.main(verbosity=2)
