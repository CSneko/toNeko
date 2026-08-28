package dev.architectury.loom.neoforge;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/*
 * Patches the Minecraft.class check in FML's CommonUserdevLaunchHandler
 * to refer to a class that is found in any mapping set (Main.class).
 *
 * See https://github.com/architectury/architectury-loom/issues/212
 */
public final class LaunchHandlerPatcher extends ClassVisitor {
	private static final String INPUT_CLASS_FILE = "net/minecraft/client/Minecraft.class";
	private static final String OUTPUT_CLASS_FILE = "net/minecraft/client/main/Main.class";

	public LaunchHandlerPatcher(ClassVisitor next) {
		super(Opcodes.ASM9, next);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodPatcher(super.visitMethod(access, name, descriptor, signature, exceptions));
	}

	private static final class MethodPatcher extends MethodVisitor {
		MethodPatcher(MethodVisitor next) {
			super(Opcodes.ASM9, next);
		}

		@Override
		public void visitLdcInsn(Object value) {
			if (INPUT_CLASS_FILE.equals(value)) {
				value = OUTPUT_CLASS_FILE;
			}

			super.visitLdcInsn(value);
		}
	}
}
