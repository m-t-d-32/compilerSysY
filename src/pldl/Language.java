package pldl;

public interface Language {
    GrammarDefinition[] getGrammarDefinition();
    TerminalRegex[] getTerminalRegexes();
    CommentRegex[] getCommentRegexes();
    String getMarkInStr();
}
