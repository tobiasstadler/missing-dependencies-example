FROM maven:3.8.4-openjdk-8 AS builder

COPY pom.xml /tmp/build/
COPY src /tmp/build/src

RUN cd /tmp/build/ && mvn package

FROM openjdk:8

COPY --from=builder /tmp/build/target/multiple-dependencies-example.jar /tmp/run/
COPY --from=builder /tmp/build/target/lib /tmp/run/lib
COPY --from=builder /tmp/build/target/elastic-apm-agent.jar /tmp/run/

CMD java -javaagent:/tmp/run/elastic-apm-agent.jar -jar /tmp/run/multiple-dependencies-example.jar ${MODE}
