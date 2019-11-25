#!/usr/bin/env python

##############################################################################
# Copyright (c) 2019 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################

import json
import os
import psutil
import requests
import signal
import shutil
import subprocess
import time
import unittest
import logging
import test_utils


class TransportPCEtesting(unittest.TestCase):

    honeynode_process1 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

    @classmethod
    def setUpClass(cls):
        print ("starting honeynode1...")
        cls.honeynode_process1 = test_utils.start_spdra_honeynode()
        time.sleep(20)

        print ("starting opendaylight...")
        cls.odl_process = test_utils.start_tpce()
        time.sleep(60)
        print ("opendaylight started")

    @classmethod
    def tearDownClass(cls):
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()
        for child in psutil.Process(cls.honeynode_process1.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.honeynode_process1.send_signal(signal.SIGINT)
        cls.honeynode_process1.wait()

    def setUp(self):
        time.sleep(5)

    def test_01_connect_SPDR_SA1(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "SPDR-SA1",
             "netconf-node-topology:username": "admin",
             "netconf-node-topology:password": "admin",
             "netconf-node-topology:host": "127.0.0.1",
             "netconf-node-topology:port": "17845",
             "netconf-node-topology:tcp-only": "false",
             "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        time.sleep(10)
        url = ("{}/operational/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
               .format(self.restconf_baseurl))
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(
            res['node'][0]['netconf-node-topology:connection-status'],
            'connected')

    def test_02_getClliNetwork(self):
        url = ("{}/config/ietf-network:networks/network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        logging.info(res)
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'NodeSA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_03_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertEqual(res['network'][0]['node'][0]['node-id'], 'SPDR-SA1')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['network-ref'], 'clli-network')
        self.assertEqual(res['network'][0]['node'][0]['supporting-node'][0]['node-ref'], 'NodeSA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:node-type'], 'XPONDER')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:model'], 'universal-switchponder')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:vendor'], 'vendorA')
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-network:ip'], '1.2.3.4')

    def test_04_getLinks_OpenroadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        # Tests related to links
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_05_getNodes_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        # Tests related to nodes
        self.assertEqual(response.status_code, requests.codes.ok)
        self.assertIn('node', res['network'][0])
        if ('node' in res['network'][0]):
            nbNode = len(res['network'][0]['node'])
            self.assertEqual(nbNode, 2)
            listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2']
            for i in range(0, nbNode):
                nodeType = res['network'][0]['node'][i]['org-openroadm-network-topology:node-type']
                nodeId = res['network'][0]['node'][i]['node-id']
                if(nodeId == 'SPDR-SA1-XPDR1'):
                    self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                                  res['network'][0]['node'][i]['supporting-node'])
                    self.assertEqual(nodeType, 'XPONDER')
                    nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                    client = 0
                    network = 0
                    for j in range(0, nbTps):
                        tpType = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-network-topology:tp-type']
                        tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                        if (tpType == 'XPONDER-CLIENT'):
                            client += 1
                        elif (tpType == 'XPONDER-NETWORK'):
                            network += 1
                    self.assertTrue(client == 4)
                    self.assertTrue(network == 1)
                    listNode.remove(nodeId)
                if(nodeId == 'SPDR-SA1-XPDR2'):
                    self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                                  res['network'][0]['node'][i]['supporting-node'])
                    self.assertEqual(nodeType, 'XPONDER')
                    nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                    client = 0
                    network = 0
                    for j in range(0, nbTps):
                        tpType = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-network-topology:tp-type']
                        tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                        if (tpType == 'XPONDER-CLIENT'):
                            client += 1
                        elif (tpType == 'XPONDER-NETWORK'):
                            network += 1
                    self.assertTrue(client == 4)
                    self.assertTrue(network == 4)
                    listNode.remove(nodeId)
                else:
                    self.assertFalse(True)
            self.assertEqual(len(listNode), 0)

    def test_06_getLinks_OtnTopology(self):
        url = ("{}/config/ietf-network:networks/network/otn-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('ietf-network-topology:link', res['network'][0])

    def test_07_getNodes_OtnTopology(self):
        url = ("{}/config/ietf-network:networks/network/otn-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        self.assertEqual(response.status_code, requests.codes.ok)
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 2)
        listNode = ['SPDR-SA1-XPDR1', 'SPDR-SA1-XPDR2']
        for i in range(0, nbNode):
            nodeId = res['network'][0]['node'][i]['node-id']
            if(nodeId == 'SPDR-SA1-XPDR1'):
                nodeType = res['network'][0]['node'][i]['org-openroadm-otn-network-topology:node-type']
                self.assertEqual(nodeType, 'MUXPDR')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': 'SPDR-SA1-XPDR1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'], 1)
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-type']
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if (tpType == 'XPONDER-CLIENT'):
                        client += 1
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-supported-interfaces']['supported-interface-capability'][0]['if-cap-type'], 'org-openroadm-port-types:if-10GE-ODU2e')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']['rate'], 'org-openroadm-otn-common-types:ODU2e')
                    elif (tpType == 'XPONDER-NETWORK'):
                        network += 1
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-supported-interfaces']['supported-interface-capability'][0]['if-cap-type'], 'org-openroadm-port-types:if-OCH-OTU4-ODU4')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']['rate'], 'org-openroadm-otn-common-types:ODU4')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['network-ref'], 'openroadm-topology')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['node-ref'], 'SPDR-SA1-XPDR1')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['tp-ref'], 'XPDR1-NETWORK1')
                self.assertTrue(client == 4)
                self.assertTrue(network == 1)
                listNode.remove(nodeId)
                nbNbl = len(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list'])
                self.assertEqual(nbNbl, 4)
                for k in range(0, nbNbl):
                    nbl = res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list'][k]
                    if (nbl['nbl-number'] == 1):
                        self.assertEqual(nbl['available-interconnect-bandwidth'], 10)
                        self.assertEqual(nbl['interconnect-bandwidth-unit'], 1000000000)
                        self.assertIn('XPDR1-NETWORK1', nbl['tp-list'])
                        self.assertIn('XPDR1-CLIENT1', nbl['tp-list'])
                        self.assertNotIn('XPDR1-CLIENT2', nbl['tp-list'])
            elif(nodeId == 'SPDR-SA1-XPDR2'):
                nodeType = res['network'][0]['node'][i]['org-openroadm-otn-network-topology:node-type']
                self.assertEqual(nodeType, 'SWITCH')
                self.assertIn({'network-ref': 'openroadm-network', 'node-ref': 'SPDR-SA1'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertIn({'network-ref': 'openroadm-topology', 'node-ref': 'SPDR-SA1-XPDR2'},
                              res['network'][0]['node'][i]['supporting-node'])
                self.assertEqual(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:xpdr-attributes']['xpdr-number'], 2)
                nbTps = len(res['network'][0]['node'][i]['ietf-network-topology:termination-point'])
                client = 0
                network = 0
                for j in range(0, nbTps):
                    tpType = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-type']
                    tpId = res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['tp-id']
                    if (tpType == 'XPONDER-CLIENT'):
                        client += 1
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-supported-interfaces']['supported-interface-capability'][0]['if-cap-type'], 'org-openroadm-port-types:if-100GE-ODU4')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']['rate'], 'org-openroadm-otn-common-types:ODU4')
                    elif (tpType == 'XPONDER-NETWORK'):
                        network += 1
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:tp-supported-interfaces']['supported-interface-capability'][0]['if-cap-type'], 'org-openroadm-port-types:if-OCH-OTU4-ODU4')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['org-openroadm-otn-network-topology:xpdr-tp-port-connection-attributes']['rate'], 'org-openroadm-otn-common-types:ODU4')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['network-ref'], 'openroadm-topology')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['node-ref'], 'SPDR-SA1-XPDR2')
                        self.assertEqual(res['network'][0]['node'][i]['ietf-network-topology:termination-point'][j]['supporting-termination-point'][0]['tp-ref'], tpId)
                self.assertTrue(client == 4)
                self.assertTrue(network == 4)
                listNode.remove(nodeId)
                nbNbl = len(res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list'])
                self.assertEqual(nbNbl, 1)
                nbl = res['network'][0]['node'][i]['org-openroadm-otn-network-topology:switching-pools']['odu-switching-pools'][0]['non-blocking-list'][0]
                self.assertIn('XPDR2-NETWORK1', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT1', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK2', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT2', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK3', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT3', nbl['tp-list'])
                self.assertIn('XPDR2-NETWORK4', nbl['tp-list'])
                self.assertIn('XPDR2-CLIENT4', nbl['tp-list'])
            else:
                self.assertFalse(True)
        self.assertEqual(len(listNode), 0)

    def test_08_disconnect_SPDR_SA1(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/SPDR-SA1"
              .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)

    def test_09_getClliNetwork(self):
        url = ("{}/config/ietf-network:networks/network/clli-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        nbNode = len(res['network'][0]['node'])
        self.assertEqual(nbNode, 1)
        self.assertEqual(res['network'][0]['node'][0]['org-openroadm-clli-network:clli'], 'NodeSA')

    def test_10_getOpenRoadmNetwork(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-network"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_11_getNodes_OpenRoadmTopology(self):
        url = ("{}/config/ietf-network:networks/network/openroadm-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        self.assertNotIn('node', res['network'][0])

    def test_12_getNodes_OtnTopology(self):
        url = ("{}/config/ietf-network:networks/network/otn-topology"
              .format(self.restconf_baseurl))
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "GET", url, headers=headers, auth=('admin', 'admin'))
        res = response.json()
        self.assertNotIn('node', res['network'][0])


if __name__ == "__main__":
    unittest.main(verbosity=2)
