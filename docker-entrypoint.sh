#!/bin/sh
set -e

# Render's managed Postgres injects a single connection-string env var in the
# "postgres://user:password@host:port/dbname" form (optionally with a query string, e.g.
# "?sslmode=require"). Spring's JDBC driver needs a bare "jdbc:postgresql://host:port/dbname"
# URL plus separate username/password - split it apart here, at container startup, since the
# real host/port/credentials are only known once Render provisions the database and differ on
# every deploy, so this can't be baked into the image. application.properties already reads
# DATABASE_URL/DATABASE_USERNAME/DATABASE_PASSWORD, so no app code changes are needed - Render
# UI env vars just need to arrive in the shape Spring expects.
#
# Credentials are exported as separate vars rather than left embedded in the URL as query
# params: Spring's datasource username/password properties are configured explicitly (with a
# "postgres" local-dev default) and Hikari's explicit username/password would otherwise win
# over whatever the URL itself carries, silently trying the wrong credentials against Render's
# real database.
if [ -n "$DATABASE_URL" ]; then
  case "$DATABASE_URL" in
    postgres://* | postgresql://*)
      without_scheme=$(echo "$DATABASE_URL" | sed -E 's#^postgres(ql)?://##')
      userinfo=$(echo "$without_scheme" | sed -E 's#^([^@]*)@.*#\1#')
      hostpart=$(echo "$without_scheme" | sed -E 's#^[^@]*@##')

      export DATABASE_USERNAME=$(echo "$userinfo" | cut -d: -f1)
      export DATABASE_PASSWORD=$(echo "$userinfo" | cut -d: -f2-)
      export DATABASE_URL="jdbc:postgresql://${hostpart}"
      ;;
  esac
fi

if [ -z "$JWT_SECRET" ]; then
  echo "WARNING: JWT_SECRET is not set - falling back to the insecure dev-only default. Set it in Render's dashboard for any real deployment." >&2
fi

exec java -jar /app/app.jar
