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
import json
import base64
import unittest
import time
import requests
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEFulltesting(unittest.TestCase):

    processes = None
    cr_serv_sample_data = {"input": {
        "sdnc-request-header": {
            "request-id": "e3028bae-a90f-4ddd-a83f-cf224eba0e58",
            "rpc-action": "service-create",
            "request-system-id": "appname",
            "notification-url": "http://localhost:8585/NotificationServer/notify"
        },
        "service-name": "service1",
        "common-id": "ASATT1234567",
        "connection-type": "service",
        "service-a-end": {
            "service-rate": "100",
            "node-id": "XPDRA01",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJP8",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "1/0/C1",
                            "port-type": "fixed",
                            "port-name": "1",
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
                            "port-device-name": "1/0/C1",
                            "port-type": "fixed",
                            "port-name": "1",
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
            "node-id": "XPDR-C1",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJT4",
                    "tx-direction": {
                        "port": {
                            "port-device-name": "1/0/C1",
                            "port-type": "fixed",
                            "port-name": "1",
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
                            "port-device-name": "1/0/C1",
                            "port-type": "fixed",
                            "port-name": "1",
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
        "due-date": "2016-11-28T00:00:01Z",
        "operator-contact": "pw1234"
    }
    }

    WAITING = 25  # nominal value is 300
    NODE_VERSION_121 = '1.2.1'
    NODE_VERSION_221 = '2.2.1'
    NODE_VERSION_71 = '7.1'

    @classmethod
    def setUpClass(cls):
        cls.processes = test_utils.start_tpce()
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION_121),
                                               ('roadma', cls.NODE_VERSION_221),
                                               ('roadmc', cls.NODE_VERSION_221),
                                               ('xpdrc', cls.NODE_VERSION_71)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")
        time.sleep(10)

    def setUp(self):  # instruction executed before each test method
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION_121))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION_71))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xprdA_N1_to_roadmA_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDRA01", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDRA01", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_07_connect_xprdC_N1_to_roadmC_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_xpdrC_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-C1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"])
        time.sleep(2)

    def test_09_add_omsAttributes_ROADMA_ROADMC(self):
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

    def test_10_add_omsAttributes_ROADMC_ROADMA(self):
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

