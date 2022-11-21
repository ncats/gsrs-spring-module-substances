package gsrs.module.substance.standardizer;

/**
 * *
 * A message about a text modification. position is the starting point in
 * the original string where a character to be removed occurred replacement
 * is the character that was replaced
 */
public class ReplacementNote {

    private int position;
    private String replacement;

    public ReplacementNote(int position, String replacement) {
        this.position = position;
        this.replacement = replacement;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }

    @Override
    public String toString() {
        return String.format("position: %d; replacement: %s", position, replacement);
    }
}