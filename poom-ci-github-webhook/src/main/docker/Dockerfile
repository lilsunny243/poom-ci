FROM openjdk:8-jdk-alpine

RUN mkdir -p /var/service/lib
RUN mkdir -p /var/service/config
COPY lib/* /var/service/lib/
COPY logback.xml /var/service/config/
COPY service-endpoint.sh /var/service/
RUN chmod a+x /var/service/service-endpoint.sh

EXPOSE 9999
ENV SERVICE_PORT=9999

ENTRYPOINT ["/var/service/service-endpoint.sh", "org.codingmatters.poom.ci.github.webhook.GithubWebhookService"]
