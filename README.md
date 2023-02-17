docker compose up -d

docker exec -i cardboard-postgres-db-1 psql -U postgres -t < src/main/resources/seed/create_tables_postgres.sql

docker exec -i cardboard-postgres-db-1 psql -U postgres -c "SELECT * FROM event_journal"

sbt run

# Next

- End turn conditions (discard)
- Type condition (canPlayLand, Sorcery speed, instant speed)
- Extract Focus
- Tests !