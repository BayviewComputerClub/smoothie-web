# smoothie-web
![License](https://img.shields.io/github/license/BayviewComputerClub/smoothie-web)
![Smoothie-Web JAR Build Test](https://github.com/BayviewComputerClub/smoothie-web/workflows/Smoothie-Web%20JAR%20Build/badge.svg)
![Site status](https://img.shields.io/website?label=site&url=https%3A%2F%2Fsmoothie.bayview.club)
![Language count](https://img.shields.io/github/languages/count/BayviewComputerClub/smoothie-web)
![Top language](https://img.shields.io/github/languages/top/BayviewComputerClub/smoothie-web)
![Repo size](https://img.shields.io/github/repo-size/BayviewComputerClub/smoothie-web)
![Open issues](https://img.shields.io/github/issues-raw/BayviewComputerClub/smoothie-web)
![Closed issues](https://img.shields.io/github/issues-closed-raw/BayviewComputerClub/smoothie-web)
![Contributor count](https://img.shields.io/github/contributors/BayviewComputerClub/smoothie-web)
![Commits](https://img.shields.io/github/commit-activity/w/BayviewComputerClub/smoothie-web?label=commits)
![Latest commit](https://img.shields.io/github/last-commit/BayviewComputerClub/smoothie-web)
![Maintenance](https://img.shields.io/maintenance/yes/2020)
![Discord](https://img.shields.io/discord/642159962587529237?color=%23e91e63&label=Discord&logo=Discord)

Open source judging website. Works in tandem with, and provides a web interface to, [smoothie-runner](https://github.com/BayviewComputerClub/smoothie-runner).

### Build from source
Install gradle, and in a terminal, run:

```shell script
gradle wrapper
./gradlew build
 ```
The compiled JAR file will be in build/libs/.

### Precompiled JAR/WAR
[Download the JAR/WAR :coffee:](https://github.com/BayviewComputerClub/smoothie-web/actions?query=workflow%3A%22Smoothie-Web+Build%22)

## Installation
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Installation)

## Configuration
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Configuration)

### application.properties
This optional file should to be in the directory where you run the JAR/WAR from.

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
