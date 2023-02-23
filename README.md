docker compose up -d

docker exec -i cardboard-postgres-db-1 psql -U postgres -t < src/main/resources/seed/create_tables_postgres.sql

docker exec -i cardboard-postgres-db-1 psql -U postgres -c "SELECT * FROM event_journal"

sbt run

# Next

- Improve triggerHandler to handle a List of events (and would run on events triggered by itself)
- Refactor "zones" storage
- Abilities on the Stack
- Improve Priority handling by implementing "Pass"
- Fix "next" command
- Extract Focus
- Tests !