module com.example.agrisens360{
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    //requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.sql;
    requires javafx.graphics;
    requires org.json;
    requires java.net.http;
    requires jakarta.mail;
    requires java.desktop;
    requires com.google.api.client.auth;
    requires com.google.api.client;
    requires com.google.api.client.json.jackson2;      // ← ajoute ça pour Desktop.browse()
    opens com.example.agrisens360 to javafx.fxml;
    opens controllers to javafx.fxml;
    exports com.example.agrisens360;
    opens entity to javafx.base;
    exports controllers;

}



