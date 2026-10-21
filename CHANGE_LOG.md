# Change Log For Substance Module

## Unreleased

### External configuration files removed
All configuration data that used to be read from external files at runtime is now shipped as
HOCON defaults and is no longer loaded from the filesystem or from a classpath resource:

* Salt data (`salt_data_public.tsv`, property `gsrs.substance.structures.saltFilePath`) is now
  supplied through `gsrs.substance.structures.salt-data` (see the new `salt-data.conf`, included
  from `substances-core.conf`). The `saltFilePath` property and the file loading code were removed.
* Code system URLs (`codeSystem.json`) are now supplied through the `CodeProcessor` configuration
  under `gsrs.entityProcessors.list.CodeProcessor.parameters.codeSystems` (see the new
  `code-systems.conf`, included from `substances-core.conf`). The optional classpath/file loading
  in `DefaultCodeSystemUrlGenerator` was removed, and `codeSystems` is now a simple map of code
  system name to URL template (`"MERCK INDEX" = "https://…/$CODE$"`) rather than a map of nested
  `{ codeSystem, url }` objects.
* Atomic weights (`atomicweightdataodd.csv`, property `atomWeightFilePath` in
  `ConfigurableMolweightProcessor`) are now supplied inline through the processor configuration as
  a `parameters.atomicWeights` list of `{ symbol, value, massNumber? }` entries. The file loading
  code and the `atomWeightFilePath` property were removed.

### Entity processors configured through `parameters`
`CodeProcessor` and `ConfigurableMolweightProcessor` are now configured through the
`parameters` attribute of their `gsrs.entityProcessors.list` entry rather than through `with`,
matching the convention used by the module's validators. For `CodeProcessor` the previous
`parameters.json` intermediate level was dropped, so the block now has exactly two attributes,
`class` and `codeSystems`. Both processors declare a delegating `@JsonCreator` constructor
because entity processors configured with `parameters` are built by Jackson.
* Default renderer options (`substances-default-renderer.json`, property
  `substance.renderer.configPath`) are no longer read from a file. Use the named renderer entries
  in `gsrs.renderers.list` (or `gsrs.renderers.selected`) instead. The
  `substance.renderer.configPath` property and the file loading code were removed.

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
