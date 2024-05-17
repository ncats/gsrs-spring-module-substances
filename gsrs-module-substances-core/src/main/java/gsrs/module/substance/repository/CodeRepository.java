package gsrs.module.substance.repository;

import gsrs.repository.GsrsVersionedRepository;
import ix.ginas.models.v1.Code;
import ix.ginas.models.v1.Name;
import ix.ginas.models.v1.Substance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Repository
@Transactional
public interface CodeRepository extends GsrsVersionedRepository<Code, UUID> {

    @Query("select s.code from Code s where s.codeSystem = ?1 and s.code like ?2")
    Stream<String> findCodeByCodeSystemAndCodeLike(String codesystem, String codeLike);

    Stream<Code> findCodesByCodeSystemAndCodeLike(String codesystem, String codeLike);

    @Query("select max(CAST(SUBSTRING(c.code, 1, LENGTH(c.code) - LENGTH(:codelike) + 1) as long)) from Code c where c.codeSystem = :codesystem and c.code like :codelike and CAST(SUBSTRING(c.code, 1, LENGTH(c.code) - LENGTH(:codelike) + 1) as long) <= :maxcode")
    Long findMaxCodeByCodeSystemAndCodeLikeAndCodeLessThan(@Param("codesystem") String codeSystem, @Param("codelike") String codeLike, @Param("maxcode") Long maxCode);

    //hibernate query will not convert uuid into a string so we have to concatenate it with empty string for this to work.
    @Query("select s from Name s where CONCAT(s.uuid, '') like ?1%")
    List<Code> findByUuidStartingWith(String partialUUID);

    List<Code> findByCodeIgnoreCase(String code);
    
    @Query("select c.uuid from Code c")
    List<UUID> getAllIDs();
    
}
