package gsrs.module.substance.standardizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the output of a text transformation. The result field contains
 * the transformed text. The ReplacementNotes contain information about the
 * specific replacements performed.
 */
public class ReplacementResult {

    private List<ReplacementNote> replacementNotes = new ArrayList<>();
    private String result;

    public ReplacementResult(String result, List<ReplacementNote> notes) {
        this.result = result;
        replacementNotes = notes;
    }

    public void update(String updatedResult, List<ReplacementNote> additionalNotes) {
        this.result = updatedResult;
        this.replacementNotes.addAll(additionalNotes);
    }

    public ReplacementResult update(ReplacementResult newResult) {
        this.result = newResult.getResult();
        this.replacementNotes.addAll(newResult.getReplacementNotes());
        return this;
    }

    public List<ReplacementNote> getReplacementNotes() {
        return replacementNotes;
    }

    public void setReplacementNotes(List<ReplacementNote> replacementNotes) {
        this.replacementNotes = replacementNotes;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return String.format("result: %s", this.result);
    }
    
    public static ReplacementResult of(String result) {
        return new ReplacementResult(result, new ArrayList<>());
    }
    public static ReplacementResult of(String result, List<ReplacementNote> notes) {
        return new ReplacementResult(result, notes);
    }
}