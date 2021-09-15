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
# some pylint false positives specific to tapi test
# pylint: disable=unsubscriptable-object
# pylint: disable=unsupported-assignment-operation

import os
import sys
import time
import unittest
import requests
sys.path.append('transportpce_tests/common/')
import test_utils


CREATED_SUCCESSFULLY = 'Result message should contain Xponder Roadm Link created successfully'

class TransportTapitesting(unittest.TestCase):

    processes = None
    WAITING = 20
    NODE_VERSION = '2.2.1'
    cr_serv_sample_data = {"input": {
        "sdnc-request-header": {
            "request-id": "request-1",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service1-OCH-OTU4",
        "common-id": "commonId",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "SPDR-SA1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSA",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "SPDR-SA1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "SPDR-SC1",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeSC",
            "subrate-eth-sla": {
                    "subrate-eth-sla": {
                        "committed-info-rate": "100000",
                        "committed-burst-size": "64"
                    }
            },
            "tx-direction": {
                "port": {
                    "port-device-name": "SPDR-SC1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "SPDR-SC1-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "due-date": "2018-06-15T00:00:01Z",
        "operator-contact": "pw1234"
    }
    }

    @classmethod
    def setUpClass(cls):
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
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
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmb', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION),
                                               ('spdra', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        if self.init_failed:
            self.fail('Feature installation failed')
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_get_tapi_topology_T100G(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "Transponder 100GE"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')
        self.assertNotIn("owned-node-edge-point", res["output"]["topology"]["node"][0],
                         'Node should contain no owned-node-edge-points')
        self.assertEqual("Tpdr100g over WDM node", res["output"]["topology"]["node"][0]["name"][0]["value"],
                         'node name should be: Tpdr100g over WDM node')
        self.assertIn("ETH", res["output"]["topology"]["node"][0]["layer-protocol-name"],
                         'Node layer protocol should contain ETH')
        self.assertEqual(1, len(res["output"]["topology"]["node"][0]["node-rule-group"]),
                         'node should contain 1 node rule group')

    def test_02_get_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertNotIn("node", res["output"]["topology"], 'Topology should contain no node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')

    def test_03_connect_rdmb(self):
        response = test_utils.mount_device("ROADM-B1", ('roadmb', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_04_check_tapi_topos(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "Transponder 100GE"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')

        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')

    def test_05_disconnect_roadmb(self):
        response = test_utils.unmount_device("ROADM-B1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_06_connect_xpdra(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_07_check_tapi_topos(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertNotIn("node", res["output"]["topology"], 'Topology should contain no node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')

    def test_08_connect_rdma(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_09_connect_rdmc(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_10_check_tapi_topos(self):
        self.test_01_get_tapi_topology_T100G()

        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(1, len(res["output"]["topology"]["node"]), 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')
        self.assertEqual("ROADM-infra", res["output"]["topology"]["node"][0]["name"][0]["value"],
                         'node name should be: ROADM-infra')
        self.assertIn("PHOTONIC_MEDIA", res["output"]["topology"]["node"][0]["layer-protocol-name"],
                         'Node layer protocol should contain PHOTONIC_MEDIA')
        self.assertEqual(1, len(res["output"]["topology"]["node"][0]["node-rule-group"]),
                         'node should contain 1 node rule group')

    def test_11_connect_xprda_n1_to_roadma_pp1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_12_connect_roadma_pp1_to_xpdra_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_13_check_tapi_topology_T100G(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "Transponder 100GE"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(1, len(res["output"]["topology"]["node"][0]["owned-node-edge-point"]),
                         'Node should contain 1 owned-node-edge-points')
        self.assertEqual("XPDR1-CLIENT1",
                         res["output"]["topology"]["node"][0]["owned-node-edge-point"][0]["name"][0]["value"],
                         'name of owned-node-edge-points should be XPDR1-CLIENT1')

    def test_14_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(3, len(nodes), 'Topology should contain 3 nodes')
        self.assertEqual(2, len(links), 'Topology should contain 2 links')
        self.assertEqual(2, count_object_with_double_key(nodes, "name", "value-name", "otsi node name"),
                         'Topology should contain 2 otsi nodes')
        self.assertEqual(1, count_object_with_double_key(nodes, "name", "value-name", "dsr/odu node name"),
                         'Topology should contain 1 dsr node')
        self.assertEqual(1, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 1 transitional link')
        self.assertEqual(1, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 1 oms link')

    def test_15_connect_xpdrc(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)

    def test_16_connect_xprdc_n1_to_roadmc_pp1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_17_connect_roadmc_pp1_to_xpdrc_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_18_check_tapi_topology_T100G(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "Transponder 100GE"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(2, len(res["output"]["topology"]["node"][0]["owned-node-edge-point"]),
                         'Node should contain 2 owned-node-edge-points')
        self.assertEqual("XPDR1-CLIENT1",
                         res["output"]["topology"]["node"][0]["owned-node-edge-point"][0]["name"][0]["value"],
                         'name of owned-node-edge-points should be XPDR1-CLIENT1')
        self.assertEqual("XPDR1-CLIENT1",
                         res["output"]["topology"]["node"][0]["owned-node-edge-point"][1]["name"][0]["value"],
                         'name of owned-node-edge-points should be XPDR1-CLIENT1')

    def test_19_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(5, len(nodes), 'Topology should contain 5 nodes')
        self.assertEqual(4, len(links), 'Topology should contain 4 links')
        self.assertEqual(3, count_object_with_double_key(nodes, "name", "value-name", "otsi node name"),
                         'Topology should contain 3 otsi nodes')
        self.assertEqual(2, count_object_with_double_key(nodes, "name", "value-name", "dsr/odu node name"),
                         'Topology should contain 2 dsr nodes')
        self.assertEqual(2, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 2 transitional links')
        self.assertEqual(2, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 2 oms links')

    def test_20_connect_spdr_sa1(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)
        # TODO replace connect and disconnect timers with test_utils.wait_until_log_contains

    def test_21_connect_spdr_sc1(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(10)
        # TODO replace connect and disconnect timers with test_utils.wait_until_log_contains

    def test_22_check_tapi_topology_T100G(self):
        self.test_18_check_tapi_topology_T100G()

    def test_23_check_tapi_topology_T0(self):
        self.test_19_check_tapi_topology_T0()

    def test_24_connect_sprda_n1_to_roadma_pp2(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_25_connect_roadma_pp2_to_spdra_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_26_connect_sprdc_n1_to_roadmc_pp2(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_27_connect_roadmc_pp2_to_spdrc_n1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP2-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_28_check_tapi_topology_T100G(self):
        self.test_18_check_tapi_topology_T100G()

    def test_29_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(9, len(nodes), 'Topology should contain 9 nodes')
        self.assertEqual(8, len(links), 'Topology should contain 8 links')
        self.assertEqual(5, count_object_with_double_key(nodes, "name", "value-name", "otsi node name"),
                         'Topology should contain 5 otsi nodes')
        self.assertEqual(4, count_object_with_double_key(nodes, "name", "value-name", "dsr/odu node name"),
                         'Topology should contain 4 dsr nodes')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 4 transitional links')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 4 oms links')

    def test_30_add_oms_attributes(self):
        # Config ROADMA-ROADMC oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.4,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)
        # Config ROADMC-ROADMA oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.4,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 100000,
                "pmd": 0.5}]}}
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_31_create_OCH_OTU4_service(self):
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_32_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(9, len(nodes), 'Topology should contain 9 nodes')
        self.assertEqual(9, len(links), 'Topology should contain 9 links')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 4 transitional links')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 4 oms links')
        self.assertEqual(1, count_object_with_double_key(links, "name", "value-name", "otn link name"),
                         'Topology should contain 1 otn link')
        for link in links:
            if link["name"][0]["value"] == "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1":
                self.assertEqual(100000, link["available-capacity"]["total-size"]["value"],
                         'OTU4 link should have an available capacity of 100 000 Mbps')
            elif link["name"][0]["value-name"] == "transitional link name":
                self.assertEqual(100, link["available-capacity"]["total-size"]["value"],
                         'link should have an available capacity of 100 Gbps')
            self.assertEqual(2, len(link["node-edge-point"]), 'link should have 2 neps')

    def test_33_create_ODU4_service(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1-ODU4"
        self.cr_serv_sample_data["input"]["service-a-end"]["service-format"] = "ODU"
        del self.cr_serv_sample_data["input"]["service-a-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-a-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"
        self.cr_serv_sample_data["input"]["service-z-end"]["service-format"] = "ODU"
        del self.cr_serv_sample_data["input"]["service-z-end"]["otu-service-rate"]
        self.cr_serv_sample_data["input"]["service-z-end"]["odu-service-rate"] = "org-openroadm-otn-common-types:ODU4"

        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_34_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(9, len(nodes), 'Topology should contain 9 nodes')
        self.assertEqual(10, len(links), 'Topology should contain 10 links')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 4 transitional links')
        self.assertEqual(4, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 4 oms links')
        self.assertEqual(2, count_object_with_double_key(links, "name", "value-name", "otn link name"),
                         'Topology should contain 2 otn links')
        for link in links:
            if link["name"][0]["value"] == "OTU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1":
                self.assertEqual(0, link["available-capacity"]["total-size"]["value"],
                         'OTU4 link should have an available capacity of 0 Mbps')
            elif link["name"][0]["value"] == "ODU4-SPDR-SA1-XPDR1-XPDR1-NETWORK1toSPDR-SC1-XPDR1-XPDR1-NETWORK1":
                self.assertEqual(100000, link["available-capacity"]["total-size"]["value"],
                         'ODU4 link should have an available capacity of 100 000 Mbps')
            elif link["name"][0]["value-name"] == "transitional link name":
                self.assertEqual(100, link["available-capacity"]["total-size"]["value"],
                         'link should have an available capacity of 100 Gbps')
            self.assertEqual(2, len(link["node-edge-point"]), 'link should have 2 neps')

    def test_35_connect_sprda_2_n2_to_roadma_pp3(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SA1", "2", "2",
                                                          "ROADM-A1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_36_connect_roadma_pp3_to_spdra_2_n2(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SA1", "2", "2",
                                                          "ROADM-A1", "1", "SRG1-PP3-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_37_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(11, len(nodes), 'Topology should contain 11 nodes')
        self.assertEqual(12, len(links), 'Topology should contain 12 links')
        self.assertEqual(6, count_object_with_double_key(nodes, "name", "value-name", "otsi node name"),
                         'Topology should contain 6 otsi nodes')
        self.assertEqual(5, count_object_with_double_key(nodes, "name", "value-name", "dsr/odu node name"),
                         'Topology should contain 5 dsr nodes')
        self.assertEqual(5, count_object_with_double_key(links, "name", "value-name", "transitional link name"),
                         'Topology should contain 5 transitional links')
        self.assertEqual(5, count_object_with_double_key(links, "name", "value-name", "OMS link name"),
                         'Topology should contain 5 oms links')
        self.assertEqual(2, count_object_with_double_key(links, "name", "value-name", "otn link name"),
                         'Topology should contain 2 otn links')

    def test_38_delete_ODU4_service(self):
        response = test_utils.service_delete_request("service1-ODU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_39_delete_OCH_OTU4_service(self):
        response = test_utils.service_delete_request("service1-OCH-OTU4")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_40_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        links = res["output"]["topology"]["link"]
        self.assertEqual(11, len(nodes), 'Topology should contain 11 nodes')
        self.assertEqual(10, len(links), 'Topology should contain 10 links')
        self.assertEqual(0, count_object_with_double_key(links, "name", "value-name", "otn link name"),
                         'Topology should contain 0 otn link')

    def test_41_disconnect_xponders_from_roadm(self):
        url = "{}/config/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        for link in links:
            if (link["org-openroadm-common-network:link-type"] == "XPONDER-OUTPUT" or
            link["org-openroadm-common-network:link-type"] == "XPONDER-INPUT"):
                 link_name = link["link-id"]
                 response = test_utils.delete_request(url+link_name)
                 self.assertEqual(response.status_code, requests.codes.ok)

    def test_42_check_tapi_topology_T0(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "T0 - Multi-layer topology"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        nodes = res["output"]["topology"]["node"]
        self.assertEqual(1, len(nodes), 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')
        self.assertEqual("ROADM-infra", res["output"]["topology"]["node"][0]["name"][0]["value"],
                         'node name should be: ROADM-infra')

    def test_43_get_tapi_topology_T100G(self):
        url = "{}/operations/tapi-topology:get-topology-details"
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "Transponder 100GE"
            }
        }
        response = test_utils.post_request(url, data)
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'Topology should contain 1 node')
        self.assertNotIn("link", res["output"]["topology"], 'Topology should contain no link')
        self.assertNotIn("owned-node-edge-point", res["output"]["topology"]["node"][0],
                         'Node should contain no owned-node-edge-points')

    def test_44_disconnect_roadma(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_45_disconnect_roadmc(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_46_check_tapi_topos(self):
        self.test_01_get_tapi_topology_T100G()
        self.test_02_get_tapi_topology_T0()

    def test_47_disconnect_xpdra(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_48_disconnect_xpdrc(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_49_disconnect_spdr_sa1(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)
        time.sleep(5)

    def test_50_disconnect_spdr_sc1(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


def find_object_with_key(list_dicts, key, value):
    for dict_ in list_dicts:
        if dict_[key] == value:
            return dict_
    return None

def count_object_with_double_key(list_dicts, key1, key2, value):
    nb = 0
    for dict in list_dicts:
        if dict[key1][0][key2] == value:
            nb += 1
    return nb


if __name__ == "__main__":
    unittest.main(verbosity=2)