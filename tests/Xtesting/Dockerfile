FROM alpine:3.18

ARG BRANCH=master

RUN apk --update add \
        build-base \
        checkbashisms \
        curl \
        linux-headers \
        git \
        gcc \
        libxml2-dev \
        libxslt-dev \
        maven \
        openjdk17-jdk \
        openssl-dev \
        python3 \
        python3-dev \
        py3-pip \
        py3-psutil \
        py3-tox \
        py3-wheel \
    && \
    git init /src/transportpce && \
    (cd /src/transportpce && \
        git fetch --tags https://git.opendaylight.org/gerrit/transportpce.git $BRANCH && \
        git checkout FETCH_HEAD) && \
    pip install xtesting \
        -r /src/transportpce/tests/requirements.txt \
        -r /src/transportpce/tests/test-requirements.txt
COPY testcases.yaml /etc/xtesting/testcases.yaml
CMD ["run_tests", "-t", "all"]
