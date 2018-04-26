FROM openjdk:8-alpine

## from https://github.com/carlossg/docker-maven/blob/798decbb2f987a14f345c017f8fa3725c2467758/jdk-8-alpine/Dockerfile

RUN apk add --no-cache curl tar bash fontconfig

ARG MAVEN_VERSION=${ci-mvn-tools.maven.version}
ARG USER_HOME_DIR="/root"
ARG SHA=707b1f6e390a65bde4af4cdaf2a24d45fc19a6ded00fff02e91626e3e42ceaff
ARG BASE_URL=https://apache.osuosl.org/maven/maven-3/${MAVEN_VERSION}/binaries

RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL -o /tmp/apache-maven.tar.gz ${BASE_URL}/apache-maven-${MAVEN_VERSION}-bin.tar.gz \
  && echo "${SHA}  /tmp/apache-maven.tar.gz" | sha256sum -c - \
  && tar -xzf /tmp/apache-maven.tar.gz -C /usr/share/maven --strip-components=1 \
  && rm -f /tmp/apache-maven.tar.gz \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn
## END FROM

### from https://github.com/nodejs/docker-node/blob/db3b27c8388136b5e529861d7c3fa12fd8328301/8/alpine/Dockerfile
#ENV NODE_VERSION ${ci-mvn-tools.node.version}
#
#RUN addgroup -g 1000 node \
#    && adduser -u 1000 -G node -s /bin/sh -D node \
#    && apk add --no-cache \
#        libstdc++ \
#    && apk add --no-cache --virtual .build-deps \
#        binutils-gold \
#        curl \
#        g++ \
#        gcc \
#        gnupg \
#        libgcc \
#        linux-headers \
#        make \
#        python \
#        xz \
#  # gpg keys listed at https://github.com/nodejs/node#release-team
#  && for key in \
#    94AE36675C464D64BAFA68DD7434390BDBE9B9C5 \
#    FD3A5288F042B6850C66B31F09FE44734EB7990E \
#    71DCFD284A79C3B38668286BC97EC7A07EDE3FC1 \
#    DD8F2338BAE7501E3DD5AC78C273792F7D83545D \
#    C4F0DFFF4E8C1A8236409D08E73BC641CC11F4C8 \
#    B9AE9905FFD7803F25714661B63B535A4C206CA9 \
#    56730D5401028683275BD23C23EFEFE93C4CFFFE \
#    77984A986EBC2AA786BC0F66B01FBB92821C587A \
#  ; do \
#    gpg --keyserver pgp.mit.edu --recv-keys "$key" || \
#    gpg --keyserver keyserver.pgp.com --recv-keys "$key" || \
#    gpg --keyserver ha.pool.sks-keyservers.net --recv-keys "$key" ; \
#  done \
#    && curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/node-v$NODE_VERSION.tar.xz" \
#    && curl -SLO --compressed "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc" \
#    && gpg --batch --decrypt --output SHASUMS256.txt SHASUMS256.txt.asc \
#    && grep " node-v$NODE_VERSION.tar.xz\$" SHASUMS256.txt | sha256sum -c - \
#    && tar -xf "node-v$NODE_VERSION.tar.xz" \
#    && cd "node-v$NODE_VERSION" \
#    && ./configure \
#    && make -j$(getconf _NPROCESSORS_ONLN) \
#    && make install \
#    && apk del .build-deps \
#    && cd .. \
#    && rm -Rf "node-v$NODE_VERSION" \
#    && rm "node-v$NODE_VERSION.tar.xz" SHASUMS256.txt.asc SHASUMS256.txt
#
#ENV YARN_VERSION 1.3.2
#
#RUN apk add --no-cache --virtual .build-deps-yarn curl gnupg tar \
#  && for key in \
#    6A010C5166006599AA17F08146C2130DFD2497F5 \
#  ; do \
#    gpg --keyserver pgp.mit.edu --recv-keys "$key" || \
#    gpg --keyserver keyserver.pgp.com --recv-keys "$key" || \
#    gpg --keyserver ha.pool.sks-keyservers.net --recv-keys "$key" ; \
#  done \
#  && curl -fSLO --compressed "https://yarnpkg.com/downloads/$YARN_VERSION/yarn-v$YARN_VERSION.tar.gz" \
#  && curl -fSLO --compressed "https://yarnpkg.com/downloads/$YARN_VERSION/yarn-v$YARN_VERSION.tar.gz.asc" \
#  && gpg --batch --verify yarn-v$YARN_VERSION.tar.gz.asc yarn-v$YARN_VERSION.tar.gz \
#  && mkdir -p /opt/yarn \
#  && tar -xzf yarn-v$YARN_VERSION.tar.gz -C /opt/yarn --strip-components=1 \
#  && ln -s /opt/yarn/bin/yarn /usr/local/bin/yarn \
#  && ln -s /opt/yarn/bin/yarn /usr/local/bin/yarnpkg \
#  && rm yarn-v$YARN_VERSION.tar.gz.asc yarn-v$YARN_VERSION.tar.gz \
#  && apk del .build-deps-yarn
### END FROM
#
#RUN npm i -g raml2html
#
#RUN apk add --no-cache imagemagick

RUN mkdir -p /m2
RUN mkdir -p /src

ENV MAVEN_HOME /usr/share/maven
ENV MAVEN_CONFIG "$USER_HOME_DIR/.m2"

VOLUME ["$USER_HOME_DIR/.m2", "/src"]
WORKDIR /src
