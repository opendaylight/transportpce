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
import sys
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportPCEtesting(unittest.TestCase):

    processes = None
    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    cr_serv_sample_data = {
        "input": {
            "end-point": [
                {
                    "layer-protocol-name": "PHOTONIC_MEDIA",
                    "service-interface-point": {
                        "service-interface-point-uuid": "b1a0d883-32b8-3b0b-93d6-7ed074f6f107"
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
                        "service-interface-point-uuid": "d1d6305e-179b-346f-b02d-8260aebe1ce8"
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
                "service-layer": "PHOTONIC_MEDIA",
                "service-type": "POINT_TO_POINT_CONNECTIVITY",
                "service-level": "Some service-level",
                "requested-capacity": {
                    "total-size": {
                        "value": "100",
                        "unit": "GB"
                    }
                }
            },
            "state": "Some state"}}

    @classmethod
    def setUpClass(cls):
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

    def setUp(self):
        time.sleep(5)

    def test_01_connect_spdrA(self):
        response = test_utils.mount_device("SPDR-SA1", ('spdra', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_spdrC(self):
        response = test_utils.mount_device("SPDR-SC1", ('spdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_05_connect_sprdA_1_N1_to_roadmA_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_spdrA_1_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SA1", "1", "1",
                                                          "ROADM-A1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_07_connect_sprdC_1_N1_to_roadmC_PP1(self):
        response = test_utils.connect_xpdr_to_rdm_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully',
                      res["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_spdrC_1_N1(self):
        response = test_utils.connect_rdm_to_xpdr_request("SPDR-SC1", "1", "1",
                                                          "ROADM-C1", "1", "SRG1-PP1-TXRX")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully',
                      res["output"]["result"])
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
        time.sleep(2)

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
        time.sleep(2)

    def test_11_check_otn_topology(self):
        response = test_utils.get_otn_topo_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 6, 'There should be 6 otn nodes')
        self.assertNotIn('ietf-network-topology:link', res['network'][0])
        time.sleep(2)

    def test_12_check_ordm_topology(self):
        response = test_utils.get_ordm_topo_request("")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        nbLink = len(res['network'][0]['ietf-network-topology:link'])
        self.assertEqual(nbNode, 13, 'There should be 13 openroadm nodes')
        self.assertEqual(nbLink, 22, 'There should be 22 openroadm links')
        time.sleep(2)

    def test_13_get_tapi_topology_details(self):
        response = test_utils.tapi_get_topology_details_request(
            "T0 - Full Multi-layer topology")
        time.sleep(2)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['output']['topology']['node'])
        nbLink = len(res['output']['topology']['link'])
        self.assertEqual(nbNode, 14, 'There should be 14 TAPI nodes')
        self.assertEqual(nbLink, 13, 'There should be 13 TAPI links')
        time.sleep(2)

    def test_14_check_sip_details(self):
        response = test_utils.tapi_get_sip_details_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbSip = len(res['output']['sip'])
        self.assertEqual(nbSip, 60, 'There should be 60 service interface point')
        time.sleep(2)

# test create connectivity service from spdrA to spdrC for Photonic_media
    def test_15_create_connectivity_service_PhotonicMedia(self):
        response = test_utils.tapi_create_connectivity_request(self.cr_serv_sample_data)
        time.sleep(self.WAITING)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        global service_pm_uuid
        service_pm_uuid = res['output']['service']['uuid']
        print("photonic media service uuid : {}".format(res['output']['service']['uuid']))

        input_dict_1 = {'administrative-state': 'LOCKED',
            'lifecycle-state': 'PLANNED',
            'operational-state': 'DISABLED',
            'service-type': 'POINT_TO_POINT_CONNECTIVITY',
            'service-layer': 'PHOTONIC_MEDIA',
            'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **res['output']['service']),
                             res['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **res['output']['service']['end-point'][0]['name'][0]),
                             res['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **res['output']['service']['end-point'][1]['name'][0]),
                             res['output']['service']['end-point'][1]['name'][0])
        time.sleep(self.WAITING)

    def test_16_get_service_PhotonicMedia(self):
        response = test_utils.get_service_list_request(
            "services/"+ str(service_pm_uuid))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], service_pm_uuid)
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

# test create connectivity service from spdrA to spdrC for odu
    def test_17_create_connectivity_service_ODU(self):
        self.cr_serv_sample_data["input"]["end-point"][0]["layer-protocol-name"] = "ODU"
        self.cr_serv_sample_data["input"]["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = "eecbfa6e-57ab-3651-9606-c22c8ce73f18"
        self.cr_serv_sample_data["input"]["end-point"][1]["layer-protocol-name"] = "ODU"
        self.cr_serv_sample_data["input"]["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = "31f83b1f-29b2-3a8e-af9b-6423dbc5aa22"
        self.cr_serv_sample_data["input"]["connectivity-constraint"]["service-layer"] = "ODU"

        response = test_utils.tapi_create_connectivity_request(self.cr_serv_sample_data)
        time.sleep(self.WAITING)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        global service_odu_uuid
        service_odu_uuid = res['output']['service']['uuid']
        print("odu service uuid : {}".format(res['output']['service']['uuid']))

        input_dict_1 = {'administrative-state': 'LOCKED',
            'lifecycle-state': 'PLANNED',
            'operational-state': 'DISABLED',
            'service-type': 'POINT_TO_POINT_CONNECTIVITY',
            'service-layer': 'ODU',
            'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1, **res['output']['service']),
                             res['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **res['output']['service']['end-point'][0]['name'][0]),
                             res['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **res['output']['service']['end-point'][1]['name'][0]),
                             res['output']['service']['end-point'][1]['name'][0])
        time.sleep(self.WAITING)

    def test_18_get_service_ODU(self):
        response = test_utils.get_service_list_request(
            "services/"+ str(service_odu_uuid))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], service_odu_uuid)
        self.assertEqual(
            res['services'][0]['connection-type'], 'infrastructure')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

# test create connectivity service from spdrA to spdrC for dsr
    def test_19_create_connectivity_service_DSR(self):
        self.cr_serv_sample_data["input"]["end-point"][0]["layer-protocol-name"] = "DSR"
        self.cr_serv_sample_data["input"]["end-point"][0]["service-interface-point"]["service-interface-point-uuid"] = "c14797a0-adcc-3875-a1fe-df8949d1a2d7"
        self.cr_serv_sample_data["input"]["end-point"][1]["layer-protocol-name"] = "DSR"
        self.cr_serv_sample_data["input"]["end-point"][1]["service-interface-point"]["service-interface-point-uuid"] = "25812ef2-625d-3bf8-af55-5e93946d1c22"
        self.cr_serv_sample_data["input"]["connectivity-constraint"]["service-layer"] = "DSR"
        self.cr_serv_sample_data["input"]["connectivity-constraint"]["requested-capacity"]["total-size"]["value"] = "10"

        response = test_utils.tapi_create_connectivity_request(self.cr_serv_sample_data)
        time.sleep(self.WAITING)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        global service_dsr_uuid
        service_dsr_uuid = res['output']['service']['uuid']
        print("dsr service uuid : {}".format(res['output']['service']['uuid']))

        input_dict_1 = {'administrative-state': 'LOCKED',
            'lifecycle-state': 'PLANNED',
            'operational-state': 'DISABLED',
            'service-type': 'POINT_TO_POINT_CONNECTIVITY',
            'service-layer': 'DSR',
            'connectivity-direction': 'BIDIRECTIONAL'
                        }
        input_dict_2 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SC1-XPDR1'}
        input_dict_3 = {'value-name': 'OpenROADM node id',
                        'value': 'SPDR-SA1-XPDR1'}

        self.assertDictEqual(dict(input_dict_1,
                                  **res['output']['service']),
                             res['output']['service'])
        self.assertDictEqual(dict(input_dict_2,
                                  **res['output']['service']['end-point'][0]['name'][0]),
                             res['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3,
                                  **res['output']['service']['end-point'][1]['name'][0]),
                             res['output']['service']['end-point'][1]['name'][0])
        time.sleep(self.WAITING)

    def test_20_get_service_DSR(self):
        response = test_utils.get_service_list_request(
            "services/"+ str(service_dsr_uuid))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], service_dsr_uuid)
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_21_get_connectivity_service_list(self):
        response = test_utils.tapi_get_service_list_request()
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        liste_service = res['output']['service']
        for ele in liste_service:
            if ele['uuid'] == service_pm_uuid:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'PHOTONIC_MEDIA')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 9, 'There should be 9 connections')
            elif ele['uuid'] == service_odu_uuid:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'ODU')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 3, 'There should be 3 connections')
            elif ele['uuid'] == service_dsr_uuid:
                self.assertEqual(ele['operational-state'], 'ENABLED')
                self.assertEqual(ele['service-layer'], 'DSR')
                nbconnection = len(ele['connection'])
                self.assertEqual(nbconnection, 1, 'There should be 1 connection')
            else:
                self.fail("get connectivity service failed")
        time.sleep(2)

    def test_22_delete_connectivity_service_DSR(self):
       response = test_utils.tapi_delete_connectivity_request(service_dsr_uuid)
       self.assertEqual(response.status_code, requests.codes.no_content)
       time.sleep(self.WAITING)

    def test_23_delete_connectivity_service_ODU(self):
        response = test_utils.tapi_delete_connectivity_request(service_odu_uuid)
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(self.WAITING)

    def test_24_delete_connectivity_service_PhotonicMedia(self):
        response = test_utils.tapi_delete_connectivity_request(service_pm_uuid)
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(self.WAITING)

    def test_25_disconnect_spdrA(self):
        response = test_utils.unmount_device("SPDR-SA1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_26_disconnect_spdrC(self):
        response = test_utils.unmount_device("SPDR-SC1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_27_disconnect_roadmA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)

    def test_28_disconnect_roadmC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok,
                         test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
