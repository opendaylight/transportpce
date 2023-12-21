#!/usr/bin/env python
##############################################################################
# Copyright (c) 2023 AT&T, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

# pylint: disable=wrong-import-order
import sys
import time
import unittest

import requests

sys.path.append("transportpce_tests/common")
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCE400GPortMappingTesting(unittest.TestCase):

    processes = None
    NETWORK1_CHECK_DICT = {"logical-connection-point": "XPDR4-NETWORK1",
                           "supporting-port": "L1",
                           "supported-interface-capability": [
                               "org-openroadm-port-types:if-otsi-otsigroup"
                           ],
                           "port-direction": "bidirectional",
                           "port-qual": "xpdr-network",
                           "supporting-circuit-pack-name": "1/1/6-PLUG-NET",
                           "xpdr-type": "regen",
                           "port-admin-state": "InService",
                           "port-oper-state": "InService",
                           "regen-profiles": {
                               "regen-profile": [
                                   "OTUC4-REGEN",
                                   "OTUC2-REGEN",
                                   "OTUC3-REGEN"
                               ]
                           }
                           }
    SUBSET_NETWORK1_CHECK_DICT = {"logical-connection-point": "XPDR4-NETWORK1",
                                  "supporting-port": "L1",
                                  "port-direction": "bidirectional",
                                  "port-qual": "xpdr-network",
                                  "supporting-circuit-pack-name": "1/1/6-PLUG-NET",
                                  "xpdr-type": "regen",
                                  "port-admin-state": "InService",
                                  "port-oper-state": "InService"
                                  }
    REGEN_CAPABILITIES = ["OTUC2-REGEN",
                          "OTUC3-REGEN",
                          "OTUC4-REGEN",
                          ]
    SUP_INT_CAPA = ["org-openroadm-port-types:if-otsi-otsigroup"]
    NODE_VERSION = "7.1"

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([("xpdra2", cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(2)

    def test_01_xpdr_device_connection(self):
        response = test_utils.mount_device("XPDR-A2",
                                           ("xpdra2", self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)
    # Check the xpdr-type as regen

    def test_02_check_xpdr_type(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertEqual(
            "regen", response["mapping"][0]["xpdr-type"])
        self.assertEqual(
            self.REGEN_CAPABILITIES,
            sorted(response["mapping"][0]["regen-profiles"]["regen-profile"]))

    # Check the xpdr-type as regen

    def test_03_check_xpdr_type(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK2")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertEqual(
            "regen", response["mapping"][0]["xpdr-type"])
        self.assertEqual(
            self.REGEN_CAPABILITIES,
            sorted(response["mapping"][0]["regen-profiles"]["regen-profile"]))

    def test_04_400g_regen_service_path_create(self):
        response = test_utils.transportpce_api_rpc_request(
            "transportpce-device-renderer", "service-path",
            {
                "service-name": "service_400g_regen",
                "wave-number": "0",
                "modulation-format": "dp-qam16",
                "operation": "create",
                "nodes": [
                    {
                        "node-id": "XPDR-A2",
                        "src-tp": "XPDR4-NETWORK1",
                        "dest-tp": "XPDR4-NETWORK2"
                    }
                ],
                "center-freq": 195.0,
                "nmc-width": 75,
                "mc-width": 87.5,
                "min-freq": 194.95625,
                "max-freq": 195.04375,
                "lower-spectral-slot-number": 582,
                "higher-spectral-slot-number": 595,
            })
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertIn("Interfaces created successfully for nodes: ", response["output"]["result"])

        self.assertEqual(
            {"node-id": "XPDR-A2",
             "otu-interface-id": [
                 "XPDR4-NETWORK1-OTUC4",
                 "XPDR4-NETWORK2-OTUC4"
             ],
             "odu-interface-id": [
                 "XPDR4-NETWORK1-ODUC4",
                 "XPDR4-NETWORK2-ODUC4"
             ],
             "och-interface-id": [
                 "XPDR4-NETWORK1-582:595",
                 "XPDR4-NETWORK1-OTSIGROUP-400G",
                 "XPDR4-NETWORK2-582:595",
                 "XPDR4-NETWORK2-OTSIGROUP-400G",
             ]},
            {x: (sorted(response["output"]["node-interface"][0][x])
                 if isinstance(response["output"]["node-interface"][0][x], list)
                 else response["output"]["node-interface"][0][x])
             for x in response["output"]["node-interface"][0].keys()})

    def test_05_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.SUBSET_NETWORK1_CHECK_DICT["supporting-otucn"] = "XPDR4-NETWORK1-OTUC4"
        self.SUBSET_NETWORK1_CHECK_DICT["lcp-hash-val"] = "AKkLPpWaa8x+"
        self.SUBSET_NETWORK1_CHECK_DICT["connection-map-lcp"] = "XPDR4-NETWORK2"
        subset = {k: v for k, v in response["mapping"][0].items() if k in self.SUBSET_NETWORK1_CHECK_DICT}
        self.assertDictEqual(subset, self.SUBSET_NETWORK1_CHECK_DICT)
        self.assertEqual(sorted(response["mapping"][0]["regen-profiles"]["regen-profile"]),
                         self.REGEN_CAPABILITIES)
        self.assertEqual(sorted(response["mapping"][0]["supported-interface-capability"]),
                         self.SUP_INT_CAPA)

    def test_06_get_portmapping_network1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK2")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.SUBSET_NETWORK1_CHECK_DICT["logical-connection-point"] = "XPDR4-NETWORK2"
        self.SUBSET_NETWORK1_CHECK_DICT["supporting-otucn"] = "XPDR4-NETWORK2-OTUC4"
        self.SUBSET_NETWORK1_CHECK_DICT["lcp-hash-val"] = "AKkLPpWaa8x9"
        self.SUBSET_NETWORK1_CHECK_DICT["connection-map-lcp"] = "XPDR4-NETWORK1"
        self.SUBSET_NETWORK1_CHECK_DICT["supporting-circuit-pack-name"] = "1/1/5-PLUG-NET"
        subset = {k: v for k, v in response["mapping"][0].items() if k in self.SUBSET_NETWORK1_CHECK_DICT}
        self.assertDictEqual(subset, self.SUBSET_NETWORK1_CHECK_DICT)
        self.assertEqual(sorted(response["mapping"][0]["regen-profiles"]["regen-profile"]),
                         self.REGEN_CAPABILITIES)
        self.assertEqual(sorted(response["mapping"][0]["supported-interface-capability"]),
                         self.SUP_INT_CAPA)

    def test_07_check_interface_oduc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK1-ODUC4")
        self.assertEqual(response["status_code"], requests.codes.ok)

        input_dict_1 = {"name": "XPDR4-NETWORK1-ODUC4",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/6-PLUG_NET",
                        "supporting-interface-list": "XPDR4-NETWORK1-OTUC4",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-CTP",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "not-terminated"
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])

    def test_08_check_interface_oduc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK2-ODUC4")
        self.assertEqual(response["status_code"], requests.codes.ok)

        input_dict_1 = {"name": "XPDR4-NETWORK2-ODUC4",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/5-PLUG_NET",
                        "supporting-interface-list": "XPDR4-NETWORK2-ODUC4",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-CTP",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "not-terminated"
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])

    # Delete the service path

    def test_09_service_path_delete_regen(self):
        response = test_utils.transportpce_api_rpc_request(
            "transportpce-device-renderer", "service-path",
            {
                "modulation-format": "dp-qam16",
                "operation": "delete",
                "service-name": "test1_regen",
                "wave-number": "0",
                "center-freq": 195.0,
                "nmc-width": 75,
                "mc-width": 87.5,
                "min-freq": 194.95625,
                "max-freq": 195.04375,
                "lower-spectral-slot-number": 582,
                "higher-spectral-slot-number": 595,
                "nodes": [
                    {
                        "node-id": "XPDR-A2",
                        "src-tp": "XPDR4-NETWORK1",
                        "dest-tp": "XPDR4-NETWORK2"
                    }
                ]
            })
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertIn("Request processed", response["output"]["result"])

    def test_10_check_no_interface_oduc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK1-ODUC4")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_11_check_no_interface_oduc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK2-ODUC4")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_12_check_no_interface_otuc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK1-OTUC1")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_13_check_no_interface_otuc4(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK2-OTUC1")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    # Check if port-mapping data is updated, where the supporting-otucn is deleted
    def test_14_check_no_otuc4(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    def test_15_check_no_otuc4(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR4-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    def test_16_check_no_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK1-OTSIGROUP-400G")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_17_check_no_interface_otsig(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK2-OTSIGROUP-400G")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_18_check_no_interface_otsi(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK1-582:595")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_19_check_no_interface_otsi(self):
        response = test_utils.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR4-NETWORK2-582:595")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    # Disconnect the XPDR
    def test_20_xpdr_device_disconnection(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_21_xpdr_device_disconnected(self):
        response = test_utils.check_device_connection("XPDR-A2")
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["connection-status"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["connection-status"]["error-tag"], "data-missing")
        self.assertEqual(response["connection-status"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")

    def test_22_xpdr_device_not_connected(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "node-info", None)
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["node-info"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["node-info"]["error-tag"], "data-missing")
        self.assertEqual(response["node-info"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")


if __name__ == "__main__":
    unittest.main(verbosity=2)
