package example.loadertests;

import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import ix.ginas.models.v1.Substance;
import java.util.function.Consumer;

/**
 *
 * populate a facet with the ration of letter to numbers in the substance GUIID.
 * Useful only to test that we can create arbitrary facets.
 * @author mitch
 */
public class TestableFacetIndexValueMaker implements IndexValueMaker<Substance> {

    @Override
    public Class<Substance> getIndexedEntityClass() {
        return Substance.class;
    }

    @Override
    public void createIndexableValues(Substance substance, Consumer<IndexableValue> consumer) {
        addLetterNumberRatioFacet(substance, consumer);
    }

    public static final String LETTER_DIGIT_RATIO_NAME = "LettersToDigits";
    private final double[] LETTER_DIGIT_RATIO_RANGE = new double[]{0,0.2,0.4,0.6,0.8,1};

    public void addLetterNumberRatioFacet(Substance s, Consumer<IndexableValue> consumer) {

        int letterCount = 0;
        int numberCount = 0;
        for (char c : s.uuid.toString().toCharArray()) {
            if (Character.isDigit(c)) {
                numberCount++;
            }
            else if (Character.isAlphabetic(c)) {
                letterCount++;
            }
        }

        double ratio =( new Double(letterCount) / new Double(numberCount));
        //System.out.println("Creating letter/digit ratio: " + ratio);
        consumer.accept(IndexableValue.simpleFacetDoubleValue(LETTER_DIGIT_RATIO_NAME, ratio,
                LETTER_DIGIT_RATIO_RANGE));

    }

}
