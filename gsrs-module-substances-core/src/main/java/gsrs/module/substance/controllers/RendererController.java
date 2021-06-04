package gsrs.module.substance.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.CachedSupplier;
import gov.nih.ncats.molwitch.Atom;
import gov.nih.ncats.molwitch.Bond;
import gov.nih.ncats.molwitch.Chemical;
import gov.nih.ncats.molwitch.MolwitchException;
import gov.nih.ncats.molwitch.io.CtTableCleaner;
import gov.nih.ncats.molwitch.renderer.ChemicalRenderer;
import gov.nih.ncats.molwitch.renderer.RendererOptions;
import gsrs.controller.GetGsrsRestApiMapping;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsRestApiController;
import gsrs.module.substance.SubstanceEntityServiceImpl;
import gsrs.module.substance.repository.StructureRepository;
import gsrs.module.substance.repository.SubstanceRepository;
import ix.core.chem.Chem;
import ix.core.models.Structure;
import ix.ginas.models.v1.Substance;
import ix.utils.UUIDUtil;
import org.freehep.graphicsio.svg.SVGGraphics2D;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@GsrsRestApiController(context = SubstanceEntityServiceImpl.CONTEXT)
public class RendererController {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private SubstanceRepository substanceRepository;

}
