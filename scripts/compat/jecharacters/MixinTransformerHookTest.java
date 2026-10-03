package me.towdium.jecharacters.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import me.towdium.jecharacters.asm.JechClassTransformer;
import me.towdium.jecharacters.asm.Transformer;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.IExtensionRegistry;
import org.spongepowered.asm.transformers.TreeTransformer;

import java.lang.reflect.Field;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Exercise the actual replacement class against the original JECharacters delegate and ASM pipeline. */
public final class MixinTransformerHookTest {
    private static int checks;
    private static final Field STACK;
    static {
        try {
            STACK = MixinTransformerHook.class.getDeclaredField("transformationStack");
            STACK.setAccessible(true);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    @SuppressWarnings("unchecked")
    private static boolean stackEmpty(MixinTransformerHook<?> hook) throws Exception {
        Object holder = STACK.get(hook);
        if (holder instanceof ThreadLocal<?> local) {
            boolean empty = ((Deque<String>) local.get()).isEmpty();
            if (empty) local.remove();
            return empty;
        }
        return ((Deque<String>) holder).isEmpty();
    }
    private static byte[] classBytes(String name) {
        ClassWriter out = new ClassWriter(0);
        out.visit(Opcodes.V25, Opcodes.ACC_PUBLIC, name.replace('.', '/'), null, "java/lang/Object", null);
        out.visitEnd();
        return out.toByteArray();
    }
    private static long markers(byte[] bytes) {
        ClassNode node = new ClassNode(); new ClassReader(bytes).accept(node, 0);
        return node.fields.stream().filter(field -> field.name.equals("jech_test_marker")).count();
    }
    private static MixinTransformerHook<Delegate> create(Delegate delegate, Probe transformer) {
        JsonObject config = new JsonObject(); config.addProperty("suffixClassName", "sample/Target");
        config.add("removals", new JsonArray());
        var hook = new MixinTransformerHook<>(delegate, new JechClassTransformer(List.of(transformer), config));
        delegate.hook = hook;
        return hook;
    }
    public static void main(String[] args) throws Exception {
        Delegate delegate = new Delegate(); Probe transformer = new Probe();
        var hook = create(delegate, transformer);
        byte[] untouched = classBytes("sample.Unrelated");
        check(hook.transformClassBytes("sample.Unrelated", "sample.Unrelated", untouched) == untouched,
                "Classes unrelated to search keep their original bytes");
        check(stackEmpty(hook), "Non-target early return must not leak a transformation stack entry");
        check(hook.transformClassBytes("sample.Generated", "sample.Generated", null) == null && stackEmpty(hook),
                "Generated null bytes are delegated without leaving an entry");
        byte[] target = classBytes("sample.Target");
        check(markers(hook.transformClassBytes("sample.Target", "sample.Target", target)) == 1 && stackEmpty(hook),
                "An accepted search class still passes through JECharacters ASM exactly once");

        delegate.fail = true;
        try { hook.transformClassBytes("sample.Target", "sample.Target", target); throw new AssertionError("Missing delegate exception"); }
        catch (InjectedFailure expected) { check(stackEmpty(hook), "Delegate exceptions clean up the stack"); }
        delegate.fail = false; transformer.fail = true;
        try { hook.transformClassBytes("sample.Target", "sample.Target", target); throw new AssertionError("Missing ASM exception"); }
        catch (InjectedFailure expected) { check(stackEmpty(hook), "ASM exceptions clean up the stack"); }
        transformer.fail = false;

        int before = transformer.calls.get(); delegate.recursive = true;
        check(markers(hook.transformClassBytes("sample.Target", "sample.Target", target)) == 1,
                "A same-class recursive load does not transform the class twice");
        check(transformer.calls.get() == before + 1 && stackEmpty(hook), "Recursive load keeps the outer entry intact");
        delegate.recursive = false;
        delegate.nested = true;
        check(markers(hook.transformClassBytes("sample.Target", "sample.Target", target)) == 1 && stackEmpty(hook),
                "A nested unrelated class returns without leaking or removing the outer entry");
        delegate.nested = false;

        // Start every worker on the same target class: another thread is not recursive loading.
        int workers = 32;
        delegate.barrier = new CyclicBarrier(workers); before = transformer.calls.get();
        try (var executor = Executors.newFixedThreadPool(workers)) {
            var jobs = java.util.stream.IntStream.range(0, workers).mapToObj(i -> executor.submit(() -> {
                byte[] result = hook.transformClassBytes("sample.Target", "sample.Target", target);
                return markers(result) == 1 && stackEmpty(hook);
            })).toList();
            for (var job : jobs) check(job.get(20, TimeUnit.SECONDS), "Concurrent target load transforms and cleans up on each worker");
        }
        check(transformer.calls.get() == before + workers, "All concurrent target loads retain the search transformation");
        delegate.barrier = null;
        try (var executor = Executors.newFixedThreadPool(workers)) {
            var jobs = java.util.stream.IntStream.range(0, workers).mapToObj(i -> executor.submit(() -> {
                for (int n = 0; n < 10000; n++) {
                    String name = "sample.Worker" + i + "_" + n;
                    if (hook.transformClassBytes(name, name, untouched) != untouched || !stackEmpty(hook)) return false;
                }
                return true;
            })).toList();
            for (var job : jobs) check(job.get(20, TimeUnit.SECONDS), "Parallel non-target loads leave no shared state or stale entries");
        }
        System.out.println("JECharacters hook: " + checks + " checks passed; 32 workers, 320,000 non-target loads; ASM retained.");
    }
    private static final class InjectedFailure extends RuntimeException {}
    private static final class Probe implements Transformer {
        final AtomicInteger calls = new AtomicInteger(); volatile boolean fail;
        @Override public Set<String> targetClasses() { return Set.of("sample/Target"); }
        @Override public boolean accept(String name) { return targetClasses().contains(name); }
        @Override public ClassNode transform(ClassNode node) {
            if (fail) throw new InjectedFailure();
            calls.incrementAndGet();
            node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "jech_test_marker", "I", null, null));
            return node;
        }
    }
    private static final class Delegate extends TreeTransformer implements IMixinTransformer {
        MixinTransformerHook<Delegate> hook;
        volatile boolean fail, recursive, nested;
        volatile CyclicBarrier barrier;
        final ThreadLocal<Boolean> entered = ThreadLocal.withInitial(() -> false);
        @Override public String getName() { return "JECharacters regression delegate"; }
        @Override public boolean isDelegationExcluded() { return false; }
        @Override public byte[] transformClassBytes(String name, String transformedName, byte[] bytes) {
            if (fail) throw new InjectedFailure();
            if (barrier != null) {
                try { barrier.await(10, TimeUnit.SECONDS); }
                catch (Exception e) { throw new RuntimeException(e); }
            }
            if (!entered.get() && (recursive || nested)) {
                entered.set(true);
                try {
                    if (recursive) hook.transformClassBytes(name, transformedName, bytes);
                    if (nested) hook.transformClassBytes("sample.Nested", "sample.Nested", classBytes("sample.Nested"));
                } finally { entered.remove(); }
            }
            return bytes;
        }
        @Override public void audit(MixinEnvironment environment) {}
        @Override public List<String> reload(String name, ClassNode node) { return List.of(); }
        @Override public boolean computeFramesForClass(MixinEnvironment environment, String name, ClassNode node) { return false; }
        @Override public byte[] transformClass(MixinEnvironment environment, String name, byte[] bytes) { return bytes; }
        @Override public boolean transformClass(MixinEnvironment environment, String name, ClassNode node) { return false; }
        @Override public boolean couldTransformClass(MixinEnvironment environment, String name) { return false; }
        @Override public byte[] generateClass(MixinEnvironment environment, String name) { return null; }
        @Override public boolean generateClass(MixinEnvironment environment, String name, ClassNode node) { return false; }
        @Override public IExtensionRegistry getExtensions() { return null; }
    }
}
