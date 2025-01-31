package org.cneko.toneko.common.mod.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

public class CustomStringArgument implements ArgumentType<String> {
    private final Pattern pattern;
    private final boolean allowEmpty;
    // 支持所有字符（不包括Emoji），长度限制为1~20
    private static final Pattern NO_EMOJI = Pattern.compile("^[\\x00-\\x7F\\u0080-\\uFFFF]{1,20}$");
    // 支持所有字符（包括Emoji），长度限制为1~40
    public static final Pattern ALLOW_EMOJI = Pattern.compile("^.{1,40}$\n");

    private CustomStringArgument(Pattern pattern, boolean allowEmpty) {
        this.pattern = pattern;
        this.allowEmpty = allowEmpty;
    }

    public static CustomStringArgument blockWord() {
        return new CustomStringArgument(NO_EMOJI, false);
    }
    public static CustomStringArgument replaceWord() {
        return new CustomStringArgument(ALLOW_EMOJI, true);
    }

    public static CustomStringArgument string(Pattern pattern) {
        return new CustomStringArgument(pattern, false);
    }

    public static CustomStringArgument string(Pattern pattern, boolean allowEmpty) {
        return new CustomStringArgument(pattern, allowEmpty);
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        int start = reader.getCursor();

        // 读取直到遇到空格或结尾
        while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
            reader.skip();
        }

        String result = reader.getString().substring(start, reader.getCursor());

        // 空值检查
        if (!allowEmpty && result.isEmpty()) {
            throw new SimpleCommandExceptionType(Component.literal("输入不能为空")).create();
        }

        // 正则验证
        if (pattern != null && !pattern.matcher(result).matches()) {
            throw new SimpleCommandExceptionType(Component.literal("输入格式无效")).create();
        }

        return result;
    }
}