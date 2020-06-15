#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################
import os
import re
import time
import unittest

import requests

import test_utils

RESTCONF_BASE_URL = "http://localhost:8181/restconf"

CODE_SHOULD_BE_200 = 'Http status code should be 200'

CODE_SHOULD_BE_201 = 'Http status code should be 201'

CREATED_SUCCESSFULLY = 'Result message should contain Xponder Roadm Link created successfully'


class TransportTapitesting(unittest.TestCase):
    odl_process = None
    sim_process1 = None
    sim_process2 = None
    sim_process3 = None
    sim_process4 = None
    sim_process5 = None

    # START_IGNORE_XTESTING

    @classmethod
    def setUpClass(cls):
        cls.init_failed = False

        cls.odl_process = test_utils.start_tpce()
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            karaf_log = os.path.join(
                os.path.dirname(os.path.realpath(__file__)),
                "..", "..", "..", "karaf", "target", "assembly", "data", "log", "karaf.log")
            searched_expr = re.escape("Blueprint container for bundle "
                                      "org.opendaylight.netconf.restconf") + ".* was successfully created"
            found = test_utils.wait_until_log_contains(karaf_log, searched_expr, time_to_wait=60)
            cls.init_failed = not found
            if not cls.init_failed:
                print("opendaylight started")
                print("installing tapi feature...")
                result = test_utils.install_karaf_feature("odl-transportpce-tapi")
                if result.returncode != 0:
                    cls.init_failed = True
                print("Restarting opendaylight...")
                test_utils.shutdown_process(cls.odl_process)
                cls.odl_process = test_utils.start_tpce()
                found = test_utils.wait_until_log_contains(karaf_log, searched_expr, time_to_wait=60)
                cls.init_failed = not found
        if not cls.init_failed:
            cls.sim_process1 = test_utils.start_sim('xpdra')

            cls.sim_process2 = test_utils.start_sim('roadma')

            cls.sim_process3 = test_utils.start_sim('roadmc')

            cls.sim_process4 = test_utils.start_sim('xpdrc')

            cls.sim_process5 = test_utils.start_sim('spdrav2')
            print("all sims started")

    @classmethod
    def tearDownClass(cls):
        test_utils.shutdown_process(cls.odl_process)
        test_utils.shutdown_process(cls.sim_process1)
        test_utils.shutdown_process(cls.sim_process2)
        test_utils.shutdown_process(cls.sim_process3)
        test_utils.shutdown_process(cls.sim_process4)
        test_utils.shutdown_process(cls.sim_process5)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        if self.init_failed:
            self.fail('Feature installation failed')
        print("execution of {}".format(self.id().split(".")[-1]))

    # END_IGNORE_XTESTING

    #  connect netconf devices
    def test_00_connect_spdr_sa1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("SPDR-SA1", test_utils.sims['spdrav2']['port'])
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_01_connect_xpdra(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("XPDR-A1", test_utils.sims['xpdra']['port'])
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_02_connect_xpdrc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-C1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("XPDR-C1", test_utils.sims['xpdrc']['port'])
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_03_connect_rdma(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("ROADM-A1", test_utils.sims['roadma']['port'])
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(20)

    def test_04_connect_rdmc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-C1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("ROADM-C1", test_utils.sims['roadmc']['port'])
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(20)

    def test_05_connect_xprda_n1_to_roadma_pp1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "1", "ROADM-A1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_06_connect_roadma_pp1_to_xpdra_n1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "1", "ROADM-A1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_07_connect_xprdc_n1_to_roadmc_pp1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "1", "ROADM-C1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_08_connect_roadmc_pp1_to_xpdrc_n1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "1", "ROADM-C1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_09_connect_xprda_n2_to_roadma_pp2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "2", "ROADM-A1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_10_connect_roadma_pp2_to_xpdra_n2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "2", "ROADM-A1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_11_connect_xprdc_n2_to_roadmc_pp2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "2", "ROADM-C1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_12_connect_roadmc_pp2_to_xpdrc_n2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "2", "ROADM-C1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_13_get_tapi_openroadm_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details".format(RESTCONF_BASE_URL)
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "openroadm-topology"
            }
        }

        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'There should be 1 node')
        self.assertEqual(len(res["output"]["topology"]["node"][0]["owned-node-edge-point"]), 4,
                         'There should be 4 owned-node-edge-points')

    def test_14_get_tapi_otn_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details".format(RESTCONF_BASE_URL)
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "otn-topology"
            }
        }

        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 4, 'There should be 4 nodes')
        self.assertEqual(len(res["output"]["topology"]["link"]), 5, 'There should be 5 links')
        link_to_check = res["output"]["topology"]["link"][0]
        # get info from first link to do deeper check
        node1_uid = link_to_check["node-edge-point"][0]["node-uuid"]
        node2_uid = link_to_check["node-edge-point"][1]["node-uuid"]
        node_edge_point1_uid = link_to_check["node-edge-point"][0]["node-edge-point-uuid"]
        node_edge_point2_uid = link_to_check["node-edge-point"][1]["node-edge-point-uuid"]
        # get node associated to link info
        nodes = res["output"]["topology"]["node"]
        node1 = find_object_with_key(nodes, "uuid", node1_uid)
        self.assertIsNotNone(node1, 'Node with uuid ' + node1_uid + ' should not be null')
        node2 = find_object_with_key(nodes, "uuid", node2_uid)
        self.assertIsNotNone(node2, 'Node with uuid ' + node2_uid + ' should not be null')
        # get edge-point associated to nodes
        node1_edge_point = node1["owned-node-edge-point"]
        node2_edge_point = node2["owned-node-edge-point"]
        node_edge_point1 = find_object_with_key(node1_edge_point, "uuid", node_edge_point1_uid)
        self.assertIsNotNone(node_edge_point1, 'Node edge point  with uuid ' + node_edge_point1_uid + 'should not be '
                                                                                                      'null')
        node_edge_point2 = find_object_with_key(node2_edge_point, "uuid", node_edge_point2_uid)
        self.assertIsNotNone(node_edge_point2, 'Node edge point with uuid ' + node_edge_point2_uid + 'should not be '
                                                                                                     'null')
        self.assertEqual(len(node_edge_point1["name"]), 1, 'There should be 1 name')
        self.assertEqual(len(node_edge_point2["name"]), 1, 'There should be 1 name')
        if node_edge_point1["layer-protocol-name"] == 'ODU':
            self.assertIn('NodeEdgePoint_N', node_edge_point1["name"][0]["value-name"], 'Value name should be '
                          'NodeEdgePoint_NX')
        elif node_edge_point1["layer-protocol-name"] == 'PHOTONIC_MEDIA':
            self.assertIn('iNodeEdgePoint_', node_edge_point1["name"][0]["value-name"], 'Value name should be '
                          'iNodeEdgePoint_X')
        else:
            self.fail('Wrong layer protocol name')

        if node_edge_point2["layer-protocol-name"] == 'ODU':
            self.assertIn('NodeEdgePoint_N', node_edge_point2["name"][0]["value-name"], 'Value name should be '
                          'NodeEdgePoint_NX')
        elif node_edge_point2["layer-protocol-name"] == 'PHOTONIC_MEDIA':
            self.assertIn('iNodeEdgePoint_', node_edge_point2["name"][0]["value-name"], 'Value name should be '
                          'iNodeEdgePoint_X')
        else:
            self.fail('Wrong layer protocol name')

    def test_15_disconnect_xpdra(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_16_disconnect_xpdrc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-C1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_17_disconnect_roadma(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_18_disconnect_roadmc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-C1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_19_disconnect_spdr_sa1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(RESTCONF_BASE_URL))
        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member


def find_object_with_key(list_dicts, key, value):
    for dict_ in list_dicts:
        if dict_[key] == value:
            return dict_
    return None


if __name__ == "__main__":
    unittest.main(verbosity=2)
