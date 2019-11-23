package club.bayview.smoothieweb.models;

public enum JudgeLanguage {

    // DO NOT RENAME ENUM VALUES, IT WILL BREAK THE DATABASE!
    // feel free to change prettyName

    // used for referring to all languages
    ALL ("all"),

    // c
    C11 ("C11"),

    // c++
    CPP11 ("C++11"),

    // java
    JAVA8 ("Java 1.8"),
    JAVA11 ("Java 11"),

    // python
    PYTHON3 ("Python 3"),

    ;

    private final String prettyName;
    JudgeLanguage(String prettyName) {
        this.prettyName = prettyName;
    }

    public String getPrettyName() {
        return prettyName;
    }

}
