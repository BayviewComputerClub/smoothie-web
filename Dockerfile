FROM openjdk:14-oracle

EXPOSE 8080

# Jenkins will build the JAR locally (to make this image smaller)
COPY ./build/libs/* /opt/smoothie-web.jar
COPY ./docker/application.yml /opt/application.yml

WORKDIR /opt/

ENTRYPOINT java -jar smoothie-web.jar
