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

CREATE TABLE IF NOT EXISTS Camera (
    id INT AUTO_INCREMENT PRIMARY KEY,
    camera_name VARCHAR(100) NOT NULL,
    stream_url VARCHAR(500) NOT NULL,
    location VARCHAR(100),
    equipment_id INT,
    sensitivity_level ENUM('NIGHT', 'DAY') NOT NULL DEFAULT 'DAY',
    alerts_enabled BOOLEAN DEFAULT TRUE,
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_camera_equipment
        FOREIGN KEY (equipment_id) REFERENCES Equipments(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS MotionEvent (
    id INT AUTO_INCREMENT PRIMARY KEY,
    camera_id INT NOT NULL,
    detection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    motion_frame_count INT DEFAULT 0,
    severity ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
    CONSTRAINT fk_motion_camera
        FOREIGN KEY (camera_id) REFERENCES Camera(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);
