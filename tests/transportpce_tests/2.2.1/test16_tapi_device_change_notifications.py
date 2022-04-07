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

# pylint: disable=too-few-public-methods


class UuidServices:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None
        self.eth = None


class TransportPCEFulltesting(unittest.TestCase):

    cr_serv_sample_data = {
        "input": {
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
                "service-layer": "ETH",
                "service-type": "POINT_TO_POINT_CONNECTIVITY",
                "service-level": "Some service-level",
                "requested-capacity": {
                    "total-size": {
                        "value": "100",
                        "unit": "GB"
                    }
                }
            },
            "state": "Some state"
        }
    }

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
        cls.processes = test_utils.start_tpce()
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
        time.sleep(10)

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDRA01", ('xpdra', self.NODE_VERSION_221))
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

    def test_05_connect_xprdA_N1_to_roadmA_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("XPDR-A1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("XPDR-A1", "1", "1",
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
    def test_11_create_connectivity_service_Ethernet(self):
        response = test_utils.tapi_create_connectivity_request(self.cr_serv_sample_data)
        time.sleep(self.WAITING)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.uuid_services.eth = res['output']['service']['uuid']
        # pylint: disable=consider-using-f-string
        print("ethernet service uuid : {}".format(self.uuid_services.eth))

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
                        'service-type': 'POINT_TO_POINT_CONNECTIVITY',
                        'service-layer': 'ETH',
                        'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'XPDR-C1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'XPDR-A1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **res['output']['service']),
                             res['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **res['output']['service']['end-point'][0]['name'][0]),
                             res['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **res['output']['service']['end-point'][1]['name'][0]),
                             res['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_12_get_service_Ethernet(self):
        response = test_utils.get_service_list_request(
            "services/" + str(self.uuid_services.eth))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], self.uuid_services.pm)
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_13_get_connectivity_service_Ethernet(self):
        response = test_utils.tapi_get_connectivity_request(str(self.uuid_services.eth))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['service']['operational-state'], 'ENABLED')
        self.assertEqual(
            res['service']['name'][0]['value'], self.uuid_services.pm)
        self.assertEqual(
            res['service']['administrative-state'], 'UNLOCKED')
        self.assertEqual(
            res['service']['lifecycle-state'], 'INSTALLED')
        time.sleep(2)

    def test_14_change_status_line_port_xpdra(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
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

    def test_15_check_update_portmapping(self):
        response = test_utils.portmapping_request("XPDR-A1")
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

    def test_16_check_update_openroadm_topo(self):
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
                if node['node-id'] == 'XPDR-A1-XPDR1' and tp['tp-id'] == 'XPDR1-NETWORK1':
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'outOfService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'outOfService')
                    nb_updated_tp += 1
                else:
                    self.assertEqual(tp['org-openroadm-common-network:operational-state'], 'inService')
                    self.assertEqual(tp['org-openroadm-common-network:administrative-state'], 'inService')
        self.assertEqual(nb_updated_tp, 1, "Only one termination-point should have been modified")

        link_list = res['network'][0]['ietf-network-topology:link']
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

    def test_17_check_update_tapi_neps(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-A1-XPDR1+OTSi")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-A1-XPDR1+DSR")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        self.assertEqual(nb_updated_neps, 4, "Only two xponder neps should have been modified")
        time.sleep(1)

    def test_18_check_update_tapi_links(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['XPDR-A1-XPDR1', 'XPDR1-NETWORK1']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 2,
                         "Only two xponder-output/input & xponder-transi links should have been modified")
        time.sleep(1)

    def test_19_check_update_service_Ethernet(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['services'][0]['operational-state'], 'outOfService')
        self.assertEqual(res['services'][0]['administrative-state'], 'outOfService')
        time.sleep(1)

    def test_20_check_update_connectivity_service_Ethernet(self):
        response = test_utils.tapi_get_connectivity_request(str(self.uuid_services.eth))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['service']['operational-state'], 'DISABLED')
        self.assertEqual(
            res['service']['administrative-state'], 'LOCKED')
        time.sleep(1)

    def test_21_restore_status_line_port_xpdra(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
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

    def test_22_check_update_portmapping_ok(self):
        response = test_utils.portmapping_request("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_23_check_update_openroadm_topo_ok(self):
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

    def test_24_check_update_tapi_neps_ok(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-A1-XPDR1+OTSi")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")

        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-A1-XPDR1+DSR")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")
        time.sleep(1)

    def test_25_check_update_tapi_links_ok(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
        for link in link_list:
            self.assertEqual(link['operational-state'], 'ENABLED')
            self.assertEqual(link['administrative-state'], 'UNLOCKED')

        time.sleep(1)

    def test_26_check_update_service1_ok(self):
        self.test_12_get_eth_service1()

    def test_26_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_27_change_status_port_roadma_srg(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C1"
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

    def test_28_check_update_portmapping(self):
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

    def test_29_check_update_openroadm_topo(self):
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

    def test_30_check_update_tapi_neps(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "ROADM-A1+PHOTONIC_MEDIA")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        self.assertEqual(nb_updated_neps, 3, "Only three roadm neps should have been modified")
        time.sleep(1)

    def test_31_check_update_tapi_links(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
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

    def test_32_restore_status_port_roadma_srg(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C1"
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

    def test_33_check_update_portmapping_ok(self):
        response = test_utils.portmapping_request("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_34_check_update_openroadm_topo_ok(self):
        self.test_23_check_update_openroadm_topo_ok()

    def test_35_check_update_tapi_neps_ok(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "ROADM-A1+PHOTONIC_MEDIA")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")

        time.sleep(1)

    def test_36_check_update_tapi_links_ok(self):
        self.test_25_check_update_tapi_links_ok()

    def test_37_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_38_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_39_change_status_line_port_roadma_deg(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/2%2F0/ports/L1"
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

    def test_40_check_update_portmapping(self):
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

    def test_41_check_update_openroadm_topo(self):
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

    def test_42_check_update_tapi_neps(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "ROADM-A1+PHOTONIC_MEDIA")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        self.assertEqual(nb_updated_neps, 3, "Only three roadm neps should have been modified")
        time.sleep(1)

    def test_43_check_update_tapi_links(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
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

    def test_44_restore_status_line_port_roadma_deg(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/2%2F0/ports/L1"
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

    def test_45_check_update_portmapping_ok(self):
        self.test_33_check_update_portmapping_ok()

    def test_46_check_update_openroadm_topo_ok(self):
        self.test_23_check_update_openroadm_topo_ok()

    def test_47_check_update_tapi_neps_ok(self):
        self.test_35_check_update_tapi_neps_ok()

    def test_48_check_update_tapi_links_ok(self):
        self.test_25_check_update_tapi_links_ok()

    def test_49_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_50_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_51_change_status_line_port_xpdrc(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
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

    def test_52_check_update_portmapping(self):
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

    def test_53_check_update_openroadm_topo(self):
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

    def test_54_check_update_tapi_neps(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-C1-XPDR1+OTSi")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-C1-XPDR1+DSR")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        self.assertEqual(nb_updated_neps, 4, "Only two xponder neps should have been modified")
        time.sleep(1)

    def test_55_check_update_tapi_links(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
        nb_updated_link = 0
        for link in link_list:
            if all(x in link['name'][0]['value'] for x in ['XPDR-C1-XPDR1', 'XPDR1-NETWORK1']):
                self.assertEqual(link['operational-state'], 'DISABLED')
                self.assertEqual(link['administrative-state'], 'LOCKED')
                nb_updated_link += 1
            else:
                self.assertEqual(link['operational-state'], 'ENABLED')
                self.assertEqual(link['administrative-state'], 'UNLOCKED')
        self.assertEqual(nb_updated_link, 2,
                         "Only two xponder-output/input & xponder-transi links should have been modified")
        time.sleep(1)

    def test_56_restore_status_line_port_xpdrc(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/1%2F0%2F1-PLUG-NET/ports/1"
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

    def test_57_check_update_portmapping_ok(self):
        response = test_utils.portmapping_request("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        mapping_list = res['nodes'][0]['mapping']
        for mapping in mapping_list:
            self.assertEqual(mapping['port-oper-state'], 'InService',
                             "Operational State should be 'InService'")
            self.assertEqual(mapping['port-admin-state'], 'InService',
                             "Administrative State should be 'InService'")
        time.sleep(1)

    def test_58_check_update_openroadm_topo_ok(self):
        self.test_23_check_update_openroadm_topo_ok()

    def test_59_check_update_tapi_neps_ok(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-C1-XPDR1+OTSi")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")

        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "XPDR-C1-XPDR1+DSR")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
        for nep in nep_list:
            self.assertEqual(nep['operational-state'], 'ENABLED',
                             "Operational State should be 'ENABLED'")
            self.assertEqual(nep['administrative-state'], 'UNLOCKED',
                             "Administrative State should be 'UNLOCKED'")
        time.sleep(1)

    def test_60_check_update_tapi_links_ok(self):
        self.test_25_check_update_tapi_links_ok()

    def test_61_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_62_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_63_change_status_port_roadma_srg(self):
        url = "{}/config/org-openroadm-device:org-openroadm-device/circuit-packs/3%2F0/ports/C2"
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

    def test_64_check_update_portmapping(self):
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

    def test_65_check_update_openroadm_topo(self):
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

    def test_66_check_update_tapi_neps(self):
        response = test_utils.tapi_get_node_details_request("T0 - Full Multi-layer topology", "ROADM-A1+PHOTONIC_MEDIA")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nep_list = res['node']['owned-node-edge-point']
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
        self.assertEqual(nb_updated_neps, 3, "Only three roadm neps should have been modified")
        time.sleep(1)

    def test_67_check_update_tapi_links(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        link_list = res['output']['topology']['link']
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

    def test_68_check_update_service1_ok(self):
        self.test_12_get_service_Ethernet()

    def test_69_check_update_connectivity_service_Ethernet_ok(self):
        self.test_13_get_connectivity_service_Ethernet()

    def test_70_delete_connectivity_service_Ethernet(self):
        response = test_utils.tapi_delete_connectivity_request(str(self.uuid_services.eth))
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(self.WAITING)

    def test_71_disconnect_xponders_from_roadm(self):
        url = "{}/config/ietf-network:networks/network/openroadm-topology/ietf-network-topology:link/"
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        links = res['network'][0]['ietf-network-topology:link']
        for link in links:
            if link["org-openroadm-common-network:link-type"] in ('XPONDER-OUTPUT', 'XPONDER-INPUT'):
                link_name = link["link-id"]
                response = test_utils.delete_request(url+link_name)
                self.assertEqual(response.status_code, requests.codes.ok)

    def test_72_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_73_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_74_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_75_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
