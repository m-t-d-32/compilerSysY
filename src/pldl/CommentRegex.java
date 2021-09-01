package pldl;

public class CommentRegex {
    String comment;
    String regex;

    CommentRegex(String comment, String regex) {
        this.comment = comment;
        this.regex = regex;
    }

    public String getComment() {
        return comment;
    }

    public String getRegex() {
        return regex;
    }
}
