# gsrs-ep-substance-extension
GSRS extension Module for Substances

This is the GSRS 3 extension module for working with IDMP Substances as specified by the ISO 11238 Substance Model.

## Installing the GSRS extension Module

Simply run:
```
./mvnw clean -U install -DskipTests
```

## Modules

### gsrs.module.substance.exporters.XslExporterFactory
The XslExporter can be used for the exporting of the substances using XSL template.

#### Dependencies
* net.sf.saxon.Saxon-HE

#### Configuration

```
ix.ginas.export.exporterfactories.substances.list.FhirR5 = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 3400,
    "parameters": {
        "format": {
            "extension": "fhir.r5.json",
            "displayName": "FHIR R5 JSON File"
        },
        "templateFile": "export-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":",
        "shouldCompress": false
    }
}
ix.ginas.export.exporterfactories.substances.list.Gsrsp = {
    "exporterFactoryClass": "gsrs.module.substance.exporters.XslExporterFactory",
    "order": 2100,
    "parameters": {
        "format": {
            "extension": "gsrsp",
            "displayName": "GSRS portable, gzipped JSON (.gsrs)"
        },
        "templateFile": "export-gsrsp.xsl",
        "header": "\t\t",
        "footer": "",
        "delimiter": "\n\t\t",
        "shouldCompress": true
    }
}
```

### gsrs.module.substance.importers.XslImportAdapterFactory
The XslImporteAdapter can be used for the importing of the substances using XSL template.

#### Dependencies
* net.sf.saxon.Saxon-HE

#### Configuration

```
gsrs.importAdapterFactories.substances.list.GSRSPImportAdapterFactory = {
    "adapterName": "GSRSP JSON Adapter",
    "importAdapterFactoryClass": "gsrs.module.substance.importers.XslImportAdapterFactory",
    "stagingAreaServiceClass": "gsrs.stagingarea.service.DefaultStagingAreaService",
    "entityServiceClass" :"gsrs.dataexchange.SubstanceStagingAreaEntityService",
    "description" : "Portable GSRSP JSON file importer",
    "supportedFileExtensions": [
        "gsrsp",
        "gz"
    ],
    "parameters": {
        "templateFile": "import-gsrsp.xsl",
        "delimiter": "\n\t\t"
    }
}

gsrs.importAdapterFactories.substances.list.FhirR5ImportAdapterFactory = {
    "adapterName": "GSRS FHIR R5 JSON Adapter",
    "importAdapterFactoryClass": "gsrs.module.substance.importers.XslImportAdapterFactory",
    "stagingAreaServiceClass": "gsrs.stagingarea.service.DefaultStagingAreaService",
    "entityServiceClass" :"gsrs.dataexchange.SubstanceStagingAreaEntityService",
    "description" : "GSRS FHIR R5 JSON file importer",
    "supportedFileExtensions": [
        "json"
    ],
    "parameters": {
        "templateFile": "import-gsrs-fhir-json.xsl",
        "header": "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\": [{\"resource\":",
        "footer": "}}]}",
        "delimiter": "},{\"resource\":"
    }
}
```

### gsrs.module.substance.processors.SubstanceReferenceProcessor
The SubstanceReferenceProcessor can be used to fix broken substance references after substances import from external GSRS system.

#### Configuration

```
gsrs.entityProcessors.list.SubstanceReferenceProcessor = {
    "entityClassName" = "ix.ginas.models.v1.SubstanceReference",
    "processor" = "gsrs.module.substance.processors.SubstanceReferenceProcessor",
    "order" = 2400,
    "with" = {
        "codeSystemPatterns" : [
            {"pattern": "^[0-9A-Z]{10}$", "codeSystem": "FDA UNII"}
        ]
    }
}
```

### gsrs.module.substance.tasks.UpdateEntityTaskInitializer
The UpdateEntityTaskInitializer task can be used for updating attributes from any Entity class in the GSRS
The optional parameter "query" can be used for granular selection of the objects.
The optional parameter "resetFields" can be used to nullify specified fields before invoking the "preUpdate" method.

#### Configuration

```
gsrs.scheduled-tasks.list.UpdateEntityTaskInitializer = {
    "scheduledTaskClass" : "gsrs.module.substance.tasks.UpdateEntityTaskInitializer",
    "order": 2000,
    "parameters" : {
        "entityClass": "ix.ginas.models.v1.Code",
        "query": "select uuid from Code where codeSystem = 'CAS'",
        "resetFields": ["url"],
        "autorun": false
    }
}
```
