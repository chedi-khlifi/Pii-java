package com.example.controllers;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;

public class UIUtils {

    private static Timeline scrollTimeline;

    /**
     * Makes a ScrollPane scroll smoothly when using the mouse wheel.
     */
    public static void makeSmooth(ScrollPane scrollPane) {
        final double speed = 4.0;
        scrollPane.setOnScroll(event -> {
            if (event.getDeltaY() == 0) return;

            double deltaY = event.getDeltaY();
            double height = scrollPane.getContent().getBoundsInLocal().getHeight();
            if (height <= 0) return;

            double vvalue = scrollPane.getVvalue();
            double newValue = vvalue - (deltaY * speed / height);
            
            if (newValue < 0) newValue = 0;
            if (newValue > 1) newValue = 1;

            if (scrollTimeline != null) scrollTimeline.stop();

            scrollTimeline = new Timeline();
            scrollTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(250), 
                new KeyValue(scrollPane.vvalueProperty(), newValue, Interpolator.EASE_BOTH)));
            scrollTimeline.play();
            
            event.consume();
        });
    }
}
