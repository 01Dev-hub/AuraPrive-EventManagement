CREATE DATABASE IF NOT EXISTS auraprive_db;
USE auraprive_db;

-- Clients Table (UHNWIs)
CREATE TABLE clients (
    client_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    secure_email VARCHAR(150) UNIQUE NOT NULL,
    concierge_tier VARCHAR(50) DEFAULT 'Private Circle',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Events Table
CREATE TABLE exclusive_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT,
    event_title VARCHAR(150) NOT NULL,
    venue_location VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    security_level VARCHAR(50) DEFAULT 'Maximum Discretion',
    FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE
);

-- Vendors Table (Private Security, Michelin Chefs, etc.)
CREATE TABLE elite_vendors (
    vendor_id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(100) NOT NULL,
    service_category VARCHAR(100) NOT NULL,
    clearance_verified BOOLEAN DEFAULT TRUE
);