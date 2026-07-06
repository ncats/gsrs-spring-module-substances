package example.substance.processor;

import gsrs.cv.api.ControlledVocabularyApi;
import gsrs.cv.api.GsrsControlledVocabularyDTO;
import gsrs.cv.api.GsrsVocabularyTermDTO;
import gsrs.module.substance.processors.GroupProcessor;
import ix.core.models.Group;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupProcessorTest {

    private static final String ACCESS_DOMAIN = "ACCESS_GROUP";

    @Mock
    private ControlledVocabularyApi controlledVocabularyApi;

    private GroupProcessor processor;

    @BeforeEach
    void setup() {
        processor = new GroupProcessor();
        ReflectionTestUtils.setField(processor, "cvApi", controlledVocabularyApi);
    }

    @Test
    void postPersistAddsMissingGroupToAccessVocabulary() throws IOException {
        when(controlledVocabularyApi.findByDomain(ACCESS_DOMAIN)).thenReturn(Optional.of(accessGroupVocabulary()));

        Group labWorkers = group("Lab Workers");
        processor.postPersist(labWorkers);

        ArgumentCaptor<GsrsControlledVocabularyDTO> updatedVocabulary =
                ArgumentCaptor.forClass(GsrsControlledVocabularyDTO.class);
        verify(controlledVocabularyApi).update(updatedVocabulary.capture());

        GsrsControlledVocabularyDTO vocabulary = updatedVocabulary.getValue();
        assertTrue(vocabulary.getTerms().stream()
                .anyMatch(term -> labWorkers.name.equals(term.getValue()) && labWorkers.name.equals(term.getDisplay())));
        assertEquals("ix.ginas.models.v1.ControlledVocabulary", vocabulary.getVocabularyTermType());
    }

    @Test
    void postPersistDoesNotAddDuplicateGroupTerm() throws IOException {
        when(controlledVocabularyApi.findByDomain(ACCESS_DOMAIN)).thenReturn(Optional.of(accessGroupVocabulary()));

        processor.postPersist(group("protected"));

        verify(controlledVocabularyApi, never()).update(any(GsrsControlledVocabularyDTO.class));
    }

    @Test
    void postPersistDoesNothingWhenAccessVocabularyIsMissing() throws IOException {
        when(controlledVocabularyApi.findByDomain(ACCESS_DOMAIN)).thenReturn(Optional.empty());

        processor.postPersist(group("Lab Workers"));

        verify(controlledVocabularyApi, never()).update(any(GsrsControlledVocabularyDTO.class));
    }

    @Test
    void postUpdateUsesSameVocabularyUpdatePath() throws IOException {
        when(controlledVocabularyApi.findByDomain(ACCESS_DOMAIN)).thenReturn(Optional.of(accessGroupVocabulary()));

        processor.postUpdate(group("Reviewers"));

        verify(controlledVocabularyApi).update(any(GsrsControlledVocabularyDTO.class));
    }

    private static GsrsControlledVocabularyDTO accessGroupVocabulary() {
        ArrayList<GsrsVocabularyTermDTO> terms = new ArrayList<>();
        terms.add(GsrsVocabularyTermDTO.builder()
                .display("protected")
                .value("protected")
                .hidden(true)
                .build());

        return GsrsControlledVocabularyDTO.builder()
                .domain(ACCESS_DOMAIN)
                .terms(terms)
                .vocabularyTermType("ix.ginas.models.v1.ControlledVocabulary")
                .build();
    }

    private static Group group(String name) {
        Group group = new Group();
        group.name = name;
        return group;
    }
}
