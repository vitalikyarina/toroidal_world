package com.toroidalworld.options;

import com.mojang.serialization.Codec;

public record WorldOption<T>(String key, int position, Codec<T> codec, T defaultValue, T inertValue) {
}
