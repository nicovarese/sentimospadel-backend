package com.sentimospadel.backend.onboarding.enums;

public enum AnswerOption {
    A(0),
    B(1),
    C(2),
    D(3),
    E(4);

    private final int value;

    AnswerOption(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }
}
