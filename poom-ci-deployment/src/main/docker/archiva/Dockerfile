FROM openjdk:8-jdk
MAINTAINER dev@flexio.fr

ENV VERSION=${archiva.version}

ADD http://mirrors.standaloneinstaller.com/apache/archiva/$VERSION/binaries/apache-archiva-$VERSION-bin.tar.gz /tmp/

RUN mkdir -p /opt
RUN tar zxf /tmp/apache-archiva-$VERSION-bin.tar.gz -C /opt
RUN rm -f /tmp/apache-archiva-$VERSION-bin.tar.gz
RUN mv /opt/apache-archiva-$VERSION /opt/apache-archiva

RUN groupadd archiva && useradd -g archiva archiva
RUN sed -i '/#RUN_AS_USER=/c\RUN_AS_USER=archiva' /opt/apache-archiva/bin/archiva

RUN mkdir -p /var/archiva/logs
RUN mkdir -p /var/archiva/data
RUN mkdir -p /var/archiva/repositories
RUN mkdir -p /var/archiva/temp
RUN mkdir -p /var/archiva/conf


RUN chown -R archiva:archiva /var/archiva

COPY start-archiva.sh /usr/local/bin/start-archiva.sh
RUN chmod a+x /usr/local/bin/start-archiva.sh

ENV ARCHIVA_BASE=/var/archiva/

EXPOSE 8080
VOLUME /var/archiva/data
VOLUME /var/archiva/logs
VOLUME /var/archiva/repositories
VOLUME /var/archiva/temp
VOLUME /var/archiva/conf

ENTRYPOINT ["/usr/local/bin/start-archiva.sh"]
CMD ["console"]



