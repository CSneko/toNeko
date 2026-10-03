package me.towdium.jecharacters.mixin;

import me.towdium.jecharacters.asm.JechClassTransformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.transformers.TreeTransformer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Local compatibility repair for the hook shipped in JECharacters Fabric 26.1.2 / 4.6.7. */
class MixinTransformerHook<T extends TreeTransformer & IMixinTransformer> extends MixinTransformerDelegate<T> {
    // Registry and resource loading may transform classes on several workers concurrently.
    // A shared deque corrupts its indices and confuses unrelated threads with recursive loads.
    private final ThreadLocal<Deque<String>> transformationStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final JechClassTransformer transformer;

    MixinTransformerHook(T delegate, JechClassTransformer transformer) {
        super(delegate);
        this.transformer = transformer;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null) return super.transformClassBytes(name, transformedName, null);
        Deque<String> stack = transformationStack.get();
        if (Objects.equals(stack.peek(), name)) {
            return super.transformClassBytes(name, transformedName, basicClass);
        }
        stack.push(name);
        try {
            basicClass = super.transformClassBytes(name, transformedName, basicClass);
            String internalName = name.replace('.', '/');
            if (transformer.getTransformers().stream().noneMatch(it -> it.accept(internalName))) return basicClass;
            ClassNode classNode = new ClassNode();
            new ClassReader(basicClass).accept(classNode, 0);
            transformer.transform(classNode);
            ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            classNode.accept(classWriter);
            return classWriter.toByteArray();
        } finally {
            // Include non-target classes and exceptions, which the original early return missed.
            stack.pop();
            if (stack.isEmpty()) transformationStack.remove();
        }
    }
}
