# Lighty-TransportPCE Controller

This project starts [TransportPCE](https://git.opendaylight.org/gerrit/#/admin/projects/transportpce) without karaf framework.

## Build & Run
* Make sure you have JDK8 or later installed
* Make sure you have [maven 3.5.0](https://maven.apache.org/download.cgi) or later installed.
* Make sure you have proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml)  in your ``~/.m2`` directory.

#### Before compiling Lighty-TransportPCE
Make sure upstream projects are compiled locally and stored in local ``~/.m2/repository``:
1. compile __transportpce/master__
```
git clone https://git.opendaylight.org/gerrit/transportpce && (cd transportpce && curl -kLo `git rev-parse --git-dir`/hooks/commit-msg https://git.opendaylight.org/gerrit/tools/hooks/commit-msg; chmod +x `git rev-parse --git-dir`/hooks/commit-msg)
cd transportpce
```
Before compilation, make sure that deviations are not present in models:
```
transportpce/ordmodels/network/src/main/yang/org-openroadm-otn-network-topology@2018-11-30.yang
transportpce/ordmodels/network/src/main/yang/org-openroadm-network-topology@2018-11-30.yang
transportpce/ordmodels/network/src/main/yang/org-openroadm-network@2018-11-30.yang
```
After deviations are removed from models, compile the project.
```
mvn clean install -DskipTests
```
2. compile __lighty-core/10.0.x__
```
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout 10.0.x
mvn clean install -DskipTests
```
#### Compile Lighty-TransportPCE
* Project is build using maven command:
```
cd ../lighty
mvn clean install
```
* After project build is done, use binary package to run the TransportPCE controller.
```
cd  target
unzip lighty-transportpce-10.0.1-SNAPSHOT-bin.zip
cd lighty-transportpce-10.0.1-SNAPSHOT
./start-controller.sh
```
* The whole build process described here and in the previous section can be performed automatically by launching the script build.sh from lighty folder.

## TransportPCE lighty.io - karaf comparison

### Application Boot test
This test compares TransportPCE application on lighty.io and karaf.
After TransportPCE is started, performance is measured using [visualvm](https://visualvm.github.io/) tool.

| Property Name                     | ODL/Karaf *    | lighty.io ** |
|-----------------------------------|----------------|--------------|
| Build size                        | 225M           | 64M          |
| Startup Time                      | ~15s           | ~6s          |
| Shutdown Time                     | ~5s            | ~100ms       |
| Process memory allocated (RSS)*** | 1236 MB        | 353 MB       |
| HEAP memory (used/allocated)      | 135 / 1008 MB  | 58 / 128 MB  |
| Metaspace (used/allocated)        | 115 / 132 MB   | 62 /  65 MB  |
| Threads (live/daemon)             | 111 / 48       | 70 /  11     |
| Classes loaded                    | 22027          | 12019        |
| No. of jars                       | 680            | 244          |

### test_end2end_lighty.py
This test compares TransportPCE application on lighty.io and karaf while running __test_end2end_lighty.py__ (4 connected netconf devices).

| Property Name                     | ODL/Karaf *    | lighty.io ** |
|-----------------------------------|----------------|--------------|
| Build size                        | 225M           | 64M          |
| Startup Time                      | ~15s           | ~6s          |
| Shutdown Time                     | ~5s            | ~100ms       |
| Process memory allocated (RSS)*** | 1185 MB        | 440 MB       |
| HEAP memory (used/allocated)      | 158 / 960 MB   | 85 / 128 MB  |
| Metaspace (used/allocated)        | 128 / 146 MB   | 83 /  87 MB  |
| Threads (live/daemon)             | 148 / 60       | 129 / 26     |
| Classes loaded                    | 24326          | 16155        |
| No. of jars                       | 680            | 244          |

`* JVM parameters: -Xms128M -Xmx2048m ...`
`** JVM parameters: -Xms128m -Xmx128m -XX:MaxMetaspaceSize=128m`
`*** sudo ps -o pid,rss,user,command ax | grep java | grep transportpce`

### Integration Test results
| Test Name                             | Failed | All | Comment                    |
|---------------------------------------|--------|-----|----------------------------|
| test_portmapping.py                   | 0      |  18 |                            |
| test_topoPortMapping.py               | 1      |   6 | (same results with karaf)  |
| test_topology.py                      | 0      |  33 |                            |
| test_renderer_service_path_nominal.py | 0      |  24 |                            |
| test_pce.py                           | 0      |  22 |                            |
| test_olm.py                           | 1      |  40 | (same results with karaf)  |
| test_end2end_lighty.py                | 18     |  52 | (same as karaf, need check)|

`karaf: executable = "../karaf/target/assembly/bin/karaf"`
`lighty.io executable = "../lighty/target/lighty-transportpce-10.0.1-SNAPSHOT/start-controller.sh": `

* __Total Success: 175__
* __Total Failed: 20__

### Postman collection
Check this [postman collection](docs/TransportPCE.postman_collection.json) for RESTCONF examples.
