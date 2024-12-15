module nsu.fit {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires com.google.common;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires com.google.protobuf;

    opens nsu.fit to javafx.fxml;
    opens nsu.fit.controllers to javafx.fxml;
    opens nsu.fit.model to com.google.common;
    opens nsu.fit.controllers.util to javafx.fxml;
    exports nsu.fit;
    exports nsu.fit.controllers;
    exports nsu.fit.controllers.util;
    exports nsu.fit.events;
    exports nsu.fit.events.switching;
    exports nsu.fit.events.model;
    exports nsu.fit.exceptions;
    exports nsu.fit.model;
    exports nsu.fit.model.snake;
    exports nsu.fit.model.communication;
    exports nsu.fit.model.communication.player;
    exports nsu.fit.model.communication.udp;
    exports nsu.fit.model.communication.converters;
}