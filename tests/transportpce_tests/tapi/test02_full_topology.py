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


class TransportPCEtesting(unittest.TestCase):

    processes = []
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'
    uuid_services = UuidServices()
    uuidTpAiOTSI = "f4c370be-e307-380d-a31b-7edc2b431e5d"
    uuidTpZiOTSI = "febb4502-6b27-3701-990b-3aa4941f48c4"
    uuidTpADSR = "3aca9b37-bc46-335d-b147-2423690dee18"
    uuidTpZDSR = "709d3595-6d56-3ad6-a3cd-5a4a09ce6f9d"
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
    # SIP+SPDR-SA1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sAeODU = "b6421484-531f-3444-adfc-e11c503f1fab"
    # SIP+SPDR-SC1-XPDR1+eODU+XPDR1-CLIENT1 UUID IS
    sZeODU = "4511fca7-0cf7-3b27-a128-3b372d5e1fa8"
    # uuid_A = uuid.UUID(bytes("SPDR-SA1-XPDR1+DSR+eOTSI+XPDR1-NETWORK1", 'utf-8'))
    # uuid_C = uuid.UUID(bytes("SPDR-SC1-XPDR1+DSR+eOTSI+XPDR1-NETWORK1", 'utf-8'))

    cr_serv_input_data = {
        "end-point": [
            {
                "layer-protocol-name": "PHOTONIC_MEDIA",
                "service-interface-point": {
                    "service-interface-point-uuid": "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
                },
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
        cls.processes = test_utils.start_sims([('spdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('spdrc', cls.NODE_VERSION)])

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

    def setUp(self):
        time.sleep(2)

    def test_01_connect_spdrA(self):
        print("Connecting SPDRA")
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_02_connect_spdrC(self):
        print("Connecting SPDRC")
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_03_connect_rdmA(self):
        print("Connecting ROADMA")
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_04_connect_rdmC(self):
        print("Connecting ROADMC")
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)
        time.sleep(2)

    def test_05_connect_sprdA_1_N1_to_roadmA_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_spdrA_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SA1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_07_connect_sprdC_1_N1_to_roadmC_PP1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_spdrC_1_N1(self):
        response = test_utils.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'SPDR-SC1', 'xpdr-num': '1', 'network-num': '1',
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

    def test_11_check_otn_topology(self):
        response = test_utils.get_ietf_network_request('otn-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 7, 'There should be 7 otn nodes')
        self.assertNotIn('ietf-network-topology:link', response['network'][0])

    def test_12_check_openroadm_topology(self):
        response = test_utils.get_ietf_network_request('openroadm-topology', 'config')
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['network'][0]['node']), 14, 'There should be 14 openroadm nodes')
        self.assertEqual(len(response['network'][0]['ietf-network-topology:link']), 22,
                         'There should be 22 openroadm links')

    def test_13_get_tapi_topology_details(self):
        self.tapi_topo["topology-id"] = test_utils.T0_FULL_MULTILAYER_TOPO_UUID
        response = test_utils.transportpce_api_rpc_request(
            'tapi-topology', 'get-topology-details', self.tapi_topo)
        time.sleep(2)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(len(response['output']['topology']['node']), 8, 'There should be 8 TAPI nodes')
        self.assertEqual(len(response['output']['topology']['link']), 3, 'There should be 3 TAPI links')
        print(response['output']['topology']['node'][0])
        print(response['output']['topology']['node'][1])
        print(response['output']['topology']['node'][2])

    def test_14_check_sip_details(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-common', 'get-service-interface-point-list', None)
        self.assertEqual(len(response['output']['sip']), 84, 'There should be 84 service interface point')

# test create connectivity service from spdrA to spdrC for Photonic_media
    def test_15_create_connectivity_service_PhotonicMedia(self):
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAOTS
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZOTS
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
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
        # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

    def test_16_get_service_PhotonicMedia(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services.pm))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.pm))
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# test create connectivity service from spdrA to spdrC for odu
    def test_17_create_connectivity_service_ODU(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sAeODU
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "ODU"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZeODU
#        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "ODU"
        self.cr_serv_input_data["connectivity-constraint"]["service-level"] = self.uuid_services.pm

        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
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
        # If the gate fails is because of the waiting time not being enough
#        time.sleep(self.WAITING)

    def test_18_get_service_ODU(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services.odu))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.odu))
        self.assertEqual(response['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

# test create connectivity service from spdrA to spdrC for dsr
    def test_19_create_connectivity_service_DSR(self):
        # pylint: disable=line-too-long
        self.cr_serv_input_data["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][0]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = self.sADSR
        self.cr_serv_input_data["end-point"][1]["layer-protocol-name"] = "DSR"
        self.cr_serv_input_data["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = self.sZDSR
#        self.cr_serv_input_data["connectivity-constraint"]["service-layer"] = "DSR"
        self.cr_serv_input_data["connectivity-constraint"]["requested-capacity"]["total-size"]["value"] = "10"
        self.cr_serv_input_data["connectivity-constraint"]["service-level"] = self.uuid_services.odu

        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
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
        # The sleep here is okey as the DSR service creation is very fast
#        time.sleep(self.WAITING)

    def test_20_get_service_DSR(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", str(self.uuid_services.dsr))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.dsr))
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_21_get_connectivity_service_list(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-list', None)
        self.assertEqual(response['status_code'], requests.codes.ok)
        liste_service = response['output']['service']
        for ele in liste_service:
            if ele['uuid'] == self.uuid_services.pm:
                self.assertEqual(ele['operational-state'], 'ENABLED')
#                self.assertEqual(ele['service-layer'], 'PHOTONIC_MEDIA')
                self.assertEqual(ele['layer-protocol-name'], 'PHOTONIC_MEDIA')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 3, 'There should be 3 connections')
            elif ele['uuid'] == self.uuid_services.odu:
                self.assertEqual(ele['operational-state'], 'ENABLED')
#                self.assertEqual(ele['service-layer'], 'ODU')
                self.assertEqual(ele['layer-protocol-name'], 'ODU')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 1, 'There should be 1 connections')
            elif ele['uuid'] == self.uuid_services.dsr:
                self.assertEqual(ele['operational-state'], 'ENABLED')
#                self.assertEqual(ele['service-layer'], 'DSR')
                self.assertEqual(ele['layer-protocol-name'], 'DSR')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 2, 'There should be 2 connections')
            else:
                self.fail("get connectivity service failed")
        time.sleep(2)

    def test_22_delete_connectivity_service_DSR(self):
        self.del_serv_input_data["uuid"] = str(self.uuid_services.dsr)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
        time.sleep(self.WAITING)

    def test_23_delete_connectivity_service_ODU(self):
        self.del_serv_input_data["uuid"] = str(self.uuid_services.odu)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
        time.sleep(self.WAITING)

    def test_24_delete_connectivity_service_PhotonicMedia(self):
        self.del_serv_input_data["uuid"] = str(self.uuid_services.pm)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.del_serv_input_data)
        self.assertIn(response["status_code"], (requests.codes.ok, requests.codes.no_content))
        time.sleep(self.WAITING)

    def test_25_get_no_tapi_services(self):
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-list', None)
        self.assertEqual(response['status_code'], requests.codes.internal_server_error)
        self.assertIn(
            {"error-type": "rpc", "error-tag": "operation-failed",
             "error-message": "No services exist in datastore"},
            response['output']['errors']['error'])

    def test_26_get_no_openroadm_services(self):
        response = test_utils.get_ordm_serv_list_request()
        self.assertEqual(response['status_code'], requests.codes.conflict)

    def test_27_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_28_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_29_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_30_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
