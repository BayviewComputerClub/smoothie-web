# smoothie-web
![License](https://img.shields.io/github/license/BayviewComputerClub/smoothie-web)
![(Unnamed workflow)](https://github.com/BayviewComputerClub/smoothie-web/workflows/(Unnamed%20workflow)/badge.svg)
![Site status](https://img.shields.io/website?label=site&url=https%3A%2F%2Fsmoothie.bayview.club)
![Discord](https://img.shields.io/discord/642159962587529237?color=%23e91e63&label=Discord&logo=Discord)


Open source judging website. Works in tandem with, and provides a web interface to, [smoothie-runner](https://github.com/BayviewComputerClub/smoothie-runner).

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
