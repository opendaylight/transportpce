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


class TransportNbiNotificationstesting(unittest.TestCase):
    processes = []
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
            "node-id": "XPDR-A1",
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

    nbi_notif_input_data = {
        "connection-type": "service",
        "id-consumer": "consumer",
        "group-id": "transportpceTest"
    }

    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        # pylint: disable=unsubscriptable-object
        # TODO: for lighty manage the activation of NBI notification feature
        cls.init_failed = False
        cls.processes = test_utils.start_tpce()
        # NBI notification feature is not installed by default in Karaf
        if "NO_ODL_STARTUP" not in os.environ or "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing NBI notification feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-nbinotifications")
            if result.returncode != 0:
                cls.init_failed = True
        if cls.init_failed:
            print("NBI notification installation feature failed...")
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
        self.assertEqual(response.status_code,
                         requests.codes.created, test_utils.CODE_SHOULD_BE_201)

    def test_02_connect_xpdrC(self):
        response = test_utils.mount_device("XPDR-C1", ('xpdrc', self.NODE_VERSION))
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

    def test_09_get_notifications_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-process-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.no_content)
        time.sleep(2)

    def test_10_create_eth_service1(self):
        self.cr_serv_input_data["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create', self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_11_get_notifications_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-process-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notifications-process-service'][-1]['service-name'], 'service1')
        self.assertEqual(response['output']['notifications-process-service'][-1]['connection-type'], 'service')
        self.assertEqual(response['output']['notifications-process-service'][-1]['message'],
                         'ServiceCreate request failed ...')
        self.assertEqual(response['output']['notifications-process-service'][-1]['response-failed'],
                         'PCE path computation failed !')
        time.sleep(2)

    def test_12_add_omsAttributes_ROADMA_ROADMC(self):
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

    def test_13_add_omsAttributes_ROADMC_ROADMA(self):
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
    def test_14_create_eth_service1(self):
        self.cr_serv_input_data["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-create', self.cr_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('PCE calculation in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_15_get_eth_service1(self):
        response = test_utils.get_ordm_serv_list_attr_request("services", "service1")
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['services'][0]['administrative-state'], 'inService')
        self.assertEqual(response['services'][0]['service-name'], 'service1')
        self.assertEqual(response['services'][0]['connection-type'], 'service')
        self.assertEqual(response['services'][0]['lifecycle-state'], 'planned')

    def test_16_get_notifications_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-process-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notifications-process-service'][-1]['service-name'], 'service1')
        self.assertEqual(response['output']['notifications-process-service'][-1]['connection-type'], 'service')
        self.assertEqual(response['output']['notifications-process-service'][-1]['message'], 'Service implemented !')

    def test_17_get_notifications_alarm_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-alarm-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['service-name'], 'service1')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['connection-type'], 'service')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['operational-state'], 'inService')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['message'],
                         'The service is now inService')

    def test_18_change_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "outOfService",
            "port-qual": "roadm-external"
        }))
        time.sleep(5)

    def test_19_get_notifications_alarm_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-alarm-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['service-name'], 'service1')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['connection-type'], 'service')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['operational-state'], 'outOfService')
        self.assertEqual(response['output']['notifications-alarm-service'][-1]['message'],
                         'The service is now outOfService')

    def test_20_restore_status_port_roadma_srg(self):
        self.assertTrue(test_utils.sims_update_cp_port(('roadma', self.NODE_VERSION), '3/0', 'C1',
                                                       {
            "port-name": "C1",
            "logical-connection-point": "SRG1-PP1",
            "port-type": "client",
            "circuit-id": "SRG1",
            "administrative-state": "inService",
            "port-qual": "roadm-external"
        }))
        time.sleep(5)

    def test_21_get_notifications_alarm_service1(self):
        self.test_17_get_notifications_alarm_service1()

    def test_22_delete_eth_service1(self):
        self.del_serv_input_data["service-delete-req-info"]["service-name"] = "service1"
        response = test_utils.transportpce_api_rpc_request(
            'org-openroadm-service', 'service-delete', self.del_serv_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertIn('Renderer service delete in progress',
                      response['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_23_get_notifications_service1(self):
        response = test_utils.transportpce_api_rpc_request(
            'nbi-notifications', 'get-notifications-process-service', self.nbi_notif_input_data)
        self.assertEqual(response['status_code'], requests.codes.ok)
        self.assertEqual(response['output']['notifications-process-service'][-1]['service-name'], 'service1')
        self.assertEqual(response['output']['notifications-process-service'][-1]['connection-type'], 'service')
        self.assertEqual(response['output']['notifications-process-service'][-1]['message'], 'Service deleted !')
        time.sleep(2)

    def test_24_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_25_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_26_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))

    def test_27_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertIn(response.status_code, (requests.codes.ok, requests.codes.no_content))


if __name__ == "__main__":
    unittest.main(verbosity=2)
