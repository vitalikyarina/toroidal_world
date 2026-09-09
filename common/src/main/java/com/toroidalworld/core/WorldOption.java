package com.toroidalworld.core;

import com.mojang.serialization.Codec;

public record WorldOption<T>(String key, int position, Codec<T> codec, T defaultValue) {
}
