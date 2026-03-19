#!/usr/bin/env python
##############################################################################
# Copyright (c) 2026 Orange, Inc. and others.  All rights reserved.
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


class TestTransportPCEEndtoend4Nodes(unittest.TestCase):
    processes = None
    cr_serv_input_data = {
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
            "node-id": "OC-MPDRA",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeOC1",
            "tx-direction": [{
                "port": {
                    "port-device-name": "OC-MPDRA-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK5",
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
                    "port-device-name": "OC-MPDRA-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK5",
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
            "node-id": "OC-MPDRA",
            "service-format": "OTU",
            "otu-service-rate": "org-openroadm-otn-common-types:OTU4",
            "clli": "NodeOC1",
            "tx-direction": [{
                "port": {
                    "port-device-name": "OC-MPDRC-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK5",
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
                    "port-device-name": "OC-MPDRC-XPDR1",
                    "port-type": "fixed",
                    "port-name": "XPDR1-NETWORK5",
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

    WAITING = 20
    NODE_VERSION_71 = '7.1'
    NODE_VERSION_OC = 'oc'

    @classmethod
    def setUpClass(cls):
        cls.PATH_CREATE_CLIENT = None
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('oc-mpdra', cls.NODE_VERSION_OC),
                                               ('roadma', cls.NODE_VERSION_71),
                                               ('roadmc', cls.NODE_VERSION_71),
                                               ('oc-mpdrc', cls.NODE_VERSION_OC)])

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

    def test_01_meta_data_insertion(self):
        response = test_utils_oc.metadata_input()
        self.assertEqual(response.status_code, requests.codes.created,
                         test_utils.CODE_SHOULD_BE_201)

    def test_02_catlog_input_insertion(self):
        response = test_utils_oc.catlog_input()
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_03_connect_oc_mpdra(self):
        response = test_utils.mount_device("OC-MPDRA", ('oc-mpdra', self.NODE_VERSION_OC))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_oc_mpdrc(self):
        response = test_utils.mount_device("OC-MPDRC", ('oc-mpdrc', self.NODE_VERSION_OC))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_roadm_a(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_71))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_06_connect_roadm_c(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_71))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_07_connect_oc_mpdra_to_roadm_a(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'OC-MPDRA', 'xpdr-num': '1', 'network-num': '5',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadm_a_to_oc_mpdra(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'OC-MPDRA', 'xpdr-num': '1', 'network-num': '5',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_09_connect_oc_mpdrc_to_roadm_c(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'OC-MPDRC', 'xpdr-num': '1', 'network-num': '5',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_10_connect_roadm_c_to_oc_mpdrc(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'OC-MPDRC', 'xpdr-num': '1', 'network-num': '5',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_11_add_omsAttributes_roadm_a_to_roadm_c(self):
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

    def test_12_add_omsAttributes_roadm_c_to_roadm_a(self):
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

    def test_13_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 9)
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 22)

    def test_14_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 2)
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_15_check_optical_channel(self):
        response = test_utils_oc.check_node_attribute2_request(
            'OC-MPDRA', 'component', 'cfp2-opt-1-1', 'openconfig-terminal-device:optical-channel')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertDictEqual(dict({'config': {'frequency': '196100000', 'operational-mode': 4308,
                                              'target-output-power': '0.0'}},
                                  **response['openconfig-terminal-device:optical-channel']),
                             response['openconfig-terminal-device:optical-channel'])

    def test_16_service_create_och_otu4(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_17_get_och_otu4_service(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-OCH-OTU4")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-OCH-OTU4')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_18_service_create_openconfig_100G(self):
        self.cr_serv_input_data["service-name"] = "service1-OC-100G"
        self.cr_serv_input_data["service-a-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-z-end"]["service-format"] = "Ethernet"
        self.cr_serv_input_data["service-a-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-a-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["tx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        self.cr_serv_input_data["service-z-end"]["rx-direction"][0]["port"]["port-name"] = "XPDR1-CLIENT1"
        del self.cr_serv_input_data["service-a-end"]["otu-service-rate"]
        del self.cr_serv_input_data["service-z-end"]["otu-service-rate"]
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_19_get_openconfig_100G_service(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1-OC-100G")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(
            response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            response['services'][0]['service-name'], 'service1-OC-100G')
        self.assertEqual(
            response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            response['services'][0]['lifecycle-state'], 'planned')

    def test_20_delete_openconfig_100G_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-OC-100G"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_21_check_service_list(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['service-list']['services']), 1)

    def test_22_delete_openconfig_och_otu4_service(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1-OCH-OTU4"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_23_disconnect_oc_mpdra(self):
        response = test_utils.unmount_device("OC-MPDRA")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_24_disconnect_oc_mpdrc(self):
        response = test_utils.unmount_device("OC-MPDRC")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_25_disconnect_roadma(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_26_disconnect_roadmc(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == '__main__':
    unittest.main()
