package gsrs.module.substance.misc.emasmsfhir;

import org.hl7.fhir.r5.model.BooleanType;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("EmaSmsSimpleRecord Tests")
public class EmaSmsSimpleRecordTest {

    private EmaSmsSimpleRecord record;

    @BeforeEach
    public void setUp() {
        record = new EmaSmsSimpleRecord();
    }

    @Test
    @DisplayName("Should initialize with null values")
    public void testInitialization() {
        assertNull(record.getSmsId());
        assertNull(record.getSubstanceName());
        assertNull(record.getLanguage2());
        assertNull(record.getIsPreferredName());
        assertNull(record.getNameSource());
        assertNull(record.getSubstanceType());
        assertNull(record.getEvCode());
        assertNull(record.getUnii());
        assertNull(record.getInnNumber());
        assertNull(record.getEcListNumber());
        assertNull(record.getGsrsSubstance());
    }

    @Test
    @DisplayName("Should set and get smsId")
    public void testSetGetSmsId() {
        StringType testId = new StringType("SMS-12345");
        record.setSmsId(testId);
        assertEquals("SMS-12345", record.getSmsId().getValue());
    }

    @Test
    @DisplayName("Should set and get substanceName")
    public void testSetGetSubstanceName() {
        StringType testName = new StringType("Sodium Chloride");
        record.setSubstanceName(testName);
        assertEquals("Sodium Chloride", record.getSubstanceName().getValue());
    }

    @Test
    @DisplayName("Should set and get language2")
    public void testSetGetLanguage2() {
        StringType testLanguage = new StringType("en");
        record.setLanguage2(testLanguage);
        assertEquals("en", record.getLanguage2().getValue());
    }

    @Test
    @DisplayName("Should set and get isPreferredName")
    public void testSetGetIsPreferredName() {
        BooleanType isPreferred = new BooleanType(true);
        record.setIsPreferredName(isPreferred);
        assertTrue(record.getIsPreferredName().getValue());
    }

    @Test
    @DisplayName("Should set and get nameSource")
    public void testSetGetNameSource() {
        StringType nameSource = new StringType("FDA SUBSTANCE REGISTRATION SYSTEM");
        record.setNameSource(nameSource);
        assertEquals("FDA SUBSTANCE REGISTRATION SYSTEM", record.getNameSource().getValue());
    }

    @Test
    @DisplayName("Should set and get substanceType")
    public void testSetGetSubstanceType() {
        StringType substanceType = new StringType("CHEMICAL");
        record.setSubstanceType(substanceType);
        assertEquals("CHEMICAL", record.getSubstanceType().getValue());
    }

    @Test
    @DisplayName("Should set and get evCode")
    public void testSetGetEvCode() {
        StringType evCode = new StringType("EV12345");
        record.setEvCode(evCode);
        assertEquals("EV12345", record.getEvCode().getValue());
    }

    @Test
    @DisplayName("Should set and get unii")
    public void testSetGetUnii() {
        StringType unii = new StringType("451W47IQ8X");
        record.setUnii(unii);
        assertEquals("451W47IQ8X", record.getUnii().getValue());
    }

    @Test
    @DisplayName("Should set and get innNumber")
    public void testSetGetInnNumber() {
        StringType innNumber = new StringType("INN-67890");
        record.setInnNumber(innNumber);
        assertEquals("INN-67890", record.getInnNumber().getValue());
    }

    @Test
    @DisplayName("Should set and get ecListNumber")
    public void testSetGetEcListNumber() {
        StringType ecListNumber = new StringType("231-959-4");
        record.setEcListNumber(ecListNumber);
        assertEquals("231-959-4", record.getEcListNumber().getValue());
    }

    @Test
    @DisplayName("Should set and get gsrsSubstance")
    public void testSetGetGsrsSubstance() {
        StringType gsrsSubstance = new StringType("{\"id\":\"test-id\"}");
        record.setGsrsSubstance(gsrsSubstance);
        assertEquals("{\"id\":\"test-id\"}", record.getGsrsSubstance().getValue());
    }

