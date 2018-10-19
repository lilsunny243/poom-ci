FROM registry.containers.ovh.net/flexio/minimal_interpretors:1.0.0


ENV APT_FLAGS="--no-install-recommends -y"
RUN apt update
RUN apt install ${APT_FLAGS} curl tar bash fontconfig ttf-dejavu git imagemagick graphviz maven

RUN apt install ${APT_FLAGS} gnupg
RUN curl -o /tmp/nodejs-setup https://deb.nodesource.com/setup_8.x
RUN chmod a+x /tmp/nodejs-setup
RUN /tmp/nodejs-setup
RUN rm /tmp/nodejs-setup
RUN apt install ${APT_FLAGS} nodejs yarn

RUN npm i -g raml2html

COPY poom-ci-utilities.jar /usr/local/lib/poom-ci-utilities.jar
COPY *.sh /usr/local/bin/
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
