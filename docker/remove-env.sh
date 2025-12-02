#!/bin/bash

echo "🛑 Zastavujem celé prostredie..."

# Zastavenie v opačnom poradí (najprv nástroje, potom databázy)
docker-compose -f pgadmin-compose.yml -p pgadmin down -v
docker-compose -f db-compose.yml -p companyapp down -v
docker-compose -f keycloak-compose.yml -p keycloak down -v
docker-compose -f kafka-compose.yml -p kafka down -v

echo "✅ Všetko vypnuté."