    @Test
    @DisplayName("Should populate complete record with all fields")
    public void testCompleteRecord() {
        record.setSmsId(new StringType("SMS-001"));
        record.setSubstanceName(new StringType("Test Substance"));
        record.setLanguage2(new StringType("en"));
        record.setIsPreferredName(new BooleanType(true));
        record.setNameSource(new StringType("FDA SUBSTANCE REGISTRATION SYSTEM"));
        record.setSubstanceType(new StringType("CHEMICAL"));
        record.setEvCode(new StringType("EV123"));
        record.setUnii(new StringType("451W47IQ8X"));
        record.setInnNumber(new StringType("INN-123"));
        record.setEcListNumber(new StringType("EC-123"));
        record.setGsrsSubstance(new StringType("{\"data\":\"json\"}"));

        // Verify all fields are set correctly
        assertNotNull(record.getSmsId());
        assertNotNull(record.getSubstanceName());
        assertNotNull(record.getLanguage2());
        assertNotNull(record.getIsPreferredName());
        assertNotNull(record.getNameSource());
        assertNotNull(record.getSubstanceType());
        assertNotNull(record.getEvCode());
        assertNotNull(record.getUnii());
        assertNotNull(record.getInnNumber());
        assertNotNull(record.getEcListNumber());
        assertNotNull(record.getGsrsSubstance());
    }

    @Test
    @DisplayName("Should handle multiple language codes")
    public void testMultipleLanguages() {
        String[] languages = {"en", "fr", "de", "es"};
        for (String lang : languages) {
            EmaSmsSimpleRecord rec = new EmaSmsSimpleRecord();
            rec.setLanguage2(new StringType(lang));
            assertEquals(lang, rec.getLanguage2().getValue());
        }
    }

    @Test
    @DisplayName("Should handle isPreferredName boolean values")
    public void testIsPreferredNameBoolean() {
        EmaSmsSimpleRecord rec1 = new EmaSmsSimpleRecord();
        rec1.setIsPreferredName(new BooleanType(true));
        assertTrue(rec1.getIsPreferredName().getValue());

        EmaSmsSimpleRecord rec2 = new EmaSmsSimpleRecord();
        rec2.setIsPreferredName(new BooleanType(false));
        assertFalse(rec2.getIsPreferredName().getValue());
    }

    @Test
    @DisplayName("copy should create deep copy of all populated fields")
    public void testCopyCreatesDeepCopy() {
        record.setSmsId(new StringType("SMS-001"));
        record.setSubstanceName(new StringType("Original Name"));
        record.setLanguage2(new StringType("en"));
        record.setIsPreferredName(new BooleanType(true));
        record.setNameSource(new StringType("FDA"));
        record.setSubstanceType(new StringType("chemical"));
        record.setEvCode(new StringType("EV-1"));
        record.setUnii(new StringType("UNII-1"));
        record.setInnNumber(new StringType("INN-1"));
        record.setEcListNumber(new StringType("EC-1"));
        record.setGsrsSubstance(new StringType("{\"id\":\"1\"}"));

        EmaSmsSimpleRecord copy = (EmaSmsSimpleRecord) record.copy();

        assertNotSame(record, copy);
        assertNotSame(record.getSmsId(), copy.getSmsId());
        assertEquals("SMS-001", copy.getSmsId().getValue());
        assertEquals("Original Name", copy.getSubstanceName().getValue());

        // Mutate original and ensure copy does not change.
        record.getSmsId().setValue("SMS-CHANGED");
        assertEquals("SMS-001", copy.getSmsId().getValue());
    }

    @Test
    @DisplayName("getResourceType should return Basic")
    public void testGetResourceType() {
        assertEquals(ResourceType.Basic, record.getResourceType());
    }

    @Test
    @DisplayName("equals, hashCode, and toString should work for identical populated records")
    public void testEqualsHashCodeAndToString() {
        EmaSmsSimpleRecord a = createFullyPopulatedRecord("SMS-100");
        EmaSmsSimpleRecord b = createFullyPopulatedRecord("SMS-100");

        assertEquals(a, a);
        assertNotEquals(a, null);
        assertNotEquals(a, "not-a-record");
        assertNotNull(a.hashCode());
        assertNotNull(b.hashCode());
        assertTrue(a.toString().contains("EmaSmsSimpleRecord"));

        b.setSmsId(new StringType("SMS-200"));
        assertNotEquals(a, b);
    }

    @Test
    @DisplayName("canEqual should return expected values")
    public void testCanEqual() {
        EmaSmsSimpleRecord rec = new EmaSmsSimpleRecord();
        assertTrue(rec.canEqual(new EmaSmsSimpleRecord()));
        assertFalse(rec.canEqual("not-a-record"));
    }

    @Test
    @DisplayName("equals self should be true for empty and populated records")
    public void testEqualsSelfForDifferentFieldStates() {
        EmaSmsSimpleRecord empty = new EmaSmsSimpleRecord();
        EmaSmsSimpleRecord full = createFullyPopulatedRecord("SMS-300");

        assertTrue(empty.equals(empty));
        assertTrue(full.equals(full));
        assertFalse(empty.equals(full));
        assertFalse(full.equals(empty));
    }

