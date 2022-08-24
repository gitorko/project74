# Project 78

RabbitMQ Stream

[https://gitorko.github.io/rabbitmq-stream/](https://gitorko.github.io/rabbitmq-stream/)

### Version

Check version

```bash
$java --version
openjdk 17.0.3 2022-04-19 LTS
```

### RabbitMQ

Run the docker command to start a rabbitmq instance

```bash
docker run -it --hostname my-rabbit --rm --name my-rabbit -e RABBITMQ_DEFAULT_USER=guest \
-e RABBITMQ_DEFAULT_PASS=guest -e RABBITMQ_SERVER_ADDITIONAL_ERL_ARGS='-rabbitmq_stream advertised_host localhost' \
-p 8080:15672 -p 5672:5672 -p 5552:5552 rabbitmq:3-management 
```

```bash
docker exec my-rabbit rabbitmq-plugins enable rabbitmq_stream
```

Open the rabbitmq console

[http://localhost:8080](http://localhost:8080)

```
user:guest
pwd: guest
```

### Dev

```bash
./gradlew bootRun
```
