package org.reladev.quickdto.testharness.impl;

public class GenericsImpl extends GenericsBaseImpl<String> {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
