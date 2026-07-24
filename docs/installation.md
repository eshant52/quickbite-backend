# Installation and Configuration

This document provides instructions for installing and configuring the QuickBite application.

## Installation Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/eshant52/quickbite-backend.git
   ```

2. Create the local environment file:
   ```bash
   cp .env.local.example .env.local
   ```
   Add your environment variables in the `.env.local` file. Make sure to set the `REDIS_USERNAME` and `REDIS_PASSWORD`
   variables for Redis authentication.

3. Start the services:
   ```bash
   docker compose --env-file .env.local --profile full up -d
   ```
   
4. Create the application environment file:
   ```bash
   cp .env.example .env
   ```
   Add your environment variables in the `.env` file. Make sure to set the required variables.

   Also, if you are using IntelliJ IDEA, you can set the environment variables in the run configuration for the application by specifying the path to the `.env` file.

5. Create Kafka topics manually when you want explicit local setup:
      ```bash
      docker exec -it my-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --create \
        --topic quickbite.order.events \
        --partitions 3 --replication-factor 1

      docker exec -it my-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --create \
        --topic quickbite.notification.events \
        --partitions 3 --replication-factor 1

      docker exec -it my-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --create \
        --topic quickbite.delivery.events \
        --partitions 3 --replication-factor 1

      docker exec -it my-kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server localhost:9092 --create \
        --topic quickbite.order.events.DLQ \
        --partitions 1 --replication-factor 1
      ```

      The application also declares these topics through Spring Kafka when it can connect to Kafka.

6. Verify that the services are running:
   ```bash
   docker ps
   ```

7. Creating PostGIS extension for the PostgreSQL database:
    1. Go to postgres:
       ```bash
       docker exec -it my-postgres psql -U <username> -d quickbite
       ```
    2. Create postgis extension for the database:
       ```sql
       CREATE EXTENSION postgis;
       ```

8. Create secrets for JWT keys:
   ```bash
   openssl genrsa -out src/main/resources/certs/private.pem 2048
   openssl rsa -in src/main/resources/certs/private.pem -pubout -out src/main/resources/certs/public.pem
   ```

---

### Details about secret files

[`src/main/resources/certs/private.pem`](../src/main/resources/certs/private.pem): This file contains the private key used for signing JWT tokens. It should be kept secure and not shared publicly.

[`src/main/resources/certs/public.pem`](../src/main/resources/certs/public.pem): This file contains the public key used for verifying JWT tokens. It can be shared publicly and is used by clients to verify the authenticity of the JWT tokens issued by the QuickBite application.

[`.env`](../.env): This file contains environment variables for the QuickBite application. It includes settings for PostgreSQL, Redis, Kafka, and other application-specific configurations. Make sure to set the required variables.

[` .env.local`](../.env.local): This file contains environment variables for `docker-compose.yml`. It includes settings for PostgreSQL, Redis and Kafka. Make sure to set the required variables.

---

### More details are as follows:

[`redis-setup/redis-entrypoint.sh`](../redis-setup/redis-entrypoint.sh):

This script is responsible for setting up the Redis ACL user and ensuring that the ACL configuration is saved. It checks
if the ACL user already exists, and if not, it creates the user with the specified username and password. The script
also ensures that the ACL file exists before starting Redis, as Redis requires the ACL file to be present for proper
configuration.

[`redis-setup/redis.conf`](../redis-setup/redis.conf):

This configuration file contains the Redis server settings, including the ACL file path. It is important to ensure that
the ACL file exists before starting Redis, as Redis will not create it automatically.

[`postgres-setup/initdb-postgis.sh`](../postgres-setup/initdb-postgis.sh):
This script is responsible for creating the PostGIS extension in the PostgreSQL database. It is executed when the
PostgreSQL container is initialized, and it ensures that the PostGIS extension is available for use in the QuickBite
application.

[`postgres-setup/update-postgis.sh`](../postgres-setup/update-postgis.sh):
This script is responsible for updating the PostGIS extension in the PostgreSQL database. It can be executed manually if
you need to update the PostGIS extension to a newer version.

[`postgres-setup/Dockerfile`](../postgres-setup/Dockerfile):
This Dockerfile is used to build a custom PostgreSQL image with the PostGIS extension. It installs the necessary
dependencies and sets up the PostGIS extension during the image build process.

[`docker-compose.yml`](../docker-compose.yml):

This file defines the services for the QuickBite application, including PostgreSQL, Redis, and Kafka. It specifies the
environment variables, volumes, and health checks for each service. The Redis service uses the `redis-entrypoint.sh`
script to set up the ACL user and ensure that the ACL configuration is saved.

Redis is exposed locally on `localhost:6377`, Kafka is exposed on `localhost:9092`, and application cache keys use the
`quickbite:` prefix to match the Redis ACL key pattern.
