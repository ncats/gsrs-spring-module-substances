package example.structureSearch;

import gsrs.module.substance.services.SubstanceStructureSearchService;
import gsrs.module.substance.services.SubstanceStructureSearchService.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SanitizeStructureRequestTest {

    @Test
    public void allDefaults(){
        SubstanceStructureSearchService.SearchRequest request = new SubstanceStructureSearchService.SearchRequest();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertNull(sanitizedSearchRequest.getOrder());
        assertNull(sanitizedSearchRequest.getQueryStructure());
        assertEquals(SanitizedSearchRequest.getDefaultCutoff(),sanitizedSearchRequest.getCutoff());
        assertEquals(SanitizedSearchRequest.getDefaultFdim(),sanitizedSearchRequest.getFdim());
        assertEquals(SanitizedSearchRequest.getDefaultType(),sanitizedSearchRequest.getType());
    }

    @Test
    public void structureSet(){
        SubstanceStructureSearchService.SearchRequest request =  SubstanceStructureSearchService.SearchRequest.builder()
                .queryStructure("CCCCC")
                .build();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertEquals("CCCCC", sanitizedSearchRequest.getQueryStructure());
    }

    @Test
    public void cutoff(){
        SubstanceStructureSearchService.SearchRequest request =  SubstanceStructureSearchService.SearchRequest.builder()
                .cutoff(.5D)
                .build();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertEquals(.5D, sanitizedSearchRequest.getCutoff(), 0.01D);
    }

    @Test
    public void fdim(){
        SubstanceStructureSearchService.SearchRequest request =  SubstanceStructureSearchService.SearchRequest.builder()
                .fdim(99)
                .build();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertEquals(99, sanitizedSearchRequest.getFdim());
    }
    @Test
    public void order(){
        SubstanceStructureSearchService.SearchRequest request =  SubstanceStructureSearchService.SearchRequest.builder()
                .order("desc")
                .build();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertEquals("desc", sanitizedSearchRequest.getOrder());
    }

    @Test
    public void type(){
        SubstanceStructureSearchService.SearchRequest request =  SubstanceStructureSearchService.SearchRequest.builder()
                .type(StructureSearchType.SIMILARITY)
                .build();

        SubstanceStructureSearchService.SanitizedSearchRequest sanitizedSearchRequest = request.sanitize();

        assertEquals(StructureSearchType.SIMILARITY, sanitizedSearchRequest.getType());
    }
}
