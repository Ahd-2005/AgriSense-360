CREATE TABLE IF NOT EXISTS Equipments (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    purchase_date DATE
);

CREATE TABLE IF NOT EXISTS Maintenance (
    id INT AUTO_INCREMENT PRIMARY KEY,
    equipment_id INT NOT NULL,
    maintenance_date DATE NOT NULL,
    maintenance_type VARCHAR(80) NOT NULL,
    cost DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_maintenance_equipment
        FOREIGN KEY (equipment_id) REFERENCES Equipments(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
