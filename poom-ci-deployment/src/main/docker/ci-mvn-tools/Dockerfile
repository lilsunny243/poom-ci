FROM openjdk:8-alpine

RUN apk add --no-cache curl tar bash fontconfig ttf-dejavu git imagemagick graphviz maven nodejs yarn php7 php7-json php7-phar php7-iconv php7-openssl npm
RUN npm i -g raml2html
RUN curl -sS https://getcomposer.org/installer | php -- --install-dir=/usr/bin --filename=composer

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
