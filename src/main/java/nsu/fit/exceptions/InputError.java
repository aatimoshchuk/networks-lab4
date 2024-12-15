package nsu.fit.exceptions;

import nsu.fit.controllers.util.InputValidator;

public enum InputError {
    EMPTY_WIDTH("Field \"Width\" cannot be empty."),
    EMPTY_HEIGHT("Field \"Height\" cannot be empty."),
    EMPTY_FOOD_STATIC("Field \"Food static\" cannot be empty."),
    EMPTY_DELAY("Field \"Delay\" cannot be empty."),
    EMPTY_NICKNAME("Field \"Nickname\" cannot be empty."),
    EMPTY_GAME_NAME("Field \"Game name\" cannot be empty."),
    INVALID_WIDTH("Width should be from " + InputValidator.MIN_WIDTH + " to " + InputValidator.MAX_WIDTH + "."),
    INVALID_HEIGHT("Height should be from " + InputValidator.MIN_HEIGHT + " to " + InputValidator.MAX_HEIGHT + "."),
    INVALID_FOOD_STATIC("Food static should be from " + InputValidator.MIN_FOOD_STATIC + " to " +
            InputValidator.MAX_FOOD_STATIC + "."),
    INVALID_DELAY("Delay should be from " + InputValidator.MIN_DELAY + " to " + InputValidator.MAX_DELAY + "."),
    INVALID_WIDTH_FORMAT("Width must be an integer."),
    INVALID_HEIGHT_FORMAT("Height must be an integer."),
    INVALID_FOOD_STATIC_FORMAT("Food static must be an integer."),
    INVALID_DELAY_FORMAT("Delay must be an integer.");

    private final String message;

    InputError(String message) {
        this.message = message;

    }

    public String getMessage() {
        return message;
    }
}
