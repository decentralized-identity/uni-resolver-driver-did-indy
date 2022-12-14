# Dockerfile for universalresolver/driver-did-indy

FROM maven:3-jdk-11 AS build
MAINTAINER Markus Sabadello <markus@danubetech.com>

# build driver-did-indy

WORKDIR /opt/driver-did-indy
ADD pom.xml /opt/driver-did-indy
RUN mvn org.apache.maven.plugins:maven-dependency-plugin:3.3.0:go-offline
ADD src/main/webapp/WEB-INF /opt/driver-did-indy/src/main/webapp/WEB-INF
RUN mvn clean package -P war
ADD . /opt/driver-did-indy
RUN mvn clean package -P war

# build image

FROM jetty:9.4-jre11-slim
MAINTAINER Markus Sabadello <markus@danubetech.com>

# install dependencies

USER root

RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get -y update && \
    apt-get install -y --no-install-recommends software-properties-common gnupg && \
    apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 68DB5E88 && \
    add-apt-repository "deb https://repo.sovrin.org/sdk/deb bionic stable" && \
    apt-get -y update && \
    apt-get install -y libindy=1.16.0-bionic && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

USER jetty

# variables

ENV uniresolver_driver_did_indy_libIndyPath=
ENV uniresolver_driver_did_indy_poolConfigs=sovrin;./sovrin/sovrin.txn;sovrin:staging;./sovrin/sovrin-staging.txn;sovrin:builder;./sovrin/sovrin-builder.txn;danube;./sovrin/danube.txn;idunion;./sovrin/idunion.txn;idunion:test;./sovrin/idunion-test.txn;indicio;./sovrin/indicio.txn;indicio:test;./sovrin/indicio-test.txn;indicio:demo;./sovrin/indicio-demo.txn;bbu;./sovrin/bbu.txn
ENV uniresolver_driver_did_indy_poolVersions=sovrin;2;sovrin:staging;2;sovrin:builder;2;danube;2;idunion;2;idunion:test;2;indicio;2;indicio:test;2;indicio:demo;2;bbu;2
ENV uniresolver_driver_did_indy_walletNames=sovrin;w1;sovrin:staging;w2;sovrin:builder;w3;danube;w4;idunion;w5;idunion:test;w6;indicio;w7;indicio:test;w8;indicio:demo;w9;bbu;w10
ENV uniresolver_driver_did_indy_submitterDidSeeds=sovrin;_;sovrin:staging;_;sovrin:builder;_;danube;_;idunion;_;idunion:test;_;indicio;_;indicio:test;_;indicio:demo;_;bbu;_

# copy from build stage

COPY --from=build --chown=jetty /opt/driver-did-indy/target/*.war /var/lib/jetty/webapps/ROOT.war
COPY --from=build --chown=jetty /opt/driver-did-indy/sovrin/ /var/lib/jetty/sovrin/

# done

EXPOSE 8080
CMD java -Djetty.http.port=8080 -jar /usr/local/jetty/start.jar
