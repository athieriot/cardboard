docker compose up -d

docker exec -i cardboard-postgres-db-1 psql -U postgres -t < src/main/resources/seed/create_tables_postgres.sql

docker exec -i cardboard-postgres-db-1 psql -U postgres -c "SELECT * FROM event_journal"

sbt run

# Next

- Abilities on the Stack
- Extract Context / "CommandController"
- Refactor CombatZone
- Improve Priority handling by implementing "Pass"
- Tests !