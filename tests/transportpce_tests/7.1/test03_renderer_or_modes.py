#!/usr/bin/env python
##############################################################################
# Copyright (c) 2022 AT&T, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append("transportpce_tests/common")
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils_rfc8040  # nopep8


class TransportPCE400GPortMappingTesting(unittest.TestCase):

    processes = None
    NETWORK2_CHECK_DICT = {"logical-connection-point": "XPDR3-NETWORK1",
                           "supporting-port": "L1",
                           "supported-interface-capability": [
                               "org-openroadm-port-types:if-otsi-otsigroup"
                           ],
                           "port-direction": "bidirectional",
                           "port-qual": "xpdr-network",
                           "supporting-circuit-pack-name": "1/1/4-PLUG-NET",
                           "xponder-type": "tpdr",
                           "port-admin-state": "InService",
                           "port-oper-state": "InService"}
    CLIENT_CAPABILITIES = ["org-openroadm-port-types:if-OCH-OTU4-ODU4",
                           "org-openroadm-port-types:if-100GE"]
    NODE_VERSION = "7.1"

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils_rfc8040.start_tpce()
        cls.processes = test_utils_rfc8040.start_sims([("xpdra2", cls.NODE_VERSION),
                                                       ("xpdrc2", cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(10)

    def test_01_xpdr_device_connection(self):
        response = test_utils_rfc8040.mount_device("XPDR-A2",
                                                   ("xpdra2", self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_01a_xpdr_device_connection(self):
        response = test_utils_rfc8040.mount_device("XPDR-C2",
                                                   ("xpdrc2", self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils_rfc8040.CODE_SHOULD_BE_201)

    # Check the correct capabilities for the client ports (if-100GE, if-100GE-ODU4,
    # if-OCH-OTU4-ODU4)
    def test_02_check_client_capabilities(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A2", "XPDR3-CLIENT1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertEqual(
            self.CLIENT_CAPABILITIES,
            response["mapping"][0]["supported-interface-capability"])

    def test_03_check_client_capabilities(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-C2", "XPDR3-CLIENT1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertEqual(
            self.CLIENT_CAPABILITIES,
            response["mapping"][0]["supported-interface-capability"])

    def test_04_100g_ofec_service_path_create(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            "transportpce-device-renderer", "service-path",
            {
                "service-name": "service_100GE_ofec",
                "wave-number": "0",
                "modulation-format": "dp-qpsk",
                "operation": "create",
                "nodes": [{
                    "node-id": "XPDR-A2",
                    "src-tp": "XPDR3-CLIENT1",
                    "dest-tp": "XPDR3-NETWORK1"
                },
                    {
                    "node-id": "XPDR-C2",
                    "src-tp": "XPDR3-CLIENT1",
                    "dest-tp": "XPDR3-NETWORK1"
                }],
                "center-freq": 193.0,
                "nmc-width": 37.5,
                "min-freq": 192.975,
                "max-freq": 193.025,
                "lower-spectral-slot-number": 265,
                "higher-spectral-slot-number": 272,
            })
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertIn("Interfaces created successfully for nodes: ", response["output"]["result"])
        # node-interface is list which does not preserve the order
        a_side = "XPDR-A2"
        z_side = "XPDR-C2"
        if response["output"]["node-interface"][0]["node-id"] == z_side:
            a_side, z_side = z_side, a_side
        self.assertEqual(
            {"node-id": a_side,
             "otu-interface-id": [
                 "XPDR3-NETWORK1-OTUC1"
             ],
             "odu-interface-id": [
                 "XPDR3-NETWORK1-ODUC1",
                 "XPDR3-NETWORK1-ODU4"
             ],
             "och-interface-id": [
                 "XPDR3-NETWORK1-265:272",
                 "XPDR3-NETWORK1-OTSIGROUP-100G"
             ],
             "eth-interface-id": [
                 "XPDR3-CLIENT1-ETHERNET"
             ]},
            response["output"]["node-interface"][0])
        self.assertEqual(
            {"node-id": z_side,
             "otu-interface-id": [
                 "XPDR3-NETWORK1-OTUC1"
             ],
             "odu-interface-id": [
                 "XPDR3-NETWORK1-ODUC1",
                 "XPDR3-NETWORK1-ODU4"
             ],
             "och-interface-id": [
                 "XPDR3-NETWORK1-265:272",
                 "XPDR3-NETWORK1-OTSIGROUP-100G"
             ],
             "eth-interface-id": [
                 "XPDR3-CLIENT1-ETHERNET"
             ]},
            response["output"]["node-interface"][1])

    def test_05_get_portmapping_network1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A2", "XPDR3-NETWORK1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-otucn"] = "XPDR3-NETWORK1-OTUC1"
        self.NETWORK2_CHECK_DICT["lcp-hash-val"] = "FDvaQIf2Z08="
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response["mapping"])

    def test_06_get_portmapping_network1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-C2", "XPDR3-NETWORK1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.NETWORK2_CHECK_DICT["supporting-otucn"] = "XPDR3-NETWORK1-OTUC1"
        self.NETWORK2_CHECK_DICT["lcp-hash-val"] = "AJpkaVmZKJk5"
        self.assertIn(
            self.NETWORK2_CHECK_DICT,
            response["mapping"])

    def test_07_check_interface_otsi(self):
        # pylint: disable=line-too-long
        response = test_utils_rfc8040.check_node_attribute_request("XPDR-A2", "interface", "XPDR3-NETWORK1-265:272")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-265:272",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG-NET",
                        "type": "org-openroadm-interfaces:otsi",
                        "supporting-port": "L1"}
        input_dict_2 = {
            "frequency": 193.00000,
            "otsi-rate": "org-openroadm-common-optical-channel-types:R100G-otsi",
            "fec": "org-openroadm-common-types:ofec",
            "transmit-power": -5,
            "provision-mode": "explicit",
            "modulation-format": "dp-qpsk"}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"]),
                             response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic1.4", "iid": [1]},
                             response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"]["flexo"])

    def test_08_check_interface_otsi(self):
        # pylint: disable=line-too-long
        response = test_utils_rfc8040.check_node_attribute_request("XPDR-C2", "interface", "XPDR3-NETWORK1-265:272")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-265:272",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG-NET",
                        "type": "org-openroadm-interfaces:otsi",
                        "supporting-port": "L1"}
        input_dict_2 = {
            "frequency": 193.00000,
            "otsi-rate": "org-openroadm-common-optical-channel-types:R100G-otsi",
            "fec": "org-openroadm-common-types:ofec",
            "transmit-power": -5,
            "provision-mode": "explicit",
            "modulation-format": "dp-qpsk"}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"]),
                             response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"])
        self.assertDictEqual({"foic-type": "org-openroadm-common-optical-channel-types:foic1.4", "iid": [1]},
                             response["interface"][0]["org-openroadm-optical-tributary-signal-interfaces:otsi"]["flexo"])

    def test_09_check_interface_otsig(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-OTSIGROUP-100G")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-OTSIGROUP-100G",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG-NET",
                        ["supporting-interface-list"][0]: "XPDR3-NETWORK1-265:272",
                        "type": "org-openroadm-interfaces:otsi-group",
                        "supporting-port": "L1"}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R100G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-otsi-group-interfaces:otsi-group"]),
                             response["interface"][0]["org-openroadm-otsi-group-interfaces:otsi-group"])

    def test_10_check_interface_otsig(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-OTSIGROUP-100G")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-OTSIGROUP-100G",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG-NET",
                        ["supporting-interface-list"][0]: "XPDR3-NETWORK1-265:272",
                        "type": "org-openroadm-interfaces:otsi-group",
                        "supporting-port": "L1"}
        input_dict_2 = {"group-id": 1,
                        "group-rate": "org-openroadm-common-optical-channel-types:R100G-otsi"}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-otsi-group-interfaces:otsi-group"]),
                             response["interface"][0]["org-openroadm-otsi-group-interfaces:otsi-group"])

    def test_11_check_interface_otuc1(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-OTUC1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-OTUC1",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        ["supporting-interface-list"][0]: "XPDR3-NETWORK1-OTSIGROUP-100G",
                        "type": "org-openroadm-interfaces:otnOtu",
                        "supporting-port": "L1"}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 1,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-otn-otu-interfaces:otu"]),
                             response["interface"][0]["org-openroadm-otn-otu-interfaces:otu"])

    def test_12_check_interface_otuc1(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-OTUC1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-OTUC1",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        ["supporting-interface-list"][0]: "XPDR3-NETWORK1-OTSIGROUP-100G",
                        "type": "org-openroadm-interfaces:otnOtu",
                        "supporting-port": "L1"}
        input_dict_2 = {"rate": "org-openroadm-otn-common-types:OTUCn",
                        "degthr-percentage": 100,
                        "tim-detect-mode": "Disabled",
                        "otucn-n-rate": 1,
                        "degm-intervals": 2}

        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-otn-otu-interfaces:otu"]),
                             response["interface"][0]["org-openroadm-otn-otu-interfaces:otu"])

    def test_13_check_interface_oduc1(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-ODUC1")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-NETWORK1-ODUC1",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        "supporting-interface-list": "XPDR3-NETWORK1-OTUC1",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "oducn-n-rate": 1
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])
        self.assertDictEqual(
            {"payload-type": "22", "exp-payload-type": "22"},
            response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]["opu"])

    def test_14_check_interface_oduc1(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-ODUC1")
        self.assertEqual(response["status_code"], requests.codes.ok)

        input_dict_1 = {"name": "XPDR3-NETWORK1-ODUC1",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        "supporting-interface-list": "XPDR3-NETWORK1-OTUC1",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP",
                        "rate": "org-openroadm-otn-common-types:ODUCn",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated",
                        "oducn-n-rate": 1
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])
        self.assertDictEqual(
            {"payload-type": "22", "exp-payload-type": "22"},
            response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]["opu"])

    def test_15_check_interface_odu4(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-ODU4")
        self.assertEqual(response["status_code"], requests.codes.ok)

        input_dict_1 = {"name": "XPDR3-NETWORK1-ODU4",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        "supporting-interface-list": "XPDR3-NETWORK1-ODUC1",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP-CTP",
                        "rate": "org-openroadm-otn-common-types:ODU4",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated"
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])
        self.assertDictEqual(
            {"payload-type": "07", "exp-payload-type": "07"},
            response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]["opu"])

        # TODO: Check the trib-port numbering

    def test_16_check_interface_odu4(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-ODU4")
        self.assertEqual(response["status_code"], requests.codes.ok)

        input_dict_1 = {"name": "XPDR3-NETWORK1-ODU4",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/4-PLUG_NET",
                        "supporting-interface-list": "XPDR3-NETWORK1-ODUC1",
                        "type": "org-openroadm-interfaces:otnOdu",
                        "supporting-port": "L1"}

        input_dict_2 = {"odu-function": "org-openroadm-otn-common-types:ODU-TTP-CTP",
                        "rate": "org-openroadm-otn-common-types:ODU4",
                        "degm-intervals": 2,
                        "degthr-percentage": 100,
                        "monitoring-mode": "terminated"
                        }
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2, **response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]),
                             response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"])
        self.assertDictEqual(
            {"payload-type": "07", "exp-payload-type": "07"},
            response["interface"][0]["org-openroadm-otn-odu-interfaces:odu"]["opu"])

        # TODO: Check the trib-port numbering

    def test_17_check_interface_100ge_client(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-CLIENT1-ETHERNET")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-CLIENT1-ETHERNET-100G",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/3-PLUG-CLIENT",
                        "type": "org-openroadm-interfaces:ethernetCsmacd",
                        "supporting-port": "C1"
                        }
        input_dict_2 = {"speed": 100000,
                        "fec": "org-openroadm-common-types:off"}
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-ethernet-interfaces:ethernet"]),
                             response["interface"][0]["org-openroadm-ethernet-interfaces:ethernet"])

    def test_18_check_interface_100ge_client(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-CLIENT1-ETHERNET")
        self.assertEqual(response["status_code"], requests.codes.ok)
        input_dict_1 = {"name": "XPDR3-CLIENT1-ETHERNET-100G",
                        "administrative-state": "inService",
                        "supporting-circuit-pack-name": "1/1/3-PLUG-CLIENT",
                        "type": "org-openroadm-interfaces:ethernetCsmacd",
                        "supporting-port": "C1"
                        }
        input_dict_2 = {"speed": 100000,
                        "fec": "org-openroadm-common-types:off"}
        self.assertDictEqual(dict(input_dict_1, **response["interface"][0]),
                             response["interface"][0])
        self.assertDictEqual(dict(input_dict_2,
                                  **response["interface"][0]["org-openroadm-ethernet-interfaces:ethernet"]),
                             response["interface"][0]["org-openroadm-ethernet-interfaces:ethernet"])

    # Delete the service path
    def test_19_service_path_delete_100ge(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            "transportpce-device-renderer", "service-path",
            {
                "service-name": "service_100GE_ofec",
                "wave-number": "0",
                "modulation-format": "dp-qpsk",
                "operation": "delete",
                "nodes": [{
                    "node-id": "XPDR-A2",
                    "src-tp": "XPDR3-CLIENT1",
                    "dest-tp": "XPDR3-NETWORK1"
                },
                    {
                    "node-id": "XPDR-C2",
                    "src-tp": "XPDR3-CLIENT1",
                    "dest-tp": "XPDR3-NETWORK1"
                }],
                "center-freq": 193.0,
                "nmc-width": 37.5,
                "min-freq": 192.975,
                "max-freq": 193.025,
                "lower-spectral-slot-number": 265,
                "higher-spectral-slot-number": 272,
            })
        self.assertEqual(response["status_code"], requests.codes.ok)
        self.assertIn("Request processed", response["output"]["result"])

    def test_20_check_no_interface_100ge_client(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-CLIENT1-ETHERNET")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_21_check_no_interface_100ge_client(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-CLIENT1-ETHERNET")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_22_check_no_interface_odu4(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-ODU4")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_23_check_no_interface_odu4(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-ODU4")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_24_check_no_interface_otuc2(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-ODUC1")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_25_check_no_interface_otuc2(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-ODUC1")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    # Check if port-mapping data is updated, where the supporting-otucn is deleted
    def test_26_check_no_otuc1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-A2", "XPDR3-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    def test_27_check_no_otuc1(self):
        response = test_utils_rfc8040.portmapping_request("XPDR-C2", "XPDR3-NETWORK1")
        self.assertRaises(KeyError, lambda: response["supporting-otucn"])

    def test_28_check_no_interface_otsig(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-OTSIGROUP-100G")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_29_check_no_interface_otsig(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-OTSIGROUP-100G")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_30_check_no_interface_otsi(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-A2", "interface", "XPDR3-NETWORK1-265:272")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    def test_31_check_no_interface_otsi(self):
        response = test_utils_rfc8040.check_node_attribute_request(
            "XPDR-C2", "interface", "XPDR3-NETWORK1-265:272")
        self.assertEqual(response["status_code"], requests.codes.conflict)

    # Disconnect the XPDR
    def test_32_xpdr_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_33_xpdr_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-A2")
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["connection-status"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["connection-status"]["error-tag"], "data-missing")
        self.assertEqual(response["connection-status"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")

    def test_34_xpdr_device_not_connected(self):
        response = test_utils_rfc8040.get_portmapping_node_info("XPDR-A2")
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["node-info"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["node-info"]["error-tag"], "data-missing")
        self.assertEqual(response["node-info"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")

    def test_35_xpdr_device_disconnection(self):
        response = test_utils_rfc8040.unmount_device("XPDR-C2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_36_xpdr_device_disconnected(self):
        response = test_utils_rfc8040.check_device_connection("XPDR-C2")
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["connection-status"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["connection-status"]["error-tag"], "data-missing")
        self.assertEqual(response["connection-status"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")

    def test_37_xpdr_device_not_connected(self):
        response = test_utils_rfc8040.get_portmapping_node_info("XPDR-C2")
        self.assertEqual(response["status_code"], requests.codes.conflict)
        self.assertIn(response["node-info"]["error-type"], ("protocol", "application"))
        self.assertEqual(response["node-info"]["error-tag"], "data-missing")
        self.assertEqual(response["node-info"]["error-message"],
                         "Request could not be completed because the relevant data model content does not exist")


if __name__ == "__main__":
    unittest.main(verbosity=2)
