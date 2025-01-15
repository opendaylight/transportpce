#!/usr/bin/env python
##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=invalid-name
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


class TransportPCE400Gtesting(unittest.TestCase):
    path_computation_input_data = {
        "service-name": "service-1",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "service-rate": "400",
            "clli": "nodeA",
            "service-format": "Ethernet",
            "node-id": "XPDR-A2"
        },
        "service-z-end": {
            "service-rate": "400",
            "clli": "nodeC",
            "service-format": "Ethernet",
            "node-id": "XPDR-C2"
        },
        "pce-routing-metric": "hop-count"
    }

    simple_topo_bi_dir_data = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        sample_files_parsed = False
        try:
            TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs",
                                            "honeynode-topo400G.json")
            with open(TOPO_BI_DIR_FILE, 'r', encoding='utf-8') as topo_bi_dir:
                cls.topo_bi_dir_data = topo_bi_dir.read()

            OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                "..", "..", "sample_configs",
                                                "honeynode-otntopo400G.json")
            with open(OTN_TOPO_BI_DIR_FILE, 'r', encoding='utf-8') as otn_topo_bi_dir:
                cls.otn_topo_bi_dir_data = otn_topo_bi_dir.read()

            OTUC4_OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                      "..", "..", "sample_configs",
                                                      "honeynode-otntopo400GwithOTUC4.json")
            with open(OTUC4_OTN_TOPO_BI_DIR_FILE, 'r', encoding='utf-8') as otuc4_otn_topo_bi_dir:
                cls.otuc4_otn_topo_bi_dir_data = otuc4_otn_topo_bi_dir.read()

            ODUC4_OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                      "..", "..", "sample_configs",
                                                      "honeynode-otntopo400GwithODUC4.json")
            with open(ODUC4_OTN_TOPO_BI_DIR_FILE, 'r', encoding='utf-8') as oduc4_otn_topo_bi_dir:
                cls.oduc4_otn_topo_bi_dir_data = oduc4_otn_topo_bi_dir.read()

            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs",
                                             "pce_portmapping_71.json")
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

    @classmethod
    def tearDownClass(cls):
        # clean datastores
        test_utils.del_portmapping()
        test_utils.del_ietf_network('openroadm-topology')
        test_utils.del_ietf_network('otn-topology')
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    # Load port mapping
    def test_01_load_port_mapping(self):
        response = test_utils.post_portmapping(self.port_mapping_data)
        self.assertIn(response['status_code'], (requests.codes.created, requests.codes.no_content))
        time.sleep(1)

    # Load openroadm topology
    def test_02_load_openroadm_topology_bi(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.topo_bi_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path Computation success
    def test_03_path_computation_400G_xpdr_bi(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])

        self.assertEqual(95, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual(191.35625, float(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-min-frequency']))
        self.assertEqual(191.44375, float(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-max-frequency']))
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(95, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual(191.35625, float(response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-min-frequency']))
        self.assertEqual(191.44375, float(response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-max-frequency']))
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(2)

    # Load otn topology
    def test_04_load_otn_topology_bi(self):
        response = test_utils.put_ietf_network('otn-topology', self.otn_topo_bi_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path Computation success
    def test_05_path_computation_OTUC4_xpdr_bi(self):
        self.path_computation_input_data["service-name"] = "service-OTUC4"
        self.path_computation_input_data["service-a-end"]["service-format"] = "OTU"
        self.path_computation_input_data["service-a-end"]["tx-direction"] =\
            {"port": {"port-device-name": "XPDR-A2-XPDR2"}}
        self.path_computation_input_data["service-z-end"]["service-format"] = "OTU"
        self.path_computation_input_data["service-z-end"]["tx-direction"] =\
            {"port": {"port-device-name": "XPDR-C2-XPDR2"}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])

        self.assertEqual(95, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual(191.35625, float(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-min-frequency']))
        self.assertEqual(191.44375, float(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-max-frequency']))
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(95, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual(191.35625, float(response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-min-frequency']))
        self.assertEqual(191.44375, float(response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-max-frequency']))
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(2)

    # Load otn topology with OTUC4 links
    def test_06_load_otuc4_otn_topology_bi(self):
        response = test_utils.put_ietf_network('otn-topology', self.otuc4_otn_topo_bi_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path Computation success
    def test_07_path_computation_ODUC4_xpdr_bi(self):
        self.path_computation_input_data["service-name"] = "service-ODUC4"
        self.path_computation_input_data["service-a-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-a-end"]["tx-direction"] = \
            {"port": {"port-device-name": "XPDR-A2-XPDR2"}}
        self.path_computation_input_data["service-z-end"]["service-format"] = "ODU"
        self.path_computation_input_data["service-z-end"]["tx-direction"] = \
            {"port": {"port-device-name": "XPDR-C2-XPDR2"}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])

        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(400, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual('dp-qam16', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(2)

    # Load otn topology with OTUC4 links
    def test_08_load_oduc4_otn_topology_bi(self):
        response = test_utils.put_ietf_network('otn-topology', self.oduc4_otn_topo_bi_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Path Computation success
    def test_09_path_computation_100G_xpdr_bi(self):
        self.path_computation_input_data["service-name"] = "service-100GE"
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-a-end"]["tx-direction"] = \
            {"port": {"port-device-name": "XPDR-A2-XPDR2",
                      "port-name": "XPDR2-CLIENT1"}}
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.path_computation_input_data["service-z-end"]["tx-direction"] = \
            {"port": {"port-device-name": "XPDR-C2-XPDR2",
                      "port-name": "XPDR2-CLIENT1"}}
        self.path_computation_input_data["service-z-end"]["service-format"] = "ODU"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])

        self.assertEqual(100, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual('1.1', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['min-trib-slot'])
        self.assertEqual('1.20', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['max-trib-slot'])
        self.assertEqual('dp-qpsk', response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(100, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual('1.1', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['min-trib-slot'])
        self.assertEqual('1.20', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['max-trib-slot'])
        self.assertEqual('dp-qpsk', response['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
