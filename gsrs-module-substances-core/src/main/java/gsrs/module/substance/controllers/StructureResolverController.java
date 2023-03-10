package gsrs.module.substance.controllers;

import gsrs.module.substance.services.StructureResolverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * This structure resolver controller is in it's own class
 * because we might one day pull it out to its own microsevice.
 */
@RestController

public class StructureResolverController {

    @Autowired
    private StructureResolverService structureResolverService;

    /*
    GET	 /resolve/*name	ix.ncats.controllers.App.resolve(name: String)
GET	 /resolve	ix.ncats.controllers.App.resolve(name: String)
     */

    @GetMapping({"/resolve"})
    public List<StructureResolverService.ResolverResult> resolveByParam(@RequestParam("name") String name){
        return structureResolverService.resolve(name);
    }
    @GetMapping({"/resolve/{name}"})
    public List<StructureResolverService.ResolverResult> resolveByPath(@PathVariable("name") String name){
        return structureResolverService.resolve(name);
    }
}
