#!/usr/bin/env python

##############################################################################
# Copyright (c) 2022 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=invalid-name
# pylint: disable=no-member
# pylint: disable=too-many-public-methods
# pylint: disable=too-many-lines

import json
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys

sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEtesting(unittest.TestCase):
    processes = None
    WAITING = 20  # nominal value is 300
    NODE_VERSION_221 = '2.2.1'
    NODE_VERSION_71 = '7.1'

    cr_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "request-1",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service1",
        "common-id": "commonId",
        "connection-type": "service",
        "service-resiliency": {
            "resiliency": "org-openroadm-common-service-types:restorable"
        },
        "service-a-end": {
            "service-rate": "400",
            "node-id": "XPDR-A2",
            "service-format": "Ethernet",
            "clli": "NodeA",
            "tx-direction": [
                {
                    "index": 0
                }
            ],
            "rx-direction": [
                {
                    "index": 0
                }
            ],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "400",
            "node-id": "XPDR-C2",
            "service-format": "Ethernet",
            "clli": "NodeC",
            "tx-direction": [
                {
                    "index": 0
                }
            ],
            "rx-direction": [
                {
                    "index": 0
                }
            ],
            "optic-type": "gray"
        },
        "due-date": "2018-06-15T00:00:01Z",
        "operator-contact": "pw1234"
    }

    del_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-delete",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"},
        "service-delete-req-info": {
            "service-name": "TBD",
            "tail-retention": "no"}
    }

    service_path_service_1_AtoZ = [
        {
            "id": "16",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "17",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "14",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-CTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "15",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX"
            }
        },
        {
            "id": "18",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP1-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "19",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-SRG1-SRG1-PP1-TXRXtoXPDR-C2-XPDR1-XPDR1-NETWORK1"
            }
        },
        {
            "id": "1",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "2",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-NETWORK1",
                "tp-node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "0",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-CLIENT1",
                "tp-node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "5",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "6",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "3",
            "resource": {
                "state": "inService",
                "link-id": "XPDR-A2-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX"
            }
        },
        {
            "id": "4",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP1-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "9",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "7",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX"
            }
        },
        {
            "id": "8",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-CTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "20",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-NETWORK1",
                "tp-node-id": "XPDR-C2-XPDR1"
            }
        },
        {
            "id": "12",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-TTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "13",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "10",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-TTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "21",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-C2-XPDR1"
            }
        },
        {
            "id": "11",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
            }
        },
        {
            "id": "22",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-CLIENT1",
                "tp-node-id": "XPDR-C2-XPDR1"
            }
        }
    ]

    service_path_service_1_rerouted_AtoZ = [
        {
            "id": "27",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-SRG1-SRG1-PP1-TXRXtoXPDR-C2-XPDR1-XPDR1-NETWORK1"
            }
        },
        {
            "id": "28",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-NETWORK1",
                "tp-node-id": "XPDR-C2-XPDR1"
            }
        },
        {
            "id": "25",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "26",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP1-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "29",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-C2-XPDR1"
            }
        },
        {
            "id": "30",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-CLIENT1",
                "tp-node-id": "XPDR-C2-XPDR1"
            }
        },
        {
            "id": "12",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-TTP-TXRX",
                "tp-node-id": "ROADM-B1-DEG1"
            }
        },
        {
            "id": "13",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-B1-DEG1"
            }
        },
        {
            "id": "10",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-TTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG1"
            }
        },
        {
            "id": "11",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX"
            }
        },
        {
            "id": "16",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-CTP-TXRX",
                "tp-node-id": "ROADM-B1-DEG2"
            }
        },
        {
            "id": "17",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-B1-DEG2"
            }
        },
        {
            "id": "14",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-CTP-TXRX",
                "tp-node-id": "ROADM-B1-DEG1"
            }
        },
        {
            "id": "15",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-B1-DEG1-DEG1-CTP-TXRXtoROADM-B1-DEG2-DEG2-CTP-TXRX"
            }
        },
        {
            "id": "18",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-TTP-TXRX",
                "tp-node-id": "ROADM-B1-DEG2"
            }
        },
        {
            "id": "19",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX"
            }
        },
        {
            "id": "1",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "2",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-NETWORK1",
                "tp-node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "0",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR1-CLIENT1",
                "tp-node-id": "XPDR-A2-XPDR1"
            }
        },
        {
            "id": "5",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "6",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "3",
            "resource": {
                "state": "inService",
                "link-id": "XPDR-A2-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX"
            }
        },
        {
            "id": "4",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP1-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "9",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-DEG1"
            }
        },
        {
            "id": "7",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG1-DEG1-CTP-TXRX"
            }
        },
        {
            "id": "8",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-CTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG1"
            }
        },
        {
            "id": "20",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-TTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG2"
            }
        },
        {
            "id": "23",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-DEG2-DEG2-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX"
            }
        },
        {
            "id": "24",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "21",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-DEG2"
            }
        },
        {
            "id": "22",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-CTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG2"
            }
        }
    ]

    service_path_service_2_AtoZ = [
        {
            "id": "16",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "17",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "14",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-CTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "15",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-DEG1-DEG1-CTP-TXRXtoROADM-C1-SRG1-SRG1-CP-TXRX"
            }
        },
        {
            "id": "18",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP2-TXRX",
                "tp-node-id": "ROADM-C1-SRG1"
            }
        },
        {
            "id": "19",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-C1-SRG1-SRG1-PP2-TXRXtoXPDR-C2-XPDR3-XPDR3-NETWORK1"
            }
        },
        {
            "id": "1",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-A2-XPDR3"
            }
        },
        {
            "id": "2",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR3-NETWORK1",
                "tp-node-id": "XPDR-A2-XPDR3"
            }
        },
        {
            "id": "0",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR3-CLIENT1",
                "tp-node-id": "XPDR-A2-XPDR3"
            }
        },
        {
            "id": "5",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "6",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-CP-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "3",
            "resource": {
                "state": "inService",
                "link-id": "XPDR-A2-XPDR3-XPDR3-NETWORK1toROADM-A1-SRG1-SRG1-PP2-TXRX"
            }
        },
        {
            "id": "4",
            "resource": {
                "state": "inService",
                "tp-id": "SRG1-PP2-TXRX",
                "tp-node-id": "ROADM-A1-SRG1"
            }
        },
        {
            "id": "9",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "7",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-SRG1-SRG1-CP-TXRXtoROADM-A1-DEG2-DEG2-CTP-TXRX"
            }
        },
        {
            "id": "8",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-CTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "20",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR3-NETWORK1",
                "tp-node-id": "XPDR-C2-XPDR3"
            }
        },
        {
            "id": "12",
            "resource": {
                "state": "inService",
                "tp-id": "DEG1-TTP-TXRX",
                "tp-node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "13",
            "resource": {
                "state": "inService",
                "node-id": "ROADM-C1-DEG1"
            }
        },
        {
            "id": "10",
            "resource": {
                "state": "inService",
                "tp-id": "DEG2-TTP-TXRX",
                "tp-node-id": "ROADM-A1-DEG2"
            }
        },
        {
            "id": "21",
            "resource": {
                "state": "inService",
                "node-id": "XPDR-C2-XPDR3"
            }
        },
        {
            "id": "11",
            "resource": {
                "state": "inService",
                "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
            }
        },
        {
            "id": "22",
            "resource": {
                "state": "inService",
                "tp-id": "XPDR3-CLIENT1",
                "tp-node-id": "XPDR-C2-XPDR3"
            }
        }
    ]

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra2', cls.NODE_VERSION_71),
                                               ('roadma', cls.NODE_VERSION_221),
                                               ('roadmb', cls.NODE_VERSION_221),
                                               ('roadmc', cls.NODE_VERSION_221),
                                               ('xpdrc2', cls.NODE_VERSION_71)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(1)

    def test_01_connect_xpdra2(self):
        response = test_utils.mount_device("XPDR-A2", ('xpdra2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrc2(self):
        response = test_utils.mount_device("XPDR-C2", ('xpdrc2', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdma(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmb(self):
        response = test_utils.mount_device("ROADM-B1", ('roadmb', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_rdmc(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_06_connect_xprda2_1_N1_to_roadma_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_07_connect_roadma_PP1_to_xpdra2_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_08_connect_xprdc2_1_N1_to_roadmc_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_09_connect_roadmc_PP1_to_xpdrc2_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_10_connect_xprda2_3_N1_to_roadma_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_11_connect_roadma_PP2_to_xpdra2_3_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_12_connect_xprdc2_3_N1_to_roadmc_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_13_connect_roadmc_PP2_to_xpdrc2_3_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_14_add_omsAttributes_roadma_roadmc(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_15_add_omsAttributes_roadmc_roadma(self):
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_16_add_omsAttributes_roadma_roadmb(self):
        # Config ROADMA-ROADMB oms-attributes
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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_17_add_omsAttributes_roadmb_roadma(self):
        # Config ROADMB-ROADMA oms-attributes
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
        response = test_utils.add_oms_attr_request(
            "ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_18_add_omsAttributes_roadmb_roadmc(self):
        # Config ROADMB-ROADMC oms-attributes
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
        response = test_utils.add_oms_attr_request(
            "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_19_add_omsAttributes_roadmc_roadmb(self):
        # Config ROADMC-ROADMB oms-attributes
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    def test_20_create_OTS_ROADMA_DEG1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-A1',
                'logical-connection-point': 'DEG1-TTP-TXRX'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-A1',
                      response["output"]["result"])

    def test_21_create_OTS_ROADMC_DEG2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-C1',
                'logical-connection-point': 'DEG2-TTP-TXRX'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-C1',
                      response["output"]["result"])

    def test_22_create_OTS_ROADMB_DEG1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-B1',
                'logical-connection-point': 'DEG1-TTP-TXRX'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG1-TTP-TXRX - OMS-DEG1-TTP-TXRX successfully created on node ROADM-B1',
                      response["output"]["result"])

    def test_23_create_OTS_ROADMB_DEG2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-device-renderer', 'create-ots-oms',
            {
                'node-id': 'ROADM-B1',
                'logical-connection-point': 'DEG2-TTP-TXRX'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Interfaces OTS-DEG2-TTP-TXRX - OMS-DEG2-TTP-TXRX successfully created on node ROADM-B1',
                      response["output"]["result"])

    def test_24_calculate_span_loss_base_all(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-olm', 'calculate-spanloss-base',
            {
                'src-type': 'all'
            })
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Success', response["output"]["result"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-B1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "23.6",
            "link-id": "ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-B1-DEG1-DEG1-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "25.7",
            "link-id": "ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-B1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        self.assertIn({
            "spanloss": "17.6",
            "link-id": "ROADM-B1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX"
        }, response["output"]["spans"])
        time.sleep(1)

    # test service-create for Eth service from xpdr to xpdr with service-resiliency
    def test_25_create_eth_service1_with_service_resiliency_restorable(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_26_get_eth_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service1')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        self.assertEqual(
            response['services'][0]['service-resiliency']['resiliency'],
            'org-openroadm-common-service-types:restorable')
        time.sleep(1)

    def test_27_get_service_path_service_1(self):
        response = test_utils.get_serv_path_list_attr("service-paths", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertCountEqual(
            self.service_path_service_1_AtoZ,
            response['service-paths'][0]['path-description']['aToZ-direction']['aToZ'])

    # test service-create for Eth service from xpdr to xpdr without service-resiliency
    def test_28_create_eth_service2_without_service_resiliency(self):
        self.cr_serv_input_data["service-name"] = "service2"
        del self.cr_serv_input_data["service-resiliency"]
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_29_get_eth_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service2')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        self.assertNotIn('service-resiliency', response['services'][0])
        time.sleep(1)

    def test_30_get_service_path_service_2(self):
        response = test_utils.get_serv_path_list_attr("service-paths", "service2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertCountEqual(
            self.service_path_service_2_AtoZ,
            response['service-paths'][0]['path-description']['aToZ-direction']['aToZ'])

    # Degrade ROADM-A1-ROADM-C1 link
    def test_31_set_pm_ROADMA_OTS_DEG2_TTP_TXRX_OpticalPowerInput(self):
        url = "{}/operations/pm-handling:pm-interact"
        body = {
            "input": {
                "rpc-action": "set",
                "pm-to-be-set-or-created": {
                    "current-pm-entry": [
                        {
                            "pm-resource-instance": "/org-openroadm-device:org-openroadm-device/org-openroadm-device"
                                                    ":interface[org-openroadm-device:name='OTS-DEG2-TTP-TXRX']",
                            "pm-resource-type": "interface",
                            "pm-resource-type-extension": "",
                            "current-pm": [
                                {
                                    "type": "opticalPowerInput",
                                    "extension": "",
                                    "location": "nearEnd",
                                    "direction": "rx",
                                    "measurement": [
                                        {
                                            "granularity": "15min",
                                            "pmParameterValue": -30,
                                            "pmParameterUnit": "dBm",
                                            "validity": "complete"
                                        },
                                        {
                                            "granularity": "24Hour",
                                            "pmParameterValue": -21.3,
                                            "pmParameterUnit": "dBm",
                                            "validity": "complete"
                                        }
                                    ]
                                }
                            ]
                        }
                    ]
                }
            }
        }
        response = requests.request("POST", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD),
                                    timeout=test_utils.REQUEST_TIMEOUT)
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json()['output']['status-message'], "The PMs has been successfully set !")
        time.sleep(self.WAITING * 2)

    def test_32_get_eth_service1(self):
        self.test_26_get_eth_service1()

    def test_33_get_service_path_service_1(self):
        response = test_utils.get_serv_path_list_attr("service-paths", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertCountEqual(
            self.service_path_service_1_rerouted_AtoZ,
            response['service-paths'][0]['path-description']['aToZ-direction']['aToZ'])

    def test_34_get_eth_service2(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service2")
        self.assertEqual(response['services'][0]['operational-state'], 'outOfService')
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service2')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        self.assertNotIn('service-resiliency', response['services'])
        time.sleep(1)

    def test_35_get_service_path_service_2(self):
        response = test_utils.get_serv_path_list_attr("service-paths", "service2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        index = self.service_path_service_2_AtoZ.index(
            {
                'id': '10',
                'resource': {
                    'state': 'inService',
                    'tp-id': 'DEG2-TTP-TXRX',
                    'tp-node-id': 'ROADM-A1-DEG2'
                }
            }
        )
        service_path_expected = self.service_path_service_2_AtoZ[:index] + [{
            'id': '10',
            'resource': {
                'state': 'outOfService',
                'tp-id': 'DEG2-TTP-TXRX',
                'tp-node-id': 'ROADM-A1-DEG2'
            }
        }] + self.service_path_service_2_AtoZ[index + 1:]
        self.assertCountEqual(service_path_expected,
                              response['service-paths'][0]['path-description']['aToZ-direction']['aToZ'])

    # Restore ROADM-A1-ROADM-C1 link
    def test_36_clear_pm_ROADMA_OTS_DEG2_TTP_TXRX_OpticalPowerInput(self):
        url = "{}/operations/pm-handling:pm-interact"
        body = {
            "input": {
                "rpc-action": "clear",
                "pm-to-get-clear-or-delete": {
                    "current-pm-entry": [
                        {
                            "pm-resource-instance": "/org-openroadm-device:org-openroadm-device/org-openroadm-device"
                                                    ":interface[org-openroadm-device:name='OTS-DEG2-TTP-TXRX']",
                            "pm-resource-type": "interface",
                            "pm-resource-type-extension": "",
                            "current-pm": [
                                {
                                    "type": "opticalPowerInput",
                                    "extension": "",
                                    "location": "nearEnd",
                                    "direction": "rx"
                                }
                            ]
                        }
                    ]
                }
            }
        }
        response = requests.request("POST", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD),
                                    timeout=test_utils.REQUEST_TIMEOUT)
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertEqual(response.json()['output']['status-message'], "The PMs has been successfully released !")
        time.sleep(2)

    def test_37_get_eth_service1(self):
        self.test_26_get_eth_service1()

    def test_38_get_service_path_service_1(self):
        self.test_33_get_service_path_service_1()

    def test_39_get_eth_service2(self):
        self.test_29_get_eth_service2()

    def test_40_get_service_path_service_2(self):
        self.test_30_get_service_path_service_2()

    def test_41_delete_eth_service2(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service2"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_42_delete_eth_service1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_43_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_44_disconnect_xpdra2(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_45_disconnect_xpdrc2(self):
        response = test_utils.unmount_device("XPDR-C2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_46_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_47_disconnect_roadmB(self):
        response = test_utils.unmount_device("ROADM-B1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_48_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
