# Dockerfile for universalresolver/driver-did-indy

FROM maven:3-eclipse-temurin-17-focal AS build
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

FROM jetty:11.0.17-jre17-eclipse-temurin
MAINTAINER Markus Sabadello <markus@danubetech.com>

# install dependencies

USER root

ADD ./lib/ /opt/lib
RUN export DEBIAN_FRONTEND=noninteractive && \
    apt-get -y update && \
    apt-get install -y --no-install-recommends software-properties-common gnupg libsodium23 libzmq5 && \
    apt-get -y update && \
    dpkg -i /opt/lib/libssl1.1_1.1.1f-1ubuntu2_amd64.deb && \
    dpkg -i /opt/lib/libindy_1.16.0-bionic_amd64.deb && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

USER jetty

# variables

ENV uniresolver_driver_did_indy_libIndyPath=
ENV uniresolver_driver_did_indy_openParallel=false
ENV uniresolver_driver_did_indy_poolConfigs=sovrin;./sovrin/sovrin.txn;sovrin:test;./sovrin/sovrin-test.txn;danube;./sovrin/danube.txn;idunion;./sovrin/idunion.txn;idunion:test;./sovrin/idunion-test.txn;indicio;./sovrin/indicio.txn;indicio:test;./sovrin/indicio-test.txn;indicio:demo;./sovrin/indicio-demo.txn;nxd;./sovrin/nxd.txn;bcovrin:test;./sovrin/bcovrin-test.txn;candy;./sovrin/candy.txn;candy:test;./sovrin/candy-test.txn;candy:dev;./sovrin/candy-dev.txn;didas;./sovrin/didas.txn
ENV uniresolver_driver_did_indy_poolVersions=sovrin;2;sovrin:test;2;danube;2;idunion;2;idunion:test;2;indicio;2;indicio:test;2;indicio:demo;2;nxd;2;findy:test;2;bcovrin;2;bcovrin:test;2;bcovrin:dev;2;candy;2;candy:test;2;candy:dev;2;didas;2
ENV uniresolver_driver_did_indy_walletNames=sovrin;w1;sovrin:test;w2;danube;w4;idunion;w5;idunion:test;w6;indicio;w7;indicio:test;w8;indicio:demo;w9;nxd;w11;findy:test;w12;bcovrin;w13;bcovrin:test;w14;bcovrin:dev;w15;candy;w16;candy:test;w17;candy:dev;w18;didas;w19
ENV uniresolver_driver_did_indy_submitterDidSeeds=sovrin;_;sovrin:test;_;danube;_;idunion;_;idunion:test;_;indicio;_;indicio:test;_;indicio:demo;_;nxd;_;findy:test;_;bcovrin;_;bcovrin:test;_;bcovrin:dev;_;candy;_;candy:test;_;candy:dev;_;didas;_

# copy from build stage

COPY --from=build --chown=jetty /opt/driver-did-indy/target/*.war /var/lib/jetty/webapps/ROOT.war
COPY --from=build --chown=jetty /opt/driver-did-indy/sovrin/ /var/lib/jetty/sovrin/

# done

EXPOSE 8080
CMD java -Djetty.http.port=8080 -jar /usr/local/jetty/start.jar
