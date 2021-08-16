package gsrs.module.substance.processors;

import ix.ginas.models.v1.Code;

import java.util.Optional;

/**
 * An interface to generate URLs for given Code and codeSystem.
 *
 * Created by katzelda on 6/29/18.
 */
public interface CodeSystemUrlGenerator {

    /**
     * Generate the URL for the given {@link Code}
     * object.
     * @param code the Code to inspect to generate the URL.
     * @return an Optional String.  If the optional is empty then
     * no URL was found.
     * @throws NullPointerException if code is null.
     */
    Optional<String> generateUrlFor(Code code);
}