# test service-create for Eth service from xpdr to xpdr
    def test_11_create_eth_service1(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_12_get_eth_service1(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_13_change_status_line_port_xpdra(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
        body = {"ports": [{
                    "port-name": "1",
                    "logical-connection-point": "XPDR1-NETWORK1",
                    "port-type": "CFP2",
                    "circuit-id": "XPDRA-NETWORK",
                    "administrative-state": "outOfService",
                    "port-qual": "xpdr-network"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8130/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_14_check_update_portmapping(self):
        response = test_utils.portmapping_request("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            if mapping['logical-connection-point'] == 'XPDR1-NETWORK1':
                self.assertEqual(mapping['port-oper-state'], 'OutOfService',
                                 "Operational State should be 'OutOfService'")
                self.assertEqual(mapping['port-admin-state'], 'OutOfService',
                                 "Administrative State should be 'OutOfService'")
            else:
                self.assertEqual(mapping['port-oper-state'], 'InService',
                                 "Operational State should be 'InService'")
                self.assertEqual(mapping['port-admin-state'], 'InService',
                                 "Administrative State should be 'InService'")
        time.sleep(1)

    def test_15_check_update_openroadm_topo(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if node['node-id'] == 'XPDRA01-XPDR1' and tp['tp-id'] == 'XPDR1-NETWORK1':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
        updated_links = ['XPDRA01-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX',
                         'ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDRA01-XPDR1-XPDR1-NETWORK1']
        nb_updated_link = 0
        for link in link_list:
            if link['link-id'] in updated_links:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'outOfService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'outOfService')
                nb_updated_link += 1
            else:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 2, "Only two xponder-output/input links should have been modified")
        time.sleep(1)

    def test_16_check_update_service1(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['services'][0]['operational-state'], 'outOfService')
        self.assertEqual(res['services'][0]['administrative-state'], 'outOfService')
        time.sleep(1)

    def test_17_restore_status_line_port_xpdra(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
        body = {"ports": [{
                    "port-name": "1",
                    "logical-connection-point": "XPDR1-NETWORK1",
                    "port-type": "CFP2",
                    "circuit-id": "XPDRA-NETWORK",
                    "administrative-state": "inService",
                    "port-qual": "xpdr-network"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8130/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_18_check_update_portmapping_ok(self):
        response = test_utils.portmapping_request("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_19_check_update_openroadm_topo_ok(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')

        link_list = res['network'][0]['ietf-network-topology:link']
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        time.sleep(1)

    def test_20_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_21_change_status_port_roadma_srg(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C1"
        body = {"ports": [{
                    "port-name": "C1",
                    "logical-connection-point": "SRG1-PP1",
                    "port-type": "client",
                    "circuit-id": "SRG1",
                    "administrative-state": "outOfService",
                    "port-qual": "roadm-external"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_22_check_update_portmapping(self):
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            if mapping['logical-connection-point'] == 'SRG1-PP1-TXRX':
                self.assertEqual(mapping['port-oper-state'], 'OutOfService',
                                 "Operational State should be 'OutOfService'")
                self.assertEqual(mapping['port-admin-state'], 'OutOfService',
                                 "Administrative State should be 'OutOfService'")
            else:
                self.assertEqual(mapping['port-oper-state'], 'InService',
                                 "Operational State should be 'InService'")
                self.assertEqual(mapping['port-admin-state'], 'InService',
                                 "Administrative State should be 'InService'")
        time.sleep(1)

    def test_23_check_update_openroadm_topo(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if node['node-id'] == 'ROADM-A1-SRG1' and tp['tp-id'] == 'SRG1-PP1-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
        updated_links = ['XPDRA01-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX',
                         'ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDRA01-XPDR1-XPDR1-NETWORK1']
        nb_updated_link = 0
        for link in link_list:
            if link['link-id'] in updated_links:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'outOfService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'outOfService')
                nb_updated_link += 1
            else:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 2, "Only two xponder-output/input links should have been modified")
        time.sleep(1)

    def test_24_restore_status_port_roadma_srg(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C1"
        body = {"ports": [{
                    "port-name": "C1",
                    "logical-connection-point": "SRG1-PP1",
                    "port-type": "client",
                    "circuit-id": "SRG1",
                    "administrative-state": "inService",
                    "port-qual": "roadm-external"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_25_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_26_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_27_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_28_change_status_line_port_roadma_deg(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/2%2F0/ports/L1"
        body = {"ports": [{
                    "port-name": "L1",
                    "logical-connection-point": "DEG2-TTP-TXRX",
                    "port-type": "LINE",
                    "circuit-id": "1",
                    "administrative-state": "outOfService",
                    "port-qual": "roadm-external"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_29_check_update_portmapping(self):
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            if mapping['logical-connection-point'] == 'DEG2-TTP-TXRX':
                self.assertEqual(mapping['port-oper-state'], 'OutOfService',
                                 "Operational State should be 'OutOfService'")
                self.assertEqual(mapping['port-admin-state'], 'OutOfService',
                                 "Administrative State should be 'OutOfService'")
            else:
                self.assertEqual(mapping['port-oper-state'], 'InService',
                                 "Operational State should be 'InService'")
                self.assertEqual(mapping['port-admin-state'], 'InService',
                                 "Administrative State should be 'InService'")
        time.sleep(1)

    def test_30_check_update_openroadm_topo(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if node['node-id'] == 'ROADM-A1-DEG2' and tp['tp-id'] == 'DEG2-TTP-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
        updated_links = ['ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX',
                         'ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX']
        nb_updated_link = 0
        for link in link_list:
            if link['link-id'] in updated_links:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'outOfService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'outOfService')
                nb_updated_link += 1
            else:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 2, "Only two xponder-output/input links should have been modified")
        time.sleep(1)

    def test_31_restore_status_line_port_roadma_srg(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/2%2F0/ports/L1"
        body = {"ports": [{
                    "port-name": "L1",
                    "logical-connection-point": "DEG2-TTP-TXRX",
                    "port-type": "LINE",
                    "circuit-id": "1",
                    "administrative-state": "inService",
                    "port-qual": "roadm-external"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_32_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_33_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_34_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_35_change_status_line_port_xpdrc(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
        body = {"ports": [{
                    "port-name": "1",
                    "port-type": "CFP2",
                    "administrative-state": "outOfService",
                    "port-qual": "xpdr-network"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8154/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_36_check_update_portmapping(self):
        response = test_utils.portmapping_request("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            if mapping['logical-connection-point'] == 'XPDR1-NETWORK1':
                self.assertEqual(mapping['port-oper-state'], 'OutOfService',
                                 "Operational State should be 'OutOfService'")
                self.assertEqual(mapping['port-admin-state'], 'OutOfService',
                                 "Administrative State should be 'OutOfService'")
            else:
                self.assertEqual(mapping['port-oper-state'], 'InService',
                                 "Operational State should be 'InService'")
                self.assertEqual(mapping['port-admin-state'], 'InService',
                                 "Administrative State should be 'InService'")
        time.sleep(1)

    def test_37_check_update_openroadm_topo(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if node['node-id'] == 'XPDR-C1-XPDR1' and tp['tp-id'] == 'XPDR1-NETWORK1':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
        updated_links = ['XPDR-C1-XPDR1-XPDR1-NETWORK1toROADM-C1-SRG1-SRG1-PP1-TXRX',
                         'ROADM-C1-SRG1-SRG1-PP1-TXRXtoXPDR-C1-XPDR1-XPDR1-NETWORK1']
        nb_updated_link = 0
        for link in link_list:
            if link['link-id'] in updated_links:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'outOfService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'outOfService')
                nb_updated_link += 1
            else:
                self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 2, "Only two xponder-output/input links should have been modified")
        time.sleep(1)

    def test_38_restore_status_line_port_xpdrc(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
        body = {"ports": [{
                    "port-name": "1",
                    "port-type": "CFP2",
                    "administrative-state": "inService",
                    "port-qual": "xpdr-network"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8154/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_39_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_40_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_41_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_42_change_status_port_roadma_srg(self):
        url = "{}/data/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C2"
        body = {"ports": [{
                    "port-name": "C2",
                    "logical-connection-point": "SRG1-PP2",
                    "port-type": "client",
                    "circuit-id": "SRG1",
                    "administrative-state": "outOfService",
                    "port-qual": "roadm-external"}]}
        response = requests.request("PUT", url.format("http://127.0.0.1:8141/restconf"),
                                    data=json.dumps(body), headers=test_utils.TYPE_APPLICATION_JSON,
                                    auth=(test_utils.ODL_LOGIN, test_utils.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(2)

    def test_43_check_update_portmapping(self):
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            if mapping['logical-connection-point'] == 'SRG1-PP2-TXRX':
                self.assertEqual(mapping['port-oper-state'], 'OutOfService',
                                 "Operational State should be 'OutOfService'")
                self.assertEqual(mapping['port-admin-state'], 'OutOfService',
                                 "Administrative State should be 'OutOfService'")
            else:
                self.assertEqual(mapping['port-oper-state'], 'InService',
                                 "Operational State should be 'InService'")
                self.assertEqual(mapping['port-admin-state'], 'InService',
                                 "Administrative State should be 'InService'")
        time.sleep(1)

    def test_44_check_update_openroadm_topo(self):
        url = test_utils.URL_CONFIG_ORDM_TOPO
        response = test_utils.get_request(url)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        node_list = res['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if node['node-id'] == 'ROADM-A1-SRG1' and tp['tp-id'] == 'SRG1-PP2-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
        nb_updated_link = 0
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 0, "No link should have been modified")
        time.sleep(1)

    def test_45_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_46_delete_eth_service1(self):
        response = test_utils.service_delete_request("service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_47_disconnect_xponders_from_roadm(self):
        url = "{}/data/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
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

    def test_48_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_49_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_50_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_51_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
