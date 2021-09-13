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
import sys
import time
import requests
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportGNPYtesting(unittest.TestCase):

    topo_cllinet_data = None
    topo_ordnet_data = None
    topo_ordtopo_data = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        try:
            sample_files_parsed = False
            TOPO_CLLINET_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "gnpy", "clliNetwork.json")
            with open(TOPO_CLLINET_FILE, 'r') as topo_cllinet:
                cls.topo_cllinet_data = topo_cllinet.read()

            TOPO_ORDNET_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "gnpy", "openroadmNetwork.json")
            with open(TOPO_ORDNET_FILE, 'r') as topo_ordnet:
                cls.topo_ordnet_data = topo_ordnet.read()

            TOPO_ORDTOPO_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "gnpy", "openroadmTopology.json")
            with open(TOPO_ORDTOPO_FILE, 'r') as topo_ordtopo:
                cls.topo_ordtopo_data = topo_ordtopo.read()
            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                     "..", "..", "sample_configs", "gnpy", "gnpy_portmapping_121.json")
            with open(PORT_MAPPING_FILE, 'r') as port_mapping:
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

        cls.processes = test_utils.start_tpce()

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(2)

     # Load port mapping
    def test_00_load_port_mapping(self):
        response = test_utils.rawpost_request(test_utils.URL_FULL_PORTMAPPING, self.port_mapping_data)
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(2)

    # Mount the different topologies
    def test_01_connect_clliNetwork(self):
        response = test_utils.rawput_request(test_utils.URL_CONFIG_CLLI_NET, self.topo_cllinet_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_02_connect_openroadmNetwork(self):
        response = test_utils.rawput_request(test_utils.URL_CONFIG_ORDM_NET, self.topo_ordnet_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_03_connect_openroadmTopology(self):
        response = test_utils.rawput_request(test_utils.URL_CONFIG_ORDM_TOPO, self.topo_ordtopo_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    # Path computed by PCE is feasible according to Gnpy
    def test_04_path_computation_FeasibleWithPCE(self):
        response = test_utils.path_computation_request("request-1", "service-1",
                                                       {"node-id": "XPONDER-1", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node1"},
                                                       {"node-id": "XPONDER-5", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node5"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '200')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'Path is calculated by PCE')
        self.assertIn('A-to-Z',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'], True)
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'], True)
        time.sleep(5)

    # Path computed by PCE is not feasible by GNPy and GNPy cannot find
    # another one (low SNR)
    def test_05_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        response = test_utils.path_computation_request("request-2", "service-2",
                                                       {"node-id": "XPONDER-1", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node1"},
                                                       {"node-id": "XPONDER-5", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node5"},
                                                       {"include_": {"ordered-hops": [
                                                           {"hop-number": "0", "hop-type": {"node-id": "OpenROADM-2"}},
                                                           {"hop-number": "1", "hop-type": {"node-id": "OpenROADM-3"}},
                                                           {"hop-number": "2", "hop-type": {"node-id": "OpenROADM-4"}}]}
                                                        })
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '500')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'No path available by PCE and GNPy ')
        self.assertIn('A-to-Z',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'],
                         False)
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'],
                         False)
        time.sleep(5)

    # #PCE cannot find a path while GNPy finds a feasible one
    def test_06_path_computation_NotFoundByPCE_FoundByGNPy(self):
        response = test_utils.path_computation_request("request-3", "service-3",
                                                       {"node-id": "XPONDER-1", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node1"},
                                                       {"node-id": "XPONDER-4", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node5"},
                                                       {"include_": {"ordered-hops": [
                                                           {"hop-number": "0", "hop-type": {"node-id": "OpenROADM-2"}},
                                                           {"hop-number": "1", "hop-type": {"node-id": "OpenROADM-3"}}]}
                                                        })
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['configuration-response-common'][
            'response-code'], '200')
        self.assertEqual(res['output']['configuration-response-common'][
            'response-message'],
            'Path is calculated by GNPy')
        self.assertIn('A-to-Z',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertIn('Z-to-A',
                      [res['output']['gnpy-response'][0]['path-dir'],
                       res['output']['gnpy-response'][1]['path-dir']])
        self.assertEqual(res['output']['gnpy-response'][1]['feasibility'], True)
        self.assertEqual(res['output']['gnpy-response'][0]['feasibility'], True)
        time.sleep(5)

    # Not found path by PCE and GNPy cannot find another one
    def test_07_path_computation_FoundByPCE_NotFeasibleByGnpy(self):
        response = test_utils.path_computation_request("request-4", "service-4",
                                                       {"node-id": "XPONDER-1", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node1"},
                                                       {"node-id": "XPONDER-4", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "Node5"},
                                                       {"include_": {"ordered-hops": [
                                                           {"hop-number": "0", "hop-type": {"node-id": "OpenROADM-2"}},
                                                           {"hop-number": "1", "hop-type": {"node-id": "OpenROADM-3"}},
                                                           {"hop-number": "2", "hop-type": {"node-id": "OpenROADM-4"}},
                                                           {"hop-number": "3", "hop-type": {"node-id": "OpenROADM-3"}}]}
                                                        })
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
        response = test_utils.delete_request(test_utils.URL_CONFIG_ORDM_TOPO)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_09_disconnect_openroadmNetwork(self):
        response = test_utils.delete_request(test_utils.URL_CONFIG_ORDM_NET)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    def test_10_disconnect_clliNetwork(self):
        response = test_utils.delete_request(test_utils.URL_CONFIG_CLLI_NET)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(3)

    # Delete portmapping
    def test_11_delete_port_mapping(self):
        response = test_utils.delete_request(test_utils.URL_FULL_PORTMAPPING)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

if __name__ == "__main__":
    # logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    unittest.main(verbosity=2)
