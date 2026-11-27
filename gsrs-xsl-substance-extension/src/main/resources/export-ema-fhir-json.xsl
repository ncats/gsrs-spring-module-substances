<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:ema="urn:gsrs:ema-rms"
                xmlns:local="urn:local"
                exclude-result-prefixes="ema fn map xs local">

    <xsl:output method="json" encoding="UTF-8"/>

    <xsl:param name="input" as="xs:string" required="yes"/>
    
    <!-- Current timestamp in ISO format for lastUpdated -->
    <xsl:variable name="current-timestamp" select="'2024-10-08T13:37:23.468+00:00'"/>

    <xsl:function name="local:epoch-to-iso" as="xs:string">
        <xsl:param name="epoch-ms" as="xs:string"/>
        <xsl:sequence select="fn:format-dateTime(
            xs:dateTime('1970-01-01T00:00:00Z')
                + xs:dayTimeDuration(concat('PT', xs:string(xs:integer(number($epoch-ms) div 1000)), 'S')),
            '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/>
    </xsl:function>

    <!-- The EMA list vocabulary lives in one shared file, keyed by term id.
         ``ema-rms-lists.xsl`` also carries the ``ema:index`` helper this side
         uses to turn those tables into the GSRS-keyed lookups below, and the
         term ``ema:term-field`` reads the display name back out of. -->
    <xsl:include href="ema-rms-lists.xsl"/>

    <!-- List 100000000009: code system, keyed by the GSRS ``codeSystem`` name.
         ECHA resolves to the id ``import-ema-fhir-json.xsl`` also accepts, so
         a document written here reads back unchanged. -->
    <xsl:variable name="code-system-term-map" as="map(*)"
                  select="ema:index($ema-code-system-terms, 'value',
                                    $ema-code-system-preferred)"/>

    <!-- List 100000075826: substance category, keyed by the GSRS
         ``substanceClass`` value. A class the list does not name falls back to
         the chemical term at the call site. -->
    <xsl:variable name="category-term-map" as="map(*)"
                  select="ema:index($ema-category-terms, 'value')"/>

    <!-- List 100000072057: language, keyed by the ISO 639-1 two letter code
         GSRS stores. A code the list does not name falls back to English at
         the call site. -->
    <xsl:variable name="language-term-map" as="map(*)"
                  select="ema:index($ema-language-terms, 'value')"/>

    <!-- List 100000000009 again, as a citation table: keyed by the label a
         GSRS ``EMA LIST`` reference quotes, so the export can turn that label
         back into the term id a FHIR ``name.source`` names.

         Twelve labels appear on more than one term in the list, and
         ``ema:index`` keeps the highest id for those. That is the rule the
         code system table follows, and it is the right one here too: a
         citation quotes a label, the label is shared, and any of the terms
         behind it names the same source. A reference whose citation the list
         does not carry is left out of the output rather than written with a
         term id that would not read back. -->
    <xsl:variable name="citation-term-map" as="map(*)"
                  select="ema:index($ema-code-system-terms, 'label')"/>

    <!-- One GSRS reference paired with the list term its citation names, or
         an empty sequence when the reference is not a citation of this list
         or the list does not carry what it cites.

         Only ``docType`` ``EMA LIST`` references are the list's own; a
         reference of any other kind may quote a phrase that happens to equal
         a term label, and reading it as a term id would invent a source the
         document never had. Past that test the label is the only way back to
         a term id, so a citation the list does not carry is dropped here as
         well rather than exported as a source pointing at no term. Both the
         map and the names side then see nothing. -->
    <xsl:function name="local:citation-entry" as="map(*)?">
        <xsl:param name="reference" as="element()"/>
        <xsl:param name="terms-by-label" as="map(*)"/>
        <xsl:variable name="term" as="xs:string?"
                      select="if (string($reference/fn:string[@key='docType']) = 'EMA LIST')
                          then map:get($terms-by-label,
                                       string($reference/fn:string[@key='citation']))
                          else ()"/>
        <xsl:sequence select="if (exists($term))
            then map:entry(string($reference/fn:string[@key='uuid']), $term)
            else ()"/>
    </xsl:function>

    <!-- The list term id a name's cited reference stands for, or an empty
         sequence when the reference is not in the map. The uuid itself is a
         GSRS row key and is not passed on: it was only ever needed to look
         the term up. -->
    <xsl:function name="local:source-entry" as="xs:string?">
        <xsl:param name="reference" as="xs:string"/>
        <xsl:param name="terms-by-uuid" as="map(*)"/>
        <xsl:sequence select="map:get($terms-by-uuid, $reference)"/>
    </xsl:function>

    <xsl:template match="/">
        <!-- Parse the JSON input string -->
        <xsl:variable name="json-doc" select="fn:json-to-xml($input)"/>
        
        <!-- Extract key values from source -->
        <xsl:variable name="root" select="$json-doc/fn:map"/>

        <!-- The GSRS reference uuids this document cites, keyed to the term id
             each one stands for. A reference the list does not carry is
             missing from the map, which is how the names side skips it. -->
        <xsl:variable name="citation-terms-by-uuid" as="map(*)"
                      select="map:merge(
                          for $reference in $root/fn:array[@key='references']/fn:map
                          return local:citation-entry($reference, $citation-term-map))"/>

        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="changeReason" select="string($root/fn:string[@key='changeReason'])"/>
        <!-- The substance's own identifier, taken from the code GSRS flags as
             the primary one and drawn from either spelling of the SMS code
             system. A GSRS uuid names a row in one database and means nothing
             to a reader of this document, so it is not exported as an ``id``.
             ``[1]`` takes the first match, because a document that names more
             than one primary code has no second place to put it. -->
        <xsl:variable name="smsid" as="xs:string"
                      select="string(($root/fn:array[@key='codes']/fn:map[
                          fn:string[@key='type'] = 'PRIMARY' and
                          fn:string[@key='codeSystem'] = ('SMSID', 'SMS_ID')]/fn:string[@key='code'])[1])"/>
        <xsl:variable name="substanceClass" select="string($root/fn:string[@key='substanceClass'])"/>
        <xsl:variable name="created" select="local:epoch-to-iso(string($root/fn:number[@key='created']))"/>
        <xsl:variable name="lastEdited" select="local:epoch-to-iso(string($root/fn:number[@key='lastEdited']))"/>

        <!-- Transform the GSRS substance to FHIR SubstanceDefinition -->
        <xsl:variable name="transformed-xml">
            <fn:map xmlns:fn="http://www.w3.org/2005/xpath-functions">
                <!-- Resource metadata -->
                <fn:string key="resourceType">SubstanceDefinition</fn:string>
                <!-- Written only when the substance carries a primary SMS
                     code: an ``id`` invented from anything else would name
                     the substance wrongly, and FHIR treats a missing id as
                     one the server should assign. -->
                <xsl:if test="$smsid">
                    <fn:string key="id">
                        <xsl:value-of select="$smsid"/>
                    </fn:string>
                </xsl:if>
                
                <!-- Meta information -->
                <fn:map key="meta">
                    <fn:string key="versionId">
                        <xsl:value-of select="$version"/>
                    </fn:string>
                    <fn:string key="lastUpdated">
                        <xsl:value-of select="$current-timestamp"/>
                    </fn:string>
                </fn:map>
                
                <!-- Contained resources (Provenance) -->
                <fn:array key="contained">
                    <fn:map>
                        <fn:string key="resourceType">Provenance</fn:string>
                        <fn:array key="target">
                            <fn:map>
                                <fn:string key="reference">#</fn:string>
                            </fn:map>
                        </fn:array>
                        <fn:string key="occurredDateTime">
                            <xsl:value-of select="$created"/>
                        </fn:string>
                        <fn:string key="recorded">
                            <xsl:value-of select="$lastEdited"/>
                        </fn:string>
                        <xsl:if test="exists($changeReason) and $changeReason ne ''">
                            <fn:array key="reason">
                                <fn:map>
                                    <fn:string key="text">
                                        <xsl:value-of select="$changeReason"/>
                                    </fn:string>
                                </fn:map>
                            </fn:array>
                        </xsl:if>
                        <fn:array key="agent">
                            <fn:map>
                                <fn:array key="role">
                                    <fn:map>
                                        <fn:array key="coding">
                                            <fn:map>
                                                <fn:string key="system">http://terminology.hl7.org/CodeSystem/provenance-participant-type</fn:string>
                                                <fn:string key="code">author</fn:string>
                                            </fn:map>
                                        </fn:array>
                                    </fn:map>
                                </fn:array>
                                <fn:map key="who">
                                    <fn:map key="identifier">
                                        <fn:string key="system">http://ema.europa.eu/OMS</fn:string>
                                        <fn:string key="value">ORG-100013412</fn:string>
                                    </fn:map>
                                </fn:map>
                            </fn:map>
                        </fn:array>
                    </fn:map>
                </fn:array>
                
                <!-- Extensions -->
                <fn:array key="extension">
                    <fn:map>
                        <fn:string key="url">https://ema.europa.eu/fhir/currentSubstance</fn:string>
                        <fn:map key="valueReference">
                            <fn:string key="reference">SubstanceDefinition/</fn:string>
                        </fn:map>
                    </fn:map>
                    <xsl:call-template name="access">
                        <xsl:with-param name="access-field" select="$root/fn:array[@key='access']"/>
                    </xsl:call-template>
                </fn:array>
                
                <!-- Identifier -->
                <xsl:if test="$smsid">
                    <fn:map key="identifier">
                        <fn:string key="system">https://spor.azure-api.net/sms/api/v2/SubstanceDefinition</fn:string>
                        <fn:string key="value">
                            <xsl:value-of select="$smsid"/>
                        </fn:string>
                    </fn:map>
                </xsl:if>
                
                <!-- Version -->
                <fn:string key="version">
                    <xsl:value-of select="$version"/>
                </fn:string>
                
                <!-- Status -->
                <fn:map key="status">
                    <xsl:call-template name="status">
                        <xsl:with-param name="deprecated-field" select="$root/fn:boolean[@key='deprecated']"/>
                    </xsl:call-template>
                </fn:map>
                
                <!-- Category -->
                <xsl:if test="not($substanceClass = ('unspecifiedSubstance', 'concept', 'reference'))">
                    <fn:map key="category">
                        <xsl:call-template name="category">
                            <xsl:with-param name="substanceClass-value" select="$substanceClass"/>
                        </xsl:call-template>
                    </fn:map>
                </xsl:if>
                
                <!-- Domain -->
                <fn:map key="domain">
                    <fn:array key="coding">
                        <fn:map>
                            <fn:string key="system">
                                <xsl:value-of select="ema:list-system($ema-domain-list)"/>
                            </fn:string>
                            <fn:string key="code">
                                <xsl:value-of select="$ema-domain-human-use"/>
                            </fn:string>
                            <fn:string key="display">
                                <xsl:value-of select="ema:term-field(
                                    $ema-domain-terms, $ema-domain-human-use, 'label')"/>
                            </fn:string>
                        </fn:map>
                    </fn:array>
                </fn:map>
                
                <!-- Names -->
                <fn:array key="name">
                    <xsl:for-each select="$root/fn:array[@key='names']/fn:map">
                        <xsl:variable name="name-value" select="fn:string[@key='name']/text()"/>
                        <xsl:variable name="type" select="fn:string[@key='type']/text()"/>
                        <xsl:variable name="preferred" select="fn:boolean[@key='displayName']/text()"/>
                        <!-- The GSRS uuid identifies a row in one database
                             and is not written as an ``id``: a FHIR id that
                             a reader cannot resolve back to anything is
                             worse than none, and FHIR allows a name to have
                             no id at all. -->
                        <fn:map>
                            <fn:array key="extension">
                                <xsl:call-template name="access">
                                    <xsl:with-param name="access-field" select="fn:array[@key='access']"/>
                                </xsl:call-template>
                            </fn:array>
                            <fn:string key="name">
                                <xsl:value-of select="$name-value"/>
                            </fn:string>
                            <fn:map key="status">
                                <xsl:call-template name="status">
                                    <xsl:with-param name="deprecated-field" select="fn:boolean[@key='deprecated']"/>
                                </xsl:call-template>
                            </fn:map>
                            <fn:string key="preferred">
                                <xsl:value-of select="$preferred"/>
                            </fn:string>
                            <fn:array key="language">
                                <xsl:for-each select="fn:array[@key='languages']/fn:string/text()">
                                    <xsl:call-template name="language">
                                        <xsl:with-param name="language-ident" select="."/>
                                    </xsl:call-template>
                                </xsl:for-each>
                            </fn:array>
                            <!-- The list terms this name was cited from. GSRS
                                 stores a citation as a reference uuid, so each
                                 one is resolved back to the term it points at
                                 and written as a code system identifier: the
                                 EMA profile puts the source on the name rather
                                 than in ``contained``. A reference the list
                                 does not carry has no term to name and is
                                 skipped by the map. -->
                            <xsl:variable name="sources" as="xs:string*"
                                          select="for $reference in fn:array[@key='references']/fn:string/text()
                                                  return local:source-entry(string($reference), $citation-terms-by-uuid)"/>
                            <xsl:if test="exists($sources)">
                                <fn:array key="source">
                                    <xsl:for-each select="$sources">
                                        <!-- A source is an identifier and
                                             nothing else. The id it could
                                             carry would be the GSRS uuid of
                                             the reference, which names a row
                                             in one database and is not
                                             something a reader of this
                                             document can follow. -->
                                        <fn:map>
                                            <fn:map key="identifier">
                                                <fn:string key="system">
                                                    <xsl:value-of select="ema:list-system($ema-code-system-list)"/>
                                                </fn:string>
                                                <fn:string key="value"><xsl:value-of select="."/></fn:string>
                                            </fn:map>
                                        </fn:map>
                                    </xsl:for-each>
                                </fn:array>
                            </xsl:if>
                        </fn:map>
                    </xsl:for-each>
                </fn:array>
                
                <!-- Structure information -->
                <xsl:call-template name="structure">
                    <xsl:with-param name="structure-node" select="$root/fn:map[@key='structure']"/>
                </xsl:call-template>
                
                <!-- Codes -->
                <xsl:if test="$root/fn:array[@key='codes']">
                    <fn:array key="code">
                        <xsl:for-each select="$root/fn:array[@key='codes']/fn:map">
                            <xsl:variable name="code-code" select="fn:string[@key='code']/text()"/>
                            <xsl:variable name="code-system" select="fn:string[@key='codeSystem']/text()"/>
                            <xsl:if test="exists($code-system) and $code-system-term-map($code-system)">
                            <!-- A code carries no ``id`` for the same reason a
                                 name does not: the GSRS uuid is a database
                                 key, not something this document can offer as
                                 an identifier. -->
                            <fn:map>
                                <fn:map key="code">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system">
                                                <xsl:value-of select="concat(
                                                    ema:list-system($ema-code-system-list),
                                                    '/terms/',
                                                    $code-system-term-map($code-system))"/>
                                            </fn:string>
                                            <fn:string key="code">
                                                <xsl:value-of select="$code-code"/>
                                            </fn:string>
                                        </fn:map>
                                    </fn:array>
                                </fn:map>
                            </fn:map>
                            </xsl:if>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <!-- Relationships and properties are not exported: this
                     direction carries the identity of the substance and the
                     list terms its names were cited from, and a relationship
                     would point at a substance this document does not carry
                     while a property would claim an observation the EMA
                     payload never made. -->
            </fn:map>
        </xsl:variable>
        
        <!-- Convert the transformed XML back to JSON -->
        <xsl:value-of select="fn:xml-to-json($transformed-xml) => fn:parse-json() => fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <xsl:template name="access">
        <xsl:param name="access-field" as="element()?"/>
        <xsl:variable name="term" as="xs:string"
                      select="if ($access-field/fn:item or $access-field/*)
                              then $ema-data-classification-confidential
                              else $ema-data-classification-public"/>
        <fn:map>
            <fn:string key="url">https://ema.europa.eu/fhir/dataClassification</fn:string>
            <fn:map key="valueCoding">
                <fn:string key="system">
                    <xsl:value-of select="ema:list-system($ema-data-classification-list)"/>
                </fn:string>
                <fn:string key="code">
                    <xsl:value-of select="$term"/>
                </fn:string>
                <fn:string key="display">
                    <xsl:value-of select="ema:term-field(
                        $ema-data-classification-terms, $term, 'label')"/>
                </fn:string>
            </fn:map>
        </fn:map>
    </xsl:template>

    <xsl:template name="status">
        <xsl:param name="deprecated-field" as="element()?"/>
        <xsl:variable name="term" as="xs:string"
                      select="if ($deprecated-field/text() eq 'true')
                              then $ema-status-non-current
                              else $ema-status-current"/>
        <fn:array key="coding">
            <fn:map>
                <fn:string key="system">
                    <xsl:value-of select="ema:list-system($ema-status-list)"/>
                </fn:string>
                <fn:string key="code">
                    <xsl:value-of select="$term"/>
                </fn:string>
                <fn:string key="display">
                    <xsl:value-of select="ema:term-field($ema-status-terms, $term, 'label')"/>
                </fn:string>
            </fn:map>
        </fn:array>
    </xsl:template>

    <xsl:template name="category">
        <xsl:param name="substanceClass-value"/>
        <xsl:variable name="term" as="xs:string"
                      select="($category-term-map($substanceClass-value), $ema-category-default)[1]"/>
        <fn:array key="coding">
            <fn:map>
                <fn:string key="system">
                    <xsl:value-of select="ema:list-system($ema-category-list)"/>
                </fn:string>
                <fn:string key="code">
                    <xsl:value-of select="$term"/>
                </fn:string>
                <fn:string key="display">
                    <xsl:value-of select="ema:term-field($ema-category-terms, $term, 'label')"/>
                </fn:string>
            </fn:map>
        </fn:array>
    </xsl:template>

    <xsl:template name="structure">
        <xsl:param name="structure-node" as="element()?"/>
        <xsl:if test="$structure-node">
            <fn:map key="structure">
                <xsl:variable name="formula" select="$structure-node/fn:string[@key='formula']/text()"/>
                <xsl:variable name="mwt" select="$structure-node/fn:number[@key='mwt']/text()"/>
                <xsl:variable name="inchi-key" select="$structure-node/fn:string[@key='_inchiKey']/text()"/>
                
                <xsl:if test="$formula">
                    <fn:string key="molecularFormula">
                        <xsl:value-of select="$formula"/>
                    </fn:string>
                </xsl:if>
                
                <xsl:if test="$mwt">
                    <fn:map key="molecularWeight">
                        <fn:map key="amount">
                            <fn:number key="value">
                                <xsl:value-of select="round($mwt * 100) div 100"/>
                            </fn:number>
                        </fn:map>
                    </fn:map>
                </xsl:if>
                
                <xsl:if test="$inchi-key">
                    <fn:array key="representation">
                        <fn:map>
                            <fn:string key="representation">
                                <xsl:value-of select="$inchi-key"/>
                            </fn:string>
                        </fn:map>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="language">
        <xsl:param name="language-ident"/>
        <xsl:variable name="term" as="xs:string"
                      select="($language-term-map($language-ident), $ema-language-default)[1]"/>
        <fn:map>
            <fn:array key="coding">
                <fn:map>
                    <fn:string key="system">
                        <xsl:value-of select="ema:list-system($ema-language-list)"/>
                    </fn:string>
                    <fn:string key="code">
                        <xsl:value-of select="$term"/>
                    </fn:string>
                    <fn:string key="display">
                        <xsl:value-of select="ema:term-field($ema-language-terms, $term, 'label')"/>
                    </fn:string>
                </fn:map>
            </fn:array>
        </fn:map>
    </xsl:template>

</xsl:stylesheet>
