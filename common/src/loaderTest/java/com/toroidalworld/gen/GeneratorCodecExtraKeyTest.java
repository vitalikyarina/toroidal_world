package com.toroidalworld.gen;

import static com.toroidalworld.gen.BakeStampFixture.noiseGenerator;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

@Timeout(60)
class GeneratorCodecExtraKeyTest {
    private static final String UNREAD_KEY = "toroidal_world:unread_probe";
    private static final String UNREAD_VALUE = "a key no codec on the path reads";

    private static HolderLookup.Provider worldgen;

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        worldgen = VanillaRegistries.createLookup();
    }

    @Test
    void aGeneratorRoundTripsWithNoExtraKey() {
        RegistryOps<Tag> ops = nbtOps();

        assertInstanceOf(NoiseBasedChunkGenerator.class, decode(ops, encode(ops)));
    }

    @Test
    void aKeyNoReaderKnowsIsToleratedAsNbt() {
        RegistryOps<Tag> ops = nbtOps();
        CompoundTag encoded = assertInstanceOf(CompoundTag.class, encode(ops));
        encoded.putString(UNREAD_KEY, UNREAD_VALUE);

        assertInstanceOf(NoiseBasedChunkGenerator.class, decode(ops, encoded));
    }

    @Test
    void aKeyNoReaderKnowsIsToleratedAsJson() {
        RegistryOps<JsonElement> ops = jsonOps();
        JsonObject encoded = assertInstanceOf(JsonObject.class, encode(ops));
        encoded.addProperty(UNREAD_KEY, UNREAD_VALUE);

        assertInstanceOf(NoiseBasedChunkGenerator.class, decode(ops, encoded));
    }

    private static RegistryOps<Tag> nbtOps() {
        return worldgen.createSerializationContext(NbtOps.INSTANCE);
    }

    private static RegistryOps<JsonElement> jsonOps() {
        return worldgen.createSerializationContext(JsonOps.INSTANCE);
    }

    private static <T> T encode(DynamicOps<T> ops) {
        return ChunkGenerator.CODEC.encodeStart(ops, noiseGenerator(worldgen)).getOrThrow();
    }

    private static <T> ChunkGenerator decode(DynamicOps<T> ops, T encoded) {
        return ChunkGenerator.CODEC.parse(ops, encoded).getOrThrow();
    }
}
