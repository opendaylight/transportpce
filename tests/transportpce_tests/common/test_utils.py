#!/usr/bin/env python
##############################################################################
# Copyright (c) 2020 Orange, Inc. and others.  All rights reserved.
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Apache License, Version 2.0
# which accompanies this distribution, and is available at
# http://www.apache.org/licenses/LICENSE-2.0
##############################################################################
import json
import os
import re
import signal
import subprocess

import psutil
import requests

import simulators

sims = simulators.sims
honeynode_executable = simulators.honeynode_executable
samples_directory = simulators.samples_directory

HONEYNODE_OK_START_MSG = "Netconf SSH endpoint started successfully at 0.0.0.0"
KARAF_OK_START_MSG = re.escape(
    "Blueprint container for bundle org.opendaylight.netconf.restconf")+".* was successfully created"


RESTCONF_BASE_URL = "http://localhost:8181/restconf"
ODL_LOGIN = "admin"
ODL_PWD = "admin"
NODES_LOGIN = "admin"
NODES_PWD = "admin"

TYPE_APPLICATION_JSON = {'Content-Type': 'application/json', 'Accept': 'application/json'}
TYPE_APPLICATION_XML = {'Content-Type': 'application/xml', 'Accept': 'application/xml'}

CODE_SHOULD_BE_200 = 'Http status code should be 200'
CODE_SHOULD_BE_201 = 'Http status code should be 201'

log_directory = os.path.dirname(os.path.realpath(__file__))

karaf_log = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "..", "karaf", "target", "assembly", "data", "log", "karaf.log")

process_list = []


def start_sims(sims_list):
    for sim in sims_list:
        print("starting simulator for " + sim + "...")
        log_file = os.path.join(log_directory, sims[sim]['logfile'])
        process = start_honeynode(log_file, sims[sim]['port'], sims[sim]['configfile'])
        if wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100):
            print("simulator for " + sim + " started")
        else:
            print("simulator for " + sim + " failed to start")
            shutdown_process(process)
            for pid in process_list:
                shutdown_process(pid)
            exit(3)
        process_list.append(process)
    return process_list


def start_tpce():
    print("starting OpenDaylight...")
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        process = start_lighty()
        # TODO: add some sort of health check similar to Karaf below
    else:
        process = start_karaf()
        if wait_until_log_contains(karaf_log, KARAF_OK_START_MSG, time_to_wait=60):
            print("OpenDaylight started !")
        else:
            print("OpenDaylight failed to start !")
            shutdown_process(process)
            for pid in process_list:
                shutdown_process(pid)
            exit(1)
    process_list.append(process)
    return process_list


def start_karaf():
    print("starting KARAF TransportPCE build...")
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "karaf", "target", "assembly", "bin", "karaf")
    with open('odl.log', 'w') as outfile:
        return subprocess.Popen(
            ["sh", executable, "server"], stdout=outfile, stderr=outfile, stdin=None)


def start_lighty():
    print("starting LIGHTY.IO TransportPCE build...")
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "lighty", "target", "tpce",
        "clean-start-controller.sh")
    with open('odl.log', 'w') as outfile:
        return subprocess.Popen(
            ["sh", executable], stdout=outfile, stderr=outfile, stdin=None)


def install_karaf_feature(feature_name: str):
    print("installing feature " + feature_name)
    executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "..", "karaf", "target", "assembly", "bin", "client")
    return subprocess.run([executable],
                          input='feature:install ' + feature_name + '\n feature:list | grep tapi \n logout \n',
                          universal_newlines=True)


def post_request(url, data, username, password):
    return requests.request(
        "POST", url, data=json.dumps(data),
        headers=TYPE_APPLICATION_JSON, auth=(username, password))


def put_request(url, data, username, password):
    return requests.request(
        "PUT", url, data=json.dumps(data), headers=TYPE_APPLICATION_JSON,
        auth=(username, password))


def delete_request(url, username, password):
    return requests.request(
        "DELETE", url, headers=TYPE_APPLICATION_JSON,
        auth=(username, password))


