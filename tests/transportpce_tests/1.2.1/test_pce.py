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


class TransportPCEtesting(unittest.TestCase):

    simple_topo_bi_dir_data = None
    simple_topo_uni_dir_data = None
    complex_topo_uni_dir_data = None
    port_mapping_data = None
    processes = None

    @classmethod
    def setUpClass(cls):
        # pylint: disable=bare-except
        try:
            sample_files_parsed = False
            TOPO_BI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                            "..", "..", "sample_configs", "honeynode-topo.xml")
            with open(TOPO_BI_DIR_FILE, 'r') as topo_bi_dir:
                cls.simple_topo_bi_dir_data = topo_bi_dir.read()

            TOPO_UNI_DIR_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                             "..", "..", "sample_configs", "NW-simple-topology.xml")

            with open(TOPO_UNI_DIR_FILE, 'r') as topo_uni_dir:
                cls.simple_topo_uni_dir_data = topo_uni_dir.read()

            TOPO_UNI_DIR_COMPLEX_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                     "..", "..", "sample_configs", "NW-for-test-5-4.xml")
            with open(TOPO_UNI_DIR_COMPLEX_FILE, 'r') as topo_uni_dir_complex:
                cls.complex_topo_uni_dir_data = topo_uni_dir_complex.read()
            PORT_MAPPING_FILE = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                     "..", "..", "sample_configs", "pce_portmapping_121.json")
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
        time.sleep(1)

     # Load port mapping
    def test_00_load_port_mapping(self):
        response = test_utils.rawpost_request(test_utils.URL_FULL_PORTMAPPING, self.port_mapping_data)
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(2)

     # Load simple bidirectional topology
    def test_01_load_simple_topology_bi(self):
        response = test_utils.put_xmlrequest(test_utils.URL_CONFIG_ORDM_TOPO, self.simple_topo_bi_dir_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Get existing nodeId
    def test_02_get_nodeId(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-SRG1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'], 'ROADMA01-SRG1')
        time.sleep(1)

    # Get existing linkId
    def test_03_get_linkId(self):
        response = test_utils.get_ordm_topo_request("link/XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['ietf-network-topology:link'][0]['link-id'],
            'XPDRA01-XPDR1-XPDR1-NETWORK1toROADMA01-SRG1-SRG1-PP1-TXRX')
        time.sleep(1)

    # Path Computation success
    def test_04_path_computation_xpdr_bi(self):
        response = test_utils.path_computation_request("request-1", "service-1",
                                                       {"node-id": "XPDRA01", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "nodeA"},
                                                       {"node-id": "XPDRC01", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "nodeC"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Path Computation success
    def test_05_path_computation_rdm_bi(self):
        response = test_utils.path_computation_request("request-1", "service-1",
                                                       {"node-id": "ROADMA01", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "NodeA"},
                                                       {"node-id": "ROADMC01", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "NodeC"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Delete topology
    def test_06_delete_simple_topology_bi(self):
        response = test_utils.delete_request(test_utils.URL_CONFIG_ORDM_TOPO)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted topology
    def test_07_test_topology_simple_bi_deleted(self):
        response = test_utils.get_ordm_topo_request("node/ROADMA01-SRG1")
        self.assertEqual(response.status_code, requests.codes.conflict)
        time.sleep(1)

    # Load simple bidirectional topology
    def test_08_load_simple_topology_uni(self):
        response = test_utils.put_xmlrequest(test_utils.URL_CONFIG_ORDM_TOPO, self.simple_topo_uni_dir_data)
        self.assertEqual(response.status_code, 201)
        time.sleep(2)

    # Get existing nodeId
    def test_09_get_nodeId(self):
        response = test_utils.get_ordm_topo_request("node/XPONDER-1-2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'],
            'XPONDER-1-2')
        time.sleep(1)

    # Get existing linkId
    def test_10_get_linkId(self):
        response = test_utils.get_ordm_topo_request("link/XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['ietf-network-topology:link'][0]['link-id'],
            'XPONDER-1-2XPDR-NW1-TX-toOpenROADM-1-2-SRG1-SRG1-PP1-RX')
        time.sleep(1)

    # Path Computation success
    def test_11_path_computation_xpdr_uni(self):
        response = test_utils.path_computation_request("request-1", "service-1",
                                                       {"node-id": "XPONDER-1-2", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "ORANGE1"},
                                                       {"node-id": "XPONDER-3-2", "service-rate": "100",
                                                           "service-format": "Ethernet", "clli": "ORANGE3"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Path Computation success
    def test_12_path_computation_rdm_uni(self):
        response = test_utils.path_computation_request("request1", "service1",
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "clli": "cll21", "node-id": "OpenROADM-2-1"},
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "clli": "ncli22", "node-id": "OpenROADM-2-2"})
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
            if atoz['id'] == '14':
                self.assertEqual(atoz['resource']['tp-id'], 'SRG1-PP1-TX')
            if ztoa['id'] == '0':
                self.assertEqual(ztoa['resource']['tp-id'], 'SRG1-PP1-RX')
        time.sleep(5)

    # Delete topology
    def test_13_delete_simple_topology(self):
        response = test_utils.delete_request(test_utils.URL_CONFIG_ORDM_TOPO)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted topology
    def test_14_test_topology_simple_deleted(self):
        response = test_utils.get_ordm_topo_request("node/XPONDER-1-2")
        self.assertEqual(response.status_code, requests.codes.conflict)
        time.sleep(1)

    # Load complex topology
    def test_15_load_complex_topology(self):
        response = test_utils.put_xmlrequest(test_utils.URL_CONFIG_ORDM_TOPO, self.complex_topo_uni_dir_data)
        self.assertEqual(response.status_code, 201)
        time.sleep(2)

    # Get existing nodeId
    def test_16_get_nodeId(self):
        response = test_utils.get_ordm_topo_request("node/XPONDER-3-2")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['node-id'],
            'XPONDER-3-2')
        time.sleep(1)

    # Test failed path computation
    def test_17_fail_path_computation(self):
        response = test_utils.post_request(test_utils.URL_PATH_COMPUTATION_REQUEST,
                                           {"input": {"service-handler-header": {"request-id": "request-1"}}})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Service Name is not set',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(2)

    # Test1 success path computation
    def test_18_success1_path_computation(self):
        response = test_utils.path_computation_request("request1", "service1",
                                                       {"service-format": "Ethernet", "service-rate": "100",
                                                        "clli": "ORANGE2", "node-id": "XPONDER-2-2",
                                                        "tx-direction": {"port": {
                                                            "port-device-name": "Some port-device-name",
                                                            "port-type": "Some port-type",
                                                            "port-name": "Some port-name",
                                                            "port-rack": "Some port-rack",
                                                            "port-shelf": "Some port-shelf",
                                                            "port-slot": "Some port-slot",
                                                            "port-sub-slot": "Some port-sub-slot"
                                                        }},
                                                           "rx-direction": {"port": {
                                                               "port-device-name": "Some port-device-name",
                                                               "port-type": "Some port-type",
                                                               "port-name": "Some port-name",
                                                               "port-rack": "Some port-rack",
                                                               "port-shelf": "Some port-shelf",
                                                               "port-slot": "Some port-slot",
                                                               "port-sub-slot": "Some port-sub-slot"
                                                           }}},
                                                       {"service-format": "Ethernet", "service-rate": "100",
                                                           "clli": "ORANGE1", "node-id": "XPONDER-1-2",
                                                           "tx-direction": {"port": {
                                                               "port-device-name": "Some port-device-name",
                                                               "port-type": "Some port-type",
                                                               "port-name": "Some port-name",
                                                               "port-rack": "Some port-rack",
                                                               "port-shelf": "Some port-shelf",
                                                               "port-slot": "Some port-slot",
                                                               "port-sub-slot": "Some port-sub-slot"
                                                           }},
                                                           "rx-direction": {"port": {
                                                               "port-device-name": "Some port-device-name",
                                                               "port-type": "Some port-type",
                                                               "port-name": "Some port-name",
                                                               "port-rack": "Some port-rack",
                                                               "port-shelf": "Some port-shelf",
                                                               "port-slot": "Some port-slot",
                                                               "port-sub-slot": "Some port-sub-slot"
                                                           }}},
                                                       {"customer-code": ["Some customer-code"],
                                                           "co-routing": {"existing-service": ["Some existing-service"]}
                                                        },
                                                       {"customer-code": ["Some customer-code"],
                                                           "co-routing": {"existing-service": ["Some existing-service"]}
                                                        },
                                                       "hop-count", {"locally-protected-links": "true"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(5)

    # Test2 success path computation with path description
    def test_19_success2_path_computation(self):
        response = test_utils.path_computation_request("request 1", "service 1",
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-1-2", "clli": "ORANGE1"},
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-3-2", "clli": "ORANGE3"})
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
        response = test_utils.path_computation_request("request 1", "service 1",
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-1-2", "clli": "ORANGE1"},
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-3-2", "clli": "ORANGE3"},
                                                       {"exclude_": {"node-id": ["OpenROADM-2-1", "OpenROADM-2-2"]}})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        self.assertEqual(9, res['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'])
        self.assertEqual(9, res['output']['response-parameters']['path-description']
                         ['zToA-direction']['zToA-wavelength-number'])
        time.sleep(5)

    # Path computation before deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    def test_21_path_computation_before_oms_attribute_deletion(self):
        response = test_utils.path_computation_request("request 1", "service 1",
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-2-2", "clli": "ORANGE2"},
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-1-2", "clli": "ORANGE1"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        nbElmPath = len(res['output']['response-parameters']['path-description']
                        ['aToZ-direction']['aToZ'])
        self.assertEqual(31, nbElmPath)
        link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state":"inService"}
        find = False
        for i in range(0, nbElmPath):
            resource_i = (res['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
                             ['resource'])
            if resource_i == link:
                find = True
        self.assertEqual(find, True)
        time.sleep(5)

    # Delete oms-attribute in the link :openroadm1-3 to openroadm1-2
    def test_22_delete_oms_attribute_in_openroadm13toopenroadm12_link(self):
        response = test_utils.del_oms_attr_request("OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2")
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Path computation after deleting oms-attribute of the link :openroadm1-3 to openroadm1-2
    def test_23_path_computation_after_oms_attribute_deletion(self):
        response = test_utils.path_computation_request("request 1", "service 1",
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-2-2", "clli": "ORANGE2"},
                                                       {"service-rate": "100", "service-format": "Ethernet",
                                                           "node-id": "XPONDER-1-2", "clli": "ORANGE1"})
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Path is calculated',
                      res['output']['configuration-response-common']['response-message'])
        nbElmPath = len(res['output']['response-parameters']['path-description']
                        ['aToZ-direction']['aToZ'])
        self.assertEqual(47, nbElmPath)
        link = {"link-id": "OpenROADM-1-3-DEG2-to-OpenROADM-1-2-DEG2", "state":"inService"}
        find = False
        for i in range(0, nbElmPath):
            resource_i = (res['output']['response-parameters']['path-description']['aToZ-direction']['aToZ'][i]
                             ['resource'])
            if resource_i == link:
                find = True
        self.assertNotEqual(find, True)
        time.sleep(5)

    # Delete complex topology
    def test_24_delete_complex_topology(self):
        response = test_utils.delete_request(test_utils.URL_CONFIG_ORDM_TOPO)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    # Test deleted complex topology
    def test_25_test_topology_complex_deleted(self):
        response = test_utils.get_ordm_topo_request("node/XPONDER-3-2")
        self.assertEqual(response.status_code, requests.codes.conflict)
        time.sleep(1)

    # Delete portmapping
    def test_26_delete_port_mapping(self):
        response = test_utils.delete_request(test_utils.URL_FULL_PORTMAPPING)
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)


if __name__ == "__main__":
    unittest.main(verbosity=2)
