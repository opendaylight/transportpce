import os
import subprocess

def start_xpdra_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode1.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17830", "sample_configs/openroadm/1.2.1/oper-XPDRA.xml"],
                stdout=outfile, stderr=outfile)

def start_roadma_full_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode2.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17821", "sample_configs/openroadm/1.2.1/oper-ROADMA-full.xml"],
                stdout=outfile, stderr=outfile)

def start_roadma_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode2.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17831", "sample_configs/openroadm/1.2.1/oper-ROADMA.xml"],
                stdout=outfile, stderr=outfile)

def start_roadmb_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode3.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17832", "sample_configs/openroadm/1.2.1/oper-ROADMB.xml"],
                stdout=outfile, stderr=outfile)

def start_roadmc_full_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode3.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17823", "sample_configs/openroadm/1.2.1/oper-ROADMC-full.xml"],
                stdout=outfile, stderr=outfile)

def start_roadmc_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode4.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17833", "sample_configs/openroadm/1.2.1/oper-ROADMC.xml"],
                stdout=outfile, stderr=outfile)

def start_xpdrc_honeynode():
    executable = ("./honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc"
                  "/honeynode-distribution-1.19.04/honeycomb-tpce")
    if os.path.isfile(executable):
        with open('honeynode4.log', 'w') as outfile:
            return subprocess.Popen(
                [executable, "17834", "sample_configs/openroadm/1.2.1/oper-XPDRC.xml"],
                stdout=outfile, stderr=outfile)

def start_tpce():
    if "USE_LIGHTY" in os.environ and os.environ['USE_LIGHTY'] == 'True':
        print ("starting LIGHTY.IO TransportPCE build...")
        executable = "../lighty/target/lighty-transportpce-12.0.0-SNAPSHOT/clean-start-controller.sh"
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))
    else:
        print ("starting KARAF TransportPCE build...")
        executable = "../karaf/target/assembly/bin/karaf"
        with open('odl.log', 'w') as outfile:
            return subprocess.Popen(
                ["sh", executable, "server"], stdout=outfile, stderr=outfile,
                stdin=open(os.devnull))