def mount_device(node_id, sim):
    url = ("{}/config/network-topology:network-topology/topology/topology-netconf/node/"
           + node_id).format(RESTCONF_BASE_URL)
    headers = {"node": [{
        "node-id": node_id,
        "netconf-node-topology:username": NODES_LOGIN,
        "netconf-node-topology:password": NODES_PWD,
        "netconf-node-topology:host": "127.0.0.1",
        "netconf-node-topology:port": sims[sim]['port'],
        "netconf-node-topology:tcp-only": "false",
        "netconf-node-topology:pass-through": {}}]}
    response = put_request(url, headers, ODL_LOGIN, ODL_PWD)
    if wait_until_log_contains(karaf_log, re.escape("Triggering notification stream NETCONF for node "+node_id), 60):
        print("Node "+node_id+" correctly added to tpce topology", end='... ', flush=True)
    else:
        print("Node "+node_id+" still not added to tpce topology", end='... ', flush=True)
        if response.status_code == requests.codes.ok:
            print("It was probably loaded at start-up", end='... ', flush=True)
        # TODO an else-clause to abort test would probably be nice here
    return response


def unmount_device(node_id):
    url = ("{}/config/network-topology:network-topology/topology/topology-netconf/node/"
           + node_id).format(RESTCONF_BASE_URL)
    response = delete_request(url, ODL_LOGIN, ODL_PWD)
    if wait_until_log_contains(karaf_log, re.escape("onDeviceDisConnected: "+node_id), 60):
        print("Node "+node_id+" correctly deleted from tpce topology", end='... ', flush=True)
    else:
        print("Node "+node_id+" still not deleted from tpce topology", end='... ', flush=True)
    return response


def generate_link_data(xpdr_node: str, xpdr_num: str, network_num: str, rdm_node: str, srg_num: str,
                       termination_num: str):
    data = {
        "networkutils:input": {
            "networkutils:links-input": {
                "networkutils:xpdr-node": xpdr_node,
                "networkutils:xpdr-num": xpdr_num,
                "networkutils:network-num": network_num,
                "networkutils:rdm-node": rdm_node,
                "networkutils:srg-num": srg_num,
                "networkutils:termination-point-num": termination_num
            }
        }
    }
    return data


def shutdown_process(process):
    if process is not None:
        for child in psutil.Process(process.pid).children():
            child.send_signal(signal.SIGINT)
            child.wait()
        process.send_signal(signal.SIGINT)


def start_honeynode(log_file: str, node_port: str, node_config_file_name: str):
    if os.path.isfile(honeynode_executable):
        with open(log_file, 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, node_port, os.path.join(samples_directory, node_config_file_name)],
                stdout=outfile, stderr=outfile)


def wait_until_log_contains(log_file, regexp, time_to_wait=20):
    found = False
    tail = None
    try:
        with timeout(seconds=time_to_wait):
            print("Searching for pattern '"+regexp+"' in "+os.path.basename(log_file), end='... ', flush=True)
            tail = subprocess.Popen(['tail', '-F', log_file], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            compiled_regexp = re.compile(regexp)
            while True:
                line = tail.stdout.readline().decode('utf-8')
                if compiled_regexp.search(line):
                    print("String found!", end=' ')
                    found = True
                    break
    except TimeoutError:
        print("String not found after "+str(time_to_wait), end=" seconds! ", flush=True)
    finally:
        if tail is not None:
            print("Stopping tail command", end='... ', flush=True)
            tail.stderr.close()
            tail.stdout.close()
            tail.kill()
            tail.wait()
        return found
# TODO try to find an alternative to subprocess+tail -f (such as https://pypi.org/project/tailhead/)


class timeout:
    def __init__(self, seconds=1, error_message='Timeout'):
        self.seconds = seconds
        self.error_message = error_message

    def handle_timeout(self, signum, frame):
        raise TimeoutError(self.error_message)

    def __enter__(self):
        signal.signal(signal.SIGALRM, self.handle_timeout)
        signal.alarm(self.seconds)

    def __exit__(self, type, value, traceback):
        signal.alarm(0)
