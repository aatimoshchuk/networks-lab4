package nsu.fit.controllers.util;

import nsu.fit.exceptions.InputError;
import nsu.fit.exceptions.InvalidInputException;

public class InputValidator {
    public static final int MIN_WIDTH = 10;
    public static final int MAX_WIDTH = 100;
    public static final int MIN_HEIGHT = 10;
    public static final int MAX_HEIGHT = 100;
    public static final int MIN_FOOD_STATIC = 0;
    public static final int MAX_FOOD_STATIC = 100;
    public static final int MIN_DELAY = 100;
    public static final int MAX_DELAY = 3000;

    public static int checkWidth(String width) throws InvalidInputException {
        if (width.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_WIDTH);
        }

        try {
            int intWidth = Integer.parseInt(width);
            if (intWidth > MAX_WIDTH || intWidth < MIN_WIDTH) {
                throw new InvalidInputException(InputError.INVALID_WIDTH);
            }
            return intWidth;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(InputError.INVALID_WIDTH_FORMAT);
        }
    }

    public static int checkHeight(String height) throws InvalidInputException {
        if (height.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_HEIGHT);
        }

        try {
            int intHeight = Integer.parseInt(height);
            if (intHeight > MAX_HEIGHT || intHeight < MIN_HEIGHT) {
                throw new InvalidInputException(InputError.INVALID_HEIGHT);
            }
            return intHeight;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(InputError.INVALID_HEIGHT_FORMAT);
        }
    }

    public static int checkFoodStatic(String foodStatic) throws InvalidInputException {
        if (foodStatic.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_FOOD_STATIC);
        }

        try {
            int intFoodStatic = Integer.parseInt(foodStatic);
            if (intFoodStatic > MAX_FOOD_STATIC || intFoodStatic < MIN_FOOD_STATIC) {
                throw new InvalidInputException(InputError.INVALID_FOOD_STATIC);
            }
            return intFoodStatic;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(InputError.INVALID_FOOD_STATIC_FORMAT);
        }
    }

    public static int checkDelay(String delay) throws InvalidInputException {
        if (delay.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_DELAY);
        }

        try {
            int intDelay = Integer.parseInt(delay);
            if (intDelay > MAX_DELAY || intDelay < MIN_DELAY) {
                throw new InvalidInputException(InputError.INVALID_DELAY);
            }
            return intDelay;
        } catch (NumberFormatException e) {
            throw new InvalidInputException(InputError.INVALID_DELAY_FORMAT);
        }
    }

    public static void checkNickname(String nickname) throws InvalidInputException {
        if (nickname.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_NICKNAME);
        }
    }

    public static void checkGameName(String gameName) throws InvalidInputException {
        if (gameName.isEmpty()) {
            throw new InvalidInputException(InputError.EMPTY_GAME_NAME);
        }
    }
}
