package com.reactnative.antelop;

enum PatternNames {
    STRONG("strong"),
    SIMPLE("simple"),
    MEDIUM("medium");

    private final String text;

    PatternNames(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
