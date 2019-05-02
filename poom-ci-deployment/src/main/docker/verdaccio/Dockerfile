FROM verdaccio/verdaccio

USER verdaccio
COPY --chown=verdaccio config.yaml /verdaccio/conf/config.yaml
COPY --chown=verdaccio .htpasswd /verdaccio/conf/.htpasswd
#
#RUN chown -R verdaccio /verdaccio/conf
#RUN chmod 777 /verdaccio/conf/.htpasswd
