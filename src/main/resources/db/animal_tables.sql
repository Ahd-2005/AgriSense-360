-- Run this on database agrisense-360 to create Animal and AnimalHealthRecord tables.

CREATE TABLE IF NOT EXISTS Animal (
    id INT AUTO_INCREMENT PRIMARY KEY,
    earTag INT,
    type VARCHAR(32) NOT NULL,
    gender VARCHAR(32) NOT NULL,
    weight DOUBLE,
    healthStatus VARCHAR(64),
    birthDate DATE,
    entryDate DATE,
    origin VARCHAR(32) NOT NULL,
    vaccinated BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS AnimalHealthRecord (
    id INT AUTO_INCREMENT PRIMARY KEY,
    animal INT NOT NULL,
    recordDate DATE NOT NULL,
    weight DOUBLE,
    appetite VARCHAR(32),
    conditionStatus VARCHAR(32) NOT NULL,
    milkYield DOUBLE,
    eggCount INT,
    woolLength DOUBLE,
    notes TEXT,
    CONSTRAINT fk_health_record_animal FOREIGN KEY (animal) REFERENCES Animal(id) ON DELETE CASCADE
);

CREATE INDEX idx_animal_health_record_animal ON AnimalHealthRecord(animal);
