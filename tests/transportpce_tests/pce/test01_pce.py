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
import time
import requests
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEtesting(unittest.TestCase):
    path_computation_input_data = {
        "service-name": "service-1",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "service-rate": "100",
            "clli": "NodeA",
            "service-format": "Ethernet",
            "node-id": "XPDRA01"
        },
        "service-z-end": {
            "service-rate": "100",
            "clli": "NodeC",
            "service-format": "Ethernet",
            "node-id": "XPDRC01"
        },
        "pce-routing-metric": "hop-count"
    }

    simple_topo_bi_dir_data = None
    simple_topo_uni_dir_data = None
    complex_topo_uni_dir_data = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        sample_files_parsed = False
        time.sleep(10)
        try:
            TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-topo.json")
            with open(TOPO_BI_DIR_FILE, 'r', encoding='utf-8') as topo_bi_dir:
                cls.simple_topo_bi_dir_data = topo_bi_dir.read()

            TOPO_UNI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "NW-simple-topology.json")

            with open(TOPO_UNI_DIR_FILE, 'r', encoding='utf-8') as topo_uni_dir:
                cls.simple_topo_uni_dir_data = topo_uni_dir.read()

            TOPO_UNI_DIR_COMPLEX_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                     "..", "..", "sample_configs", "NW-for-test-5-4.json")
            with open(TOPO_UNI_DIR_COMPLEX_FILE, 'r', encoding='utf-8') as topo_uni_dir_complex:
                cls.complex_topo_uni_dir_data = topo_uni_dir_complex.read()
            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "pce_portmapping_121.json")
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

    def setUp(self):  # instruction executed before each test method
        time.sleep(1)

    # Load port mapping
    def test_01_load_port_mapping(self):
        response = test_utils.post_portmapping(self.port_mapping_data)
        self.assertIn(response['status_code'], (requests.codes.created, requests.codes.no_content))
        time.sleep(1)

    # Load simple bidirectional topology
    def test_02_load_simple_topology_bi(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.simple_topo_bi_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Get existing nodeId
    def test_03_get_nodeId(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'ROADMA01-SRG1', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node']['node-id'], 'ROADMA01-SRG1')
        time.sleep(1)

    # Get existing linkId
    def test_04_get_linkId(self):
        response = test_utils.get_ietf_network_link_request(
            'openroadm-topology', 'XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link']['link-id'], 'XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX')
        time.sleep(1)

    # Path Computation success
    def test_05_path_computation_xpdr_bi(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Path Computation success
    def test_06_path_computation_rdm_bi(self):
        self.path_computation_input_data["service-a-end"]["node-id"] = "ROADMA01"
        self.path_computation_input_data["service-z-end"]["node-id"] = "ROADMC01"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Load simple bidirectional topology
    def test_07_load_simple_topology_uni(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.simple_topo_uni_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Get existing nodeId
    def test_08_get_nodeId(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPONDER-1-2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node']['node-id'], 'XPONDER-1-2')
        time.sleep(1)

    # Get existing linkId
    def test_09_get_linkId(self):
        response = test_utils.get_ietf_network_link_request(
            'openroadm-topology', 'XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link']['link-id'], 'XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX')
        time.sleep(1)

    # Path Computation success
    def test_10_path_computation_xpdr_uni(self):
        self.path_computation_input_data["service-a-end"]["node-id"] = "XPONDER-1-2"
        self.path_computation_input_data["service-a-end"]["clli"] = "ORANGE1"
        self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-3-2"
        self.path_computation_input_data["service-z-end"]["clli"] = "ORANGE3"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Path Computation success
    def test_11_path_computation_rdm_uni(self):
        self.path_computation_input_data["service-a-end"]["node-id"] = "OpenROADM-2-1"
        self.path_computation_input_data["service-a-end"]["clli"] = "cll21"
        self.path_computation_input_data["service-z-end"]["node-id"] = "OpenROADM-2-2"
        self.path_computation_input_data["service-z-end"]["clli"] = "ncli22"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        # ZtoA path test
        atozList = len(response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'])
        ztoaList = len(response['output']['response-parameters']['path-description']['zToA-direction']['zToA'])
        self.assertEqual(atozList, 15)
        self.assertEqual(ztoaList, 15)
        for i in range(0, 15):
            atoz = response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
            ztoa = response['output']['response-parameters']['path-description']['zToA-direction']['zToA'][i]
            if atoz['id'] == '14':
                self.assertEqual(atoz['resource']['tp-id'], 'SRG1-PP1-TX')
            if ztoa['id'] == '0':
                self.assertEqual(ztoa['resource']['tp-id'], 'SRG1-PP1-RX')
        time.sleep(2)

    # Load complex topology
    def test_12_load_complex_topology(self):
        response = test_utils.put_ietf_network('openroadm-topology', self.complex_topo_uni_dir_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(1)

    # Get existing nodeId
    def test_13_get_nodeId(self):
        response = test_utils.get_ietf_network_node_request('openroadm-topology', 'XPONDER-3-2', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node']['node-id'], 'XPONDER-3-2')
        time.sleep(1)

    # Test failed path computation
    def test_14_fail_path_computation(self):
        del self.path_computation_input_data["service-name"]
        del self.path_computation_input_data["service-a-end"]
        del self.path_computation_input_data["service-z-end"]
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Service Name is not set',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Test1 success path computation
    def test_15_success1_path_computation(self):
        self.path_computation_input_data["service-name"] = "service 1"
        self.path_computation_input_data["service-a-end"] = {"service-format": "Ethernet", "service-rate": "100",
                                                             "clli": "ORANGE2", "node-id": "XPONDER-2-2"}
        self.path_computation_input_data["service-z-end"] = {"service-format": "Ethernet", "service-rate": "100",
                                                             "clli": "ORANGE1", "node-id": "XPONDER-1-2"}
        self.path_computation_input_data["hard-constraints"] = {"customer-code": ["Some customer-code"],
                                                                "co-routing": {
                                                                    "service-identifier-list": [{
                                                                        "service-identifier": "Some existing-service"}]
        }}
        self.path_computation_input_data["soft-constraints"] = {"customer-code": ["Some customer-code"],
                                                                "co-routing": {
                                                                    "service-identifier-list": [{
                                                                        "service-identifier": "Some existing-service"}]
        }}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])

        time.sleep(4)

    # Test2 success path computation with path description
    def test_16_success2_path_computation(self):
        self.path_computation_input_data["service-a-end"]["node-id"] = "XPONDER-1-2"
        self.path_computation_input_data["service-a-end"]["clli"] = "ORANGE1"
        self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-3-2"
        self.path_computation_input_data["service-z-end"]["clli"] = "ORANGE3"
        del self.path_computation_input_data["hard-constraints"]
        del self.path_computation_input_data["soft-constraints"]
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        self.assertEqual(5, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(5, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(4)

    # Test3 success path computation with hard-constraints exclude
    def test_17_success3_path_computation(self):
        self.path_computation_input_data["hard-constraints"] = {"exclude":
                                                                {"node-id": ["OpenROADM-2-1", "OpenROADM-2-2"]}}
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        self.assertEqual(10, response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(10, response['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(4)

    # Path computation before deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    def test_18_path_computation_before_oms_attribute_deletion(self):
        self.path_computation_input_data["service-a-end"]["node-id"] = "XPONDER-2-2"
        self.path_computation_input_data["service-a-end"]["clli"] = "ORANGE2"
        self.path_computation_input_data["service-z-end"]["node-id"] = "XPONDER-1-2"
        self.path_computation_input_data["service-z-end"]["clli"] = "ORANGE1"
        del self.path_computation_input_data["hard-constraints"]
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        path_depth = len(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ'])
        self.assertEqual(31, path_depth)
        link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state": "inService"}
        find = False
        for i in range(0, path_depth):
            resource_i = (response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
                          ['resource'])
            if resource_i == link:
                find = True
        self.assertEqual(find, True)
        time.sleep(4)

    # Delete oms-attribute in the link :openroadm1-3 to openroadm1-2
    def test_19_delete_oms_attribute_in_openroadm13toopenroadm12_link(self):
        response = test_utils.del_oms_attr_request("OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))
        time.sleep(2)

    # Path computation after deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    def test_20_path_computation_after_oms_attribute_deletion(self):
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        path_depth = len(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ'])
        self.assertEqual(47, path_depth)
        link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state": "inService"}
        find = False
        for i in range(0, path_depth):
            resource_i = (response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
                          ['resource'])
            if resource_i == link:
                find = True
        self.assertNotEqual(find, True)
        time.sleep(5)


if __name__ == "__main__":
    unittest.main(verbosity=2)
