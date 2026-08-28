package dev.architectury.loom.forge;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Patches {@code ModDirTransformerDiscovererPatch} in Forge 49.0.50+ so that it doesn't try to create modules
 * directly out of UnionFS root paths created by Union Relauncher. SecureModules can't infer the module names
 * from those paths, so the game crashes without this patch.
 */
public final class ModDirTransformerDiscovererPatch extends ClassVisitor {
	public ModDirTransformerDiscovererPatch(ClassVisitor classVisitor) {
		super(Opcodes.ASM9, classVisitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		MethodVisitor next = super.visitMethod(access, name, descriptor, signature, exceptions);

		if (name.equals("isServiceProvider") && descriptor.equals("(Ljava/nio/file/Path;)Z")) {
			return new MethodVisitor(Opcodes.ASM9, next) {
				@Override
				public void visitCode() {
					super.visitCode();

					// Don't even try to examine the path if it's not a default fs path, just return false.
					var after = new Label();
					visitVarInsn(Opcodes.ALOAD, 0);
					visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/nio/file/Path", "getFileSystem", "()Ljava/nio/file/FileSystem;", true);
					visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/file/FileSystems", "getDefault", "()Ljava/nio/file/FileSystem;", false);
					visitJumpInsn(Opcodes.IF_ACMPEQ, after);
					visitInsn(Opcodes.ICONST_0);
					visitInsn(Opcodes.IRETURN);
					visitLabel(after);
				}
			};
		}

		return next;
	}
}
