#!/bin/sh
set -eu

# Render supplies a PostgreSQL URI, but the JDBC driver does not accept URI
# user-info. Render injects username and password as separate variables, so only
# the host, port, database, and query string are retained here.
if [ -n "${DATABASE_URL:-}" ] && [ -z "${SPRING_DATASOURCE_URL:-}" ]; then
	case "$DATABASE_URL" in
		postgresql://*|postgres://*)
			export SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_URL##*@}"
			;;
		*)
			echo "DATABASE_URL must use the postgres:// or postgresql:// scheme." >&2
			exit 1
			;;
	esac
fi

exec java -XX:MaxRAMPercentage=75.0 -jar app.jar
