<h1>Simple SpingBoot JPA demo using Thymeleaf/Bootstrap</h1>

Simple SpingBoot JPA demo using Thymeleaf/Bootstrap with web UI as as well as REST based API

<h3>Rest Endpoints</h3>

```
$ curl http://{url}/emps | jq -r

$ curl http://{url}/emp/1 | jq -r
```

<h3>Run Locally</h3>

- git clone https://github.com/papicella/SpringBootPCFPas.git

- cd SpringBootPCFPas

- mvn package or mvn spring-boot:run

- access as follows

```
http://localhost:8080/
```

<h3>Push to Cloud Foundry</h3>

- edit manifest.yml to use a MySQL service

```
---
applications:
- name: springboot-jpa-bootstrap-employee
  memory: 512M
  instances: 1
  random-route: true
  timeout: 180
  path: ./target/springbootpcfpas-0.0.1-SNAPSHOT.jar
  services:
    - apples-mysql
  env:
    JAVA_OPTS: -Djava.security.egd=file:///dev/urando
    SPRING_PROFILES_ACTIVE: cloud
```

- cf push

<h3>Screen Shots</h3>

![alt tag](https://dl.dropboxusercontent.com/u/15829935/platform-demos/images/spring-pas-pcf-1.png)

![alt tag](https://dl.dropboxusercontent.com/u/15829935/platform-demos/images/spring-pas-pcf-2.png)

<hr />
Pas Apicella [papicella at pivotal.io] is a Senior Platform Architect at Pivotal Australia 