#!/bin/bash
set -e

# These variables are expected to be set in the environment
DBNAME=${POSTGRES_DB:-carlsbad}
DBUSER=${DBUSR:-batman}
DBPASS=${DBPW:-foobar}

echo "Starting CARLSBAD database initialization..."

# Create the application role
psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname "postgres" <<-EOSQL
    CREATE ROLE $DBUSER WITH LOGIN PASSWORD '$DBPASS';
    ALTER ROLE $DBUSER CREATEDB;
EOSQL

# Create the database if it doesn't exist (though the official image usually creates it)
# We use the official image's POSTGRES_DB for the name.

if [ -f "/tmp/carlsbad.pgdump" ]; then
    echo "Restoring database from /tmp/carlsbad.pgdump..."
    pg_restore -v --no-owner --schema public -d "$DBNAME" /tmp/carlsbad.pgdump || echo "pg_restore finished with some warnings (this is often normal)"
else
    echo "Warning: /tmp/carlsbad.pgdump not found. Skipping restore."
fi

# Grant privileges
psql -v ON_ERROR_STOP=1 --username "${POSTGRES_USER:-postgres}" --dbname "$DBNAME" <<-EOSQL
    GRANT SELECT ON ALL TABLES IN SCHEMA public TO $DBUSER;
    GRANT SELECT ON ALL SEQUENCES IN SCHEMA public TO $DBUSER;
    GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO $DBUSER;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO $DBUSER;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON SEQUENCES TO $DBUSER;
    ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT EXECUTE ON FUNCTIONS TO $DBUSER;
EOSQL

echo "CARLSBAD database initialization complete."
