#!/bin/bash

# Farby pre krajší výstup
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 Štartujem vývojové prostredie...${NC}"

# 1. KAFKA
if [ -f "kafka-compose.yml" ]; then
    echo -e "${GREEN}➡️  Spúšťam Kafka Cluster...${NC}"
    # -p kafka: Projekt sa bude volať 'kafka'
    docker-compose -f kafka-compose.yml -p kafka up -d
else
    echo -e "${RED}❌ Súbor kafka-docker-compose.yml sa nenašiel!${NC}"
fi

# 2. KEYCLOAK
if [ -f "keycloak-compose.yml" ]; then
    echo -e "${GREEN}➡️  Spúšťam Keycloak & Postgres...${NC}"
    # -p keycloak: DÔLEŽITÉ! Vytvorí sieť 'keycloak_keycloak_network', ktorú očakáva pgAdmin
    docker-compose -f keycloak-compose.yml -p keycloak up -d
else
    echo -e "${RED}❌ Súbor keycloak-compose.yml (Keycloak) sa nenašiel!${NC}"
fi

# 3. COMPANY APP DB
if [ -f "db-compose.yml" ]; then
    echo -e "${GREEN}➡️  Spúšťam DB App Database...${NC}"
    # -p companyapp: DÔLEŽITÉ! Vytvorí sieť 'companyapp_company-app-net', ktorú očakáva pgAdmin
    docker-compose -f db-compose.yml -p companyapp up -d
else
    echo -e "${RED}❌ Súbor db-compose.yml sa nenašiel!${NC}"
fi

# 4. PGADMIN (Musí ísť posledný, lebo sa pripája na siete predchádzajúcich)
if [ -f "pgadmin-compose.yml" ]; then
    echo -e "${GREEN}➡️  Spúšťam PGAdmin...${NC}"
    docker-compose -f pgadmin-compose.yml -p pgadmin up -d
else
    echo -e "${RED}❌ Súbor pgadmin-compose.yml sa nenašiel!${NC}"
fi

echo -e "${BLUE}✅ Hotovo! Všetky služby bežia.${NC}"
echo -e "   - Kafka Broker:   localhost:9092"
echo -e "   - Kafka UI:       http://localhost:8888"
echo -e "   - Keycloak:       http://localhost:8081"
echo -e "   - PGAdmin:        http://localhost:5050"
echo -e "   - Company DB:     localhost:5433"
echo -e "   - Audit log DB:   localhost:5434"
