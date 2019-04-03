FROM verdaccio/verdaccio

ADD config.yaml /verdaccio/conf/config.yaml
ADD .htpasswd /verdaccio/conf/.htpasswd

RUN chmod a+r /verdaccio/conf/config.yaml
RUN chmod a+r /verdaccio/conf/.htpasswd
