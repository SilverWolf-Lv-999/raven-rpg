package keystrokesmod.utility.question;

public final class QuestionEntry {
    private final String answer;
    private final String type;

    public QuestionEntry(String type, String answer) {
        this.answer = answer;
        this.type = type;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isClick() {
        return "click".equalsIgnoreCase(type);
    }

    public boolean isSend() {
        return "chat".equalsIgnoreCase(type) || "send".equalsIgnoreCase(type);
    }
}
