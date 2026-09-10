package com.toroidalworld.compat;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.FieldModel;
import java.lang.classfile.MethodModel;

public record ModSymbol(String owner, String member, String descriptor) {
    private static final String CLASS_SUFFIX = ".class";

    private static final boolean UNREADABLE_COUNTS_AS_CARRIED = true;

    public boolean carriedBy(ClassLoader classLoader) {
        try (InputStream classFile = classLoader.getResourceAsStream(this.owner + CLASS_SUFFIX)) {
            if (classFile == null) {
                return false;
            }

            return carriedBy(ClassFile.of().parse(classFile.readAllBytes()));
        } catch (IOException | RuntimeException unreadable) {
            return UNREADABLE_COUNTS_AS_CARRIED;
        }
    }

    @Override
    public String toString() {
        return this.owner + "#" + this.member + ":" + this.descriptor;
    }

    private boolean carriedBy(ClassModel model) {
        for (FieldModel field : model.fields()) {
            if (this.member.equals(field.fieldName().stringValue())
                    && this.descriptor.equals(field.fieldType().stringValue())) {
                return true;
            }
        }

        for (MethodModel method : model.methods()) {
            if (this.member.equals(method.methodName().stringValue())
                    && this.descriptor.equals(method.methodType().stringValue())) {
                return true;
            }
        }

        return false;
    }
}