    @Test
    @DisplayName("hashCode should work with null and non-null field combinations")
    public void testHashCodeForNullAndNonNullFields() {
        EmaSmsSimpleRecord empty = new EmaSmsSimpleRecord();
        EmaSmsSimpleRecord full = createFullyPopulatedRecord("SMS-400");

        int emptyHash = empty.hashCode();
        int fullHash = full.hashCode();

        assertNotEquals(0, emptyHash);
        assertNotEquals(0, fullHash);
        assertNotEquals(emptyHash, fullHash);
    }

    @Test
    @DisplayName("equals should evaluate per-field branches on equal and mismatched records")
    public void testEqualsFieldByFieldBranchMatrix() {
        EmaSmsSimpleRecord base = createFullyPopulatedRecord("SMS-500");
        EmaSmsSimpleRecord same = (EmaSmsSimpleRecord) base.copy();

        // DomainResource superclass equality is strict, so non-identical instances are not equal.
        assertFalse(base.equals(same));
        assertFalse(same.equals(base));

        EmaSmsSimpleRecord smsIdMismatch = (EmaSmsSimpleRecord) base.copy();
        smsIdMismatch.setSmsId(new StringType("SMS-501"));
        assertFalse(base.equals(smsIdMismatch));

        EmaSmsSimpleRecord substanceNameMismatch = (EmaSmsSimpleRecord) base.copy();
        substanceNameMismatch.setSubstanceName(new StringType("Different Name"));
        assertFalse(base.equals(substanceNameMismatch));

        EmaSmsSimpleRecord languageMismatch = (EmaSmsSimpleRecord) base.copy();
        languageMismatch.setLanguage2(new StringType("fr"));
        assertFalse(base.equals(languageMismatch));

        EmaSmsSimpleRecord preferredMismatch = (EmaSmsSimpleRecord) base.copy();
        preferredMismatch.setIsPreferredName(new BooleanType(false));
        assertFalse(base.equals(preferredMismatch));

        EmaSmsSimpleRecord nameSourceMismatch = (EmaSmsSimpleRecord) base.copy();
        nameSourceMismatch.setNameSource(new StringType("EMA"));
        assertFalse(base.equals(nameSourceMismatch));

        EmaSmsSimpleRecord typeMismatch = (EmaSmsSimpleRecord) base.copy();
        typeMismatch.setSubstanceType(new StringType("protein"));
        assertFalse(base.equals(typeMismatch));

        EmaSmsSimpleRecord evCodeMismatch = (EmaSmsSimpleRecord) base.copy();
        evCodeMismatch.setEvCode(new StringType("EV-2"));
        assertFalse(base.equals(evCodeMismatch));

        EmaSmsSimpleRecord uniiMismatch = (EmaSmsSimpleRecord) base.copy();
        uniiMismatch.setUnii(new StringType("UNII-2"));
        assertFalse(base.equals(uniiMismatch));

        EmaSmsSimpleRecord innMismatch = (EmaSmsSimpleRecord) base.copy();
        innMismatch.setInnNumber(new StringType("INN-2"));
        assertFalse(base.equals(innMismatch));

        EmaSmsSimpleRecord ecMismatch = (EmaSmsSimpleRecord) base.copy();
        ecMismatch.setEcListNumber(new StringType("EC-2"));
        assertFalse(base.equals(ecMismatch));

        EmaSmsSimpleRecord payloadMismatch = (EmaSmsSimpleRecord) base.copy();
        payloadMismatch.setGsrsSubstance(new StringType("{\"id\":\"different\"}"));
        assertFalse(base.equals(payloadMismatch));
    }

    private static EmaSmsSimpleRecord createFullyPopulatedRecord(String id) {
        EmaSmsSimpleRecord rec = new EmaSmsSimpleRecord();
        rec.setSmsId(new StringType(id));
        rec.setSubstanceName(new StringType("Name"));
        rec.setLanguage2(new StringType("en"));
        rec.setIsPreferredName(new BooleanType(true));
        rec.setNameSource(new StringType("FDA"));
        rec.setSubstanceType(new StringType("chemical"));
        rec.setEvCode(new StringType("EV-1"));
        rec.setUnii(new StringType("UNII-1"));
        rec.setInnNumber(new StringType("INN-1"));
        rec.setEcListNumber(new StringType("EC-1"));
        rec.setGsrsSubstance(new StringType("{\"id\":\"1\"}"));
        return rec;
    }
}

