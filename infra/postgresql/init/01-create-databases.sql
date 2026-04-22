-- QTS PostgreSQL Schema - Part 1: Create Databases
-- Run as superuser (postgres)

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_stat_statements";

-- Create databases
CREATE DATABASE qts_trade;
CREATE DATABASE qts_auth;
CREATE DATABASE qts_risk;

-- Verify
SELECT datname FROM pg_database WHERE datname IN ('qts_trade', 'qts_auth', 'qts_risk');