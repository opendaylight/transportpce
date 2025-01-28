#!/usr/bin/env python
##############################################################################
# Copyright (c) 2023 Orange, Inc. and others.  All rights reserved.
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
import time
import requests
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEtesting(unittest.TestCase):
    path_computation_input_data = {
        "service-name": "service-AB",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "service-rate": "100",
            "clli": "NodeA",
            "service-format": "OC",
            "node-id": "ROADM-A1"
        },
        "service-z-end": {
            "service-rate": "100",
            "clli": "NodeB",
            "service-format": "OC",
            "node-id": "ROADM-B1"
        },
        "pce-routing-metric": "hop-count"
    }

    openraodm_topo_with_service_BC = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        sample_files_parsed = False
        time.sleep(10)
        try:
            TOPO_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                     "..", "..", "sample_configs", "honeynode-topo-3-RDM.json")
            with open(TOPO_FILE, 'r', encoding='utf-8') as topo:
                cls.openraodm_topo_with_service_BC = topo.read()

            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "portmapping-3-RDM-221.json")
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

        cls.processes = test_utils.start_tpce()
        time.sleep(5)

    @classmethod
    def tearDownClass(cls):
        # clean datastores
        test_utils.del_portmapping()
        test_utils.del_ietf_network('openroadm-topology')
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(1)

    def test_01_load_port_mapping(self):
        response = test_utils.post_portmapping(self.port_mapping_data)
        self.assertIn(response['status_code'], (requests.codes.created, requests.codes.no_content))
        time.sleep(1)

    def test_02_load_openroadm_topology(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.openraodm_topo_with_service_BC)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path Computation success
    def test_03_path_computation_AB(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual('Path is calculated by PCE',
                         response['output']['configuration-response-common']['response-message'])
        self.assertEqual(
            0,
            response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(191.325,
                         float(response['output']['response-parameters']['path-description']['aToZ-direction']
                               ['aToZ-min-frequency']))
        self.assertEqual(191.375,
                         float(response['output']['response-parameters']['path-description']['aToZ-direction']
                               ['aToZ-max-frequency']))
        self.assertEqual(
            0,
            response['output']['response-parameters']['path-description']['zToA-direction']['zToA-wavelength-number'])
        self.assertEqual(191.325,
                         float(response['output']['response-parameters']['path-description']['zToA-direction']
                               ['zToA-min-frequency']))
        self.assertEqual(191.375,
                         float(response['output']['response-parameters']['path-description']['zToA-direction']
                               ['zToA-max-frequency']))


if __name__ == "__main__":
    unittest.main(verbosity=2)
