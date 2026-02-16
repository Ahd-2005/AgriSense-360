CREATE TABLE Parcelle (
                          id INT AUTO_INCREMENT PRIMARY KEY,
                          nom VARCHAR(100) NOT NULL,
                          surface DOUBLE NOT NULL,
                          localisation VARCHAR(150),
                          typeSol VARCHAR(80),
                          statut VARCHAR(50)
);


CREATE TABLE Culture (
                         id INT AUTO_INCREMENT PRIMARY KEY,
                         nom VARCHAR(100) NOT NULL,
                         typeCulture VARCHAR(80),
                         datePlantation DATE,
                         dateRecolte DATE,
                         etat VARCHAR(50),
                         surface DOUBLE,
                         img VARCHAR(255),
                         parcelleId INT NOT NULL,

                         FOREIGN KEY (parcelleId) REFERENCES Parcelle(id)
                             ON DELETE CASCADE
                             ON UPDATE CASCADE
);