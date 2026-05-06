#!/usr/bin/env python

##############################################################################
# Copyright (c) 2021 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-public-methods
# pylint: disable=consider-using-f-string
# pylint: disable=line-too-long

import os
import unittest
import xml.etree.ElementTree as ET
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


# pylint: disable=too-few-public-methods
class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class UuidServices2:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class UuidServices3:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None


class TransportPCEtesting(unittest.TestCase):

    processes = []
    LONG_WAIT = 20  # nominal value is 300
    SHORT_WAIT = LONG_WAIT / 4
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()
    uuid_services2 = UuidServices2()
    uuid_services3 = UuidServices3()
    uuidTpAiOTSI = "f4c370be-e307-380d-a31b-7edc2b431e5d"
    uuidTpZiOTSI = "febb4502-6b27-3701-990b-3aa4941f48c4"
    uuidTpADSR = "3aca9b37-bc46-335d-b147-2423690dee18"
    uuidTpZDSR = "709d3595-6d56-3ad6-a3cd-5a4a09ce6f9d"
    uuidSpdrSA1xpdr1 = "4e44bcc5-08d3-3fee-8fac-f021489e5a61"
    uuidOnepSpdrSA1xpdr1OTS = "21efd6a4-2d81-3cdb-aabb-b983fb61904e"
    uuidOnepSpdrSA1xpdr1OTU = "7d2a0549-63e3-3c7f-b4dc-5653e8a81dbe"
    uuidOnepSpdrSA1xpdr1OTSi = "f32f5e9e-d167-31ba-a9e4-8f1efdb8786d"
    uuidOnepSpdrSA1xpdr1iODU = "2bdca70f-ef1e-3e56-b251-07eda88f31ba"
    uuidOnepSpdrSA1xpdr1eODUC1 = "72c6b97a-3944-3d88-9882-b7e688bb2772"
    uuidOnepSpdrSA1xpdr1DSRC1 = "c6cd334c-51a1-3995-bed3-5cf2b7445c04"
    #   SIP uuids
    # SIP+SPDR-SA1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sAOTS = "38d81f55-1798-3520-ba16-08efa56630c4"
    # SIP+SPDR-SC1-XPDR1+PHOTONIC_MEDIA_OTS+XPDR1-NETWORK1 UUID IS
    sZOTS = "97d9ba27-0efa-3010-8b98-b4d73240120c"
    # SIP+SPDR-SA1-XPDR1+eOTSi+XPDR1-NETWORK1 UUID IS
    sAeOTS = "f4dbce65-6191-3c84-b351-29ccb0629221"
    # SIP+SPDR-SC1-XPDR1+eOTSi+XPDR1-NETWORK1 UUID IS
    sZeOTS = "faabebd3-d7af-3389-96a7-261672744591"
    # SIP+SPDR-SA1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sADSR = "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
    # SIP+SPDR-SC1-XPDR1+DSR+XPDR1-CLIENT1 UUID IS
    sZDSR = "25812ef2-625d-3bf8-af55-5e93946d1c22"
    # SIP UUID IS
    sAeODU = "b6421484-531f-3444-adfc-e11c503f1fab"
    # SIP+SPDR-SC1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sZeODU = "4511fca7-0cf7-3b27-a128-3b372d5e1fa8"
    # SIP+SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2AOTS = "53e45a61-48a5-3d47-a045-b8418c3e9f8a"
    # SIP+SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK1 UUID IS
    s2ZOTS = "c7549fcc-895f-3fa2-ad81-ea00e25772e9"
    # SIP+SPDR-SA1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    s3AOTS = "858e8cbf-d264-3e6f-858e-cdb6257d3aa9"
    # SIP+SPDR-SC1-XPDR3+PHOTONIC_MEDIA_OTS+XPDR3-NETWORK1 UUID IS
    s3ZOTS = "6431ec11-f109-39f6-9568-c037f0e006f3"
    # SIP+SPDR-SA1-XPDR3+eODU+XPDR3-CLIENT1 UUID IS
    s3AeODU = "ba18706f-e576-324f-91d3-916200203dd3"
    # SIP+SPDR-SC1-XPDR3+eODU+XPDR3-CLIENT1 UUID IS
    s3ZeODU = "8299a120-f2df-30d7-a9f5-a407f9968689"
    # SIP+SPDR-SA1-XPDR3+iODU+XPDR3-NETWORK1 UUID IS
    s3AiODU = "3f395974-bb7a-3411-ac4c-f791752bdf06"
    # SIP+SPDR-SC1-XPDR3+iODU+XPDR3-NETWORK UUID IS
    s3ZiODU = "2fe1c941-f9c9-34f4-86c4-8e97ae880ecd"

    cr_serv_input_data = {
        "name": [
            {
                "value-name": "service-name",
                "value": "servicephotonic-1"
            }
        ],
        "end-point": [
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
                },
                "connection-end-point": [
                    {
                        "topology-uuid": "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                        "node-uuid": "4e44bcc5-08d3-3fee-8fac-f021489e5a61",
                        "node-edge-point-uuid": "21efd6a4-2d81-3cdb-aabb-b983fb6190C",
                        "connection-end-point-uuid": "d8ef5622-df73-322f-8b62-e51a2ec3f797"
                    }
                ],
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "SPDR-SA1-XPDR1",
                "name": [
                    {
                        "value-name": "OpenROADM node id",
                        "value": "SPDR-SA1-XPDR1"
                    }
                ]
            },
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "25812ef2-625d-3bf8-af55-5e93946d1c22"
                },
                "connection-end-point": [
                    {
                        "topology-uuid": "393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7",
                        "node-uuid": "215ee18f-7869-3492-94d2-0f24ed0a3023",
                        "node-edge-point-uuid": "ff10784b-3da2-3b88-88c3-27abc02b66fe",
                        "connection-end-point-uuid": "cf91f296-7ce7-3d15-a868-0c65d3f76453"
                    }
                ],
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "SPDR-SC1-XPDR1",
                "name": [
                    {
                        "value-name": "OpenROADM node id",
                        "value": "SPDR-SC1-XPDR1"
                    }
                ]
            }
        ],
        "connectivity-constraint": {
            "service-type": "POINT_TO_POINT_CONNECTIVITY",
            "service-level": "Some service-level",
            "requested-capacity": {
                "total-size": {
                    "value": "100",
                    "unit": "tapi-common:CAPACITY_UNIT_GBPS"
                }
            }
        },
        "topology-constraint": [
            {
                "local-id": "localIdTopoConstraint",
                "name": [
                    {
                        "value-name": "Dumb constraint",
                        "value": "for debug1"
                    }
                ]
            }
        ],
        "state": "LOCKED",
        "layer-protocol-name": "PHOTONIC_MEDIA"}

    del_serv_input_data = {"uuid": "TBD"}

    tapi_topo = {"topology-id": "TBD"}

    @classmethod
    def setUpClass(cls):
        cls.init_failed = False
        if "NO_ODL_STARTUP" not in os.environ or "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing tapi feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed = True
        if cls.init_failed:
            print("tapi installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('xpdra', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):
        time.sleep(self.LONG_WAIT)

    def _wait_until_connected(self, node: str):
        deadline = time.time() + self.LONG_WAIT
        while time.time() < deadline:
            result = test_utils.check_device_connection(node)
            if result.get('connection-status') == 'connected':
                return
            time.sleep(0.5)
        self.fail(f"Node {node} did not reach 'connected' state within {self.LONG_WAIT}s")

    def test_01_connect_spdrA(self):
        print("Connecting SPDRA")
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("SPDR-SA1")

    def test_02_connect_spdrC(self):
        print("Connecting SPDRC")
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("SPDR-SC1")

    def test_02b_connect_xpdrA(self):
        print("Connecting XPDRA")
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("XPDR-A1")

    def test_02c_connect_xpdrC(self):
        print("Connecting XPDRC")
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("XPDR-C1")

    def test_03_connect_rdmA(self):
        print("Connecting ROADMA")
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("ROADM-A1")

    def test_04_connect_rdmC(self):
        print("Connecting ROADMC")
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        self._wait_until_connected("ROADM-C1")

    def test_05_connect_sprdA_1_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_06_connect_roadmA_PP1_to_spdrA_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_07_connect_sprdA_2_N1_to_roadmA_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_08_connect_roadmA_PP2_to_spdrA_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_09_connect_sprdA_3_N1_to_roadmA_PP3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_10_connect_roadmA_PP3_to_spdrA_1_N3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_10b_connect_xpdrA_1_N1_to_roadmA_PP4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_10c_connect_roadmA_PP4_to_xpddrA_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_11_connect_sprdC_1_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_12_connect_roadmC_PP1_to_spdrC_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_13_connect_sprdC_2_N1_to_roadmC_PP2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_14_connect_roadmC_PP2_to_spdrC_2_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP2-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_15_connect_sprdC_3_N1_to_roadmC_PP3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_16_connect_roadmC_PP3_to_spdrC_1_N3(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '3', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP3-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_16b_connect_xpdrC_1_N1_to_roadmC_PP4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_16c_connect_roadmC_PP4_to_xpdrC_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_17_connect_sprdA_2_N2_to_roadmA_PP4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_18_connect_roadmA_PP4_to_spdrA_2_N2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_19_connect_sprdC_2_N2_to_roadmC_PP4(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(self.SHORT_WAIT)

    def test_20_connect_roadmC_PP4_to_spdrC_2_N2(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '2', 'network-num': '2',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP4-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(self.LONG_WAIT)

    def test_21_add_omsAttributes_ROADMA_ROADMC(self):
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
        time.sleep(self.SHORT_WAIT)

    def test_22_add_omsAttributes_ROADMC_ROADMA(self):
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
        time.sleep(self.LONG_WAIT)

    def test_23_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 10, 'There should be 10 otn nodes')
        self.assertNotIn('ietf-network-topology:link', response['network'][0])
        time.sleep(self.SHORT_WAIT)

    def test_24_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 17, 'There should be 17 openroadm nodes')
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 38,
                         'There should be 38 openroadm links')
        time.sleep(self.LONG_WAIT)

    def test_25_get_tapi_topology_details(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['output']['topology']['node']), 11, 'There should be 11 TAPI nodes')
        self.assertEqual(len(response['output']['topology']['link']), 11, 'There should be 11 TAPI links')
        time.sleep(self.SHORT_WAIT)

    def test_26_check_sip_details(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-common', 'get-service-interface-point-list', None)
        self.assertEqual(len(response['output']['sip']), 104, 'There should be 104 service interface point')
        time.sleep(self.SHORT_WAIT)

    def test_27_get_tapi_node_details_before_PhtService_creation(self):
        response = test_utils.get_tapi_topology_node(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, self.uuidSpdrSA1xpdr1, self.uuidOnepSpdrSA1xpdr1OTS, "nonconfig")
        self.assertEqual(response['status_code'], requests.codes.ok)
        input_dict_1 = {"spectrum-capability-pac": {"available-spectrum": [{"upper-frequency": "196125000000000",
                                                                            "lower-frequency": "191325000000000"}],
                                                    "supportable-spectrum": [{"upper-frequency": "196125000000000",
                                                                              "lower-frequency": "191325000000000"}]}
                        }
        self.assertDictEqual(dict(input_dict_1,
                                  **response['onep'][0]['tapi-photonic-media:photonic-media-node-edge-point-spec']),
                             response['onep'][0]['tapi-photonic-media:photonic-media-node-edge-point-spec'])
        time.sleep(self.SHORT_WAIT)

    # test create connectivity service from spdrA to spdrC for Photonic_media
    def test_28_create_connectivity_service_PhotonicMedia(self):
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAOTS
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZOTS
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services.pm))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        # 'service-layer': 'PHOTONIC_MEDIA',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        # 'connectivity-direction': 'BIDIRECTIONAL'
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_29_get_service_PhotonicMedia(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicephotonic-1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "servicephotonic-1")
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.SHORT_WAIT)

    def test_30_create_connectivity_service2_PhotonicMedia(self):
        self.cr_serv_input_data["name"][0]["value"] = "servicePhotonic2"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s2AOTS
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s2ZOTS
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR2"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = "38c114ae-9c0e-3068-bb27-db2dbd81220b"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = "ff69716d-325d-35fc-baa9-0c4bcd992bbd"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = "a4411c09-2913-334d-b652-8837b8e9c5f8"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = "d852c340-77db-3f9a-96e8-cb4de8e1004a"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = "8557ddb5-4ee9-33e0-ba0f-adf713868625"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = "54100e71-96c1-3bd4-bf3c-9cd9cdfdb78dA"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR2"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR2"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)

        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services2.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services2.pm))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR2'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR2'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_31_get_service2_PhotonicMedia(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicePhotonic2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "servicePhotonic2")
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.SHORT_WAIT)

    def test_32_create_connectivity_service3_PhotonicMedia(self):
        self.cr_serv_input_data["name"][0]["value"] = "servicePhotonic3"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s3AOTS
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s3ZOTS
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = "4582e51f-2b2d-3b70-b374-86c463062710"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = "339d9470-aef0-304f-9667-b685e1624653"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = "45f90fd9-b53d-38d2-82e0-bbea63588d6a"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = "c1f06957-c0b9-32be-8492-e278b2d4a3aa"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = "c0dda59b-8af1-3996-ba32-8fc4b12d944a"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = "c6059833-28ce-3cd5-a600-80b2ac1165f0"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)

        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services3.pm = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("photonic media service uuid : {}".format(self.uuid_services3.pm))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'layer-protocol-name': 'PHOTONIC_MEDIA',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR3'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR3'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_33_get_service3_PhotonicMedia(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "servicePhotonic3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "servicePhotonic3")
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.SHORT_WAIT)

    # test create connectivity service from spdrA to spdrC for odu
    def test_34_create_connectivity_service_ODU(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["name"][0]["value"] = "serviceODU4_1"
        self.cr_serv_input_data["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAeODU
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZeODU
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR1"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR1"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = "4e44bcc5-08d3-3fee-8fac-f021489e5a61"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = "72c6b97a-3944-3d88-9882-b7e688bb2772"
        # self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = "215ee18f-7869-3492-94d2-0f24ed0a3023"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = "99e1461c-4679-3dc5-9f59-b3054dce08a4"
        # self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        #        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "ODU"
        self.cr_serv_input_data["connectivity-constraint"]["service-level"] = self.uuid_services.pm

        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)

        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.odu = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("odu service uuid : {}".format(self.uuid_services.odu))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'layer-protocol-name': 'ODU',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_35_get_service_ODU(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceODU4_1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "serviceODU4_1")
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.SHORT_WAIT)

    # test create connectivity service from spdrA to spdrC for odu
    def test_36_create_connectivity_service3_ODU(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["name"][0]["value"] = "serviceODU4_3"
        self.cr_serv_input_data["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.s3AiODU
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.s3ZiODU
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR3"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR3"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = "4582e51f-2b2d-3b70-b374-86c463062710"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = "9f4f5e08-5590-30e4-a7ff-f9cad13090bc"
        # self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = "c1f06957-c0b9-32be-8492-e278b2d4a3aa"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = "abd4fb5d-7ec0-3fbc-884a-d6e6615f7981"
        # self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        #        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "ODU"
        self.cr_serv_input_data["connectivity-constraint"]["service-level"] = self.uuid_services3.pm

        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services3.odu = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("odu service uuid : {}".format(self.uuid_services3.odu))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'layer-protocol-name': 'ODU',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR3'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR3'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_37_get_service3_ODU(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceODU4_3")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "serviceODU4_3")
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(self.SHORT_WAIT)

    # test create connectivity service from spdrA to spdrC for dsr
    def test_38_create_connectivity_service_DSR(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["name"][0]["value"] = "serviceDSR"
        self.cr_serv_input_data["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sADSR
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZDSR
        self.cr_serv_input_data["end-point"][0]["local-id"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["local-id"] = "SPDR-SC1-XPDR1"
        self.cr_serv_input_data["end-point"][0]["name"][0]["value"] = "SPDR-SA1-XPDR1"
        self.cr_serv_input_data["end-point"][1]["name"][0]["value"] = "SPDR-SC1-XPDR1"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-uuid"] = "4e44bcc5-08d3-3fee-8fac-f021489e5a61"
        self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["node-edge-point-uuid"] = "c6cd334c-51a1-3995-bed3-5cf2b7445c04"
        # self.cr_serv_input_data["end-point"][0]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-uuid"] = "215ee18f-7869-3492-94d2-0f24ed0a3023"
        self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["node-edge-point-uuid"] = "50b7521a-4a38-358f-9846-45c55813416a"
        # self.cr_serv_input_data["end-point"][1]["connection-end-point"][0]["connection-end-point-uuid"] = ""
        #        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "DSR"
        self.cr_serv_input_data["connectivity-constraint"]["requested-capacity"]["total-size"]["value"] = "10"
        self.cr_serv_input_data["connectivity-constraint"]["service-level"] = self.uuid_services.odu

        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.dsr = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("dsr service uuid : {}".format(self.uuid_services.dsr))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'layer-protocol-name': 'DSR',
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1,
                                  **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2,
                                  **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3,
                                  **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        serv_name = self.cr_serv_input_data["name"][0]["value"]
        self.assertTrue(
            test_utils.wait_until_log_contains(
                test_utils.TPCE_LOG,
                [f'Done, OpenROADM service {serv_name} copied to TAPI!',
                 f'Copy OpenROADM service {serv_name} to TAPI failed!'],
                self.LONG_WAIT),
            f'Timed out waiting for service {serv_name} to complete')

    def test_39_get_service_DSR(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "serviceDSR")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], "serviceDSR")
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(120)
        print("Time to retrieve topology : 120 seconds... Hurry up")

    def test_40_get_tapi_context(self):
        url = {'rfc8040': '{}/data/tapi-common:context',
               'draft-bierman02': '{}/operational/tapi-common:context'}
        response = requests.request(
            'GET', url[test_utils.RESTCONF_VERSION].format(test_utils.RESTCONF_BASE_URL),
            headers=test_utils.TYPE_APPLICATION_XML,
            auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD),
            timeout=test_utils.REQUEST_TIMEOUT)
        self.assertEqual(response.status_code, requests.codes.ok)
        root = ET.fromstring(response.text)
        ET.indent(root)
        tree = ET.ElementTree(root)
        ref_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), '..', '..', 'tapi-context.xml')
        with open(ref_path, 'wb') as f:
            tree.write(f, encoding='utf-8', xml_declaration=True)


if __name__ == "__main__":
    unittest.main(verbosity=2)
