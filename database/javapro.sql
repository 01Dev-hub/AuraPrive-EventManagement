CREATE DATABASE IF NOT EXISTS auraprive_db;
USE auraprive_db;

CREATE TABLE IF NOT EXISTS clients (
    client_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    membership_tier VARCHAR(60) NOT NULL,
    encrypted_contact VARCHAR(150) NOT NULL
);

CREATE TABLE IF NOT EXISTS elite_vendors (
    vendor_id INT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(120) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    security_clearance VARCHAR(60) NOT NULL
);

CREATE TABLE IF NOT EXISTS exclusive_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    client_id INT NOT NULL,
    event_title VARCHAR(150) NOT NULL,
    event_type VARCHAR(30) NOT NULL DEFAULT 'GALA',
    venue_location VARCHAR(150) NOT NULL,
    event_date DATE NOT NULL,
    security_level VARCHAR(60) NOT NULL,
    vip_guests INT NOT NULL DEFAULT 50,
    FOREIGN KEY (client_id) REFERENCES clients(client_id) ON DELETE CASCADE
);

INSERT IGNORE INTO clients (client_id, full_name, membership_tier, encrypted_contact) VALUES
(1, 'Maharaja Vikramaditya Rathore', 'Sovereign Founders Circle', 'rathore@vault.auraprive.in'),
(2, 'Arundhati Devi Scindia', 'Diplomatic Black Tier', 'scindia@vault.auraprive.in'),
(3, 'Rajeshwar Singhania', 'Apex Syndicate', 'singhania@vault.auraprive.in');

INSERT IGNORE INTO elite_vendors (vendor_id, vendor_name, specialty, security_clearance) VALUES
(1, 'Garuda Sovereign Security', 'Counter-Surveillance & Airspace Lock', 'Maximum Discretion'),
(2, 'Dawat-e-Khas Royal Gastronomy', 'Bespoke Heritage Culinary Curation', 'Diplomatic Tier'),
(3, 'Vayu Executive Aviation', 'Unlisted Charter & Helipad Logistics', 'Maximum Discretion');

INSERT IGNORE INTO exclusive_events (event_id, client_id, event_title, event_type, venue_location, event_date, security_level, vip_guests) VALUES
(1, 1, 'Udaipur Royal Solstice Gala', 'GALA', 'Taj Lake Palace, Udaipur', '2026-12-31', 'Maximum Discretion', 60),
(2, 2, 'Jodhpur Sovereign Founders Summit', 'SUMMIT', 'Umaid Bhawan Palace, Jodhpur', '2026-11-18', 'Diplomatic Tier', 40);