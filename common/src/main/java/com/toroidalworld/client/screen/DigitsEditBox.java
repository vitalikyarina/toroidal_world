package com.toroidalworld.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

// An edit box that only ever holds digits. Filtering sits on insertText — the one funnel both typing and pasting go
// through — because vanilla's EditBox has no value filter to hand a predicate to.
public class DigitsEditBox extends EditBox {
    public DigitsEditBox(Font font, int width, int height, Component message) {
        super(font, width, height, message);
    }

    @Override
    public void insertText(String text) {
        String digits = text.chars().filter(Character::isDigit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (!digits.isEmpty() || text.isEmpty()) {
            super.insertText(digits);
        }
    }
}
