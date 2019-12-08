package club.bayview.smoothieweb.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JudgeLanguage {

    // DO NOT RENAME ENUM VALUES, IT WILL BREAK THE DATABASE!
    // not enum so that spring data mongodb can serialize the full object properly
    // feel free to change prettyName

    // used for referring to all languages
    public static JudgeLanguage ALL = new JudgeLanguage("ALL", "All"),

    // c
    C11 = new JudgeLanguage("c11", "C11"),

    // c++
    CPP11 = new JudgeLanguage("c++11", "C++11"),
    CPP14 = new JudgeLanguage("c++14", "C++14"),
    CPP17 = new JudgeLanguage("c++17", "C++17"),
    CPP98 = new JudgeLanguage("c++98", "C++98"),

    // java
    JAVA8 = new JudgeLanguage("java8", "Java 1.8"),
    JAVA11 = new JudgeLanguage("java11", "Java 11"),

    // python
    PYTHON3 = new JudgeLanguage("python3", "Python 3")
            ;

    public static List<JudgeLanguage> values = Arrays.asList(ALL, C11, CPP11, CPP14, CPP17, CPP98, JAVA8, JAVA11, PYTHON3);

    public static List<JudgeLanguage> getLanguages() {
        var langs = new ArrayList<>(values);
        langs.remove(ALL);
        return langs;
    }

    public static String nameToPretty(String name) {
        for (var lang : values) if (name.equals(lang.getName())) return lang.getPrettyName();
        return null;
    }

    public static String prettyToName(String pretty) {
        for (var lang : values) if (pretty.equals(lang.getPrettyName())) return lang.getName();
        return null;
    }

    public static JudgeLanguage valueOf(String str) {
        for (var lang : values) if (str.equals(lang.getName())) return lang;
        return null;
    }

    private final String name, prettyName;
    JudgeLanguage(String name, String prettyName) {
        this.name = name;
        this.prettyName = prettyName;
    }

    public String getName() {
        return name;
    }

    public String getPrettyName() {
        return prettyName;
    }

}
