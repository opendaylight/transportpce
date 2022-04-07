#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
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
# pylint: disable=wrong-import-order
import sys
import unittest
import time
import requests
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


# pylint: disable=too-few-public-methods
class UuidSubscriptions:
    def __init__(self):
        # pylint: disable=invalid-name
        self.pm = None
        self.odu = None
        self.dsr = None
        self.eth = None


class TransportNbiNotificationstesting(unittest.TestCase):
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

    cr_notif_subs_sample_data = {
        "input": {
            "subscription-filter": {
                "requested-notification-types": [
                    "ALARM_EVENT"
                ],
                "requested-object-types": [
                    "CONNECTIVITY_SERVICE"
                ],
                "requested-layer-protocols": [
                    "ETH"
                ],
                "requested-object-identifier": [
                    "76d8f07b-ead5-4132-8eb8-cf3fdef7e079"
                ],
                "include-content": True,
                "local-id": "localId",
                "name": [
                    {
                        "value-name": "Subscription name",
                        "value": "test subscription"
                    }
                ]
            },
            "subscription-state": "ACTIVE"
        }
    }

    cr_get_notif_list_sample_data = {
        "input": {
            "subscription-id-or-name": "c07e7fd1-0377-4fbf-8928-36c17b0d0d68",
            "time-period": "time-period"
        }
    }

    processes = []
    uuid_services = UuidServices()
    uuid_subscriptions = UuidSubscriptions()
    WAITING = 25  # nominal value is 300
    NODE_VERSION_221 = '2.2.1'

    @classmethod
    def setUpClass(cls):
        # pylint: disable=unsubscriptable-object
        # TODO: for lighty manage the activation of NBI notification feature
        cls.init_failed_nbi = False
        cls.init_failed_tapi = False
        os.environ['JAVA_MIN_MEM'] = '1024M'
        os.environ['JAVA_MAX_MEM'] = '4096M'
        cls.processes = test_utils.start_tpce()
        # NBI notification feature is not installed by default in Karaf
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing NBI notification feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-nbinotifications")
            if result.returncode != 0:
                cls.init_failed_nbi = True
            print("installing tapi feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed_tapi = True
            print("Restarting OpenDaylight...")
            test_utils.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils.start_karaf()
            test_utils.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils.wait_until_log_contains(
                test_utils.KARAF_LOG, test_utils.KARAF_OK_START_MSG, time_to_wait=60)
        if cls.init_failed_nbi:
            print("NBI notification installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        if cls.init_failed_tapi:
            print("tapi installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils.start_sims([('xpdra', cls.NODE_VERSION),
                                               ('roadma', cls.NODE_VERSION),
                                               ('roadmc', cls.NODE_VERSION),
                                               ('xpdrc', cls.NODE_VERSION)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION))
        self.assertEqual(response.status_code, requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION))
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

    def test_14_create_notifications_subscription_service(self):
        self.cr_notif_subs_sample_data["input"]["subscription-filter"]["requested-object-identifier"][0] = str(
            self.uuid_services.eth)
        response = test_utils.tapi_create_notification_subscription_service_request(self.cr_notif_subs_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.uuid_subscriptions.eth = res['output']['subscription-service']['uuid']
        self.assertEqual(res['output']['subscription-service']['subscription-filter']
                         ['requested-object-types'][0], 'CONNECTIVITY_SERVICE')
        self.assertEqual(res['output']['subscription-service']['subscription-filter']
                         ['requested-notification-types'][0], 'ALARM_EVENT')
        self.assertEqual(res['output']['subscription-service']['subscription-filter']
                         ['requested-object-identifier'][0], str(self.uuid_services.eth))
        time.sleep(2)

    def test_15_change_status_port_roadma_srg(self):
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

    def test_16_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_sample_data["input"]["subscription-id-or-name"] = str(self.uuid_subscriptions.eth)
        response = test_utils.tapi_get_notifications_list_request(self.cr_get_notif_list_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['notification'][-1]['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(res['output']['notification'][-1]['target-object-type'], 'CONNECTIVITY_SERVICE')
        self.assertEqual(res['output']['notification'][-1]['changed-attributes'][0]['new-value'], 'LOCKED')
        self.assertEqual(res['output']['notification'][-1]['changed-attributes'][1]['new-value'], 'DISABLED')
        time.sleep(2)

    def test_17_restore_status_port_roadma_srg(self):
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

    def test_18_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_sample_data["input"]["subscription-id-or-name"] = str(self.uuid_subscriptions.eth)
        response = test_utils.tapi_get_notifications_list_request(self.cr_get_notif_list_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['notification'][-1]['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(res['output']['notification'][-1]['target-object-type'], 'CONNECTIVITY_SERVICE')
        self.assertEqual(res['output']['notification'][-1]['changed-attributes'][0]['new-value'], 'UNLOCKED')
        self.assertEqual(res['output']['notification'][-1]['changed-attributes'][1]['new-value'], 'ENABLED')
        time.sleep(2)

    def test_19_delete_connectivity_service_Ethernet(self):
        response = test_utils.tapi_delete_connectivity_request(str(self.uuid_services.eth))
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(self.WAITING)

    def test_20_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_21_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_22_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_23_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
