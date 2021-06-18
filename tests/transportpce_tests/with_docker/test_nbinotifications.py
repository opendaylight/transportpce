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
import sys
import unittest
import time
import requests
sys.path.append('transportpce_tests/common/')
import test_utils


class TransportNbiNotificationstesting(unittest.TestCase):
    processes = None
    cr_serv_sample_data = {"input": {
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
            "tx-direction": {
                "port": {
                    "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Tx.ge-5/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.3",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "ROUTER_SNJSCAMCJP8_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Rx.ge-5/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJP8_000000.00_00",
                    "lgx-port-name": "LGX Back.4",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "service-z-end": {
            "service-rate": "100",
            "node-id": "XPDR-C1",
            "service-format": "Ethernet",
            "clli": "SNJSCAMCJT4",
            "tx-direction": {
                "port": {
                    "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Tx.ge-1/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                    "lgx-port-name": "LGX Back.29",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "rx-direction": {
                "port": {
                    "port-device-name": "ROUTER_SNJSCAMCJT4_000000.00_00",
                    "port-type": "router",
                    "port-name": "Gigabit Ethernet_Rx.ge-1/0/0.0",
                    "port-rack": "000000.00",
                    "port-shelf": "00"
                },
                "lgx": {
                    "lgx-device-name": "LGX Panel_SNJSCAMCJT4_000000.00_00",
                    "lgx-port-name": "LGX Back.30",
                    "lgx-port-rack": "000000.00",
                    "lgx-port-shelf": "00"
                }
            },
            "optic-type": "gray"
        },
        "due-date": "2016-11-28T00:00:01Z",
        "operator-contact": "pw1234"
    }
    }

    WAITING = 20  # nominal value is 300
    NODE_VERSION = '2.2.1'

    @classmethod
    def setUpClass(cls):
        # TODO: for lighty manage the activation of NBI notification feature
        cls.init_failed = False
        cls.processes = test_utils.start_tpce()
        # NBI notification feature is not installed by default in Karaf
        if "USE_LIGHTY" not in os.environ or os.environ['USE_LIGHTY'] != 'True':
            print("installing NBI notification feature...")
            result = test_utils.install_karaf_feature("odl-transportpce-nbinotifications")
            if result.returncode != 0:
                cls.init_failed = True
            print("Restarting OpenDaylight...")
            test_utils.shutdown_process(cls.processes[0])
            cls.processes[0] = test_utils.start_karaf()
            test_utils.process_list[0] = cls.processes[0]
            cls.init_failed = not test_utils.wait_until_log_contains(
                test_utils.KARAF_LOG, test_utils.KARAF_OK_START_MSG, time_to_wait=60)
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

    def test_09_get_notifications_service1(self):
        data = {
            "input": {
                "connection-type": "service",
                "id-consumer": "consumer",
                "group-id": "transportpceTest"
            }
        }
        response = test_utils.get_notifications_service_request(data)
        self.assertEqual(response.status_code, requests.codes.no_content)
        time.sleep(2)

    def test_10_create_eth_service1(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_11_get_notifications_service1(self):
        data = {
            "input": {
                "connection-type": "service",
                "id-consumer": "consumer",
                "group-id": "transportpceTest"
            }
        }
        response = test_utils.get_notifications_service_request(data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['notification-service'][-1]['service-name'], 'service1')
        self.assertEqual(res['output']['notification-service'][-1]['connection-type'], 'service')
        self.assertEqual(res['output']['notification-service'][-1]['message'], 'ServiceCreate request failed ...')
        self.assertEqual(res['output']['notification-service'][-1]['response-failed'],
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
        response = test_utils.add_oms_attr_request("ROADM-A1-DEG2-DEG2-TTP-TXRXtoROADM-C1-DEG1-DEG1-TTP-TXRX", data)
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
        response = test_utils.add_oms_attr_request("ROADM-C1-DEG1-DEG1-TTP-TXRXtoROADM-A1-DEG2-DEG2-TTP-TXRX", data)
        self.assertEqual(response.status_code, requests.codes.created)

    # test service-create for Eth service from xpdr to xpdr
    def test_14_create_eth_service1(self):
        self.cr_serv_sample_data["input"]["service-name"] = "service1"
        response = test_utils.service_create_request(self.cr_serv_sample_data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('PCE calculation in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(self.WAITING)

    def test_15_get_eth_service1(self):
        response = test_utils.get_service_list_request("services/service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['services'][0]['administrative-state'], 'inService')
        self.assertEqual(
            res['services'][0]['service-name'], 'service1')
        self.assertEqual(
            res['services'][0]['connection-type'], 'service')
        self.assertEqual(
            res['services'][0]['lifecycle-state'], 'planned')
        time.sleep(2)

    def test_16_get_notifications_service1(self):
        data = {
            "input": {
                "connection-type": "service",
                "id-consumer": "consumer",
                "group-id": "transportpceTest"
            }
        }
        response = test_utils.get_notifications_service_request(data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['notification-service'][-1]['service-name'], 'service1')
        self.assertEqual(res['output']['notification-service'][-1]['connection-type'], 'service')
        self.assertEqual(res['output']['notification-service'][-1]['message'], 'Service implemented !')
        time.sleep(2)

    def test_17_delete_eth_service1(self):
        response = test_utils.service_delete_request("service1")
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertIn('Renderer service delete in progress',
                      res['output']['configuration-response-common']['response-message'])
        time.sleep(20)

    def test_18_get_notifications_service1(self):
        data = {
            "input": {
                "connection-type": "service",
                "id-consumer": "consumer",
                "group-id": "transportpceTest"
            }
        }
        response = test_utils.get_notifications_service_request(data)
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['output']['notification-service'][-1]['service-name'], 'service1')
        self.assertEqual(res['output']['notification-service'][-1]['connection-type'], 'service')
        self.assertEqual(res['output']['notification-service'][-1]['message'], 'Service deleted !')
        time.sleep(2)

    def test_19_disconnect_XPDRA(self):
        response = test_utils.unmount_device("XPDR-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_20_disconnect_XPDRC(self):
        response = test_utils.unmount_device("XPDR-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_21_disconnect_ROADMA(self):
        response = test_utils.unmount_device("ROADM-A1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)

    def test_22_disconnect_ROADMC(self):
        response = test_utils.unmount_device("ROADM-C1")
        self.assertEqual(response.status_code, requests.codes.ok, test_utils.CODE_SHOULD_BE_200)


if __name__ == "__main__":
    unittest.main(verbosity=2)
