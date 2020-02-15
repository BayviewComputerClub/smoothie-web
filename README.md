# smoothie-web
![License](https://img.shields.io/github/license/BayviewComputerClub/smoothie-web)
![Gradle Test](https://github.com/BayviewComputerClub/smoothie-web/workflows/Gradle%20Test/badge.svg)
![Site status](https://img.shields.io/website?label=site&url=https%3A%2F%2Fsmoothie.bayview.club)
![Discord](https://img.shields.io/discord/642159962587529237?color=%23e91e63&label=Discord&logo=Discord)
![Commits](https://img.shields.io/github/commit-activity/w/BayviewComputerClub/smoothie-web?label=commits)
![Contributor count](https://img.shields.io/github/contributors/BayviewComputerClub/smoothie-web)
![Smoothie-Web Jar Build Test](https://github.com/BayviewComputerClub/smoothie-web/workflows/Smoothie-Web%20Jar%20Build%20Test/badge.svg)
![Maintenance](https://img.shields.io/maintenance/yes/2020)

Open source judging website. Works in tandem with, and provides a web interface to, [smoothie-runner](https://github.com/BayviewComputerClub/smoothie-runner).

[Get those *beautiful* JARs :coffee:](https://github.com/BayviewComputerClub/smoothie-web/actions?query=workflow%3A%22Build+Smoothie-Web+Jar%22)

## application.properties
```
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
