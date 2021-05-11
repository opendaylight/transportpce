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
                cls.topo_bi_dir_data = topo_bi_dir.read()

            OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-otntopo400G.json")
            with open(OTN_TOPO_BI_DIR_FILE, 'r') as otn_topo_bi_dir:
                cls.otn_topo_bi_dir_data = otn_topo_bi_dir.read()

            OTUC4_OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-otntopo400GwithOTUC4.json")
            with open(OTUC4_OTN_TOPO_BI_DIR_FILE, 'r') as otuc4_otn_topo_bi_dir:
                cls.otuc4_otn_topo_bi_dir_data = otuc4_otn_topo_bi_dir.read()

            ODUC4_OTN_TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-otntopo400GwithODUC4.json")
            with open(ODUC4_OTN_TOPO_BI_DIR_FILE, 'r') as oduc4_otn_topo_bi_dir:
                cls.oduc4_otn_topo_bi_dir_data = oduc4_otn_topo_bi_dir.read()

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

    # Load openroadm topology
    def test_02_load_openroadm_topology_bi(self):
        response = test_utils.put_jsonrequest(test_utils.URL_CONFIG_ORDM_TOPO, self.topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path Computation success
    def test_03_path_computation_400G_xpdr_bi(self):
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

    # Load otn topology
    def test_04_load_otn_topology_bi(self):
        response = test_utils.put_jsonrequest(test_utils.URL_CONFIG_OTN_TOPO, self.otn_topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path Computation success
    def test_05_path_computation_OTUC4_xpdr_bi(self):
        response = test_utils.path_computation_request("request-1", "service-OTUC4",
                                                       {"service-rate": "400", "clli": "NodeA",
                                                           "service-format": "OTU", "node-id": "XPDR-A2",
                                                           "rx-direction": {"port": {"port-device-name": "XPDR-A2-XPDR2"}}
                                                       },
                                                       {"service-rate": "400", "clli": "NodeC",
                                                           "service-format": "OTU", "node-id": "XPDR-C2",
                                                           "rx-direction": {"port": {"port-device-name": "XPDR-C2-XPDR2"}}
                                                       })
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

    # Load otn topology with OTUC4 links
    def test_06_load_otuc4_otn_topology_bi(self):
        response = test_utils.put_jsonrequest(test_utils.URL_CONFIG_OTN_TOPO, self.otuc4_otn_topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path Computation success
    def test_07_path_computation_ODUC4_xpdr_bi(self):
        response = test_utils.path_computation_request("request-1", "service-ODUC4",
                                                       {"service-rate": "400", "clli": "NodeA", "service-format": "ODU",
                                                           "node-id": "XPDR-A2-XPDR2",
                                                           "tx-direction": {"port": {"port-device-name": "XPDR-A2-XPDR2"}}
                                                       },
                                                       {"service-rate": "400", "clli": "NodeC", "service-format": "ODU",
                                                           "node-id": "XPDR-C2-XPDR2",
                                                           "tx-direction": {"port": {"port-device-name": "XPDR-C2-XPDR2"}}
                                                       })
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])

        self.assertEqual(400, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual('dp-qam16', res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(400, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual('dp-qam16', res['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(5)

    # Load otn topology with OTUC4 links
    def test_08_load_oduc4_otn_topology_bi(self):
        response = test_utils.put_jsonrequest(test_utils.URL_CONFIG_OTN_TOPO, self.oduc4_otn_topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path Computation success
    def test_09_path_computation_100G_xpdr_bi(self):
        response = test_utils.path_computation_request("request-1", "service-100GE",
                                                       {"service-rate": "100", "clli": "NodeA", "service-format": "Ethernet",
                                                        "node-id": "XPDR-A2",
                                                        "tx-direction": {"port": {"port-device-name": "XPDR-A2-XPDR2",
                                                           "port-name": "XPDR2-CLIENT1"}}},
                                                       {"service-rate": "100", "clli": "NodeC", "service-format": "Ethernet",
                                                        "node-id": "XPDR-C2",
                                                        "tx-direction": {"port": {"port-device-name": "XPDR-C2-XPDR2",
                                                           "port-name": "XPDR2-CLIENT1"}}})

        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])

        self.assertEqual(100, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['rate'])
        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['trib-port-number'])
        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['trib-slot-number'])
        self.assertEqual('dp-qpsk', res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['modulation-format'])

        self.assertEqual(100, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['rate'])
        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['trib-port-number'])
        self.assertEqual(1, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['trib-slot-number'])
        self.assertEqual('dp-qpsk', res['output']['response-parameters']['path-description']
                         ['zToA-direction']['modulation-format'])
        time.sleep(5)

if __name__ == "__main__":
    unittest.main(verbosity=2)
