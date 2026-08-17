-- Run as postgres superuser. Adjust password before production.
CREATE USER linkchat WITH PASSWORD 'linkchat';
CREATE DATABASE linkchat OWNER linkchat;
GRANT ALL PRIVILEGES ON DATABASE linkchat TO linkchat;
