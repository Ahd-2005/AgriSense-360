module com.example.agrisens360 {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires javafx.media;
    requires javafx.graphics;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    requires java.sql;
    requires java.desktop;
    requires java.net.http;

    requires org.json;
    requires jakarta.mail;

    requires com.google.api.client.auth;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;

    // ✅ Ajouté pour Azure Face API (jackson)
    requires com.fasterxml.jackson.databind;

    opens com.example.agrisens360 to javafx.fxml;
    opens controllers to javafx.fxml;
    opens entity to javafx.base;

    exports com.example.agrisens360;
    exports controllers;
}