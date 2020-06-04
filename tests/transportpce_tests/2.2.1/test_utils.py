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

HONEYNODE_OK_START_MSG = re.escape("Netconf SSH endpoint started successfully at 0.0.0.0")

TYPE_APPLICATION_JSON = {'content-type': 'application/json'}

honeynode_executable = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "honeynode", "2.2.1", "honeynode-simulator", "honeycomb-tpce")

# TODO: Need needs to be fixed
if os.name == 'posix':
    honeynode_executable = os.path.join(
        os.path.dirname(os.path.realpath(__file__)),
        "..", "..", "honeynode", "2.2.1", "honeynode-distribution", "target",
        "honeynode-distribution-1.19.04-hc", "honeynode-distribution-1.19.04",
        "honeycomb-tpce")

samples_directory = os.path.join(
    os.path.dirname(os.path.realpath(__file__)),
    "..", "..", "sample_configs", "openroadm", "2.2.1")

log_directory = os.path.dirname(os.path.realpath(__file__))


def start_xpdra_honeynode():
    log_file = os.path.join(log_directory, "oper-XPDRA.log")
    process = start_node(log_file, "17840", "oper-XPDRA.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_roadma_honeynode():
    log_file = os.path.join(log_directory, "oper-ROADMA.log")
    process = start_node(log_file, "17841", "oper-ROADMA.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_roadmb_honeynode():
    log_file = os.path.join(log_directory, "oper-ROADMB.log")
    process = start_node(log_file, "17842", "oper-ROADMB.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_roadmc_honeynode():
    log_file = os.path.join(log_directory, "oper-ROADMC.log")
    process = start_node(log_file, "17843", "oper-ROADMC.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_xpdrc_honeynode():
    log_file = os.path.join(log_directory, "oper-XPDRC.log")
    process = start_node(log_file, "17844", "oper-XPDRC.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_spdra_honeynode():
    log_file = os.path.join(log_directory, "oper-SPDRAv2.log")
    process = start_node(log_file, "17845", "oper-SPDRAv2.xml")
    wait_until_log_contains(log_file, HONEYNODE_OK_START_MSG, 100)
    return process


def start_tpce():
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        print("starting LIGHTY.IO TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "lighty", "target", "tpce",
            "clean-start-controller.sh")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))
    else:
        print("starting KARAF TransportPCE build...")
        executable = os.path.join(
            os.path.dirname(os.path.realpath(__file__)),
            "..", "..", "..", "karaf", "target", "assembly", "bin", "karaf")
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable, "server"], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))


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


def generate_connect_data(node_id: str, node_port: str):
    data = {"node": [{
        "node-id": node_id,
        "netconf-node-topology:username": "admin",
        "netconf-node-topology:password": "admin",
        "netconf-node-topology:host": "127.0.0.1",
        "netconf-node-topology:port": node_port,
        "netconf-node-topology:tcp-only": "false",
        "netconf-node-topology:pass-through": {}}]}
    return data


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


def start_node(log_file: str, node_port: str, node_config_file_name: str):
    if os.path.isfile(honeynode_executable):
        with open(log_file, 'w') as outfile:
            return subprocess.Popen(
                [honeynode_executable, node_port, os.path.join(samples_directory, node_config_file_name)],
                stdout=outfile, stderr=outfile)


def wait_until_log_contains(log_file, searched_string, time_to_wait=20):
    found = False
    tail = None
    try:
        with timeout(seconds=time_to_wait):
            print("Waiting for " + searched_string)
            tail = subprocess.Popen(['tail', '-F', log_file], stdout=subprocess.PIPE, stderr=subprocess.PIPE)
            regexp = re.compile(searched_string)
            while True:
                line = tail.stdout.readline().decode('utf-8')
                if regexp.search(line):
                    print("Searched string found.")
                    found = True
                    break
    except TimeoutError:
        print("Cannot find string "+searched_string+" after waiting for "+str(time_to_wait))
    finally:
        if tail is not None:
            print("Stopping tail command")
            tail.stderr.close()
            tail.stdout.close()
            tail.kill()
            tail.wait()
        return found


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
