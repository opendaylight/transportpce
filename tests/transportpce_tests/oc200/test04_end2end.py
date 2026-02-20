#!/usr/bin/env python
##############################################################################
# Copyright © 2026 NTT and others.  All rights reserved.
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
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_oc  # nopep8


class TransportPCEOC200End2EndTesting(unittest.TestCase):
    """
    End-to-end functional tests for OpenConfig 2.0.0 (oc200) XPDR devices
    with OpenROADM 7.1 ROADMs.

    Test topology:
        XPDR-A2 (oc200) <-> ROADM-A1 (7.1) <-> ROADM-C1 (7.1) <-> XPDR-C2 (oc200)

    This test validates:
    - Device connections and port mapping
    - XPDR-ROADM link creation
    - OMS attributes configuration
    - Service creation (OTU/400G OTUCn)
    - Service path verification
    - Service deletion
    """

    processes = None
    WAITING = 20
    NODE_VERSION_OC = 'oc200'
    NODE_VERSION_ROADM = '7.1'

    cr_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "request-400",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service-test-OTU-400",
        "common-id": "commonId",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "400",
            "node-id": "XPDR-A2",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTUCn",
            "clli": "NodeA",
            "tx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK9",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK9",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "400",
            "node-id": "XPDR-C2",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTUCn",
            "clli": "NodeC",
            "tx-direction": [{
                "port": {
                    "port-device-name": "XPDR-C2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK9",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-C2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK9",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "due-date": "2026-02-04T17:30:01Z",
        "operator-contact": "pw1234"
    }

    del_serv_input_data = {
        "sdnc-request-header": {
            "request-id": "request-400",
            "rpc-action": "service-delete",
            "request-system-id": "appname"
        },
        "service-delete-req-info": {
            "service-name": "service-test-OTU-400",
            "tail-retention": "yes"
        }
    }

    cr_serv_client_input_data = {
        "sdnc-request-header": {
            "request-id": "request-client-400",
            "rpc-action": "service-create",
            "request-system-id": "appname"
        },
        "service-name": "service-test-100-1",
        "common-id": "commonId-client",
        "connection-type": "infrastructure",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "XPDR-A2",
            "service-format": "Ethernet",
            "clli": "NodeA",
            "tx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-CLIENT1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-A2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-CLIENT1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "XPDR-C2",
            "service-format": "Ethernet",
            "clli": "NodeC",
            "tx-direction": [{
                "port": {
                    "port-device-name": "XPDR-C2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-CLIENT1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "rx-direction": [{
                "port": {
                    "port-device-name": "XPDR-C2-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-CLIENT1",
                    "port-rack": "000000.00",
                    "port-shelf": "Chassis#1"
                },
                "lgx": {
                    "lgx-device-name": "Some lgx-device-name",
                    "lgx-port-name": "Some lgx-port-name",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                },
                "index": 0
            }],
            "optic-type": "gray"
        },
        "due-date": "2026-02-04T17:30:01Z",
        "operator-contact": "pw1234"
    }

    del_serv_client_input_data = {
        "sdnc-request-header": {
            "request-id": "request-client-400",
            "rpc-action": "service-delete",
            "request-system-id": "appname"
        },
        "service-delete-req-info": {
            "service-name": "service-test-100-1",
            "tail-retention": "yes"
        }
    }

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        # Start 2 OC200 XPDR simulators and 2 OpenROADM 7.1 ROADM simulators
        cls.processes = test_utils.start_sims([
            ('mpdra', cls.NODE_VERSION_OC),
            ('mpdrc', cls.NODE_VERSION_OC),
            ('roadma', cls.NODE_VERSION_ROADM),
            ('roadmc', cls.NODE_VERSION_ROADM)
        ])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        test_utils_oc.del_metadata()
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")
        test_utils.copy_karaf_log(cls.__name__)

    def setUp(self):
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    # ==================== SETUP PHASE ====================

    def test_01_meta_data_insertion(self):
        response = test_utils_oc.metadata_input_oc200()
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_02_catalog_input_insertion(self):
        response = test_utils_oc.catalog_input_oc200()
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    # ==================== DEVICE CONNECTION ====================

    def test_03_connect_xpdrA2(self):
        response = test_utils.mount_device("XPDR-A2",
                                           ('mpdra', self.NODE_VERSION_OC))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_xpdrA2_check(self):
        response = test_utils.check_device_connection("XPDR-A2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_05_connect_xpdrC2(self):
        response = test_utils.mount_device("XPDR-C2",
                                           ('mpdrc', self.NODE_VERSION_OC))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_06_connect_xpdrC2_check(self):
        response = test_utils.check_device_connection("XPDR-C2")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_07_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1",
                                           ('roadma', self.NODE_VERSION_ROADM))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_08_connect_rdmA_check(self):
        response = test_utils.check_device_connection("ROADM-A1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    def test_09_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1",
                                           ('roadmc', self.NODE_VERSION_ROADM))
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_10_connect_rdmC_check(self):
        response = test_utils.check_device_connection("ROADM-C1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['connection-status'], 'connected')

    # ==================== PORT MAPPING VERIFICATION ====================

    def test_11_xpdrA2_portmapping_NETWORK9(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-NETWORK9")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {
                'logical-connection-point': 'XPDR1-NETWORK9',
                'port-qual': 'switch-network',
                'port-oper-state': 'ACTIVE',
                'rate': '400',
                'xpdr-type': 'mpdr',
                'openconfig-info': {
                    'supported-optical-channels': ['linecard-1-line-opt-1-1']
                },
                'supported-operational-mode': ['4308'],
                'supporting-circuit-pack-name': 'linecard-1-line-transceiver-1',
                'lcp-hash-val': 'AIGiVAQ4gDit',
                'supported-interface-capability': ['org-openroadm-port-types:if-OTUCn-ODUCn'],
                'port-direction': 'bidirectional',
                'port-admin-state': 'ENABLED',
                'supporting-port': 'linecard-1-line-port-1'
            },
            response['mapping'])

    def test_12_xpdrC2_portmapping_NETWORK9(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C2", "mapping", "XPDR1-NETWORK9")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn(
            {
                'logical-connection-point': 'XPDR1-NETWORK9',
                'port-qual': 'switch-network',
                'port-oper-state': 'ACTIVE',
                'rate': '400',
                'xpdr-type': 'mpdr',
                'openconfig-info': {
                    'supported-optical-channels': ['linecard-1-line-opt-1-1']
                },
                'supported-operational-mode': ['4308'],
                'supporting-circuit-pack-name': 'linecard-1-line-transceiver-1',
                'lcp-hash-val': 'ZbICgmaBrJs=',
                'supported-interface-capability': ['org-openroadm-port-types:if-OTUCn-ODUCn'],
                'port-direction': 'bidirectional',
                'port-admin-state': 'ENABLED',
                'supporting-port': 'linecard-1-line-port-1'
            },
            response['mapping'])

    def test_13_rdmA_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node-info']['node-type'], 'rdm')

    def test_14_rdmC_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-C1", "node-info", None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node-info']['node-type'], 'rdm')

    def test_15_xpdrA2_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        expected = {
            'logical-connection-point': 'XPDR1-CLIENT1',
            'port-qual': 'switch-client',
            'port-oper-state': 'INACTIVE',
            'rate': '100',
            'xpdr-type': 'mpdr',
            'supported-operational-mode': ['4308'],
            'supporting-circuit-pack-name': 'linecard-1-client-transceiver-1',
            'lcp-hash-val': 'AODABTVSOHH0',
            'supported-interface-capability': ['org-openroadm-port-types:if-100GE-ODU4'],
            'port-direction': 'bidirectional',
            'port-admin-state': 'DISABLED',
            'supporting-port': 'linecard-1-client-port-1'
        }
        expected_sorted = test_utils.recursive_sort(expected)
        response_sorted = [
            test_utils.recursive_sort(item) for item in response['mapping']
        ]
        self.assertIn(expected_sorted, response_sorted)

    def test_16_xpdrC2_portmapping_CLIENT1(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C2", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        expected = {
            'logical-connection-point': 'XPDR1-CLIENT1',
            'port-qual': 'switch-client',
            'port-oper-state': 'INACTIVE',
            'rate': '100',
            'xpdr-type': 'mpdr',
            'supported-operational-mode': ['4308'],
            'supporting-circuit-pack-name': 'linecard-1-client-transceiver-1',
            'lcp-hash-val': 'M5cViLS5z3o=',
            'supported-interface-capability': ['org-openroadm-port-types:if-100GE-ODU4'],
            'port-direction': 'bidirectional',
            'port-admin-state': 'DISABLED',
            'supporting-port': 'linecard-1-client-port-1'
        }
        expected_sorted = test_utils.recursive_sort(expected)
        response_sorted = [
            test_utils.recursive_sort(item) for item in response['mapping']
        ]
        self.assertIn(expected_sorted, response_sorted)

    def test_17_xpdrA2_switching_pool(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "switching-pool-lcp", "1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching-pool-lcp'][0]['switching-pool-type'])

    def test_18_xpdrC2_switching_pool(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C2", "switching-pool-lcp", "1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual("blocking",
                         response['switching-pool-lcp'][0]['switching-pool-type'])

    # ==================== TOPOLOGY LINK CREATION ====================

    def test_19_connect_xpdrA2_N9_to_rdmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '9',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_20_connect_rdmA_PP1_to_xpdrA2_N9(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A2', 'xpdr-num': '1', 'network-num': '9',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_21_connect_xpdrC2_N9_to_rdmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '9',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_22_connect_rdmC_PP1_to_xpdrC2_N9(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C2', 'xpdr-num': '1', 'network-num': '9',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_23_add_omsAttributes_ROADMA_ROADMC(self):
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

    def test_24_add_omsAttributes_ROADMC_ROADMA(self):
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

    # ==================== END-TO-END SERVICE CREATION ====================

    def test_25_create_OTU4_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_26_get_OTU4_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-test-OTU-400")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-test-OTU-400')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')

    def test_27_check_optical_channel_xpdrA2(self):
        response = test_utils_oc.check_node_attribute2_request(
            "XPDR-A2", "component", "linecard-1-line-opt-1-1",
            "openconfig-terminal-device:optical-channel")
        self.assertEqual(response['status_code'], requests.codes.ok)
        optchannel = response['openconfig-terminal-device:optical-channel']
        self.assertEqual(optchannel['config']['frequency'], '196081250')
        self.assertEqual(optchannel['config']['target-output-power'], '0.0')
        self.assertEqual(optchannel['config']['operational-mode'], 4308)

    def test_28_check_network_port_admin_state(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-NETWORK9")
        self.assertEqual(response['status_code'], requests.codes.ok)
        for port in response['mapping']:
            if port['logical-connection-point'] == 'XPDR1-NETWORK9':
                self.assertEqual(port['port-admin-state'], 'ENABLED')
                break

    def test_29_check_network_tx_laser(self):
        response = test_utils_oc.check_node_attribute2_request(
            "XPDR-A2", "component", "linecard-1-line-transceiver-1",
            "openconfig-platform-transceiver:transceiver")
        self.assertEqual(response['status_code'], requests.codes.ok)
        transceiver = response['openconfig-platform-transceiver:transceiver']
        self.assertIn('channel', transceiver['physical-channels'])
        self.assertIn('tx-laser', transceiver['physical-channels']['channel'][0]['config'])
        self.assertEqual(transceiver['physical-channels']['channel'][0]['config']['tx-laser'], True)

    def test_30_check_optical_channel_xpdrC2(self):
        response = test_utils_oc.check_node_attribute2_request(
            "XPDR-C2", "component", "linecard-1-line-opt-1-1",
            "openconfig-terminal-device:optical-channel")
        self.assertEqual(response['status_code'], requests.codes.ok)
        optchannel = response['openconfig-terminal-device:optical-channel']
        self.assertEqual(optchannel['config']['frequency'], '196081250')
        self.assertEqual(optchannel['config']['target-output-power'], '0.0')
        self.assertEqual(optchannel['config']['operational-mode'], 4308)

    def test_31_check_network_port_admin_state_xpdrC2(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C2", "mapping", "XPDR1-NETWORK9")
        self.assertEqual(response['status_code'], requests.codes.ok)
        for port in response['mapping']:
            if port['logical-connection-point'] == 'XPDR1-NETWORK9':
                self.assertEqual(port['port-admin-state'], 'ENABLED')
                break

    def test_32_create_client_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_client_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_33_get_client_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-test-100-1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service-test-100-1')
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')

    def test_34_check_client_port_admin_state(self):
        response = test_utils.get_portmapping_node_attr("XPDR-A2", "mapping", "XPDR1-CLIENT1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        for port in response['mapping']:
            if port['logical-connection-point'] == 'XPDR1-CLIENT1':
                self.assertEqual(port['port-admin-state'], 'ENABLED')
                break

    def test_35_check_client_tx_laser(self):
        response = test_utils_oc.check_node_attribute2_request(
            "XPDR-A2", "component", "linecard-1-client-transceiver-1",
            "openconfig-platform-transceiver:transceiver")
        self.assertEqual(response['status_code'], requests.codes.ok)
        transceiver = response['openconfig-platform-transceiver:transceiver']
        self.assertIn('channel', transceiver['physical-channels'])
        self.assertIn('tx-laser', transceiver['physical-channels']['channel'][0]['config'])
        self.assertEqual(transceiver['physical-channels']['channel'][0]['config']['tx-laser'], True)

    # ==================== SERVICE DELETION ====================

    def test_36_delete_client_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_client_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        response_message = response['output']['configuration-response-common']['response-message']
        self.assertIn('Renderer service delete in progress', response_message)
        time.sleep(self.WAITING)

    def test_37_check_no_client_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-test-100-1")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_38_delete_OTU4_service(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        response_message = response['output']['configuration-response-common']['response-message']
        self.assertIn('Renderer service delete in progress', response_message)
        time.sleep(self.WAITING)

    def test_39_check_no_service(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service-test-OTU-400")
        self.assertEqual(response['status_code'], requests.codes.conflict)

    # ==================== CLEANUP ====================

    def test_40_disconnect_xpdrA2(self):
        response = test_utils.unmount_device("XPDR-A2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_41_disconnect_xpdrC2(self):
        response = test_utils.unmount_device("XPDR-C2")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_42_disconnect_rdmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_43_disconnect_rdmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
