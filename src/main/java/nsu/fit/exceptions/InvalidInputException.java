package nsu.fit.exceptions;

public class InvalidInputException extends Exception {
    private final InputError inputError;
    public InvalidInputException(InputError inputError) {
        super(inputError.getMessage());
        this.inputError = inputError;
    }

    public InputError getError() {
        return inputError;
    }
}
