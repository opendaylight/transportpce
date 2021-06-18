#!/usr/bin/env python

#############################################################################
# Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
#############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods

import unittest
import time
import requests
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportOlmTesting(unittest.TestCase):

    processes = None
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_xpdrA_device_connected(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_xpdrC_device_connected(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_rdmA_device_connected(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_rdmC_device_connected(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xprdA_to_roadmA(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_06_connect_roadmA_to_xpdrA(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_07_connect_xprdC_to_roadmC(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_08_connect_roadmC_to_xpdrC(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_09_create_OTS_ROADMA(self):
        response = test_utils.create_ots_oms_request("ROADM-A1", "DEG1-TTP-TXRX")
        time.sleep(10)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-A1',
                      res["output"]["result"])

    def test_10_create_OTS_ROADMC(self):
        response = test_utils.create_ots_oms_request("ROADM-C1", "DEG2-TTP-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-C1',
                      res["output"]["result"])

    def test_11_get_PM_ROADMA(self):
        url = "{}/operations/transportpce-olm:get-pm"
        data = {
            "input": {
                "node-id": "ROADM-A1",
                "resource-type": "interface",
                "granularity": "15min",
                "resource-identifier": {
                    "resource-name": "OTS-DEG2-TTP-TXRX"
                }
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({
            "pmparameter-name": "OpticalPowerOutput",
            "pmparameter-value": "2.5"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalReturnLoss",
            "pmparameter-value": "40"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalPowerInput",
            "pmparameter-value": "-21.1"
        }, res["output"]["measurements"])

    def test_12_get_PM_ROADMC(self):
        url = "{}/operations/transportpce-olm:get-pm"
        data = {
            "input": {
                "node-id": "ROADM-C1",
                "resource-type": "interface",
                "granularity": "15min",
                "resource-identifier": {
                    "resource-name": "OTS-DEG1-TTP-TXRX"
                }
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn({
            "pmparameter-name": "OpticalPowerOutput",
            "pmparameter-value": "4.6"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalReturnLoss",
            "pmparameter-value": "49.1"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalPowerInput",
            "pmparameter-value": "-15.1"
        }, res["output"]["measurements"])

    def test_13_calculate_span_loss_base_ROADMA_ROADMC(self):
        url = "{}/operations/transportpce-olm:calculate-spanloss-base"
        data = {
            "input": {
                "src-type": "link",
                "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success',
                      res["output"]["result"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        time.sleep(5)

    def test_14_calculate_span_loss_base_all(self):
        url = "{}/operations/transportpce-olm:calculate-spanloss-base"
        data = {
            "input": {
                "src-type": "all"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success',
                      res["output"]["result"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        time.sleep(5)

    def test_15_get_OTS_DEG2_TTP_TXRX_ROADMA(self):
        response = test_utils.check_netconf_node_request(
            "ROADM-A1",
            "interface/OTS-DEG2-TTP-TXRX/org-openroadm-optical-transport-interfaces:ots")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(17.6, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-transmit'])
        self.assertEqual(25.7, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-receive'])

    def test_16_get_OTS_DEG1_TTP_TXRX_ROADMC(self):
        response = test_utils.check_netconf_node_request(
            "ROADM-C1",
            "interface/OTS-DEG1-TTP-TXRX/org-openroadm-optical-transport-interfaces:ots")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(25.7, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-transmit'])
        self.assertEqual(17.6, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-receive'])

    def test_17_servicePath_create_AToZ(self):
        response = test_utils.service_path_request("create", "test", "1",
                                                   [{"node-id": "XPDR-A1",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADM-A1",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADM-C1",
                                                     "dest-tp": "SRG1-PP1-TXRX", "src-tp": "DEG1-TTP-TXRX"},
                                                    {"node-id": "XPDR-C1",
                                                     "dest-tp": "XPDR1-CLIENT1", "src-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes', res["output"]["result"])
        # time.sleep(40)
        time.sleep(10)

    def test_18_servicePath_create_ZToA(self):
        response = test_utils.service_path_request("create", "test", "1",
                                                   [{"node-id": "XPDR-C1",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADM-C1",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADM-A1",
                                                     "src-tp": "DEG2-TTP-TXRX", "dest-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "XPDR-A1",
                                                     "src-tp": "XPDR1-NETWORK1", "dest-tp": "XPDR1-CLIENT1"}],
                                                   196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes', res["output"]["result"])
        # time.sleep(40)
        time.sleep(10)

    def test_19_service_power_setup_XPDRA_XPDRC(self):
        url = "{}/operations/transportpce-olm:service-power-setup"
        data = {
            "input": {
                "service-name": "test",
                "wave-number": 1,
                "nodes": [
                    {
                        "dest-tp": "XPDR1-NETWORK1",
                        "src-tp": "XPDR1-CLIENT1",
                        "node-id": "XPDR-A1"
                    },
                    {
                        "dest-tp": "DEG2-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADM-A1"
                    },
                    {
                        "dest-tp": "SRG1-PP1-TXRX",
                        "src-tp": "DEG1-TTP-TXRX",
                        "node-id": "ROADM-C1"
                    },
                    {
                        "dest-tp": "XPDR1-CLIENT1",
                        "src-tp": "XPDR1-NETWORK1",
                        "node-id": "XPDR-C1"
                    }
                ],
                "lower-spectral-slot-number": 761,
                "higher-spectral-slot-number": 768
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success', res["output"]["result"])

    def test_20_get_interface_XPDRA_XPDR1_NETWORK1(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A1",
            "interface/XPDR1-NETWORK1-761:768/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(-5, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
        self.assertEqual(196.1, res['org-openroadm-optical-channel-interfaces:och']['frequency'])

    def test_21_get_roadmconnection_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("gainLoss", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(2.0, res['roadm-connections'][0]['target-output-power'])

    def test_22_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADM-C1", "roadm-connections/DEG1-TTP-TXRX-SRG1-PP1-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("power", res['roadm-connections'][0]['opticalControlMode'])

    def test_23_service_power_setup_XPDRC_XPDRA(self):
        url = "{}/operations/transportpce-olm:service-power-setup"
        data = {
            "input": {
                "service-name": "test",
                "wave-number": 1,
                "nodes": [
                    {
                        "dest-tp": "XPDR1-NETWORK1",
                        "src-tp": "XPDR1-CLIENT1",
                        "node-id": "XPDR-C1"
                    },
                    {
                        "dest-tp": "DEG1-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADM-C1"
                    },
                    {
                        "src-tp": "DEG2-TTP-TXRX",
                        "dest-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADM-A1"
                    },
                    {
                        "src-tp": "XPDR1-NETWORK1",
                        "dest-tp": "XPDR1-CLIENT1",
                        "node-id": "XPDR-A1"
                    }
                ],
                "lower-spectral-slot-number": 761,
                "higher-spectral-slot-number": 768
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success', res["output"]["result"])

    def test_24_get_interface_XPDRC_XPDR1_NETWORK1(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-C1",
            "interface/XPDR1-NETWORK1-761:768/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(-5, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
        self.assertEqual(196.1, res['org-openroadm-optical-channel-interfaces:och']['frequency'])

    def test_25_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADM-C1", "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("gainLoss", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(2.0, res['roadm-connections'][0]['target-output-power'])

    def test_26_service_power_turndown_XPDRA_XPDRC(self):
        url = "{}/operations/transportpce-olm:service-power-turndown"
        data = {
            "input": {
                "service-name": "test",
                "wave-number": 1,
                "nodes": [
                    {
                        "dest-tp": "XPDR1-NETWORK1",
                        "src-tp": "XPDR1-CLIENT1",
                        "node-id": "XPDR-A1"
                    },
                    {
                        "dest-tp": "DEG2-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADM-A1"
                    },
                    {
                        "dest-tp": "SRG1-PP1-TXRX",
                        "src-tp": "DEG1-TTP-TXRX",
                        "node-id": "ROADM-C1"
                    },
                    {
                        "dest-tp": "XPDR1-CLIENT1",
                        "src-tp": "XPDR1-NETWORK1",
                        "node-id": "XPDR-C1"
                    }
                ],
                "lower-spectral-slot-number": 761,
                "higher-spectral-slot-number": 768
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success', res["output"]["result"])

    def test_27_get_roadmconnection_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADM-A1", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("off", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(-60, res['roadm-connections'][0]['target-output-power'])

    def test_28_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADM-C1", "roadm-connections/DEG1-TTP-TXRX-SRG1-PP1-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("off", res['roadm-connections'][0]['opticalControlMode'])

    def test_29_servicePath_delete_AToZ(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDR-A1",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADM-A1",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADM-C1",
                                                     "dest-tp": "SRG1-PP1-TXRX", "src-tp": "DEG1-TTP-TXRX"},
                                                    {"node-id": "XPDR-C1",
                                                     "dest-tp": "XPDR1-CLIENT1", "src-tp": "XPDR1-NETWORK1"}],
                                                   196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

    def test_30_servicePath_delete_ZToA(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDR-C1",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADM-C1",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADM-A1",
                                                     "src-tp": "DEG2-TTP-TXRX", "dest-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "XPDR-A1",
                                                     "src-tp": "XPDR1-NETWORK1", "dest-tp": "XPDR1-CLIENT1"}],
                                                   196.053125, 40, 196.025, 196.08125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

   #"""to test case where SRG where the xpdr is connected to has no optical range data"""

    def test_31_connect_xprdA_to_roadmA(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "2",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_32_connect_roadmA_to_xpdrA(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "2",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_33_servicePath_create_AToZ(self):
        response = test_utils.service_path_request("create", "test2", "2",
                                                   [{"node-id": "XPDR-A1",
                                                     "dest-tp": "XPDR1-NETWORK2", "src-tp": "XPDR1-CLIENT2"},
                                                    {"node-id": "ROADM-A1",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP2-TXRX"}],
                                                   196.1, 40, 196.075, 196.125, 753,
                                                    760)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes', res["output"]["result"])
        # time.sleep(40)
        time.sleep(10)

    def test_34_get_interface_XPDRA_XPDR1_NETWORK2(self):
        response = test_utils.check_netconf_node_request(
            "XPDR-A1",
            "interface/XPDR1-NETWORK2-753:760/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(-5, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
#         self.assertEqual(2, res['org-openroadm-optical-channel-interfaces:och']['wavelength-number'])

    def test_35_servicePath_delete_AToZ(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDR-A1",
                                                     "dest-tp": "XPDR1-NETWORK2", "src-tp": "XPDR1-CLIENT2"},
                                                    {"node-id": "ROADM-A1",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP2-TXRX"}],
                                                   196.053125, 40, 196.025, 196.08125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

    def test_36_xpdrA_device_disconnected(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_37_xpdrC_device_disconnected(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_38_calculate_span_loss_current(self):
        url = "{}/operations/transportpce-olm:calculate-spanloss-current"
        response = test_utils.post_request(url, None)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success',
                      res["output"]["result"])
        time.sleep(5)

    def test_39_rdmA_device_disconnected(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_40_rdmC_device_disconnected(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
