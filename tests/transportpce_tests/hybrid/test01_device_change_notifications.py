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
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import sys
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8


class TransportPCEFulltesting(unittest.TestCase):

    processes = None
    cr_serv_input_data = {
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
            "tx-direction": [{"index": 0}],
            "rx-direction": [{"index": 0}],
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "XPDR-C1",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJT4",
            "tx-direction": [{"index": 0}],
            "rx-direction": [{"index": 0}],
            "optic-type": "gray"
        },
        "due-date": "2016-11-28T00:00:01Z",
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
        time.sleep(3)

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION_121))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION_71))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xpdrA_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDRA01', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDRA01', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

    def test_07_connect_xpdrC_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])

    def test_08_connect_roadmC_PP1_to_xpdrC_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])

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
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
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
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

# test service-create for Eth service from xpdr to xpdr
    def test_11_create_eth_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create',
            self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_12_get_eth_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request(
            "services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['service-name'], 'service1')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_13_change_status_line_port_xpdra(self):
        self.assertTrue(test_utils.sims_update_cp_port(('xpdra', self.NODE_VERSION_121), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "logical-connection-point": "XPDR1-NETWORK1",
            "port-type": "CFP2",
            "circuit-id": "XPDRA-NETWORK",
            "administrative-state": "outOfService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_14_check_update_portmapping(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
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
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if nodeId == 'XPDRA01-XPDR1' and tp['tp-id'] == 'XPDR1-NETWORK1':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = response['network'][0]['ietf-network-topology:link']
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
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['operational-state'], 'outOfService')
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        time.sleep(1)

    def test_17_restore_status_line_port_xpdra(self):
        self.assertTrue(test_utils.sims_update_cp_port(('xpdra', self.NODE_VERSION_121), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "logical-connection-point": "XPDR1-NETWORK1",
            "port-type": "CFP2",
            "circuit-id": "XPDRA-NETWORK",
            "administrative-state": "inService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_18_check_update_portmapping_ok(self):
        response = test_utils.get_portmapping_node_attr("XPDRA01", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_19_check_update_openroadm_topo_ok(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeMapId = node['node-id'].split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')

        link_list = response['network'][0]['ietf-network-topology:link']
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        time.sleep(1)

    def test_20_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_21_change_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "outOfService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_22_check_update_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
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
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if nodeId == 'ROADM-A1-SRG1' and tp['tp-id'] == 'SRG1-PP1-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = response['network'][0]['ietf-network-topology:link']
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
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "inService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_25_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_26_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_27_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_28_change_status_line_port_roadma_deg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '2/0', 'L1',
                                                       {
            "port-name": "L1",
            "logical-connection-point": "DEG2-TTP-TXRX",
            "port-type": "LINE",
            "circuit-id": "1",
            "administrative-state": "outOfService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_29_check_update_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
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
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if nodeId == 'ROADM-A1-DEG2' and tp['tp-id'] == 'DEG2-TTP-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = response['network'][0]['ietf-network-topology:link']
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
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '2/0', 'L1',
                                                       {
            "port-name": "L1",
            "logical-connection-point": "DEG2-TTP-TXRX",
            "port-type": "LINE",
            "circuit-id": "1",
            "administrative-state": "inService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_32_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_33_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_34_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_35_change_status_line_port_xpdrc(self):
        self.assertTrue(test_utils.sims_update_cp_port(('xpdrc', self.NODE_VERSION_71), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "port-type": "CFP2",
            "administrative-state": "outOfService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_36_check_update_portmapping(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
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
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if nodeId == 'XPDR-C1-XPDR1' and tp['tp-id'] == 'XPDR1-NETWORK1':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = response['network'][0]['ietf-network-topology:link']
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
        self.assertTrue(test_utils.sims_update_cp_port(('xpdrc', self.NODE_VERSION_71), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "port-type": "CFP2",
            "administrative-state": "inService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_39_check_update_portmapping_ok(self):
        self.test_18_check_update_portmapping_ok()

    def test_40_check_update_openroadm_topo_ok(self):
        self.test_19_check_update_openroadm_topo_ok()

    def test_41_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_42_change_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C2',
                                                       {
            "port-name": "C2",
            "logical-connection-point": "SRG1-PP2",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "outOfService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_43_check_update_portmapping(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
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
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if (nodeMapId == 'TAPI') :
                continue
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                if nodeId == 'ROADM-A1-SRG1' and tp['tp-id'] == 'SRG1-PP2-TXRX':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = response['network'][0]['ietf-network-topology:link']
        nb_updated_link = 0
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 0, "No link should have been modified")
        time.sleep(1)

    def test_45_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_46_delete_eth_service1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete',
            self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_47_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_48_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDRA01")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_49_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_50_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_51_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
