# smoothie-web
![License](https://img.shields.io/github/license/BayviewComputerClub/smoothie-web)
![Smoothie-Web JAR Build Test](https://github.com/BayviewComputerClub/smoothie-web/workflows/Smoothie-Web%20JAR%20Build/badge.svg)
![Site status](https://img.shields.io/website?down_message=offline&label=site%20status&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAABgAAAAYCAYAAADgdz34AAAABGdBTUEAALGPC%2FxhBQAAACBjSFJNAAB6JgAAgIQAAPoAAACA6AAAdTAAAOpgAAA6mAAAF3CculE8AAAABmJLR0QA%2FwD%2FAP%2BgvaeTAAAAB3RJTUUH5AIQDzEB0d6H0gAAAsVJREFUSMedlctvTVEYxX%2Ffvue2Gq8WVUIPiUeQCE0qCDEhREq82ogOGGjNGDEx8G8w6EzCpNFEhaSJRyREIjEgTBC0JvQhXmn1nrOXwb3q3OvcR%2BzZ%2Fda%2B%2B7e%2FtfbexygZ42EvBnUx6hbaBTQCHw0bMHgIxIuG%2B6h1WBoAaIrxdwVtCemTwYVfxFdnEdBcI8Sll2Up8BbBpXoy64Rq7qAMwASpq6wGTuQQo2HP%2FwGcGQ77mcFdzGDdBteSMMGhLG5JrV1YJbGwy%2FUeDQHLC%2BWcwSnB9QxGtcBdJdGAwOy1wYNEOSvodFh9LV1UBARmTMtHYA8ozmSX0KZaTKoIiBCLgwYDbSyxcxFweApfNeyKAAnGoqmVgo5%2FNDg4C7esmk1lAeNhLx4htB9YlTJlHbBbwFhr%2BS7KAoTIYPOATtJPWyDocliDr3AWUwET4ZnC7tkq2FLsTNHYIdRGBZtSAR5PHc4JdQFzEtIL4E3idxNw9FXwrWzYqQABObRGsC9ZN%2Bgz6C%2BZ27EhmheWC%2FsfwHjYS5Q36AAQJqQR4I5hg8DXRH0tsFfAaOvp6gCPyOIWAMdKpPsOe2fw3OBpco3CzZ6ddhSKABMrehHCw07B5oT0y2AgRnGEfgA3S9bZJtSeZlJQtHuJwFwQyXcBDQnpJfDIAJff5pDQcMLC%2BcDxAPf4y4ozkWdaELDwQ19xBwJiaYNgd0m4t2M0alj%2BAcS9KXkAEZyI8AM5%2BXNZ6mbMmgGMhz1M58M9BCxN5g4MZjCah%2FsIFJAjjg27AUwl5jXmnxRtt8TFnAF4oA7XIjhSYuMTw15Y4T%2BNI1dw%2BU7uGdyiyggARpadRHkr2g2aBZ8L1gjo92gy6WW9yzLpp78bdh40AexR%2FkIGYF8xm7ncQWGhQnh6Bnb4r4w3eAsUfbnmvr8MwFjY88HhzgotB%2BYLnGETHvwfl34Ds4MB62QXRNAAAAAldEVYdGRhdGU6Y3JlYXRlADIwMjAtMDItMTZUMTU6NDk6MDEtMDU6MDAnA9ASAAAAJXRFWHRkYXRlOm1vZGlmeQAyMDIwLTAyLTE2VDE1OjQ5OjAxLTA1OjAwVl5orgAAAABJRU5ErkJggg%3D%3D&up_message=online&url=https%3A%2F%2Fsmoothie.bayview.club)
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
The compiled JAR file will be in `build/libs/`.

### Precompiled JAR/WAR
[Download the JAR/WAR :coffee:](https://github.com/BayviewComputerClub/smoothie-web/actions?query=workflow%3A%22Smoothie-Web+Build%22)

## Installation
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Installation)

## Configuration
[Wiki](https://github.com/BayviewComputerClub/smoothie-web/wiki/Configuration)

### application.yml
This optional file should to be in the directory where you run the JAR/WAR from.

```
# These are useful to change
# Feel free to change into conventional YML format, they are listed
# on separate lines for easy pasting.

# Domains that the instance is accessible from (for CORS)
smoothieweb.domains: "http://localhost:80, https://localhost:443, https://localhost:3000, http://localhost:3000"

# Port
server.port: 8080

# Mongo
spring.data.mongodb:
    host: localhost
    port: 27017
    database: main

# Redis
spring.redis:
    host: localhost
    port: 6379
    password: 

# SMTP
spring.mail:
    host: 
    port: 587
    username:
    password:
    properties.mail.smtp:
        auth: true
        starttls.enable: true

# Captcha
google.recaptcha.key:
    site:
    secret:

# Site info
smoothieweb:
    url: localhost:8080
    contact-email: no-email@configured.com

# Email verification
smoothieweb.email-verification:
    secret: PLEASECHANGE
    enabled: false

# Default admin password when database is created
smoothieweb.admin.password: password

# Whether or not to show stack traces on error pages
smoothieweb.error.debug: true

# Spring boot admin server
spring.boot.admin.client.url: "http://localhost:8081"
```
