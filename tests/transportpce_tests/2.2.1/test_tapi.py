#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################


import signal
import time
import unittest

import psutil
import requests

import test_utils

RESTCONF_BASE_URL = "http://localhost:8181/restconf"

CODE_SHOULD_BE_200 = 'Http status code should be 200'

CODE_SHOULD_BE_201 = 'Http status code should be 201'

CREATED_SUCCESSFULLY = 'Result message should contain Xponder Roadm Link created successfully'


class TransportTapitesting(unittest.TestCase):
    honeynode_process1 = None
    honeynode_process2 = None
    honeynode_process3 = None
    honeynode_process4 = None
    honeynode_process5 = None

    # START_IGNORE_XTESTING

    @classmethod
    def setUpClass(cls):

        print("starting XPDRA...")
        cls.honeynode_process1 = test_utils.start_xpdra_honeynode()
        time.sleep(20)

        print("starting ROADMA...")
        cls.honeynode_process2 = test_utils.start_roadma_honeynode()
        time.sleep(20)

        print("starting ROADMC...")
        cls.honeynode_process3 = test_utils.start_roadmc_honeynode()
        time.sleep(20)

        print("starting XPDRC...")
        cls.honeynode_process4 = test_utils.start_xpdrc_honeynode()
        time.sleep(20)

        print("starting SPDRA...")
        cls.honeynode_process5 = test_utils.start_spdra_honeynode()
        time.sleep(20)
        print("all honeynodes started")

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.honeynode_process1.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process1.send_signal(signal.SIGINT)
        cls.honeynode_process1.wait()
        for child in psutil.Process(cls.honeynode_process2.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process2.send_signal(signal.SIGINT)
        cls.honeynode_process2.wait()
        for child in psutil.Process(cls.honeynode_process3.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process3.send_signal(signal.SIGINT)
        cls.honeynode_process3.wait()
        for child in psutil.Process(cls.honeynode_process4.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process4.send_signal(signal.SIGINT)
        cls.honeynode_process4.wait()
        for child in psutil.Process(cls.honeynode_process5.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process5.send_signal(signal.SIGINT)
        cls.honeynode_process5.wait()
        print("all processes killed")

    def setUp(self):  # instruction executed before each test method
        print("execution of {}".format(self.id().split(".")[-1]))

    # END_IGNORE_XTESTING

    #  connect netconf devices
    def test_00_connect_spdr_sa1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("SPDR-SA1", "17845")
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_01_connect_xpdra(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("XPDR-A1", "17840")
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_02_connect_xpdrc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-C1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("XPDR-C1", "17844")
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(10)

    def test_03_connect_rdma(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("ROADM-A1", "17841")
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(20)

    def test_04_connect_rdmc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-C1"
               .format(RESTCONF_BASE_URL))
        data = test_utils.generate_connect_data("ROADM-C1", "17843")
        response = test_utils.put_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.created, CODE_SHOULD_BE_201)  # pylint: disable=no-member
        time.sleep(20)

    def test_05_connect_xprda_n1_to_roadma_pp1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "1", "ROADM-A1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_06_connect_roadma_pp1_to_xpdra_n1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "1", "ROADM-A1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_07_connect_xprdc_n1_to_roadmc_pp1(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "1", "ROADM-C1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_08_connect_roadmc_pp1_to_xpdrc_n1(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "1", "ROADM-C1", "1", "SRG1-PP1-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_18_connect_xprda_n2_to_roadma_pp2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "2", "ROADM-A1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_19_connect_roadma_pp2_to_xpdra_n2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-A1", "1", "2", "ROADM-A1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_20_connect_xprdc_n2_to_roadmc_pp2(self):
        url = "{}/operations/transportpce-networkutils:init-xpdr-rdm-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "2", "ROADM-C1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Xponder Roadm Link created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_21_connect_roadmc_pp2_to_xpdrc_n2(self):
        url = "{}/operations/transportpce-networkutils:init-rdm-xpdr-links".format(RESTCONF_BASE_URL)
        data = test_utils.generate_link_data("XPDR-C1", "1", "2", "ROADM-C1", "1", "SRG1-PP2-TXRX")
        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertIn('Roadm Xponder links created successfully', res["output"]["result"],
                      CREATED_SUCCESSFULLY)
        time.sleep(2)

    def test_22_get_tapi_openroadm_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details".format(RESTCONF_BASE_URL)
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "openroadm-topology"
            }
        }

        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 1, 'There should be 1 node')
        self.assertEqual(len(res["output"]["topology"]["node"][0]["owned-node-edge-point"]), 4,
                         'There should be 4 owned-node-edge-point')
        time.sleep(2)

    def test_23_get_tapi_otn_topology(self):
        url = "{}/operations/tapi-topology:get-topology-details".format(RESTCONF_BASE_URL)
        data = {
            "tapi-topology:input": {
                "tapi-topology:topology-id-or-name": "otn-topology"
            }
        }

        response = test_utils.post_request(url, data, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        res = response.json()
        self.assertEqual(len(res["output"]["topology"]["node"]), 4, 'There should be 4 node')
        time.sleep(2)

    def test_24_disconnect_xpdra(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-A1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_25_disconnect_xpdrc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDR-C1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_26_disconnect_roadma(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-A1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_27_disconnect_roadmc(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/ROADM-C1"
               .format(RESTCONF_BASE_URL))

        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member
        time.sleep(10)

    def test_28_disconnect_spdr_sa1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(RESTCONF_BASE_URL))
        response = test_utils.delete_request(url, 'admin', 'admin')
        self.assertEqual(response.status_code, requests.codes.ok, CODE_SHOULD_BE_200)  # pylint: disable=no-member


if __name__ == "__main__":
    unittest.main(verbosity=2)
