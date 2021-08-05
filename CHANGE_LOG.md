# Change Log For Substance Module

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
