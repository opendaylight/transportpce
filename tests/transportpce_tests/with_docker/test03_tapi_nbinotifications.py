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
            "service-type": "POINT_TO_POINT_CONNECTIVITY",
            "service-level": "Some service-level",
            "requested-capacity": {
                "total-size": {
                    "value": "100",
                    "unit": "tapi-common:CAPACITY_UNIT_GBPS"
                }
            }
        },
        "state": "LOCKED",
        "layer-protocol-name": "ETH"
    }

    tapi_serv_details = {"uuid": "TBD"}

    cr_notif_subs_input_data = {
        "subscription-filter": {
            "requested-notification-types": [
                "NOTIFICATION_TYPE_ATTRIBUTE_VALUE_CHANGE"
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

    cr_get_notif_list_input_data = {
        "subscription-id": "c07e7fd1-0377-4fbf-8928-36c17b0d0d68",
    }

    processes = []
    uuid_services = UuidServices()
    uuid_subscriptions = UuidSubscriptions()
    WAITING = 20  # nominal value is 300
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
        if "NO_ODL_STARTUP" not in os.environ or "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing NBI notification feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-nbinotifications")
            if result.returncode != 0:
                cls.init_failed_nbi = True
            print("installing tapi feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-tapi")
            if result.returncode != 0:
                cls.init_failed_tapi = True
        if cls.init_failed_nbi:
            print("NBI notification installation feature failed...")
            test_utils.shutdown_process(cls.processes[0])
            sys.exit(2)
        if cls.init_failed_tapi:
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

    def setUp(self):  # instruction executed before each test method
        print(f'execution of {self.id().split(".")[-1]}')

    def test_01_connect_xpdrA(self):
        response = test_utils.mount_device("XPDR-A1", ('xpdra', self.NODE_VERSION_221))
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION_221))
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
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_services.eth = response['output']['service']['uuid']
        # pylint: disable=consider-using-f-string

        input_dict_1 = {'administrative-state': 'LOCKED',
                        'lifecycle-state': 'PLANNED',
                        'operational-state': 'DISABLED',
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

    def test_14_create_notifications_subscription_service(self):
        self.cr_notif_subs_input_data["subscription-filter"]["requested-object-identifier"][0] =\
            str(self.uuid_services.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-notification', 'create-notification-subscription-service', self.cr_notif_subs_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.uuid_subscriptions.eth = response['output']['subscription-service']['uuid']
        self.assertEqual(response['output']['subscription-service']['subscription-filter'][0]
                         ['requested-notification-types'][0],
                         'tapi-notification:NOTIFICATION_TYPE_ATTRIBUTE_VALUE_CHANGE')
        self.assertEqual(response['output']['subscription-service']['subscription-filter'][0]
                         ['requested-object-identifier'][0], str(self.uuid_services.eth))
        time.sleep(2)

    def test_15_change_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "outOfService",
            "port-qual": "roadm-external"
        }))
        time.sleep(5)

    def test_16_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_input_data["subscription-id"] = str(self.uuid_subscriptions.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-notification', 'get-notification-list', self.cr_get_notif_list_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notification'][0]['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][0]['old-value'], 'UNLOCKED')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][0]['new-value'], 'LOCKED')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][1]['value-name'],
                         'operationalState')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][1]['old-value'], 'ENABLED')
        self.assertEqual(response['output']['notification'][0]['changed-attributes'][1]['new-value'], 'DISABLED')
        time.sleep(2)

    def test_17_restore_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION_221), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "inService",
            "port-qual": "roadm-external"
        }))
        time.sleep(5)

    def test_18_get_tapi_notifications_connectivity_service_Ethernet(self):
        self.cr_get_notif_list_input_data["subscription-id"] = str(self.uuid_subscriptions.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-notification', 'get-notification-list', self.cr_get_notif_list_input_data)
        if response['output']['notification'][0]['event-time-stamp'] >\
                response['output']['notification'][1]['event-time-stamp']:
            new_notif = response['output']['notification'][0]
        else:
            new_notif = response['output']['notification'][1]
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(new_notif['target-object-identifier'], str(self.uuid_services.eth))
        self.assertEqual(new_notif['changed-attributes'][0]['value-name'], 'administrativeState')
        self.assertEqual(new_notif['changed-attributes'][0]['old-value'], 'LOCKED')
        self.assertEqual(new_notif['changed-attributes'][0]['new-value'], 'UNLOCKED')
        self.assertEqual(new_notif['changed-attributes'][1]['value-name'], 'operationalState')
        self.assertEqual(new_notif['changed-attributes'][1]['old-value'], 'DISABLED')
        self.assertEqual(new_notif['changed-attributes'][1]['new-value'], 'ENABLED')
        time.sleep(2)

    def test_19_delete_connectivity_service_Ethernet(self):
        self.tapi_serv_details["uuid"] = str(self.uuid_services.eth)
        response = test_utils.transportpce_api_rpc_request(
            'tapi-connectivity', 'delete-connectivity-service', self.tapi_serv_details)
        self.assertIn(response['status_code'], (requests.codes.ok, requests.codes.no_content))
        time.sleep(self.WAITING)

    def test_20_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_21_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_22_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_23_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
