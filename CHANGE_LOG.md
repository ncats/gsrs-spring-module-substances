# Change Log For Substance Module

## 0.8.2 

### Renderer
* new conf property for named renderer options so you don't need to include a json file.
  There are 2 new conf parameters : `substance.renderer.style` which defaults
  to a value of `"CONF"`  which means to use the conf file specified by
  `substance.renderer.configPath`.  You may also set this value to any of the named Renderers
  such as `INN`, `USP`, `ball and stick`, or `DEFAULT`. and it will use those
  pre-defined render settings.
  For backwards compatibility, we also allow for the GSRS 2.x conf parameter `gsrs.renderers.selected` which can have
  the same values.
  If both `substance.renderer.style` and `gsrs.renderers.selected` are specified,
  precedence is given to `gsrs.renderers.selected`.
  
## 0.7.5

### Renderer
* new conf property for default renderer options json file is now called `substance.renderer.configPath`.
  If not overridden by client conf, the default is specified in substances-core.conf is 
  `substance.renderer.configPath="substances-default-renderer.json"`.
  
### Initial Substance Loader
* gsrs file specified by property ix.ginas.load.file only loaded now if substance table is empty
### Testing
  * added new helper methods to `AbstractSubstanceJpaEntityTestSuperClass` to pull out
    filtered JSON records from gsrs encoded files either as a Stream or as a Yield. optional 
    parameters can filter by SubstanceClass(es). Any more involved filtering requires
    parsing the JSON into a Substance object.
    
#### Testing API Changes
  * changed name of `AbstractSubstanceJpaEntityTest2` to `AbstractSubstanceJpaEntityTestSuperClass`
