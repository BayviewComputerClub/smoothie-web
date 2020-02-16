# smoothie-web
![License](https://img.shields.io/github/license/BayviewComputerClub/smoothie-web)
![Smoothie-Web JAR Build Test](https://github.com/BayviewComputerClub/smoothie-web/workflows/Smoothie-Web%20JAR%20Build/badge.svg)
![Site status](https://img.shields.io/website?label=site&url=https%3A%2F%2Fsmoothie.bayview.club)
![Discord](https://img.shields.io/discord/642159962587529237?color=%23e91e63&label=Discord&logo=Discord)
![Commits](https://img.shields.io/github/commit-activity/w/BayviewComputerClub/smoothie-web?label=commits)
![Contributor count](https://img.shields.io/github/contributors/BayviewComputerClub/smoothie-web)
![Latest commit](https://img.shields.io/github/last-commit/BayviewComputerClub/smoothie-web)
![Maintenance](https://img.shields.io/maintenance/yes/2020)

Open source judging website. Works in tandem with, and provides a web interface to, [smoothie-runner](https://github.com/BayviewComputerClub/smoothie-runner).

### Build from source
Install gradle, and in a terminal, run:
    ```shell script
    $ gradle wrapper
    $ ./gradlew build
    ```
The compiled JAR file will be in build/libs/.

### Precompiled JAR
[Download the JAR :coffee:](https://github.com/BayviewComputerClub/smoothie-web/actions?query=workflow%3A%22Smoothie-Web+JAR+Build%22)

## Installation
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Installation)

## Configuration
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Configuration)

### application.properties
This optional file should to be in the directory where you run the jar from.

```
# These are useful to change
# Mongo
spring.data.mongodb.port=27017
spring.data.mongodb.database=main

# Redis
spring.redis.host=localhost
spring.redis.password=
spring.redis.port=6379

# Captcha
google.recaptcha.key.site=
google.recaptcha.key.secret=

# Default admin password
smoothieweb.admin.password=password

# Set upload limit
spring.servlet.multipart.max-file-size = 1GB
spring.servlet.multipart.max-request-size = 1GB

# Spring boot admin server
spring.boot.admin.client.url=http://localhost:8081
```
