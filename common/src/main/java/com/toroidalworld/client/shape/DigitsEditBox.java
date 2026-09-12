package com.toroidalworld.client.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

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

    public @Nullable Integer number() {
        try {
            return Integer.parseInt(this.getValue());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
