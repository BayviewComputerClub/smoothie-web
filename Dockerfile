FROM gradle:6.2-jdk11

EXPOSE 8080

COPY . /smoothie-web/build
WORKDIR /smoothie-web/build

RUN gradle build && \
    mv build/libs/build* /smoothie-web.jar

WORKDIR /

ENTRYPOINT java -jar smoothie-web.jar
