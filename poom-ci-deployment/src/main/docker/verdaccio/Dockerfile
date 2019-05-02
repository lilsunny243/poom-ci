FROM verdaccio/verdaccio

ADD config.yaml /verdaccio/conf/config.yaml
ADD .htpasswd /verdaccio/conf/.htpasswd

RUN chown verdacci:verdacci /verdaccio/conf/.htpasswd
RUN chown verdacci:verdacci /verdaccio/conf/config.yaml
