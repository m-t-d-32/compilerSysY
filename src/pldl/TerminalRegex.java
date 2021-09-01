package pldl;

public class TerminalRegex {
    String terminal;
    String regex;

    TerminalRegex(String terminal, String regex) {
        this.terminal = terminal;
        this.regex = regex;
    }

    public String getTerminal() {
        return terminal;
    }

    public String getRegex() {
        return regex;
    }
}
