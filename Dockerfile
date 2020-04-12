FROM openjdk:14-oracle

EXPOSE 8080

# Jenkins will build the JAR locally (to make this image smaller)
COPY ./build/libs/build* /opt/smoothie-web.jar

WORKDIR /opt/

ENTRYPOINT java -jar smoothie-web.jar
