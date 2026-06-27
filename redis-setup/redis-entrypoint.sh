#!/bin/sh
set -e

# Ensure the ACL file exists before Redis starts, since `aclfile` in redis.conf
# requires the file to be present — Redis won't create it automatically.
mkdir -p /data
touch /data/users.acl

# Start Redis server in the background
redis-server /usr/local/etc/redis/redis.conf &
REDIS_PID=$!

# Wait for Redis to be ready to accept commands
echo "Waiting for Redis to initialize..."
for i in $(seq 1 10); do
  if redis-cli ping 2>/dev/null | grep -q "PONG"; then
    echo "Redis is ready."
    break
  fi
  sleep 1
done

if ! redis-cli ping 2>/dev/null | grep -q "PONG"; then
  echo "ERROR: Redis did not become ready in time"
  exit 1
fi

# Set up ACL user if password is provided
if [ -n "${REDIS_PASSWORD}" ]; then
  if redis-cli ACL LIST | grep -q "^user ${REDIS_USERNAME} "; then
    echo "✓ Redis ACL user '${REDIS_USERNAME}' already exists, skipping creation."
  else
    echo "Setting up Redis ACL user '${REDIS_USERNAME}'..."

    if ! redis-cli ACL SETUSER "${REDIS_USERNAME}" on ">${REDIS_PASSWORD}" \
        +@read +@write +@connection +acl \
        -flushall -flushdb \
        ~quickbite:*; then # here is project specific key pattern
      echo "ERROR: Failed to create ${REDIS_USERNAME} user"
      exit 1
    fi
    echo "✓ Redis ACL user '${REDIS_USERNAME}' created successfully."

    echo "Saving ACL configuration..."
    if redis-cli --user "${REDIS_USERNAME}" --pass "${REDIS_PASSWORD}" ACL SAVE; then
      echo "✓ ACL configuration saved."
    else
      echo "WARNING: ACL SAVE failed — likely no 'aclfile' configured in redis.conf. ACLs will reset on container restart unless an aclfile is set."
    fi
  fi

  echo "Redis ACL configuration complete! ('default' user left enabled for local dev convenience)"
else
  echo "WARNING: REDIS_PASSWORD not set, skipping ACL setup."
fi

# Bring Redis to foreground
wait "$REDIS_PID"