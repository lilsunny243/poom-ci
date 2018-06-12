FROM openjdk:8-alpine

RUN apk add --no-cache curl tar bash fontconfig ttf-dejavu git imagemagick graphviz maven nodejs yarn
RUN npm i -g raml2html

COPY poom-ci-utilities-${org.codingmatters.poom.ci:poom-ci-utilities:jar:uber.version}-uber.jar /usr/local/lib/poom-ci-utilities.jar
COPY trigger-downstream.sh /usr/local/bin/
COPY upload-project-metadata.sh /usr/local/bin/
RUN chmod a+x  /usr/local/bin/*.sh

RUN mkdir -p /m2
RUN mkdir -p /src

ENV USER_HOME_DIR="/root"
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"
ENV MAVEN_MS "256m"
ENV MAVEN_MX "512m"
ENV MAVEN_OPTS "-Xms$MAVEN_MS -Xmx$MAVEN_MX"

VOLUME ["$USER_HOME_DIR/.m2", "/src"]
WORKDIR /src
