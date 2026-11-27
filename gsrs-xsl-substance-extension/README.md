# gsrs-xsl-substance-extension
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

#### Additional XSL template parameters

Both factories support the optional `xslParameters` property, a name/value map of
extra parameters set on the XSL transformer in addition to the ones provided by
the adapter itself (`input` for the exporter; `input` and `input-format` for the
importer). Values are passed as strings. Parameter names can be plain (`name`),
prefixed (`prefix:name`), Clark (`{uri}name`) or EQName (`Q{uri}name`) notation.
Prefixed names require the corresponding mapping in the optional `xslNamespaces`
property.

The parameters can be supplied in three places:

| Place | Exporter | Importer |
|---|---|---|
| GSRS configuration file | `parameters` block of the factory entry | `parameters` block of the factory entry |
| Per request | detailed parameters of the export request | adapter settings of the import job |
| Built-in (always set by the adapter) | `input` | `input`, `input-format` |

Configuration values form the base, request-time values override them, and the
built-in parameters are applied last so they always win.

##### Exporter example

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
        "shouldCompress": false,
        "xslParameters": {
            "language": "en",
            "emit-ids": "true",
            "idmp:version": "2"
        },
        "xslNamespaces": {
            "idmp": "urn:gsrs:idmp"
        }
    }
}
```

The same values can be overridden for a single export request by sending them in
the request's detailed parameters:

```
{
    "format": "fhir.r5.json",
    "xslParameters": {
        "language": "de"
    }
}
```

`xslParameters` and `xslNamespaces` are published in the exporter's parameter
schema, so they are rendered as free-form key/value fields in the GSRS export
dialog.

##### Exporter example: resolving `linkingID` values with `linkingIdCodeSystem`

Every exported substance reference carries a `linkingID` that GSRS derives from
the reference's `approvalID`, `refuuid` or name, which only means something
inside the deployment the export was taken from. Setting `linkingIdCodeSystem` to a
code system name rewrites those values:

```
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
        "shouldCompress": true,
        "linkingIdCodeSystem": "FDA UNII"
    }
}
```

For every object that has a `refuuid`, the exporter looks up the referenced
substance by that uuid, finds its code in the configured code system, and writes
`linkingID` as `<codeSystem>:<code>`, for example `FDA UNII:C67P1734QJ`. The
rewrite happens on the substance JSON before it is handed to the XSL template, so
a template that keeps `linkingID` writes the resolved identifier.

A reference whose substance has no code in that system is left with the
`linkingID` it had, and each referenced substance is looked up only once per
export, because a substance cited from several places is still one row. Leaving
`linkingIdCodeSystem` empty (the default) keeps the identifiers GSRS derives.

The configured value is a default, so a single export can resolve a different
code system by passing `linkingIdCodeSystem` in the request's detailed parameters:

```
{
    "format": "gsrsp",
    "linkingIdCodeSystem": "SMS_ID"
}
```


##### GSRSP exporter example: setting `code-system`

The `export-gsrsp.xsl` template adds a provenance entry to every exported
substance. Its `citation` names the code system the portable document is written
for, and it comes from the `code-system` parameter, which defaults to `FDA UNII`:

```xml
<xsl:param name="code-system" as="xs:string" select="'FDA UNII'"/>
```

A registry that records another identifier system sets the parameter instead of
editing the stylesheet. In the GSRS configuration file:

```
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
        "shouldCompress": true,
        "xslParameters": {
            "code-system": "SMS_ID"
        }
    }
}
```

`code-system` is a plain name, so no `xslNamespaces` entry is needed. For a
single export the same override goes in the request's detailed parameters:

```
{
    "format": "gsrsp",
    "xslParameters": {
        "code-system": "SMS_ID"
    }
}
```

Because the parameter has a default in the stylesheet, an export that does not
set it still succeeds and is cited as `FDA UNII`.

##### Importer example

```
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
        "delimiter": "},{\"resource\":",
        "xslParameters": {
            "language": "en",
            "emit-ids": "true",
            "idmp:version": "2"
        },
        "xslNamespaces": {
            "idmp": "urn:gsrs:idmp"
        }
    }
}
```

For the importer the same values can be supplied per import job in the adapter
settings, either nested under `parameters` or directly on the settings node:

```
{
    "parameters": {
        "templateFile": "import-gsrs-fhir-json.xsl",
        "xslParameters": {
            "language": "de"
        }
    }
}
```

##### Importer example: remapping code system names with `codeSystemMappings`

The `codeSystemMappings` parameter is a name/value map that renames code systems
found in the imported records. Each entry maps an input code system name to the
name it must have after import. For example, records that use `UNII` as the code
system can be normalised to the canonical `FDA UNII`:

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
        "delimiter": "\n\t\t",
        "codeSystemMappings": {
            "UNII": "FDA UNII"
        }
    }
}
```

The remapping is applied to the `codeSystem` of every imported `Code` and to
the code system read from a `<codeSystem>:<code>` `linkingID` before the
referenced substance is looked up. Codes with a code system that has no entry in
the map are left unchanged.


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
