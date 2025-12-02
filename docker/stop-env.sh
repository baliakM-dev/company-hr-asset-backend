#!/bin/bash

echo "🛑 Zastavujem celé prostredie..."

# Zastavenie v opačnom poradí (najprv nástroje, potom databázy)
docker-compose -f pgadmin-compose.yml -p pgadmin down
docker-compose -f db-compose.yml -p companyapp down
docker-compose -f keycloak-compose.yml -p keycloak down
docker-compose -f kafka-compose.yml -p kafka down

echo "✅ Všetko vypnuté."
