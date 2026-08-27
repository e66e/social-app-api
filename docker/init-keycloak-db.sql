CREATE DATABASE keycloak_db;

CREATE USER kc_user WITH PASSWORD 'kc_secret';

\c keycloak_db
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO kc_user;
GRANT ALL ON SCHEMA public TO kc_user;