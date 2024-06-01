package org.cneko.toneko.common.util;

import java.util.HashSet;

public class Messaging {


    public static class PetPhrase{

        //有可能需要将它和后缀一起合并到PetPhrase里面
        public static HashSet<Character> IGNORE_CHARACTER;

        public final String phrase;

        public final int ignoreCharacterStart,ignoreAfter;
        public final boolean ignoreEnglish;

        public PetPhrase(String phrase, boolean ignoreEnglish,int ignoreAfter)throws IllegalArgumentException{
            this.phrase=phrase;
            ignoreCharacterStart= getLastIndexOfNotIgnoreCharacter(phrase)+1;
            this.ignoreEnglish=ignoreEnglish;
            if(ignoreAfter>ignoreCharacterStart) throw new IllegalArgumentException("ignoreAfter>=phrase.length()-ignoreCharacterStart");
            this.ignoreAfter=ignoreAfter;
        }

        public String addPhrase(String text){
            if(ignoreEnglish&&isTextEnglish(text))return text;
            int textIgnoreCharacterStart= getLastIndexOfNotIgnoreCharacter(text)+1;

            if(textIgnoreCharacterStart>=ignoreCharacterStart-ignoreAfter &&
                    text.substring(textIgnoreCharacterStart-(ignoreCharacterStart-ignoreAfter),textIgnoreCharacterStart)
                            .equals(phrase.substring(ignoreAfter,ignoreCharacterStart)) ) //如果text最后的有效部分与口癖的有效部分匹配 "ab".substring(2,2)合法
                return text;
//        System.out.println("\""+text.substring(textIgnoreCharacterStart-(ignoreCharacterStart-ignoreAfter),textIgnoreCharacterStart)+"\" \""+phrase.substring(ignoreAfter,ignoreCharacterStart)+"\"");

            if(textIgnoreCharacterStart==text.length()) //如果没有忽略的
                return text.substring(0,textIgnoreCharacterStart)+phrase;
            else return text.substring(0,textIgnoreCharacterStart)+phrase.substring(0,ignoreCharacterStart)+text.substring(textIgnoreCharacterStart);
        }

        /**
         *
         * @return 最后一个不是忽略字符的字符的索引。可能返回-1
         */
        public static int getLastIndexOfNotIgnoreCharacter(String text){
            int check=text.length();
            do {
                check--;
            } while (check >= 0 && IGNORE_CHARACTER.contains(text.charAt(check)));
            return check;
        }

        /**
         * @return 文本的所有字符是否都小于255
         */

        public static boolean isTextEnglish(String text){
            for(char c:text.toCharArray())
                if(c>255) return false;
            return true;
        }


        @Override
        public String toString() {
            return "PetPhrase{" +
                    "phrase='" + phrase + '\'' +
                    ", ignoreCharacterStart=" + ignoreCharacterStart +
                    ", ignoreAfter=" + ignoreAfter +
                    ", ignoreEnglish=" + ignoreEnglish +
                    '}';
        }
    }
}
