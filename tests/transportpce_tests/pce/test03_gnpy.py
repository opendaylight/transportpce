#!/usr/bin/env python

##############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import os
# pylint: disable=wrong-import-order
import sys
import subprocess
import time
import requests
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportGNPYtesting(unittest.TestCase):
    path_computation_input_data = {
        "service-name": "service-1",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request-1"
        },
        "service-a-end": {
            "service-rate": "100",
            "clli": "Node1",
            "service-format": "Ethernet",
            "node-id": "XPONDER-1"
        },
        "service-z-end": {
            "service-rate": "100",
            "clli": "Node5",
            "service-format": "Ethernet",
            "node-id": "XPONDER-5"
        },
        "pce-routing-metric": "hop-count"
    }

    topo_cllinet_data = None
    topo_ordnet_data = None
    topo_ordtopo_data = None
    port_mapping_data = None
    processes = []

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        sample_files_parsed = False
        try:
            TOPO_CLLINET_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "gnpy", "clliNetwork.json")
            with open(TOPO_CLLINET_FILE, 'r', encoding='utf-8') as topo_cllinet:
                cls.topo_cllinet_data = topo_cllinet.read()

            TOPO_ORDNET_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "gnpy", "openroadmNetwork.json")
            with open(TOPO_ORDNET_FILE, 'r', encoding='utf-8') as topo_ordnet:
                cls.topo_ordnet_data = topo_ordnet.read()

            TOPO_ORDTOPO_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "gnpy", "openroadmTopology.json")
            with open(TOPO_ORDTOPO_FILE, 'r', encoding='utf-8') as topo_ordtopo:
                cls.topo_ordtopo_data = topo_ordtopo.read()
            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "gnpy", "gnpy_portmapping_121.json")
            with open(PORT_MAPPING_FILE, 'r', encoding='utf-8') as port_mapping:
                cls.port_mapping_data = port_mapping.read()
            sample_files_parsed = True
        except PermissionError as err:
            print("Permission Error when trying to read sample files\n", err)
            sys.exit(2)
        except FileNotFoundError as err:
            print("File Not found Error when trying to read sample files\n", err)
            sys.exit(2)
        except:
            print("Unexpected error when trying to read sample files\n", sys.exc_info()[0])
            sys.exit(2)
        finally:
            if sample_files_parsed:
                print("sample files content loaded")

        with open('gnpy.log', 'w', encoding='utf-8') as outfile:
            print('starting GNPy REST server...')
            # pylint: disable=consider-using-with
            test_utils.process_list.append(subprocess.Popen(
                ['gnpy-rest'], stdout=outfile, stderr=outfile, stdin=None))
        cls.processes = test_utils.start_tpce()

    @classmethod
    def tearDownClass(cls):
        # clean datastores
        test_utils.del_portmapping()
        test_utils.del_ietf_network('openroadm-topology')
        test_utils.del_ietf_network('openroadm-network')
        test_utils.del_ietf_network('clli-network')
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(1)

     # Load port mapping
    def test_00_load_port_mapping(self):
        response = test_utils.post_portmapping(self.port_mapping_data)
        self.assertIn(response['status_code'], (requests.codes.created, requests.codes.no_content))
        time.sleep(1)

    # Mount the different topologies
    def test_01_connect_clliNetwork(self):
        response = test_utils.put_ietf_network('clli-network', self.topo_cllinet_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    def test_02_connect_openroadmNetwork(self):
        response = test_utils.put_ietf_network('openroadm-network', self.topo_ordnet_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    def test_03_connect_openroadmTopology(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.topo_ordtopo_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path computed by PCE is feasible according to Gnpy
    def test_04_path_computation_FeasibleWithPCE(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['configuration-response-common']['response-code'], '200')
        self.assertEqual(response['output']['configuration-response-common']['response-message'],
                         'Path is calculated by PCE')
        self.assertIn('A-to-Z',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(response['output']['gnpy-response'][0]['feasibility'], True)
        self.assertEqual(response['output']['gnpy-response'][1]['feasibility'], True)
        time.sleep(2)

    # Path computed by PCE is not feasible by GNPy and GNPy cannot find
    # another one (low SNR)
    def test_05_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        self.path_computation_input_data["service-name"] = "service-2"
        self.path_computation_input_data["service-handler-header"]["request-id"] = "request-2"
        self.path_computation_input_data["hard-constraints"] =\
            {"include": {"node-id": ["OpenROADM-2", "OpenROADM-3", "OpenROADM-4"]}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['configuration-response-common'][
            'response-code'], '500')
        self.assertEqual(response['output']['configuration-response-common'][
            'response-message'],
            'No path available by PCE and GNPy ')
        self.assertIn('A-to-Z',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(response['output']['gnpy-response'][0]['feasibility'],
                         False)
        self.assertEqual(response['output']['gnpy-response'][1]['feasibility'],
                         False)
        time.sleep(2)

    # #PCE cannot find a path while GNPy finds a feasible one
    def test_06_path_computation_FoundByPCE_FoundByGNPy(self):
        self.path_computation_input_data["service-name"] = "service-3"
        self.path_computation_input_data["service-handler-header"]["request-id"] = "request-3"
        self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-4"
        self.path_computation_input_data["hard-constraints"] =\
            {"include": {"node-id": ["OpenROADM-2", "OpenROADM-3"]}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['configuration-response-common'][
            'response-code'], '200')
        self.assertEqual(response['output']['configuration-response-common'][
            'response-message'],
            'Path is calculated by PCE')
        self.assertIn('A-to-Z',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [response['output']['gnpy-response'][0]['path-dir'],
                       response['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(response['output']['gnpy-response'][1]['feasibility'], True)
        self.assertEqual(response['output']['gnpy-response'][0]['feasibility'], True)
        time.sleep(2)

    # Not found path by PCE and GNPy cannot find another one
    def test_07_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        self.path_computation_input_data["service-name"] = "service-4"
        self.path_computation_input_data["service-handler-header"]["request-id"] = "request-4"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "400"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "400"
        self.path_computation_input_data["service-z-end"]["clli"] = "Node4"
        self.path_computation_input_data["hard-constraints"] =\
            {"include": {"node-id": ["OpenROADM-3", "OpenROADM-2", "OpenROADM-5"]}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['configuration-response-common'][
            'response-code'], '500')
        self.assertEqual(response['output']['configuration-response-common'][
            'response-message'],
            'No path available by PCE and GNPy ')
        time.sleep(2)


if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    unittest.main(verbosity=2)
