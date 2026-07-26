#!/bin/sh
set -eu

# Render provides postgresql://user:password@host:port/database.
# pgJDBC needs jdbc:postgresql://host:port/database, while username and
# password are passed separately through DB_USERNAME and DB_PASSWORD.
if [ -n "${DATABASE_URL:-}" ]; then
  DB_URL=$(printf '%s' "$DATABASE_URL" | sed -E 's#^postgres(ql)?://[^@]+@#jdbc:postgresql://#')
  export DB_URL
fi

exec java -jar /app/app.jar
