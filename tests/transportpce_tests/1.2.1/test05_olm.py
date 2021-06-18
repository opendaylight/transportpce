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
    NODE_VERSION = '1.2.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma-full', cls.NODE_VERSION),
                                               ('roadmc-full', cls.NODE_VERSION),
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
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_xpdrC_device_connected(self):
        response = test_utils.mount_device("XPDRC01", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_rdmA_device_connected(self):
        response = test_utils.mount_device("ROADMA01", ('roadma-full', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_rdmC_device_connected(self):
        response = test_utils.mount_device("ROADMC01", ('roadmc-full', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xprdA_to_roadmA(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRA01", "1", "1",
                                                          "ROADMA01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_06_connect_roadmA_to_xpdrA(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRA01", "1", "1",
                                                          "ROADMA01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_07_connect_xprdC_to_roadmC(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRC01", "1", "1",
                                                          "ROADMC01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_08_connect_roadmC_to_xpdrC(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRC01", "1", "1",
                                                          "ROADMC01", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_09_create_OTS_ROADMA(self):
        response = test_utils.create_ots_oms_request("ROADMA01", "DEG1-TTP-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADMA01',
                      res["output"]["result"])

    def test_10_create_OTS_ROADMC(self):
        response = test_utils.create_ots_oms_request("ROADMC01", "DEG2-TTP-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADMC01',
                      res["output"]["result"])

    def test_11_get_PM_ROADMA(self):
        url = "{}/operations/transportpce-olm:get-pm"
        data = {
            "input": {
                "node-id": "ROADMA01",
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
            "pmparameter-value": "2.5"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalReturnLoss",
            "pmparameter-value": "49.9"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalPowerInput",
            "pmparameter-value": "3"
        }, res["output"]["measurements"])

    def test_12_get_PM_ROADMC(self):
        url = "{}/operations/transportpce-olm:get-pm"
        data = {
            "input": {
                "node-id": "ROADMC01",
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
            "pmparameter-value": "18.1"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalReturnLoss",
            "pmparameter-value": "48.8"
        }, res["output"]["measurements"])
        self.assertIn({
            "pmparameter-name": "OpticalPowerInput",
            "pmparameter-value": "-3.2"
        }, res["output"]["measurements"])

    def test_13_calculate_span_loss_base_ROADMA_ROADMC(self):
        url = "{}/operations/transportpce-olm:calculate-spanloss-base"
        data = {
            "input": {
                "src-type": "link",
                "link-id": "ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success',
                      res["output"]["result"])
        self.assertIn({
            "spanloss": "5.7",
            "link-id": "ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX"
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
            "spanloss": "15.1",
            "link-id": "ROADMC01-DEG2-DEG2-TTP-TXRXtoROADMA01-DEG1-DEG1-TTP-TXRX"
        }, res["output"]["spans"])
        self.assertIn({
            "spanloss": "5.7",
            "link-id": "ROADMA01-DEG1-DEG1-TTP-TXRXtoROADMC01-DEG2-DEG2-TTP-TXRX"
        }, res["output"]["spans"])
        time.sleep(5)

    def test_15_get_OTS_DEG1_TTP_TXRX_ROADMA(self):
        response = test_utils.check_netconf_node_request(
            "ROADMA01",
            "interface/OTS-DEG1-TTP-TXRX/org-openroadm-optical-transport-interfaces:ots")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(5.7, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-transmit'])
        self.assertEqual(15.1, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-receive'])

    def test_16_get_OTS_DEG2_TTP_TXRX_ROADMC(self):
        response = test_utils.check_netconf_node_request(
            "ROADMC01",
            "interface/OTS-DEG2-TTP-TXRX/org-openroadm-optical-transport-interfaces:ots")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(15.1, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-transmit'])
        self.assertEqual(5.7, res['org-openroadm-optical-transport-interfaces:ots']['span-loss-receive'])

    def test_17_servicePath_create_AToZ(self):
        response = test_utils.service_path_request("create", "test", "1",
                                                   [{"node-id": "XPDRA01",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADMA01",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADMC01",
                                                     "dest-tp": "SRG1-PP1-TXRX", "src-tp": "DEG2-TTP-TXRX"},
                                                    {"node-id": "XPDRC01",
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
                                                   [{"node-id": "XPDRC01",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADMC01",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADMA01",
                                                     "src-tp": "DEG1-TTP-TXRX", "dest-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "XPDRA01",
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
                        "node-id": "XPDRA01"
                    },
                    {
                        "dest-tp": "DEG1-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADMA01"
                    },
                    {
                        "dest-tp": "SRG1-PP1-TXRX",
                        "src-tp": "DEG2-TTP-TXRX",
                        "node-id": "ROADMC01"
                    },
                    {
                        "dest-tp": "XPDR1-CLIENT1",
                        "src-tp": "XPDR1-NETWORK1",
                        "node-id": "XPDRC01"
                    }
                ],
                "center-freq": 196.1,
                "width": 40,
                "min-freq": 196.075,
                "max-freq": 196.125,
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
            "XPDRA01",
            "interface/XPDR1-NETWORK1-761:768/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(0, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
        self.assertEqual(1, res['org-openroadm-optical-channel-interfaces:och']['wavelength-number'])

    def test_21_get_roadmconnection_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("gainLoss", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(-3.3, res['roadm-connections'][0]['target-output-power'])

    def test_22_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADMC01", "roadm-connections/DEG2-TTP-TXRX-SRG1-PP1-TXRX-761:768")
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
                        "node-id": "XPDRC01"
                    },
                    {
                        "dest-tp": "DEG2-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADMC01"
                    },
                    {
                        "src-tp": "DEG1-TTP-TXRX",
                        "dest-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADMA01"
                    },
                    {
                        "src-tp": "XPDR1-NETWORK1",
                        "dest-tp": "XPDR1-CLIENT1",
                        "node-id": "XPDRA01"
                    }
                ],
                "center-freq": 196.1,
                "width": 40,
                "min-freq": 196.075,
                "max-freq": 196.125,
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
            "XPDRC01",
            "interface/XPDR1-NETWORK1-761:768/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(0, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
        self.assertEqual(1, res['org-openroadm-optical-channel-interfaces:och']['wavelength-number'])

    def test_25_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADMC01", "roadm-connections/SRG1-PP1-TXRX-DEG2-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("gainLoss", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(2, res['roadm-connections'][0]['target-output-power'])

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
                        "node-id": "XPDRA01"
                    },
                    {
                        "dest-tp": "DEG1-TTP-TXRX",
                        "src-tp": "SRG1-PP1-TXRX",
                        "node-id": "ROADMA01"
                    },
                    {
                        "dest-tp": "SRG1-PP1-TXRX",
                        "src-tp": "DEG2-TTP-TXRX",
                        "node-id": "ROADMC01"
                    },
                    {
                        "dest-tp": "XPDR1-CLIENT1",
                        "src-tp": "XPDR1-NETWORK1",
                        "node-id": "XPDRC01"
                    }
                ],
                "center-freq": 196.1,
                "width": 40,
                "min-freq": 196.075,
                "max-freq": 196.125,
                "lower-spectral-slot-number": 761,
                "higher-spectral-slot-number": 768
            }
        }
        response = test_utils.post_request(url, data)
        print(response.json())
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Success', res["output"]["result"])

    def test_27_get_roadmconnection_ROADMA(self):
        response = test_utils.check_netconf_node_request("ROADMA01", "roadm-connections/SRG1-PP1-TXRX-DEG1-TTP-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("off", res['roadm-connections'][0]['opticalControlMode'])
        self.assertEqual(-60, res['roadm-connections'][0]['target-output-power'])

    def test_28_get_roadmconnection_ROADMC(self):
        response = test_utils.check_netconf_node_request("ROADMC01", "roadm-connections/DEG2-TTP-TXRX-SRG1-PP1-TXRX-761:768")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual("off", res['roadm-connections'][0]['opticalControlMode'])

    def test_29_servicePath_delete_AToZ(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDRA01",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADMA01",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADMC01",
                                                     "dest-tp": "SRG1-PP1-TXRX", "src-tp": "DEG2-TTP-TXRX"},
                                                    {"node-id": "XPDRC01",
                                                     "dest-tp": "XPDR1-CLIENT1", "src-tp": "XPDR1-NETWORK1"}],
                                                    196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

    def test_30_servicePath_delete_ZToA(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDRC01",
                                                     "dest-tp": "XPDR1-NETWORK1", "src-tp": "XPDR1-CLIENT1"},
                                                    {"node-id": "ROADMC01",
                                                     "dest-tp": "DEG2-TTP-TXRX", "src-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "ROADMA01",
                                                     "src-tp": "DEG1-TTP-TXRX", "dest-tp": "SRG1-PP1-TXRX"},
                                                    {"node-id": "XPDRA01",
                                                     "src-tp": "XPDR1-NETWORK1", "dest-tp": "XPDR1-CLIENT1"}],
                                                    196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

    #"""to test case where SRG where the xpdr is connected to has no optical range data"""

    def test_31_connect_xprdA_to_roadmA(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRA01", "1", "2",
                                                          "ROADMA01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])

    def test_32_connect_roadmA_to_xpdrA(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRA01", "1", "2",
                                                          "ROADMA01", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])

    def test_33_servicePath_create_AToZ(self):
        response = test_utils.service_path_request("create", "test2", "2",
                                                   [{"node-id": "XPDRA01",
                                                     "dest-tp": "XPDR1-NETWORK2", "src-tp": "XPDR1-CLIENT2"},
                                                    {"node-id": "ROADMA01",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP2-TXRX"}],
                                                    196.05, 40, 196.025, 196.075, 753,
                                                    760)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm-connection successfully created for nodes', res["output"]["result"])
        # time.sleep(40)
        time.sleep(10)

    def test_34_get_interface_XPDRA_XPDR1_NETWORK2(self):
        response = test_utils.check_netconf_node_request(
            "XPDRA01",
            "interface/XPDR1-NETWORK2-753:760/org-openroadm-optical-channel-interfaces:och")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(-5, res['org-openroadm-optical-channel-interfaces:och']['transmit-power'])
        self.assertEqual(2, res['org-openroadm-optical-channel-interfaces:och']['wavelength-number'])

    def test_35_servicePath_delete_AToZ(self):
        response = test_utils.service_path_request("delete", "test", "1",
                                                   [{"node-id": "XPDRA01",
                                                     "dest-tp": "XPDR1-NETWORK2", "src-tp": "XPDR1-CLIENT2"},
                                                    {"node-id": "ROADMA01",
                                                     "dest-tp": "DEG1-TTP-TXRX", "src-tp": "SRG1-PP2-TXRX"}],
                                                    196.1, 40, 196.075, 196.125, 761,
                                                    768)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Request processed', res["output"]["result"])
        time.sleep(10)

    def test_36_xpdrA_device_disconnected(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_37_xpdrC_device_disconnected(self):
        response = test_utils.unmount_device("XPDRC01")
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
        response = test_utils.unmount_device("ROADMA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_40_rdmC_device_disconnected(self):
        response = test_utils.unmount_device("ROADMC01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
