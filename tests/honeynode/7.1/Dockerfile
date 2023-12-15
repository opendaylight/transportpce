FROM alpine

ADD honeynode-simulator /root/honeynode-simulator
ADD get.xml /root/honeynode-simulator/get.xml
RUN apk add --update --no-cache openjdk11 bash procps \
    && cd /root \
    && cd honeynode-simulator

CMD ./root/honeynode-simulator/honeycomb-tpce 830 ./root/honeynode-simulator/get.xml