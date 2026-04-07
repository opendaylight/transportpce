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

    # SPDR-SA1-XPDR1 UUID is
    s1Auuid = "4e44bcc5-08d3-3fee-8fac-f021489e5a61"
    # SPDR-SA1-XPDR2 UUID is
    s2Auuid = "38c114ae-9c0e-3068-bb27-db2dbd81220b"
    # SPDR-SA1-XPDR3 UUID is
    s3Auuid = "4582e51f-2b2d-3b70-b374-86c463062710"
    # SPDR-SC1-XPDR1 UUID is
    s1Cuuid = "215ee18f-7869-3492-94d2-0f24ed0a3023"
    # SPDR-SC1-XPDR2 UUID is
    s2Cuuid = "d852c340-77db-3f9a-96e8-cb4de8e1004a"
    # SPDR-SC1-XPDR3 UUID is
    s3Cuuid = "c1f06957-c0b9-32be-8492-e278b2d4a3aa"
    # ROADM-A1-SRG1 UUID is
    rSRG1Auuid = "63a5b34b-02da-390f-835b-177e7bc7e1a8"
    # ROADM-A1-DEG1 UUID is
    rDEG1Auuid = "e456933a-2e0c-3510-aca8-4a4546389eb4"
    # ROADM-C1-SRG1 UUID is
    rSRG1Cuuid = "12bcf05d-e3f8-3d76-8142-c5016f203b6d"
    # ROADM-C1-DEG2 UUID is
    rDEG2Cuuid = "73e9c6e1-ffba-325e-b0cb-64a6dce83fc8"
    # ROADM-TA1-DEG1 UUID is
    rDEG1TAuuid = "25e097a6-a5f8-36f5-b2e2-b6f18f9d3a0d"
    # ROADM-TA1-DEG2 UUID is
    rDEG2TAuuid = "2d258988-a00d-37d1-85dd-1fefa18544ca"
    # ROADM-TC1-DEGG1 UUID is
    rDEG1TCuuid = "4b1c0438-fd8a-33d2-b0d0-5be812555c71"
    # ROADM-TC1-DEG2 UUID is
    rDEG2TCuuid = "d9d39946-b069-33fe-9763-d9843c819110"
    # LCP (NEP) SPDR-SA1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2--NETWORK2 UUID IS
    nAX2OTS2 = "4e4bf439-b457-3260-865c-db716e2647c2"
    # LCP (NEP) SPDR-SC1-XPDR2+PHOTONIC_MEDIA_OTS+XPDR2-NETWORK2 UUID IS
    nZX2OTS2 = "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"

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
    path_computation_input_data = {
        "service-name": "1a58e7bd-c997-48eb-9967-fa54afb02f47",
        "resource-reserve": "true",
        "service-handler-header": {
            "request-id": "request1"
        },
        "service-a-end": {
            "clli": "38c114ae-9c0e-3068-bb27-db2dbd81220b",
            "service-format": "OTU",
            "tx-direction": {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "rx-direction": {
                "logical-connection-point": "4e4bf439-b457-3260-865c-db716e2647c2"
            },
            "node-id": "SPDR-SA1-XPDR2+XPONDER"
        },
        "service-z-end": {
            "clli": "d852c340-77db-3f9a-96e8-cb4de8e1004a",
            "service-format": "OTU",
            "tx-direction": {
                "logical-connection-point": "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
            },
            "rx-direction": {
                "logical-connection-point": "ae235ae1-f17f-3619-aea9-e6b2a9e850c3"
            },
            "node-id": "SPDR-SC1-XPDR2+XPONDER"
        },
        "pce-routing-metric": "hop-count"
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
                                                    "..", "..", "..", "karaf221", "target", "assembly", "daexim",
                                                    "odl_backup_operational2.json")
        # First import TAPI context so that the file odl_backup_models.json is created by DAEXIM
        # pylint: disable=unused-variable
        response = test_utils.transportpce_api_rpc_request('data-export-import',
                                                           'schedule-export',
                                                           cls.daexim_export_input)
        time.sleep(10)
        print(response)
        shutil.copyfile(TOPO_TAPI_SBI_FILE_ORG_PATH, TOPO_TAPI_SBI_FILE_DEST_PATH)
        # Copy to DataStore the TAPI context including SBI topology as originally in the tapiSbiTopo160226.json file
        response = test_utils.transportpce_api_rpc_request('data-export-import',
                                                           'immediate-import',
                                                           cls.daexim_import_input)
        # create OpenRoadm Topology connecting simulated nodes
        cls.processes = test_utils_generate_tapi_topo.init_simulators()
        test_utils_generate_tapi_topo.connect_xpdrs()
        test_utils_generate_tapi_topo.connect_rdms()
        test_utils_generate_tapi_topo.interconnect_roadm_and_xpdr()
        # OMS attributes voluntarily not copied to DataStore so that path computation on alternative path crossing TAPI
        # domain can be triggered
        # test_utils_generate_tapi_topo.add_oms_attributes_to_roadm2roadm_links()
        time.sleep(5)
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
        # Config ROADMA1-ROADMTA oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.0,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 0,
                "fiber-type": "smf",
                "SRLG-length": 1000,
                "pmd": 0.01}]}}
        response = test_utils.add_oms_attr_request(
            "TAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX", data)
        if response.status_code != requests.codes.created:
            print("OMS attributes add on link ROADMA1-ROADMTA1 failed")
        else:
            print("OMS attributes added on link ROADMA1-ROADMTA1")
        response = test_utils.add_oms_attr_request(
            "ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX", data)
        if response.status_code != requests.codes.created:
            print("OMS attributes add on link ROADMA1-ROADMTA1 failed")
        else:
            print("OMS attributes added on link ROADMA1-ROADMTA1")
        # time.sleep(50)

    def test_05_get_tapi_link_details(self):
        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "56175211-d7b8-3cbd-9aea-9a349985d144", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "15c4ffad-b64f-3a25-85a4-7736599ad3f5", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'TAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "56175211-d7b8-3cbd-9aea-9a349985d144", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "15c4ffad-b64f-3a25-85a4-7736599ad3f5", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'TAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link1(response['link'][0])

    def test_06_get_openroadm_topology_link_details(self):
        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-A1-DEG1-DEG1-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'ROADM-A1-DEG1')
        self.assertEqual(response['link']['source']['source-tp'], 'DEG1-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['destination']['dest-tp'], 'ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'TAPI-SBI-ABS-NODE-ROADM-TA1+PHOTONIC_MEDIA_OTS+DEG1-TTP-TXRXtoROADM-A1-DEG1-DEG1-TTP-TXRX', 'config')
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
        # time.sleep(25)
        if (response['status_code'] != requests.codes.ok
                or 'Unidirectional Roadm-to-Roadm Inter-Domain Link created successfully'
                not in response["output"]["result"]):
            print("interdomain Link creation from ROADMC1 to ROADMTC1 failed")
        else:
            print("interdomain Link creation from ROADMC1 to ROADMTC1 succeeded")
            # Config ROADMA1-ROADMTA oms-attributes
        data = {"span": {
            "auto-spanloss": "true",
            "spanloss-base": 11.0,
            "spanloss-current": 12,
            "engineered-spanloss": 12.2,
            "link-concatenation": [{
                "SRLG-Id": 1,
                "fiber-type": "smf",
                "SRLG-length": 1000,
                "pmd": 0.01}]}}
        response = test_utils.add_oms_attr_request(
            "TAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX", data)
        if response.status_code != requests.codes.created:
            print("OMS attributes add on link ROADMC1-ROADMTC1 failed")
        else:
            print("OMS attributes added on link ROADMC1-ROADMTC1")
        response = test_utils.add_oms_attr_request(
            "ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX", data)
        if response.status_code != requests.codes.created:
            print("OMS attributes add on link ROADMC1-ROADMTC1 failed")
        else:
            print("OMS attributes added on link ROADMC1-ROADMTC1")
        print("TIME TO UPLOAD FULL TOPOLOGY with Interdomain link in TAPI")
        time.sleep(50)

    def test_08_get_tapi_link2_details(self):
        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "2fbcfe28-6513-382a-9650-15ed08aaca08", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.T0_FULL_MULTILAYER_TOPO_UUID, "0fae8565-71ac-34c5-8279-60438dd03702", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'TAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "2fbcfe28-6513-382a-9650-15ed08aaca08", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

        response = test_utils.get_tapi_topology_link(
            test_utils.SBI_TOPO_UUID, "0fae8565-71ac-34c5-8279-60438dd03702", "nonconfig")
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['link'][0]['name'][0]['value-name'], 'tapi-interdomain-link')
        self.assertEqual(response['link'][0]['name'][0]['value'],
                         'TAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX')
        self.check_node_edge_point_inter_domain_link2(response['link'][0])

    def test_09_get_openroadm_topology_link2_details(self):
        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'ROADM-C1-DEG2-DEG2-TTP-TXRXtoTAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'ROADM-C1-DEG2')
        self.assertEqual(response['link']['source']['source-tp'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['destination']['dest-tp'], 'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

        response = test_utils.get_ietf_network_link_request('openroadm-topology',
                                                            'TAPI-SBI-ABS-NODE-ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRXtoROADM-C1-DEG2-DEG2-TTP-TXRX', 'config')
        time.sleep(2)
        self.assertEqual(response['link']['source']['source-node'], 'TAPI-SBI-ABS-NODE')
        self.assertEqual(response['link']['source']['source-tp'], 'ROADM-TC1+PHOTONIC_MEDIA_OTS+DEG2-TTP-TXRX')
        self.assertEqual(response['link']['destination']['dest-node'], 'ROADM-C1-DEG2')
        self.assertEqual(response['link']['destination']['dest-tp'], 'DEG2-TTP-TXRX')
        self.assertEqual(response['link']['org-openroadm-common-network:operational-state'], 'inService')
        self.assertEqual(response['link']['transportpce-or-network-augmentation:link-class'], 'inter-domain')
        self.assertEqual(response['link']['org-openroadm-common-network:link-type'], 'ROADM-TO-ROADM')

        #################################################################################################
        # CONFIGURATION USED FOR THE TEST : OpenROADM TOPOLOGY created dynamically using Simulators     #
        #                                TAPI-SBI Topology loaded from a file through DAEXIM feature    #
        #  Note : XPDRA and C (as well as TA and TC) not represented for simplification                 #
        #                                                                                               #
        #    SPDR-SA1     ROADM-A1                                 ROADM-C1             SPDR-SC1        #
        #     ____     _______      ____                         ____                     ____          #
        #    |    |   |   S3  |    |    |                       |    |                ___|1   |         #
        #    |X3 1|---|PP3    |----| D2 |----------->-----------| D1 |     _______   /   | X1 |         #
        #    |____|   |       |_  /|    |-----------<-----------|    |____|  S1   | /    |____|         #
        #     ____    |_______| \/ |____| (OMS Characteristics  |____|    |    PP1|-      ____          #
        #    |    |    _______  /\  __|_   Not loaded to avoid   _|__     |       |      |    |         #
        #    | X1 |   |   S1  |-  \|    |  link selection in    |    |____|    PP2|------|1 X2|         #
        #    |___1|---|PP1    |----| D1 |  path computation)    | D2 |    |    PP4|------|2___|         #
        #     ____    |       |    |    |                       |    |    |    PP3|--\    ____          #
        #    |   1|---|PP2    |    |____|   OPENROADM TOPOLOGY  |____|    |_______|   \  |    |         #
        #    | X2 |   |       |      ||                           ||                   \_| X3 |         #
        #    |___2|---|PP4    |      ||                           ||                     |1___|         #
        #             |_______|      ||                           ||                                    #
        #  - - - - - - - - - - - - - || <<  Inter-Domain Links >> || - - - - - - - - - - - - - - -      #
        #                            ||                           ||                                    #
        #  SPDR-STA1     ROADM-TA1   ||                           ||  ROADM-TC1         SPDR-STC1       #
        #     ____     _______      _||_                         _||_                     ____          #
        #    |    |   |   S3  |    |    |                       |    |                ___|1   |         #
        #    |X3 1|---|PP3    |----| D1 |                       | D2 |     _______   /   | X1 |         #
        #    |____|   |       |_  /|    |  TAPI-SBI-TOPOLOGY    |    |____|  S1   | /    |____|         #
        #     ____    |_______| \/ |____|                       |____|    |    PP1|-      ____          #
        #    |    |    _______  /\  __|_                         _|__     |       |      |    |         #
        #    | X1 |   |   S1  |-  \|    |                       |    |____|    PP2|------|1 X2|         #
        #    |___1|---|PP1    |----| D2 |----------->-----------| D1 |    |    PP4|------|2___|         #
        #     ____    |       |    |    |-----------<-----------|    |    |    PP3|--\    ____          #
        #    |   1|---|PP2    |    |____|                       |____|    |_______|   \  |    |         #
        #    | X2 |   |       |                                                        \_| X3 |         #
        #    |___2|---|PP4    |                                                          |1___|         #
        #             |_______|                                                                         #
        #                                                                                               #
        #                                                                                               #
        #################################################################################################

    # Test Path Computation success forcing path through TAPI-SBI Nodes
    def test_10_successfull_path_computation_phtnc_service_spdr_x2(self):
        # Path Computation from NEP of SPDR-SA1-XPDR2-NETWORK2 to NEP of of SPDR-SC1-XPDR2-NETWORK2
        self.path_computation_input_data["service-a-end"]["service-rate"] = "100"
        self.path_computation_input_data["service-z-end"]["service-rate"] = "100"
        response = test_utils.transportpce_api_rpc_request('transportpce-pce',
                                                           'path-computation-request',
                                                           self.path_computation_input_data)
        # pylint: disable=consider-using-f-string
        print("getServiceResponse Service1 PhotonicMedia: {}".format(response))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Path is calculated',
                      response['output']['configuration-response-common']['response-message'])
        print(response['output']['response-parameters']['path-description'])
        self.assertEqual(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-wavelength-number'], 0)
        self.assertEqual(response['output']['response-parameters']['path-description']
                         ['aToZ-direction']['aToZ-max-frequency'], '196.125')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['rate'], 100)
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']['width'],
                         '40.0')
        self.assertEqual(response['output']['response-parameters']['path-description']['aToZ-direction']
                         ['modulation-format'], 'dp-qpsk')
        node_to_find = [self.s2Auuid, self.s2Cuuid, self.rSRG1Auuid, self.rDEG1Auuid, self.rSRG1Cuuid, self.rDEG2Cuuid,
                        self.rDEG1TAuuid, self.rDEG2TAuuid, self.rDEG1TCuuid, self.rDEG2TCuuid]
        link_to_find = ['64207363-986f-3495-94c2-d2a3290ac8a2', '99000caf-8388-3206-8529-c08a88403033',
                        'a8163bcc-4c19-3173-b0f3-2e6212700b4e', '785ef191-05e8-35d2-8a09-07ba8f781364',
                        '56175211-d7b8-3cbd-9aea-9a349985d144', '15c4ffad-b64f-3a25-85a4-7736599ad3f5',
                        '3c74210a-e24c-3810-878f-d10dc26334d8', '319e2eac-1d45-3c37-bc49-8a2ac5995c5a',
                        '8dcc68d4-4bf8-3294-b8f1-1af1fcb3e037', '8db52c4e-f7bd-36e1-9f26-cdcd8d9f95c5',
                        '2fbcfe28-6513-382a-9650-15ed08aaca08', '0fae8565-71ac-34c5-8279-60438dd03702',
                        'eee8d936-ec09-33ed-a349-6a724a543f13', '89b18d8c-5fa4-3bad-8b62-dc18b6d6b6e1',
                        '5b7bb978-e3f8-3fe7-8709-880f7c94c60f', 'b0cc503c-e25e-32c6-8956-1ad8729e6b57']
        tp_to_find = ['d55a3ea0-11e9-3f58-9f57-853ad07474a0', 'a1f8e632-7aa9-3669-96c2-a8c173b53c28',
                      '4085f64d-fc95-3e0b-845b-35ef5779bb25', 'cd2619fb-1ae0-3785-a6fd-2b71f468cb6c',
                      '40974321-34ab-3093-ad13-c6922bdf570b', '4e4bf439-b457-3260-865c-db716e2647c2',
                      'fc147a13-c13a-3675-aec9-7a9ad75f7884', 'b748f459-2ad2-355c-a0cf-5fff6401b26d',
                      '44bd7778-f186-31a1-9c8c-e230f916706a', 'f57846b2-1faf-3555-b1cf-66b7957df2a5',
                      'f1e6b52f-d934-3d4a-bd70-b678800ec74d', '21190688-06b5-32be-be30-c73c9199b603',
                      '5bf5f7a6-2146-3fc6-863f-26cb9c266e81', 'a8bc2984-7bd8-30cc-b705-8092a4c6fc18',
                      '05f973f2-a9a2-3750-8351-352f72f4af39', 'c49aa9ec-8978-3f95-ad49-bb7e2d2f73b7',
                      'ae235ae1-f17f-3619-aea9-e6b2a9e850c3', 'b2a2185e-1bd8-3fc1-8083-b50d08a1f21f']
        self.assertTrue(self.find_resource_in_response(node_to_find, response),
                        'Found expected Optical Nodes in PathDescription')
        self.assertTrue(self.find_resource_in_response(tp_to_find, response),
                        'Found expected  tps in PathDescription')
        self.assertTrue(self.find_link_in_response(link_to_find, response),
                        'Found expected link in PathDescription')
        time.sleep(self.WAITING)

    def check_node_edge_point_inter_domain_link1(self, linkresponse):
        self.assertEqual(linkresponse['node-edge-point'][1]['topology-uuid'],
                         '393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7')
        self.assertEqual(linkresponse['node-edge-point'][0]['topology-uuid'],
                         'a21e4756-4d70-3d40-95b6-f7f630b4a13b')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-uuid'], '3b726367-6f2d-3e3f-9033-d99b61459075')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-uuid'], 'f929e2dc-3c08-32c3-985f-c126023efc43')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-edge-point-uuid'],
                         'f57846b2-1faf-3555-b1cf-66b7957df2a5')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-edge-point-uuid'],
                         '21190688-06b5-32be-be30-c73c9199b603')

    def check_node_edge_point_inter_domain_link2(self, linkresponse):
        self.assertEqual(linkresponse['node-edge-point'][1]['topology-uuid'],
                         '393f09a4-0a0b-3d82-a4f6-1fbbc14ca1a7')
        self.assertEqual(linkresponse['node-edge-point'][0]['topology-uuid'],
                         'a21e4756-4d70-3d40-95b6-f7f630b4a13b')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-uuid'], '7a44ea23-90d1-357d-8754-6e88d404b670')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-uuid'], '4986dca9-2d59-3d79-b306-e11802bcf1e6')
        self.assertEqual(linkresponse['node-edge-point'][0]['node-edge-point-uuid'],
                         '4085f64d-fc95-3e0b-845b-35ef5779bb25')
        self.assertEqual(linkresponse['node-edge-point'][1]['node-edge-point-uuid'],
                         'b748f459-2ad2-355c-a0cf-5fff6401b26d')

    def find_resource_in_response(self, resource_list, response):
        result_list = []
        for resourceToFind in resource_list:
            res_found_in_atoz = False
            res_found_in_ztoa = False
            for atozElt in response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ']:
                # print(f"atozElt = {atozElt}")
                if ('tp-id' in atozElt['resource'].keys() and atozElt['resource']['tp-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
                if ('node-id' in atozElt['resource'].keys() and atozElt['resource']['node-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
            for ztoaElt in response['output']['response-parameters']['path-description']['zToA-direction']['zToA']:
                # print(f"ztoaElt = {ztoaElt}")
                if ('tp-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['tp-id'] == resourceToFind
                        and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
                if ('node-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['node-id'] == resourceToFind
                        and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
            if res_found_in_atoz and res_found_in_ztoa:
                result_list.append(True)
            else:
                result_list.append(False)
        if False in result_list:
            return False
        return True

    def find_link_in_response(self, link_list, response):
        result_list = []
        for resourceToFind in link_list:
            res_found_in_atoz = False
            res_found_in_ztoa = False
            for atozElt in response['output']['response-parameters']['path-description']['aToZ-direction']['aToZ']:
                # print(f"atozElt = {atozElt}")
                if ('link-id' in atozElt['resource'].keys() and atozElt['resource']['link-id'] == resourceToFind
                        and atozElt['resource']['state'] == 'inService'):
                    res_found_in_atoz = True
                    break
            for ztoaElt in response['output']['response-parameters']['path-description']['zToA-direction']['zToA']:
                # print(f"ztoaElt = {ztoaElt}")
                if ('link-id' in ztoaElt['resource'].keys() and ztoaElt['resource']['link-id'] == resourceToFind
                        and ztoaElt['resource']['state'] == 'inService'):
                    res_found_in_ztoa = True
                    break
            if (res_found_in_atoz or res_found_in_ztoa) and not (res_found_in_atoz and res_found_in_ztoa):
                result_list.append(True)
            else:
                result_list.append(False)
        print("Link Result List: {}".format(result_list))
        if False in result_list:
            return False
        return True


if __name__ == "__main__":
    unittest.main(verbosity=2)
