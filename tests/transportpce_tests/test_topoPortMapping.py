#!/usr/bin/env python

##############################################################################
#Copyright (c) 2017 Orange, Inc. and others.  All rights reserved.
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

class TransportPCEtesting(unittest.TestCase):

    testtools_process1 = None
    testtools_process2 = None
    odl_process = None
    restconf_baseurl = "http://localhost:8181/restconf"

#START_IGNORE_XTESTING

    @classmethod
    def __start_testtools(cls):
        executable = ("./netconf/netconf/tools/netconf-testtool/target/"
                      "netconf-testtool-1.5.0-executable.jar")
        if os.path.isfile(executable):
            if not os.path.exists("transportpce_tests/log"):
                os.makedirs("transportpce_tests/log")
            if os.path.isfile("./transportpce_tests/log/topoPortMap.log"):
                os.remove("transportpce_tests/log/topoPortMap.log")
            with open('transportpce_tests/log/testtools_ROADMA.log', 'w') as outfile1:
                cls.testtools_process1 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/openroadm/1.2.1/sample-config-ROADMA.xml","--starting-port","17831"],
                    stdout=outfile1)
            with open('transportpce_tests/log/testtools_XPDRA.log', 'w') as outfile2:
                cls.testtools_process2 = subprocess.Popen(
                    ["java", "-jar", executable, "--schemas-dir", "schemas",
                     "--initial-config-xml", "sample_configs/openroadm/1.2.1/sample-config-XPDRA.xml","--starting-port","17830"],
                    stdout=outfile2)

    @classmethod
    def __start_odl(cls):
        executable = "../karaf/target/assembly/bin/karaf"
        with open('transportpce_tests/log/odl.log', 'w') as outfile:
            cls.odl_process = subprocess.Popen(
                ["bash", executable, "server"], stdout=outfile,
                stdin=open(os.devnull))

    @classmethod
    def setUpClass(cls):
        cls.__start_testtools()
        cls.__start_odl()
        time.sleep(40)

    @classmethod
    def tearDownClass(cls):
        cls.testtools_process1.send_signal(signal.SIGINT)
        cls.testtools_process1.wait()
        cls.testtools_process2.send_signal(signal.SIGINT)
        cls.testtools_process2.wait()
        for child in psutil.Process(cls.odl_process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        cls.odl_process.send_signal(signal.SIGINT)
        cls.odl_process.wait()

    def setUp(self):
        time.sleep(10)

#END_IGNORE_XTESTING

    #Connect the ROADMA
    def test_01_connect_roadma(self):
        #Config ROADMA
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {"node": [{
             "node-id": "ROADMA",
             "netconf-node-topology:username": "admin",
             "netconf-node-topology:password": "admin",
             "netconf-node-topology:host": "127.0.0.1",
             "netconf-node-topology:port": "17831",
             "netconf-node-topology:tcp-only": "false",
             "netconf-node-topology:pass-through": {}}]}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "PUT", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.created)
        #seems sometimes to return 200 instead of 201
        #self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(10)

    #Verify the termination points of the ROADMA
    def test_02_compareOpenroadmTopologyPortMapping(self):
        #Verify the termination points related to the SRGs
        nbSrg=1
        for s in range(1,nbSrg+1):
            url_topo="{}/config/ietf-network:network/openroadm-topology/node/ROADMA-SRG"+`s`
            with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                outfile1.write('Config: '+`s`+' : '+url_topo+'\n')
            url = (url_topo.format(self.restconf_baseurl))
            headers = {'content-type': 'application/json'}
            response_topo = requests.request(
                "GET", url, headers=headers, auth=('admin', 'admin'))
            self.assertEqual(response_topo.status_code, requests.codes.ok)
            res_topo = response_topo.json()
            nbTP=len(res_topo['node'][0]['ietf-network-topology:termination-point'])
            for i in range(0,nbTP):
                tp_id=res_topo['node'][0]['ietf-network-topology:termination-point'][i]['tp-id']
                if(not "CP" in tp_id):
                    url_map="{}/config/transportpce-portmapping:network/nodes/ROADMA/mapping/"+tp_id
                    with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                        outfile1.write('Config: '+`i`+'/'+ `nbTP`+' : '+url_map+'\n')
                    url = (url_map.format(self.restconf_baseurl))
                    headers = {'content-type': 'application/json'}
                    response_portMap = requests.request(
                        "GET", url, headers=headers, auth=('admin', 'admin'))
                    self.assertEqual(response_portMap.status_code, requests.codes.ok)

        #Verify the termination points related to the degrees
        nbDeg=2
        for d in range(1,nbDeg+1):
            url_topo="{}/config/ietf-network:network/openroadm-topology/node/ROADMA-DEG"+`d`
            with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                outfile1.write(url_topo+'\n')
            url = (url_topo.format(self.restconf_baseurl))
            headers = {'content-type': 'application/json'}
            response_topo = requests.request(
                "GET", url, headers=headers, auth=('admin', 'admin'))
            self.assertEqual(response_topo.status_code, requests.codes.ok)
            res_topo = response_topo.json()
            nbTP=len(res_topo['node'][0]['ietf-network-topology:termination-point'])
            for i in range(0,nbTP):
                tp_id=res_topo['node'][0]['ietf-network-topology:termination-point'][i]['tp-id']
                if(not "CTP" in tp_id):
                    url_map ="{}/config/transportpce-portmapping:network/nodes/ROADMA/mapping/"+tp_id
                    with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                        outfile1.write('Config: '+`i`+'/'+ `nbTP`+' : '+url_map+'\n')
                    url = (url_map.format(self.restconf_baseurl))
                    headers = {'content-type': 'application/json'}
                    response_portMap = requests.request(
                        "GET", url, headers=headers, auth=('admin', 'admin'))
                    self.assertEqual(response_portMap.status_code, requests.codes.ok)
        time.sleep(1)

    #Disconnect the ROADMA
    def test_03_disconnect_device(self):
        url = ("{}/config/network-topology:"
                "network-topology/topology/topology-netconf/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/ROADMA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        time.sleep(5)

    #Connect the XPDRA
    def test_04_connect_xpdr(self):
         #Config XPDRA
         url = ("{}/config/network-topology:"
                 "network-topology/topology/topology-netconf/node/XPDRA"
                .format(self.restconf_baseurl))
         data = {"node": [{
              "node-id": "XPDRA",
              "netconf-node-topology:username": "admin",
              "netconf-node-topology:password": "admin",
              "netconf-node-topology:host": "127.0.0.1",
              "netconf-node-topology:port": "17830",
              "netconf-node-topology:tcp-only": "false",
              "netconf-node-topology:pass-through": {}}]}
         headers = {'content-type': 'application/json'}
         response = requests.request(
              "PUT", url, data=json.dumps(data), headers=headers,
              auth=('admin', 'admin'))
         self.assertEqual(response.status_code, requests.codes.created)
         #seems sometimes to return 200 instead of 201
         #self.assertEqual(response.status_code, requests.codes.ok)
         time.sleep(15)

    #Verify the termination points related to XPDR
    def test_05_compareOpenroadmTopologyPortMapping(self):
        nbXPDR=1
        for p in(1,nbXPDR+1):
            if(p > nbXPDR):
                break;
            url_topo = "{}/config/ietf-network:network/openroadm-topology/node/XPDRA-XPDR"+`p`
            with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                outfile1.write('Config: '+`p`+' : '+url_topo+'\n')
            url = (url_topo.format(self.restconf_baseurl))
            headers = {'content-type': 'application/json'}
            response_topo = requests.request(
                "GET", url, headers=headers, auth=('admin', 'admin'))
            self.assertEqual(response_topo.status_code, requests.codes.ok)
            res_topo = response_topo.json()
            nbTP=len(res_topo['node'][0]['ietf-network-topology:termination-point'])
            for i in range(0,nbTP):
                tp_id=res_topo['node'][0]['ietf-network-topology:termination-point'][i]['tp-id']
                url_map = "{}/config/transportpce-portmapping:network/nodes/XPDRA/mapping/"+tp_id
                with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                    outfile1.write('Config: '+`i`+'/'+ `nbTP`+' : '+url_map+'\n')
                url = url_map.format(self.restconf_baseurl)
                headers = {'content-type': 'application/json'}
                response_portMap = requests.request(
                    "GET", url, headers=headers, auth=('admin', 'admin'))
                self.assertEqual(response_portMap.status_code, requests.codes.ok)
                if("CLIENT" in tp_id):
                    #Verify the tail equipment id of the client
                    xpdr_client=res_topo['node'][0]['ietf-network-topology:termination-point'][i]["org-openroadm-network-topology:xpdr-client-attributes"]["tail-equipment-id"]
                    url_map = "{}/config/transportpce-portmapping:network/nodes/XPDRA/mapping/"+xpdr_client
                    with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                        outfile1.write('Config: '+`i`+'/'+ `nbTP`+' : '+xpdr_client+'\n')
                    url = url_map.format(self.restconf_baseurl)
                    headers = {'content-type': 'application/json'}
                    response_xpdrClient = requests.request(
                        "GET", url, headers=headers, auth=('admin', 'admin'))
                    self.assertEqual(response_xpdrClient.status_code, requests.codes.ok)
                if("NETWORK" in tp_id):
                    #Verify the tail equipment id of the network
                    xpdr_network=res_topo['node'][0]['ietf-network-topology:termination-point'][i]["org-openroadm-network-topology:xpdr-network-attributes"]["tail-equipment-id"]
                    url_map = "{}/config/transportpce-portmapping:network/nodes/XPDRA/mapping/"+xpdr_network
                    with open('./transportpce_tests/log/topoPortMap.log', 'a') as outfile1:
                        outfile1.write('Config: '+`i`+'/'+ `nbTP`+' : '+xpdr_network+'\n')
                    url = url_map.format(self.restconf_baseurl)
                    headers = {'content-type': 'application/json'}
                    response_xpdrNetwork = requests.request(
                        "GET", url, headers=headers, auth=('admin', 'admin'))
                    self.assertEqual(response_xpdrNetwork.status_code, requests.codes.ok)

    #Disconnect the XPDRA
    def test_06_disconnect_device(self):
        url = ("{}/config/network-topology:"
               "network-topology/topology/topology-netconf/node/XPDRA"
              .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
            "DELETE", url, data=json.dumps(data), headers=headers,
            auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)
        #Delete in the openroadm-network
        url = ("{}/config/ietf-network:network/openroadm-network/node/XPDRA"
               .format(self.restconf_baseurl))
        data = {}
        headers = {'content-type': 'application/json'}
        response = requests.request(
             "DELETE", url, data=json.dumps(data), headers=headers,
             auth=('admin', 'admin'))
        self.assertEqual(response.status_code, requests.codes.ok)


if __name__ == "__main__":
    #logging.basicConfig(filename='./transportpce_tests/log/response.log',filemode='w',level=logging.DEBUG)
    #logging.debug('I am there')
    unittest.main(verbosity=2)
