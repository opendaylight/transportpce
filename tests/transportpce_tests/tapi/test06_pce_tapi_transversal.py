#!/usr/bin/env python

##############################################################################
# Copyright (c) 2025 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

# pylint: disable=no-member
# pylint: disable=too-many-lines
# pylint: disable=too-many-public-methods
# pylint: disable=invalid-name
# pylint: disable=unused-import
# pylint: disable=line-too-long
# pylint: disable=consider-using-f-string

import os
import unittest
import time
import requests
# pylint: disable=wrong-import-order
import shutil
import sys
# from builtins import True
sys.path.append('transportpce_tests/common/')
# pylint: disable=wrong-import-position
# pylint: disable=import-error
import test_utils  # nopep8
import test_utils_generate_tapi_topo  # nopep8


# pylint: disable=too-few-public-methods
class TransportPCEtest(unittest.TestCase):

    processes = None
    topo_tapi_sbi_data = None
    WAITING = 10  # nominal value is 300
    NODE_VERSION = '2.2.1'

    del_serv_input_data = {"uuid": "TBD"}

    tapi_topo = {"topology-id": "TBD"}
    load_topo_input = {
        "input": {
            "file-path": "../../sample_configs/tapiSBItopo/tapiSbiTopo160226.json",
            "topology-uuid": "a21e4756-4d70-3d40-95b6-f7f630b4a13b"
        }
    }
    daexim_import_input = {
        "check-models": "true",
        "clear-stores": "none",
        "file-name-filter": "odl_backup_operational2.json",
        "strict-data-consistency": "true"
    }
    daexim_export_input = {
        "data-export-import:run-at": 50,
        "data-export-import:local-node-only": "true",
        "data-export-import:strict-data-consistency": "true",
        "data-export-import:included-modules": [
            {
                "module-name": "tapi-common",
                "data-store": "operational"
            }
        ]
    }
    rpc_interdomain_TA1DEG1_to_A1DEG1_input = {
        "z-end": {
            "rdm-node": "ROADM-A1",
            "deg-num": "1",
            "termination-point": "DEG1-TTP-TXRX"
        },
        "a-end": {
            "rdm-node": "ROADM-TA1+PHOTONIC_MEDIA",
            "termination-point": "ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX",
            "rdm-topology-uuid": "a21e4756-4d70-3d40-95b6-f7f630b4a13b",
            "rdm-node-uuid": "f929e2dc-3c08-32c3-985f-c126023efc43",
            "rdm-nep-uuid": "21190688-06b5-32be-be30-c73c9199b603"
        }
    }
    rpc_interdomain_TC1DEG2_to_C1DEG2_input = {
        "z-end": {
            "rdm-node": "ROADM-C1",
            "deg-num": "2",
            "termination-point": "DEG2-TTP-TXRX"
        },
        "a-end": {
            "rdm-node": "ROADM-TC1+PHOTONIC_MEDIA",
            "termination-point": "ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX",
            "rdm-topology-uuid": "a21e4756-4d70-3d40-95b6-f7f630b4a13b",
            "rdm-node-uuid": "7a44ea23-90d1-357d-8754-6e88d404b670",
            "rdm-nep-uuid": "4085f64d-fc95-3e0b-845b-35ef5779bb25"
        }
    }

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
        if cls.init_failed:
            print("tapi installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)

        # pylint: disable=bare-except
        TOPO_TAPI_SBI_FILE_ORG_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                   "..", "..", "sample_configs", "tapiSBItopo",
                                                   "tapiSbiTopo160226.json")
        print("sourcefile copied: {}".format(TOPO_TAPI_SBI_FILE_ORG_PATH))
        TOPO_TAPI_SBI_FILE_DEST_PATH = os.path.join(os.path.dirname(os.path.realpath(__file__)),
                                                    "..", "..", "..", "karaf", "target", "assembly", "daexim",
                                                    "odl_backup_operational2.json")
        print("Destfile copied to : {}".format(TOPO_TAPI_SBI_FILE_DEST_PATH))
        shutil.copyfile(TOPO_TAPI_SBI_FILE_ORG_PATH, TOPO_TAPI_SBI_FILE_DEST_PATH)
        # First import TAPI context so that the file odl_backup_models.json is created by DAEXIM
        # pylint: disable=unused-variable
        response = test_utils.transportpce_api_rpc_request('data-export-import',
                                                           'schedule-export',
                                                           cls.daexim_export_input)
        time.sleep(10)
        # Copy to DataStore the TAPI context including SBI topology as originally in the tapiSbiTopo160226.json file
        response = test_utils.transportpce_api_rpc_request('data-export-import',
                                                           'immediate-import',
                                                           cls.daexim_import_input)
        # create OpenRoadm Topology connecting simulated nodes
        cls.processes = test_utils_generate_tapi_topo.init_simulators()
        test_utils_generate_tapi_topo.connect_xpdrs()
        test_utils_generate_tapi_topo.connect_rdms()
        test_utils_generate_tapi_topo.interconnect_roadm_and_xpdr()
        test_utils_generate_tapi_topo.add_oms_attributes_to_roadm2roadm_links()
        print("TIME TO UPLOAD FULL TOPOLOGY")
        time.sleep(5)
        # test_utils_generate_tapi_topo.create_services_on_infra()
        print("Entering PCE Test 06 to test PCE algo for TAPI")

    @classmethod
    def tearDownClass(cls):
        # test_utils_generate_tapi_topo.cleanup_and_disconnect_devices()
        # pylint: disable=not-an-iterable
        test_utils_generate_tapi_topo.uninstall_Tapi_Feature()
        print("Exiting PCE Test 06 to test PCE algo for TAPI on transversal topologies")
        # test_utils_generate_tapi_topo.tearDownClass(cls)
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")
        test_utils.copy_karaf_log(cls.__name__)

    def setUp(self):
        time.sleep(2)

    def test_01_get_tapi_topology_details(self):
        self.tapi_topo["topology-id"] = test_utils.SBI_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['output']['topology']['node']), 8, 'There should be 8 TAPI nodes')
        self.assertEqual(len(response['output']['topology']['link']), 7, 'There should be 7 TAPI links')
        print("TIME TO UPLOAD FULL OpenROADMTOPOLOGY")
        time.sleep(2)

    def test_02_check_tapi_sbi_abs_node_otn_layer(self):
        response = test_utils. get_ietf_network_node_request('otn-topology', 'TAPI-SBI-ABS-NODE', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node']['node-id'], "TAPI-SBI-ABS-NODE", 'TAPI SBI ABS Node shall be there')
        self.assertEqual(response['node']['transportpce-or-network-augmentation:yang-data-model'],
                         'tapi-ext', 'TAPI SBI ABS Node is of tapi-ext model type')
        self.assertEqual(response['node']['org-openroadm-common-network:operational-state'], 'inService',
                         'TAPI SBI ABS Node shall have inService operationalState')
        self.assertEqual(response['node']['org-openroadm-common-network:administrative-state'], 'inService',
                         'TAPI SBI ABS Node shall have inService adminState')
        self.assertEqual(response['node']['org-openroadm-common-network:node-type'], 'ROADM',
                         'TAPI SBI ABS Node shall be identified as a ROADM')
        self.assertEqual(response['node']['transportpce-or-network-augmentation:topology-uuid'],
                         'Uuid{value=a21e4756-4d70-3d40-95b6-f7f630b4a13b}',
                         'TAPI SBI ABS Node shall have the topo UUID of SBI Topology')
        self.assertEqual(len(response['node']['ietf-network-topology:termination-point']), 48,
                         'TAPI SBI ABS shall have 2(STA1/STC1) x3 (XPDR1/2/3) x4 (ClientPort) x2 (eODU/DSR) otn ports')

    def test_03_check_tapi_sbi_abs_node_topology_layer(self):
        response = test_utils. get_ietf_network_node_request('openroadm-topology', 'TAPI-SBI-ABS-NODE', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['node']['node-id'], "TAPI-SBI-ABS-NODE", 'TAPI SBI ABS Node shall be there')
        self.assertEqual(response['node']['transportpce-or-network-augmentation:yang-data-model'],
                         'tapi-ext', 'TAPI SBI ABS Node is of tapi-ext model type')
        self.assertEqual(response['node']['org-openroadm-common-network:operational-state'], 'inService',
                         'TAPI SBI ABS Node shall have inService operationalState')
        self.assertEqual(response['node']['org-openroadm-common-network:administrative-state'], 'inService',
                         'TAPI SBI ABS Node shall have inService adminState')
        self.assertEqual(response['node']['org-openroadm-common-network:node-type'], 'ROADM',
                         'TAPI SBI ABS Node shall be identified as a ROADM')
        self.assertEqual(response['node']['transportpce-or-network-augmentation:topology-uuid'],
                         'Uuid{value=a21e4756-4d70-3d40-95b6-f7f630b4a13b}',
                         'TAPI SBI ABS Node shall have the topo UUID of SBI Topology')
        # TAPI SBI ABS shall have 2(SPDR-STA1/STC1)x6(XPDR1(1)/2(4)/3(1) OTS Network ports
        # + 2 (ROADM-TA1/RTC1) x 2 (DEG1+2) TTP OTS ports + 4x2(ROADM TAI SRG1/3) + 4 (ROADM TCI SRG1) OTS PP Ports
        self.assertEqual(len(response['node']['ietf-network-topology:termination-point']), 28,
                         'TAPI SBI ABS shall have a total of 28 OTS ports (SPDR Network ports & ROADM PP and TTP ports')

    def test_04_add_2_unidir_interdomain_link_between_ROADMA1_and_ROADMTA1(self):
        # pylint: disable=too-many-branches
        # pylint: disable=too-many-statements
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-inter-domain-links', self.rpc_interdomain_TA1DEG1_to_A1DEG1_input)
        print("TIME TO CHECK LINK")
        time.sleep(2)
        if (response['status_code'] != requests.codes.ok
                or 'Unidirectional Roadm-to-Roadm Inter-Domain Link created successfully'
                not in response["output"]["result"]):
            print("interdomain Link creation from ROADMA1 to ROADMTA1 failed")
        else:
            print("interdomain Link creation from ROADMA1 to ROADMTA1 succeeded")
        # # Config ROADMA1-ROADMTA oms-attributes
        # data = {"span": {
        #     "auto-spanloss": "true",
        #     "spanloss-base": 11.0,
        #     "spanloss-current": 12,
        #     "engineered-spanloss": 12.2,
        #     "link-concatenation": [{
        #         "SRLG-Id": 0,
        #         "fiber-type": "smf",
        #         "SRLG-length": 1000,
        #         "pmd": 0.01}]}}
        # response = test_utils.add_oms_attr_request(
        #     "TAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX", data)
        # if response.status_code != requests.codes.created:
        #     print("OMS attributes add on link ROADMA1-ROADMTA1 failed")
        # else:
        #     print("OMS attributes added on link ROADMA1-ROADMTA1")
        # response = test_utils.add_oms_attr_request(
        #     "ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX", data)
        # if response.status_code != requests.codes.created:
        #     print("OMS attributes add on link ROADMA1-ROADMTA1 failed")
        # else:
        #     print("OMS attributes added on link ROADMA1-ROADMTA1")
        # print("TIME TO UPLOAD FULL TOPOLOGY with Interdomain link in TAPI")
        # time.sleep(50)

    def test_05_get_tapi_link_details(self):
        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "e75aa1cb-210e-35fa-bea8-0bcd72bc929e", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-A1-DEG1+DEG1-TTP-TXRXtoROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "21ae9687-75d9-3421-9562-330f11c15d74", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "e75aa1cb-210e-35fa-bea8-0bcd72bc929e", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-A1-DEG1+DEG1-TTP-TXRXtoROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "21ae9687-75d9-3421-9562-330f11c15d74", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

    def test_06_get_openroadm_topology_link_details(self):
        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-A1-DEG1-DEG1-TTP-TXRXtoROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'ROADM-A1-DEG1')
        self.assertEqual(response['link']['source']['source-tp'], 'DEG1-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['destination']['dest-tp'], 'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['source']['source-tp'], 'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'ROADM-A1-DEG1')
        self.assertEqual(response['link']['destination']['dest-tp'], 'DEG1-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

    def test_07_add_2_unidir_interdomain_link_between_ROADMC1_and_ROADMTC1(self):
        # pylint: disable=too-many-branches
        # pylint: disable=too-many-statements
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-inter-domain-links', self.rpc_interdomain_TC1DEG2_to_C1DEG2_input)
        print("TIME TO CHECK LINK")
        time.sleep(25)
        if (response['status_code'] != requests.codes.ok
                or 'Unidirectional Roadm-to-Roadm Inter-Domain Link created successfully'
                not in response["output"]["result"]):
            print("interdomain Link creation from ROADMC1 to ROADMTC1 failed")
        else:
            print("interdomain Link creation from ROADMC1 to ROADMTC1 succeeded")

    def test_08_get_tapi_link2_details(self):
        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "792e8bd8-2a22-3507-991c-ad0d71e3cc42", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-C1-DEG2+DEG2-TTP-TXRXtoROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "7f673699-a153-3b3f-aa5e-50fbbbae7752", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "792e8bd8-2a22-3507-991c-ad0d71e3cc42", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-C1-DEG2+DEG2-TTP-TXRXtoROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "7f673699-a153-3b3f-aa5e-50fbbbae7752", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

    def test_09_get_openroadm_topology_link2_details(self):
        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-C1-DEG2-DEG2-TTP-TXRXtoROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'ROADM-C1-DEG2')
        self.assertEqual(response['link']['source']['source-tp'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['destination']['dest-tp'], 'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['source']['source-tp'], 'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'ROADM-C1-DEG2')
        self.assertEqual(response['link']['destination']['dest-tp'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

    def check_node_edge_point_inter_domain_link1(self, linkresponse):
        self.assertEqual(linkresponse['node-edge-point'][0]['topology-uuid'],
                         '393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7')
        self.assertEqual(linkresponse['node-edge-point'][1]['topology-uuid'],
                         'a21e4756-4d70-3d40-95b6-f7f630b4a13b')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-uuid'], 'b0afe75c-0280-3895-a8fd-39425dff0604')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-uuid'], 'f929e2dc-3c08-32c3-985f-c126023efc43')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-edge-point-uuid'],
                         'e0a21694-fe23-3fb2-ad17-b7d81cf57d32')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-edge-point-uuid'],
                         '21190688-06b5-32be-be30-c73c9199b603')

    def check_node_edge_point_inter_domain_link2(self, linkresponse):
        self.assertEqual(linkresponse['node-edge-point'][1]['topology-uuid'],
                         '393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7')
        self.assertEqual(linkresponse['node-edge-point'][0]['topology-uuid'],
                         'a21e4756-4d70-3d40-95b6-f7f630b4a13b')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-uuid'], '7a44ea23-90d1-357d-8754-6e88d404b670')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-uuid'], '6d6d896d-ecf0-3360-897d-915ddd0e9122')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-edge-point-uuid'],
                         '4085f64d-fc95-3e0b-845b-35ef5779bb25')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-edge-point-uuid'],
                         '3aebb9bf-1109-37e5-a547-eeb4fcc28d80')


if __name__ == "__main__":
    unittest.main(verbosity=2)
