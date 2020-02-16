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

## Installation
1. Install [Docker](https://www.docker.com/).<br>
    _Windows users: you'll want to setup Docker in “Linux containers” mode._
2. Install [Portainer](https://portainer.io) in a terminal:
    ```shell script
    docker volume create portainer_data
    docker run -d -p 8000:8000 -p 9000:9000 --name=portainer --restart=always -v /var/run/docker.sock:/var/run/docker.sock -v portainer_data:/data portainer/portainer
    ``` 
3. Go to Portainer at [localhost:9000](http://localhost:9000). An electron app is also available on the [AUR](https://aur.archlinux.org/packages/portainer-desktop/).
4. Set a username and password. Don't forget this, you'll need it to login in the future.
5. In Portainer, go to `App Templates` on the sidebar.
6. In the list of app templates, click Mongo. A config dialog should pop up at the top of the page.
7. Give the container a name if you'd like, and click `Show advanced options`. Under `Port mapping`, copy the port from `container` to `host`.
8. Leave everything else as-is unless you know what you're doing, and deploy the container.
9. Repeat 6-8 for Redis.

### Build from source
1. Install gradle, and in a terminal, run:
    ```shell script
    gradle wrapper
    ```
2. Clone smoothie and run it:
    ```shell script
    git clone https://github.com/BayviewComputerClub/smoothie-web.git
    cd smoothie-web
    chmod +x run.sh
    ./run.sh # If debugging use ./run.sh --debug
    ```

### Use precompiled JAR
1. [Download the JAR :coffee:](https://github.com/BayviewComputerClub/smoothie-web/actions?query=workflow%3A%22Smoothie-Web+JAR+Build%22)
2. If you want, make an `application.properties` file in the same folder as the JAR. See [application.properties](#applicationproperties) below.
3. Run the JAR. You can use the command:
    ```shell script
    java -jar smoothie-web.jar # add --debug flag to debug
    ```

#### Go to http://localhost:8080/ to use smoothie-web.

## Configuration
You'll want to setup [smoothie-runner](https://github.com/BayviewComputerClub/smoothie-runner).
1. Either use the [Docker Hub image](https://hub.docker.com/r/espidev/smoothie-runner) or build it from source:
    ```shell script
    git clone https://github.com/BayviewComputerClub/smoothie-runner.git
    cd smoothie-runner
    docker build . -t bsscc/smoothie-runner
    ```
2. In Portainer, go to Containers on the sidebar and add a container.
3. Name it `smoothie-runner`, then use `bsscc/smoothie-runner:latest` as the Image.
4. Under `Network ports configuration`, click `publish a new network port`, and use `6821` as both host and container.
5. Deploy the container.
6. Go to http://localhost:8080 and login with the default credentials `admin` and `password`.
7. Change the admin password at `Hello, admin! > Account Settings > Change Password`.
8. Add a runner at `Admin > Runners` on the topbar.
9. Click `New Runner`, add a name and description, and use `localhost` and `6821` for the host and port. Click `save`.

You may also want to configure recaptcha.

### application.properties
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
