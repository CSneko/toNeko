package dev.architectury.loom.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import net.fabricmc.loom.util.ExceptionUtil;

public final class ClassVisitorUtil {
	public static void rewriteClassFile(Path path, UnaryOperator<ClassVisitor> visitorFactory) throws IOException {
		try {
			final byte[] inputBytes = Files.readAllBytes(path);
			final var reader = new ClassReader(inputBytes);
			final var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
			reader.accept(visitorFactory.apply(writer), 0);
			final byte[] outputBytes = writer.toByteArray();

			if (!Arrays.equals(inputBytes, outputBytes)) {
				Files.write(path, outputBytes);
			}
		} catch (IOException e) {
			throw ExceptionUtil.createDescriptiveWrapper(IOException::new, "Failed to patch " + path, e);
		}
	}
}
