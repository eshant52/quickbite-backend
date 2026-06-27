# Installation and Configuration
This document provides instructions for installing and configuring the QuickBite application.

## Prerequisites
- Docker
- Docker Compose

## Installation Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/eshant52/quickbite-backend.git
   ```

2. Create the local environment file:
   ```bash
   cp .env.local.example .env.local
   ```
   Add your environment variables in the `.env.local` file. Make sure to set the `REDIS_USERNAME` and `REDIS_PASSWORD` variables for Redis authentication.

3. Start the services:
   ```bash
   docker compose --env-file .env.local --profile full up -d
   ```
   
---

### More details are as follows:

[`redis-setup/redis-entrypoint.sh`](../redis-setup/redis-entrypoint.sh):

This script is responsible for setting up the Redis ACL user and ensuring that the ACL configuration is saved. It checks if the ACL user already exists, and if not, it creates the user with the specified username and password. The script also ensures that the ACL file exists before starting Redis, as Redis requires the ACL file to be present for proper configuration.

[`redis-setup/redis.conf`](../redis-setup/redis.conf):

This configuration file contains the Redis server settings, including the ACL file path. It is important to ensure that the ACL file exists before starting Redis, as Redis will not create it automatically.

[`docker-compose.yml`](../docker-compose.yml): 

This file defines the services for the QuickBite application, including PostgreSQL, Redis, and Kafka. It specifies the environment variables, volumes, and health checks for each service. The Redis service uses the `redis-entrypoint.sh` script to set up the ACL user and ensure that the ACL configuration is saved.