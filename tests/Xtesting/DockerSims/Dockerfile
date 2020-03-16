FROM maven:3.6.3-jdk-11-slim AS builder
ARG GIT_REF=master
RUN apt-get update \
    && apt-get install -y git \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir /tmp/git_repo \
    && git clone -b "$GIT_REF"  https://git.opendaylight.org/gerrit/transportpce /tmp/git_repo \
    && cd /tmp/git_repo/tests \
    && ./buildHoneynode.sh  \
    && ./buildHoneynode.sh 2.2.1 \
    && mkdir -p /tmp/honeynode/honeynode_1.2.1/config \
    && mkdir -p /tmp/honeynode/honeynode_2.2.1/config \
    && mv /tmp/git_repo/tests/honeynode/1.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc/honeynode-distribution-1.19.04/* /tmp/honeynode/honeynode_1.2.1/. \
    && mv /tmp/git_repo/tests/honeynode/2.2.1/honeynode-distribution/target/honeynode-distribution-1.19.04-hc/honeynode-distribution-1.19.04/* /tmp/honeynode/honeynode_2.2.1/. \
    && cp /tmp/git_repo/tests/sample_configs/openroadm/1.2.1/* /tmp/honeynode/honeynode_1.2.1/config/. \
    && cp /tmp/git_repo/tests/sample_configs/openroadm/2.2.1/* /tmp/honeynode/honeynode_2.2.1/config/.




FROM adoptopenjdk/openjdk11:alpine-jre
LABEL Name="Roadm, XPDR Openroadm simulators"
LABEL Description="Simulators for Openroadm devices based on honeycomb project"
LABEL Maintainer="transportpce-dev@lists.opendaylight.org"
LABEL Url="https://git.opendaylight.org/gerrit/transportpce"

ENV DEVICE_VERSION=1.2.1
ENV DEVICE_FILE=oper-ROADMA-full.xml
RUN addgroup gsimulator && adduser -D usimulator -G gsimulator -u 100001\
  && mkdir -p /opt/application/simulators
COPY --chown=usimulator:gsimulator --from=builder /tmp/honeynode/ /opt/application/simulators/honeynode
COPY --chown=usimulator:gsimulator --from=builder /tmp/git_repo/tests/sample_configs/openroadm/ /opt/application/simulators/honeynode/config
RUN find /opt/application/simulators -name "honeycomb-tpce" -exec  sed -i "/reflectwarn.sh/d" {} +
USER usimulator
WORKDIR /opt/application/simulators/honeynode
HEALTHCHECK CMD wget --header "Authorization: Basic `echo -n "${USER}:${PASSWORD}" | base64`"  http://localhost:8180/restconf/ -S -O -
EXPOSE 8080/tcp 8180/tcp
CMD /opt/application/simulators/honeynode/honeynode_$DEVICE_VERSION/honeycomb-tpce 8080 /opt/application/simulators/honeynode/config/$DEVICE_VERSION/$DEVICE_FILE
