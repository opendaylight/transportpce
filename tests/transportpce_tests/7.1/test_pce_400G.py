#!/usr/bin/env python
##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
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


class TransportPCE400Gtesting(unittest.TestCase):

    simple_topo_bi_dir_data = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        try:
            sample_files_parsed = False
            TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-topo400G.json")
            with open(TOPO_BI_DIR_FILE, 'r') as topo_bi_dir:
                cls.simple_topo_bi_dir_data = topo_bi_dir.read()

            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                     "..", "..", "sample_configs", "pce_portmapping_71.json")
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

    def setUp(self):  # instruction executed before each test method
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    # Load port mapping
    def test_01_load_port_mapping(self):
        response = test_utils.put_jsonrequest(test_utils.URL_FULL_PORTMAPPING, self.port_mapping_data)
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.created))
        time.sleep(2)

    # Load simple bidirectional topology
    def test_02_load_simple_topology_bi(self):
        response = test_utils.put_jsonrequest(test_utils.URL_CONFIG_ORDM_TOPO, self.simple_topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path Computation success
    def test_03_path_computation_xpdr_bi(self):
        response = test_utils.path_computation_request("request-1", "service-1",
                                                       {"node-id": "XPDR-A2", "service-rate": "400",
                                                           "service-format": "Ethernet", "clli": "nodeA"},
                                                       {"node-id": "XPDR-C2", "service-rate": "400",
                                                           "service-format": "Ethernet", "clli": "nodeC"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])

        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(400, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual(196.0375, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-min-frequency'])
        self.assertEqual(196.12500, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-max-frequency'])
        self.assertEqual('dp-qam16', res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        self.assertEqual(400, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual(196.0375, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-min-frequency'])
        self.assertEqual(196.12500, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-max-frequency'])
        self.assertEqual('dp-qam16', res['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(5)

    # Test deleted complex topology
    def test_04_test_topology_complex_deleted(self):
        response = test_utils.get_ordm_topo_request("node/XPONDER-3-2")
        self.assertEqual(response.status_code, requests.codes.conflict)
        time.sleep(1)

    # Delete portmapping
    def test_05_delete_port_mapping(self):
        response = test_utils.delete_request(test_utils.URL_FULL_PORTMAPPING)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
