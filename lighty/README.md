# Lighty-TransportPCE Controller

This project starts [TransportPCE](https://git.opendaylight.org/gerrit/#/admin/projects/transportpce) without karaf framework.

## Build & Run
* Make sure you have JDK11 or later installed.
* Make sure you have [maven 3.5.2](https://maven.apache.org/download.cgi) or later installed.
* Make sure you have proper [settings.xml](https://github.com/opendaylight/odlparent/blob/master/settings.xml)  in your ``~/.m2`` directory.

#### Before compiling Lighty-TransportPCE
Make sure upstream projects are compiled locally and stored in local ``~/.m2/repository``:
1. compile __transportpce/master__
```
git clone https://git.opendaylight.org/gerrit/transportpce && (cd transportpce && curl -kLo `git rev-parse --git-dir`/hooks/commit-msg https://git.opendaylight.org/gerrit/tools/hooks/commit-msg; chmod +x `git rev-parse --git-dir`/hooks/commit-msg)
cd transportpce
```
mvn clean install -s tests/odl_settings.xml -DskipTests -Dmaven.javadoc.skip=true -Dodlparent.spotbugs.skip -Dodlparent.checkstyle.skip
```
2. compile __lighty-core/12.1.x__
```
git clone https://github.com/PantheonTechnologies/lighty-core.git
cd lighty-core
git checkout 12.1.x
mvn clean install -DskipTests -Dmaven.javadoc.skip=true
```
#### Compile Lighty-TransportPCE
* Project is build using maven command:
```
cd ../lighty
mvn clean install -Dmaven.javadoc.skip=true
```
* After project build is done, use binary package to run the TransportPCE controller.
```
cd  target
unzip lighty-transportpce-12.1.0-SNAPSHOT-bin.zip
cd lighty-transportpce-12.1.0-SNAPSHOT
./start-controller.sh
```
* The whole build process described here and in the previous section can be performed automatically by launching the script build.sh from lighty folder.

## TransportPCE lighty.io - karaf comparison

see the previous version of this file in README.neon.md
