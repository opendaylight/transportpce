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
import test_utils_rfc8040  # nopep8


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
    cr_serv_input_data = {
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

    tapi_serv_details = {
        "tapi-connectivity:input": {
            "tapi-connectivity:service-id-or-name": "TBD"}}

    cr_notif_subs_input_data = {
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

    cr_get_notif_list_input_data = {
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
        cls.processes = test_utils_rfc8040.start_tpce()
        # NBI notification feature is not installed by default in Karaf
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing NBI notification feature...")
            result = test_utils_rfc8040.install_karaf_feature("odl-transportpce-nbinotifications")
            if result.returncode != 0:
                cls.init_failed_nbi = True
            print("installing tapi feature...")
            result = test_utils_rfc8040.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed_tapi = True
            print("Restarting OpenDaylight...")
            test_utils_rfc8040.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils_rfc8040.start_karaf()
            test_utils_rfc8040.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils_rfc8040.wait_until_log_contains(
                test_utils_rfc8040.KARAF_LOG, test_utils_rfc8040.KARAF_OK_START_MSG, time_to_wait=60)
        if cls.init_failed_nbi:
            print("NBI notification installation feature failed...")
            test_utils_rfc8040.shutdown_process(cls.processes[0])
            sys.exit(2)
        if cls.init_failed_tapi:
            print("tapi installation feature failed...")
            test_utils_rfc8040.shutdown_process(cls.processes[0])
            sys.exit(2)
        cls.processes = test_utils_rfc8040.start_sims([('xpdra', cls.NODE_VERSION_221),
                                                       ('roadma', cls.NODE_VERSION_221),
                                                       ('roadmc', cls.NODE_VERSION_221),
                                                       ('xpdrc', cls.NODE_VERSION_221)])

    @classmethod
    def tearDownClass(cls):
        # pylint: disable=not-an-iterable
        for process in cls.processes:
            test_utils_rfc8040.shutdown_process(process)
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        # pylint: disable=consider-using-f-string
        print("execution of {}".format(self.id().split(".")[-1]))

    def test_01_connect_xpdrA(self):
        response = test_utils_rfc8040.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils_rfc8040.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_03_connect_rdmA(self):
        response = test_utils_rfc8040.mount_device("ROADM-A1", ('roadma', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_04_connect_rdmC(self):
        response = test_utils_rfc8040.mount_device("ROADM-C1", ('roadmc', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils_rfc8040.CODE_SHOULD_BE_201)

    def test_05_connect_xprdA_N1_to_roadmA_PP1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_06_connect_roadmA_PP1_to_xpdrA_N1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-rdm-xpdr-links',
            {'links-input': {'xpdr-node': 'XPDR-A1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-A1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Roadm Xponder links created successfully', response["output"]["result"])
        time.sleep(2)

    def test_07_connect_xprdC_N1_to_roadmC_PP1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
            'transportpce-networkutils', 'init-xpdr-rdm-links',
            {'links-input': {'xpdr-node': 'XPDR-C1', 'xpdr-num': '1', 'network-num': '1',
                             'rdm-node': 'ROADM-C1', 'srg-num': '1', 'termination-point-num': 'SRG1-PP1-TXRX'}})
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Xponder Roadm Link created successfully', response["output"]["result"])
        time.sleep(2)

    def test_08_connect_roadmC_PP1_to_xpdrC_N1(self):
        response = test_utils_rfc8040.transportpce_api_rpc_request(
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
        response = test_utils_rfc8040.add_oms_attr_request(
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
        response = test_utils_rfc8040.add_oms_attr_request(
            "ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    # test service-create for Eth service from xpdr to xpdr
    def test_11_create_connectivity_service_Ethernet(self):
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'create-connectivity-service', self.cr_serv_input_data)
        time.sleep(self.WAITING)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.eth = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string

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

        self.assertDictEqual(dict(input_dict_1, **response['output']['service']),
                             response['output']['service'])
        self.assertDictEqual(dict(input_dict_2, **response['output']['service']['end-point'][0]['name'][0]),
                             response['output']['service']['end-point'][0]['name'][0])
        self.assertDictEqual(dict(input_dict_3, **response['output']['service']['end-point'][1]['name'][0]),
                             response['output']['service']['end-point'][1]['name'][0])
        # If the gate fails is because of the waiting time not being enough
        time.sleep(self.WAITING)

    def test_12_get_service_Ethernet(self):
        response = test_utils_rfc8040.get_ordm_serv_list_attr_request("services", str(self.uuid_services.eth))
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], str(self.uuid_services.eth))
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')
        time.sleep(1)

    def test_13_get_connectivity_service_Ethernet(self):
        self.tapi_serv_details["tapi-connectivity:input"]["tapi-connectivity:service-id-or-name"] =\
            str(self.uuid_services.eth)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'get-connectivity-service-details', self.tapi_serv_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['service']['operational-state'], 'ENABLED')
        self.assertEqual(response['output']['service']['name'][0]['value'], self.uuid_services.eth)
        self.assertEqual(response['output']['service']['administrative-state'], 'UNLOCKED')
        self.assertEqual(response['output']['service']['lifecycle-state'], 'INSTALLED')

    def test_14_create_notifications_subscription_service(self):
        self.cr_notif_subs_input_data["input"]["subscription-filter"]["requested-object-identifier"][0] =\
            str(self.uuid_services.eth)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-notification', 'create-notification-subscription-service', self.cr_notif_subs_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_subscriptions.eth = response['output']['subscription-service']['uuid']
        self.assertEqual(response['output']['subscription-service']['subscription-filter']
                         ['requested-object-types'][0], 'CONNECTIVITY_SERVICE')
        self.assertEqual(response['output']['subscription-service']['subscription-filter']
                         ['requested-notification-types'][0], 'ALARM_EVENT')
        self.assertEqual(response['output']['subscription-service']['subscription-filter']
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
                                    data=json.dumps(body), headers=test_utils_rfc8040.TYPE_APPLICATION_JSON,
                                    auth=(test_utils_rfc8040.ODL_LOGIN, test_utils_rfc8040.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        # If the gate fails is because of the waiting time not being enough
        time.sleep(2)

    def test_16_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_input_data["input"]["subscription-id-or-name"] = str(self.uuid_subscriptions.eth)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-notification', 'get-notification-list', self.cr_get_notif_list_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notification'][0]['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(response['output']['notification'][0]['target-object-type'], 'CONNECTIVITY_SERVICE')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][0]['new-value'], 'LOCKED')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][1]['new-value'], 'DISABLED')
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
                                    data=json.dumps(body), headers=test_utils_rfc8040.TYPE_APPLICATION_JSON,
                                    auth=(test_utils_rfc8040.ODL_LOGIN, test_utils_rfc8040.ODL_PWD))
        self.assertEqual(response.status_code, requests.codes.ok)
        # If the gate fails is because of the waiting time not being enough
        time.sleep(2)

    def test_18_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_input_data["input"]["subscription-id-or-name"] = str(self.uuid_subscriptions.eth)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-notification', 'get-notification-list', self.cr_get_notif_list_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notification'][1]['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(response['output']['notification'][1]['target-object-type'], 'CONNECTIVITY_SERVICE')
        self.assertEqual(response['output']['notification'][1]['changed-attributes'][0]['new-value'], 'UNLOCKED')
        self.assertEqual(response['output']['notification'][1]['changed-attributes'][1]['new-value'], 'ENABLED')
        time.sleep(2)

    def test_19_delete_connectivity_service_Ethernet(self):
        self.tapi_serv_details["tapi-connectivity:input"]["tapi-connectivity:service-id-or-name"] =\
            str(self.uuid_services.eth)
        response = test_utils_rfc8040.tapi_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.tapi_serv_details)
        self.assertEqual(response['status_code'], requests.codes.ok)
        time.sleep(self.WAITING)

    def test_20_disconnect_XPDRA(self):
        response = test_utils_rfc8040.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_21_disconnect_XPDRC(self):
        response = test_utils_rfc8040.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_22_disconnect_ROADMA(self):
        response = test_utils_rfc8040.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.no_content)

    def test_23_disconnect_ROADMC(self):
        response = test_utils_rfc8040.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.no_content)


if __name__ == "__main__":
    unittest.main(verbosity=2)
