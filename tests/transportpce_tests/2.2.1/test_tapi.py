#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import os
import sys
import time
import unittest

import requests

from common import test_utils


CREATED_SUCCESSFULLY = 'Result message should contain Xponder Roadm Link created successfully'


class TransportTapitesting(unittest.TestCase):

    processes = None

    @classmethod
    def setUpClass(cls):
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4069M'
        cls.processes = test_utils.start_tpce()
        # TAPI feature is not installed by default in Karaf
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing tapi feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed = True
            print("Restarting OpenDaylight...")
            test_utils.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils.start_karaf()
            test_utils.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils.wait_until_log_contains(
                test_utils.KARAF_LOG, test_utils.KARAF_OK_START_MSG, time_to_wait=60)
        if cls.init_failed:
            print("tapi installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils.start_sims(['xpdra', 'roadma', 'roadmc', 'xpdrc', 'spdra'])

    @classmethod
    def tearDownClass(cls):
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        if self.init_failed:
            self.fail('Feature installation failed')
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_00_connect_spdr_sa1(self):
        response = test_utils.mount_device("SPDR-SA1", 'spdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)
        # TODO replace connect and disconnect timers with test_utils.wait_until_log_contains

    def test_01_connect_xpdra(self):
        response = test_utils.mount_device("XPDR-A1", 'xpdra')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_02_connect_xpdrc(self):
        response = test_utils.mount_device("XPDR-C1", 'xpdrc')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_03_connect_rdma(self):
        response = test_utils.mount_device("ROADM-A1", 'roadma')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(20)

    def test_04_connect_rdmc(self):
        response = test_utils.mount_device("ROADM-C1", 'roadmc')
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(20)

    def test_05_connect_xprda_n1_to_roadma_pp1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_06_connect_roadma_pp1_to_xpdra_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_07_connect_xprdc_n1_to_roadmc_pp1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_08_connect_roadmc_pp1_to_xpdrc_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_09_connect_xprda_n2_to_roadma_pp2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "2",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_10_connect_roadma_pp2_to_xpdra_n2(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "2",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_11_connect_xprdc_n2_to_roadmc_pp2(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C1", "1", "2",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_12_connect_roadmc_pp2_to_xpdrc_n2(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C1", "1", "2",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_13_get_tapi_openroadm_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "openroadm-topology"
            }
        }

        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'There should be 1 node')
        self.assertEqual(len(res["output"]["topology"]["node"][0]["owned-node-edge-point"]), 4,
                         'There should be 4 owned-node-edge-points')

    def test_14_get_tapi_otn_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "otn-topology"
            }
        }

        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
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
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(10)

    def test_16_disconnect_xpdrc(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(10)

    def test_17_disconnect_roadma(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(10)

    def test_18_disconnect_roadmc(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(10)

    def test_19_disconnect_spdr_sa1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


def find_object_with_key(list_dicts, key, value):
    for dict_ in list_dicts:
        if dict_[key] == value:
            return dict_
    return None


if __name__ == "__main__":
    unittest.main(verbosity=2)
