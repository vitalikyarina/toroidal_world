package com.toroidalworld.compat;

import java.io.IOException;
import java.io.InputStream;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public record ModSymbol(String owner, String member, String descriptor) {
    private static final String CLASS_SUFFIX = ".class";

    private static final int READ_MEMBERS_ONLY = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;

    private static final boolean UNREADABLE_COUNTS_AS_CARRIED = true;

    public boolean carriedBy(ClassLoader classLoader) {
        try (InputStream classFile = classLoader.getResourceAsStream(this.owner + CLASS_SUFFIX)) {
            if (classFile == null) {
                return false;
            }

            MemberSearch search = new MemberSearch(this.member, this.descriptor);
            new ClassReader(classFile.readAllBytes()).accept(search, READ_MEMBERS_ONLY);

            return search.found();
        } catch (IOException | RuntimeException unreadable) {
            return UNREADABLE_COUNTS_AS_CARRIED;
        }
    }

    @Override
    public String toString() {
        return this.owner + "#" + this.member + ":" + this.descriptor;
    }

    private static final class MemberSearch extends ClassVisitor {
        private final String member;
        private final String descriptor;

        private boolean found;

        private MemberSearch(String member, String descriptor) {
            super(Opcodes.ASM9);
            this.member = member;
            this.descriptor = descriptor;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String fieldDescriptor, String signature,
                Object value) {
            this.found |= matches(name, fieldDescriptor);
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String methodDescriptor, String signature,
                String[] exceptions) {
            this.found |= matches(name, methodDescriptor);
            return null;
        }

        private boolean found() {
            return this.found;
        }

        private boolean matches(String name, String memberDescriptor) {
            return this.member.equals(name) && this.descriptor.equals(memberDescriptor);
        }
    }
}
