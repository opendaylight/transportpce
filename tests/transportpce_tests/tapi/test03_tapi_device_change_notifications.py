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
import os
import unittest
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
        self.eth = None
# XPDR-A1-XPDR1+XPONDER Uuid is:
# 4378fc29-6408-39ec-8737-5008c3dc49e5
# XPDR-C1-XPDR1+XPONDER Uuid is:
# 1770bea4-b1da-3b20-abce-7d182c0ec0df
# ROADM-A1+PHOTONIC_MEDIA Uuid is:
# 3b726367-6f2d-3e3f-9033-d99b61459075


class TransportPCEFulltesting(unittest.TestCase):

    cr_serv_input_data = {
        "end-point": [
            {
                "layer-protocol-name": "DSR",
                "service-interface-point": {
                    "service-interface-point-uuid": "b1f4bd3b-7fa9-367b-a8ab-6e80293238df"
                },
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "XPDR-C1-XPDR1",
                "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "XPDR-C1-XPDR1"
                        }
                ]
            },
            {
                "layer-protocol-name": "DSR",
                "service-interface-point": {
                    "service-interface-point-uuid": "b5964ce9-274c-3f68-b4d1-83c0b61bc74e"
                },
                "administrative-state": "UNLOCKED",
                "operational-state": "ENABLED",
                "direction": "BIDIRECTIONAL",
                "role": "SYMMETRIC",
                "protection-role": "WORK",
                "local-id": "XPDR-A1-XPDR1",
                "name": [
                        {
                            "value-name": "OpenROADM node id",
                            "value": "XPDR-A1-XPDR1"
                        }
                ]
            }
        ],
        "connectivity-constraint": {
            #            "service-layer": "ETH",
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
        "layer-protocol-name": "ETH"
    }

    del_serv_input_data = {"uuid": "TBD"}

    tapi_topo = {"topology-id": "TBD"}

    node_details = {
        "topology-id": "TBD",
        "node-id": "TBD"
    }

    tapi_serv_details = {"uuid": "TBD"}

    processes = []
    uuid_services = UuidServices()
    WAITING = 25  # nominal value is 300
    NODE_VERSION_221 = '2.2.1'

    @classmethod
    def setUpClass(cls):
        # pylint: disable=unsubscriptable-object
        cls.init_failed = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
        cls.processes = test_utils.start_tpce()
        # TAPI feature is not installed by default in Karaf
        if "NO_ODL_STARTUP" not in os.environ or "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
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
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION_221),
                                               ('roadma', cls.NODE_VERSION_221),
                                               ('roadmc', cls.NODE_VERSION_221),
                                               ('xpdrc', cls.NODE_VERSION_221)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")
        result = test_utils.uninstall_karaf_feature("odl-transportpce-tapi")
        if result.returncode != 0:
            print("tapi desinstallation feature failed...")
        else:
            print("Tapi Feature uninstalled")

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))
        time.sleep(1)

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_xpdrA_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_07_connect_xpdrC_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_xpdrC_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
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
    def test_11_create_connectivity_service_Ethernet(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.uuid_services.eth = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        # 'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        # 'service-layer': 'ETH',
                        'layer-protocol-name': 'ETH',
                        # 'connectivity-direction': 'BIDIRECTIONAL'
                        'direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'XPDR-C1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'XPDR-A1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

    def test_12_get_service_Ethernet(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services.eth))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.eth))
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_13_get_connectivity_service_Ethernet(self):
        self.tapi_serv_details["uuid"] = str(self.uuid_services.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-details', self.tapi_serv_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['service']['operational-state'], 'ENABLED')
        self.assertEqual(response['output']['service']['name'][0]['value'], self.uuid_services.eth)
        self.assertEqual(response['output']['service']['administrative-state'], 'UNLOCKED')
        self.assertEqual(response['output']['service']['lifecycle-state'], 'INSTALLED')

    def test_14_change_status_line_port_xpdrc(self):
        self.assertTrue(test_utils.sims_update_cp_port(('xpdrc', self.NODE_VERSION_221), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "port-type": "CFP2",
            "administrative-state": "outOfService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_15_check_update_portmapping(self):
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

    def test_16_check_update_openroadm_topo(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
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
        time.sleep(10)

    def test_17_check_update_tapi_neps(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = "XPDR-C1-XPDR1+OTSi"
        self.node_details["node-id"] = "1770bea4-b1da-3b20-abce-7d182c0ec0df"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        nb_updated_neps = 0
        for nep in nep_list:
            if 'XPDR1-NETWORK1' in nep['name'][0]['value']:
                self.assertEqual(nep['operational-state'], 'DISABLED',
                                 "Operational State should be 'DISABLED'")
                self.assertEqual(nep['administrative-state'], 'LOCKED',
                                 "Administrative State should be 'LOCKED'")
                nb_updated_neps += 1
            else:
                self.assertEqual(nep['operational-state'], 'ENABLED',
                                 "Operational State should be 'ENABLED'")
                self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                                 "Administrative State should be 'UNLOCKED'")
        self.node_details["node-id"] = "XPDR-C1-XPDR1+DSR"
        self.assertEqual(nb_updated_neps, 4, "4 xponder neps (OTS, OTSI_MC, iOTU, eODU) should have been modified")
        time.sleep(1)

    def test_18_check_update_tapi_links(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        link_list = response['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['XPDR-C1-XPDR1', 'XPDR1-NETWORK1']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 1,
                         "Only one xponder-output/input bidirectional link should have been modified")
        time.sleep(1)

    def test_19_check_update_service_Ethernet(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services.eth))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['operational-state'], 'outOfService')
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')

    def test_20_check_update_connectivity_service_Ethernet(self):
        self.tapi_serv_details["uuid"] = str(self.uuid_services.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-details', self.tapi_serv_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['service']['operational-state'], 'DISABLED')
        self.assertEqual(response['output']['service']['administrative-state'], 'LOCKED')
        time.sleep(1)

    def test_21_restore_status_line_port_xpdrc(self):
        self.assertTrue(test_utils.sims_update_cp_port(('xpdrc', self.NODE_VERSION_221), '1/0/1-PLUG-NET', '1',
                                                       {
            "port-name": "1",
            "port-type": "CFP2",
            "administrative-state": "inService",
            "port-qual": "xpdr-network"
        }))
        time.sleep(2)

    def test_22_check_update_portmapping_ok(self):
        response = test_utils.get_portmapping_node_attr("XPDR-C1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_23_check_update_openroadm_topo_ok(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        for node in node_list:
            nodeMapId = node['node-id'].split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
            tp_list = node['ietf-network-topology:termination-point']
            for tp in tp_list:
                self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')

        link_list = response['network'][0]['ietf-network-topology:link']
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        time.sleep(1)

    def test_24_check_update_tapi_neps_ok(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = "XPDR-C1-XPDR1+OTSi"
        self.node_details["node-id"] = "1770bea4-b1da-3b20-abce-7d182c0ec0df"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")
        time.sleep(1)

    def test_25_check_update_tapi_links_ok(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        link_list = response['output']['topology']['link']
        for link in link_list:
            self.assertEqual(link['operational-state'], 'ENABLED')
            self.assertEqual(link['administrative-state'], 'UNLOCKED')
        time.sleep(1)

    def test_26_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_27_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_28_change_status_port_roadma_srg(self):
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

    def test_29_check_update_portmapping(self):
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

    def test_30_check_update_openroadm_topo(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            nodeId = node['node-id']
            nodeMapId = nodeId.split("-")[0]
            if nodeMapId == 'TAPI':
                continue
            self.assertEqual(node['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(node['org-openroadm-common-network:administrative-state'], 'inService')
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
        updated_links = ['XPDR-A1-XPDR1-XPDR1-NETWORK1toROADM-A1-SRG1-SRG1-PP1-TXRX',
                         'ROADM-A1-SRG1-SRG1-PP1-TXRXtoXPDR-A1-XPDR1-XPDR1-NETWORK1']
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

    def test_31_check_update_tapi_neps(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = "ROADM-A1+PHOTONIC_MEDIA"
        self.node_details["node-id"] = "3b726367-6f2d-3e3f-9033-d99b61459075"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        nb_updated_neps = 0
        for nep in nep_list:
            if 'SRG1-PP1-TXRX' in nep['name'][0]['value']:
                self.assertEqual(nep['operational-state'], 'DISABLED',
                                 "Operational State should be 'DISABLED'")
                self.assertEqual(nep['administrative-state'], 'LOCKED',
                                 "Administrative State should be 'LOCKED'")
                nb_updated_neps += 1
            else:
                self.assertEqual(nep['operational-state'], 'ENABLED',
                                 "Operational State should be 'ENABLED'")
                self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                                 "Administrative State should be 'UNLOCKED'")
        self.assertEqual(nb_updated_neps, 2, "Only 2 roadm SRG-PP nep (OTS/MC)should have been modified")
        time.sleep(1)

    def test_32_check_update_tapi_links(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        link_list = response['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['ROADM-A1', 'SRG1-PP1-TXRX']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 1,
                         "Only one xponder-output/input link should have been modified")
        time.sleep(1)

    def test_33_check_update_service_Ethernet(self):
        self.test_19_check_update_service_Ethernet()

    def test_34_check_update_connectivity_service_Ethernet(self):
        self.test_20_check_update_connectivity_service_Ethernet()

    def test_35_restore_status_port_roadma_srg(self):
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

    def test_36_check_update_portmapping_ok(self):
        response = test_utils.get_portmapping_node_attr("ROADM-A1", None, None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        mapping_list = response['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_37_check_update_openroadm_topo_ok(self):
        self.test_23_check_update_openroadm_topo_ok()

    def test_38_check_update_tapi_neps_ok(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = "ROADM-A1+PHOTONIC_MEDIA"
        self.node_details["node-id"] = "3b726367-6f2d-3e3f-9033-d99b61459075"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")

        time.sleep(1)

    def test_39_check_update_tapi_links_ok(self):
        self.test_25_check_update_tapi_links_ok()

    def test_40_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_41_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_42_change_status_line_port_roadma_deg(self):
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

    def test_43_check_update_portmapping(self):
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

    def test_44_check_update_openroadm_topo(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            nodeMapId = node['node-id'].split("-")[0]
            if nodeMapId == 'TAPI':
                continue
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

    def test_45_check_update_tapi_neps(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = "ROADM-A1+PHOTONIC_MEDIA"
        self.node_details["node-id"] = "3b726367-6f2d-3e3f-9033-d99b61459075"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        nb_updated_neps = 0
        for nep in nep_list:
            if 'DEG2-TTP-TXRX' in nep['name'][0]['value']:
                self.assertEqual(nep['operational-state'], 'DISABLED',
                                 "Operational State should be 'DISABLED'")
                self.assertEqual(nep['administrative-state'], 'LOCKED',
                                 "Administrative State should be 'LOCKED'")
                nb_updated_neps += 1
            else:
                self.assertEqual(nep['operational-state'], 'ENABLED',
                                 "Operational State should be 'ENABLED'")
                self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                                 "Administrative State should be 'UNLOCKED'")
        self.assertEqual(nb_updated_neps, 4, "4 roadm NEPS should have been modified (OTS/OMS/MC/OTSI_MC")
        time.sleep(1)

    def test_46_check_update_tapi_links(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        link_list = response['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['ROADM-A1', 'DEG2-TTP-TXRX']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 1,
                         "Only one rdm-rdm link should have been modified")
        time.sleep(1)

    def test_47_check_update_service_Ethernet(self):
        self.test_19_check_update_service_Ethernet()

    def test_48_check_update_connectivity_service_Ethernet(self):
        self.test_20_check_update_connectivity_service_Ethernet()

    def test_49_restore_status_line_port_roadma_deg(self):
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

    def test_50_check_update_portmapping_ok(self):
        self.test_36_check_update_portmapping_ok()

    def test_51_check_update_openroadm_topo_ok(self):
        self.test_23_check_update_openroadm_topo_ok()

    def test_52_check_update_tapi_neps_ok(self):
        self.test_38_check_update_tapi_neps_ok()

    def test_53_check_update_tapi_links_ok(self):
        self.test_25_check_update_tapi_links_ok()

    def test_54_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_55_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_56_change_status_port_roadma_srg(self):
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

    def test_57_check_update_portmapping(self):
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

    def test_58_check_update_openroadm_topo(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        node_list = response['network'][0]['node']
        nb_updated_tp = 0
        for node in node_list:
            nodeMapId = node['node-id'].split("-")[0]
            if nodeMapId == 'TAPI':
                continue
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

        link_list = response['network'][0]['ietf-network-topology:link']
        nb_updated_link = 0
        for link in link_list:
            self.assertEqual(link['org-openroadm-common-network:operational-state'], 'inService')
            self.assertEqual(link['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_link, 0, "No link should have been modified")
        time.sleep(1)

    def test_59_check_update_tapi_neps(self):
        self.node_details["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
#        self.node_details["node-id"] = uuid.UUID(bytes="ROADM-A1+PHOTONIC_MEDIA".bytes)
        self.node_details["node-id"] = "3b726367-6f2d-3e3f-9033-d99b61459075"
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-node-details', self.node_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        nep_list = response['output']['node']['owned-node-edge-point']
        nb_updated_neps = 0
        for nep in nep_list:
            if 'SRG1-PP2-TXRX' in nep['name'][0]['value']:
                self.assertEqual(nep['operational-state'], 'DISABLED',
                                 "Operational State should be 'DISABLED'")
                self.assertEqual(nep['administrative-state'], 'LOCKED',
                                 "Administrative State should be 'LOCKED'")
                nb_updated_neps += 1
            else:
                self.assertEqual(nep['operational-state'], 'ENABLED',
                                 "Operational State should be 'ENABLED'")
                self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                                 "Administrative State should be 'UNLOCKED'")
        self.assertEqual(nb_updated_neps, 1, "Only 1 roadm neps OTS should have been modified for SRG2PP")
        time.sleep(1)

    def test_60_check_update_tapi_links(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        link_list = response['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['ROADM-A1', 'SRG1-PP2-TXRX']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 0,
                         "No link should have been modified")
        time.sleep(1)

    def test_61_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_62_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_63_delete_connectivity_service_Ethernet(self):
        self.del_serv_input_data["uuid"] = str(self.uuid_services.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(self.WAITING)

    def test_64_disconnect_xponders_from_roadm(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                response = test_utils.del_ietf_network_link_request(
                    'openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_65_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_66_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_67_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_68_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_69_restore_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C2',
                                                       {
            "port-name": "C2",
            "logical-connection-point": "SRG1-PP2",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "inService",
            "port-qual": "roadm-external"
        }))
        time.sleep(2)

    def test_70_clean_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        links = response['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT', 'ROADM-TO-ROADM'):
                response = test_utils.del_ietf_network_link_request('openroadm-topology', link['link-id'], 'config')
                self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
