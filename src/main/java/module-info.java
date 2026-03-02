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
    //requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires static java.xml;
    requires java.sql;
    requires com.google.gson;
    requires java.net.http;
    requires java.desktop;
    requires jakarta.mail;

    requires org.json;

    requires com.google.api.client.auth;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;

    requires com.fasterxml.jackson.databind;
    requires kernel;
    requires layout;
    requires json.simple;
    requires com.google.zxing;
    requires com.google.zxing.javase;

    opens com.example.agrisens360 to javafx.fxml;
    opens controllers to javafx.fxml;
    exports com.example.agrisens360;
    opens entity to javafx.base;
    exports controllers;

}



