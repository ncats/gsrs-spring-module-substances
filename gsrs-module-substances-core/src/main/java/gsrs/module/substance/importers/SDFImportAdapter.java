package gsrs.module.substance.importers;

import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import ix.ginas.models.v1.Substance;

import java.io.InputStream;
import java.util.stream.Stream;

public class SDFImportAdapter implements AbstractImportSupportingGsrsEntityController.ImportAdapter<Substance> {
    @Override
    public Stream<Substance> parse(InputStream is) {
        return null;
    }
}
