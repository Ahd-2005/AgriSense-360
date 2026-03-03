module com.example.agrisense360 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires java.sql;
    requires java.net.http;
    requires java.desktop;
    requires org.json;

    opens com.example.agrisense360.controllers to javafx.fxml;
    opens com.example.agrisense360.entity to javafx.base;
    opens controllers to javafx.fxml;
    opens entity to javafx.base;
    
    exports com.example.agrisense360.controllers;
    exports com.example.agrisense360.entity;
    exports com.example.agrisense360.services;
    exports com.example.agrisense360.utils;
    exports com.example.agrisense360.tests;
    exports controllers;
    exports entity;
    exports services;
    exports utils;
}



