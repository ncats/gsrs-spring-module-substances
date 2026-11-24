<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:array="http://www.w3.org/2005/xpath-functions/array"
                xmlns:map="http://www.w3.org/2005/xpath-functions/map"
                xmlns:ema="urn:gsrs:ema-rms"
                xmlns:local="urn:local"
                exclude-result-prefixes="array ema fn map xs local">

    <xsl:output method="json" encoding="UTF-8" indent="yes"/>

    <xsl:param name="input" as="xs:string" required="yes"/>

    <xsl:variable name="json-doc" select="fn:json-to-xml($input)"/>
    <xsl:variable name="root" select="$json-doc/fn:map"/>

    <!-- The EMA list vocabulary lives in one shared file, keyed by term id.
         This side reads the GSRS value straight off each term, so no reverse
         index is needed here, and the list ids and the terms it tests against
         come from the same file rather than being repeated below. -->
    <xsl:include href="ema-rms-lists.xsl"/>

    <!-- The extension the EMA profile puts on an element to say who may read
         it, carrying a term from ``$ema-data-classification-list``. This is
         the one part of the classification a term id alone cannot express:
         the list says which classification, the extension says it is one. -->
    <xsl:variable name="ema-data-classification-url" as="xs:string"
                  select="'https://ema.europa.eu/fhir/dataClassification'"/>

    <!-- Every list term a name cites, paired with the reference uuid GSRS will
         store it under.

         One term may be cited by several names and GSRS keeps one reference
         per citation, so the pairs are computed once here and both the
         ``names`` and ``references`` templates read this same map. Terms are
         collected in document order and deduplicated, which keeps a run
         reproducible for a given payload. -->
    <xsl:variable name="citation-terms" as="xs:string*"
                  select="distinct-values(
                      for $source in $root/fn:array[@key='name']/fn:map/fn:array[@key='source']/fn:map
                      return local:citation-term($source))"/>

    <xsl:variable name="citation-uuids" as="map(xs:string, xs:string)"
                  select="local:citation-uuid-map($citation-terms)"/>

    <!-- The reference a name falls back on.

         GSRS requires every name to cite at least one reference, and the EMA
         payload does not always say where a name came from. One default
         reference stands in for the whole import: it records that the
         substance arrived from an EMA list, so every name cites it and GSRS
         stores the publication once however many names rely on it. -->
    <xsl:variable name="default-citation-uuid" as="xs:string"
                  select="local:random-uuid(concat(string(current-dateTime()), '-default'))"/>

    <!-- The default reference's citation, and the tag that marks it as
         reusable content. Both are constants because the reference describes
         the import itself rather than anything the payload says. -->
    <xsl:variable name="default-citation" as="xs:string" select="'EMA import'"/>
    <xsl:variable name="public-domain-tag" as="xs:string" select="'PUBLIC_DOMAIN_RELEASE'"/>

    <xsl:function name="local:iso-to-epoch" as="xs:string">
        <xsl:param name="iso" as="xs:string"/>
        <xsl:variable name="dt" select="xs:dateTime(replace($iso, 'Z$', '+00:00'))"/>
        <xsl:sequence select="xs:string(xs:integer(($dt - xs:dateTime('1970-01-01T00:00:00Z')) div xs:dayTimeDuration('PT0.001S')) )"/>
    </xsl:function>

    <!-- A random version 4 UUID, the identifier GSRS gives a reference it has
         not seen before.

         The payload the sender wrote carries an ``id`` of its own, but the
         list term a citation points at is not a substance: it means the same
         thing on both sides, so a fresh reference is minted here rather than
         the sender's id restored. The seed comes from the document timestamp
         and a slot number, because ``random-number-generator()`` with no seed
         returns the same stream on every call under SaxonC and would hand out
         one identifier to every citation. -->
    <xsl:function name="local:random-uuid" as="xs:string">
        <xsl:param name="seed" as="xs:string"/>
        <xsl:variable name="digits" select="'0123456789abcdef'"/>
        <xsl:variable name="nibbles" as="xs:integer*"
                      select="fold-left(1 to 32, array { random-number-generator($seed), array { } },
                          function($state, $slot) {
                              let $step := $state?1?next()
                              return array { $step,
                                  array:append($state?2, xs:integer($step?number * 16)) }
                          })?2?*"/>
        <xsl:variable name="hex" as="xs:string*"
                      select="for $position in 1 to 32 return
                          if ($position = 13) then '4'
                          else if ($position = 17) then substring('89ab', $nibbles[$position] mod 4 + 1, 1)
                          else substring($digits, $nibbles[$position] + 1, 1)"/>
        <xsl:sequence select="string-join((
            string-join(subsequence($hex, 1, 8), ''),
            string-join(subsequence($hex, 9, 4), ''),
            string-join(subsequence($hex, 13, 4), ''),
            string-join(subsequence($hex, 17, 4), ''),
            string-join(subsequence($hex, 21, 12), '')), '-')"/>
    </xsl:function>

    <!-- The list term a FHIR ``name.source`` names.

         The EMA profile puts a code system identifier on the source rather
         than a ``#`` reference to a contained resource, so the term id is the
         last segment of ``identifier.value``. A source with no such value is
         not a list citation and is left out. -->
    <xsl:function name="local:citation-term" as="xs:string?">
        <xsl:param name="source" as="element()?"/>
        <xsl:variable name="value" select="string($source/fn:map[@key='identifier']/fn:string[@key='value'])"/>
        <xsl:sequence select="if ($value) then $value else ()"/>
    </xsl:function>

    <!-- The list terms one FHIR name cites, deduplicated in the order its
         sources mention them. A source carrying no term id is not a citation
         and is left out, so a name with no sources yields the empty
         sequence. -->
    <xsl:function name="local:cited-terms" as="xs:string*">
        <xsl:param name="name-node" as="element()"/>
        <xsl:sequence select="distinct-values(
            for $source in $name-node/fn:array[@key='source']/fn:map
            return local:citation-term($source))"/>
    </xsl:function>

    <!-- Whether a ``status`` says the element is no longer current.

         List 200000005003 carries one of two terms, ``Current`` and
         ``Non-Current``, so the code alone decides it. The display is
         accepted too, because a payload written by hand often carries the
         display without the code, and a status that names neither is read as
         current: a new substance is current unless something says otherwise. -->
    <xsl:function name="local:deprecated" as="xs:boolean">
        <xsl:param name="status" as="element()?"/>
        <xsl:variable name="coding" select="$status/fn:array[@key='coding']/fn:map"/>
        <xsl:sequence select="exists($coding[
            fn:string[@key='code']/text() = $ema-status-non-current
            or fn:string[@key='display']/text() = ema:term-field(
                $ema-status-terms, $ema-status-non-current, 'label')])"/>
    </xsl:function>

    <!-- The ``access`` list a ``dataClassification`` extension calls for.

         ``Public`` means the element is publishable as it stands, which GSRS
         spells as no access entries at all; anything else has to be marked
         ``protected``. An element carrying no classification at all is not
         public: the absence of a statement is not a statement that it may be
         published, so it stays protected like an explicit ``Confidential``. -->
    <xsl:function name="local:access" as="xs:string*">
        <xsl:param name="element-node" as="element()?"/>
        <xsl:variable name="classification"
                      select="$element-node/fn:array[@key='extension']/fn:map[
                          fn:string[@key='url']/text() = $ema-data-classification-url]"/>
        <xsl:variable name="coding"
                      select="$classification/fn:map[@key='valueCoding']"/>
        <xsl:sequence select="if ($coding/fn:string[@key='code']/text() = $ema-data-classification-public
            or $coding/fn:string[@key='display']/text() = ema:term-field(
                $ema-data-classification-terms, $ema-data-classification-public, 'label'))
            then ()
            else 'protected'"/>
    </xsl:function>

    <!-- Every list term the document cites, in the order the names mention
         them, each paired with a random uuid.

         One name may cite a term several others also cite, and GSRS stores
         one reference per citation, so the map is what keeps a term's
         identifier the same wherever it appears: a name's ``references`` and
         the ``references`` array are both written from it. -->
    <xsl:function name="local:citation-uuid-map" as="map(xs:string, xs:string)">
        <xsl:param name="terms" as="xs:string*"/>
        <xsl:variable name="now" select="string(current-dateTime())"/>
        <xsl:sequence select="map:merge(
            for $position in 1 to count($terms)
            return map:entry(
                $terms[$position],
                local:random-uuid(concat($now, '-', $position))))"/>
    </xsl:function>

    <xsl:template match="/">
        <xsl:variable name="substance">
            <fn:map>
                <xsl:call-template name="substance"/>
            </fn:map>
        </xsl:variable>
        <xsl:value-of select="fn:xml-to-json($substance) => fn:parse-json() => fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <xsl:template name="substance">
        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="deprecated" select="local:deprecated($root/fn:map[@key='status'])"/>

        <!-- Every EMA substance lands in GSRS as a concept, whatever the FHIR
             category says. A concept is the class that carries names and
             codes without claiming a structure, which is what this import
             keeps, so the category no longer decides the class. -->
        <fn:string key="substanceClass">concept</fn:string>
        <xsl:if test="$version">
            <fn:string key="version"><xsl:value-of select="$version"/></fn:string>
        </xsl:if>
        <!-- An EMA document describes a registration, not a GSRS review, so
             nothing in it can grant approval: the substance arrives unvetted
             and GSRS spells that ``pending``. A deprecated substance carries
             no status at all, because the flag alone is what the server needs
             to know about it. -->
        <xsl:if test="not($deprecated)">
            <fn:string key="status">pending</fn:string>
        </xsl:if>
        <xsl:if test="$deprecated">
            <fn:boolean key="deprecated">true</fn:boolean>
        </xsl:if>

        <!-- ``access`` is written for every substance, because an empty list
             and a missing one mean different things to GSRS: the empty list
             publishes the substance, a restricted one is what ``protected``
             states. Leaving the key out would let the server decide. -->
        <fn:array key="access">
            <xsl:for-each select="local:access($root)">
                <fn:string><xsl:value-of select="."/></fn:string>
            </xsl:for-each>
        </fn:array>

        <xsl:variable name="smsId" select="$root/fn:map[@key='identifier']/fn:string[@key='value']/text()"/>
        <xsl:variable name="approvalId" select="$root/fn:array[@key='identifier']/fn:map[fn:string[@key='system']='https://gsrs.ncats.nih.gov/api/v1/approvalID']/fn:string[@key='value']/text()"/>
        <xsl:if test="$approvalId">
            <fn:string key="approvalID"><xsl:value-of select="$approvalId"/></fn:string>
        </xsl:if>

        <xsl:variable name="provenance" select="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='Provenance'][1]"/>
        <xsl:if test="$provenance/fn:string[@key='occurredDateTime']/text()">
            <fn:number key="created"><xsl:value-of select="local:iso-to-epoch(string($provenance/fn:string[@key='occurredDateTime']/text()))"/></fn:number>
        </xsl:if>
        <xsl:if test="$provenance/fn:string[@key='recorded']/text()">
            <fn:number key="lastEdited"><xsl:value-of select="local:iso-to-epoch(string($provenance/fn:string[@key='recorded']/text()))"/></fn:number>
        </xsl:if>
        <xsl:if test="$provenance/fn:array[@key='reason']/fn:map/fn:string[@key='text']/text()">
            <fn:string key="changeReason"><xsl:value-of select="$provenance/fn:array[@key='reason']/fn:map/fn:string[@key='text']/text()"/></fn:string>
        </xsl:if>
        <xsl:if test="$provenance/fn:array[@key='agent']/fn:map/fn:map[@key='who']/fn:string[@key='display']/text()">
            <fn:string key="approvedBy"><xsl:value-of select="$provenance/fn:array[@key='agent']/fn:map/fn:map[@key='who']/fn:string[@key='display']/text()"/></fn:string>
        </xsl:if>

        <xsl:call-template name="names"/>
        <xsl:call-template name="codes">
            <xsl:with-param name="smsId" select="$smsId"/>
        </xsl:call-template>
        <xsl:call-template name="references"/>
    </xsl:template>

    <xsl:template name="names">
        <xsl:if test="$root/fn:array[@key='name']/fn:map">
            <fn:array key="names">
                <xsl:for-each select="$root/fn:array[@key='name']/fn:map">
                    <fn:map>
                        <fn:string key="name"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                        <!-- GSRS requires a name type and the EMA payload does
                             not always carry one, so a missing or empty FHIR
                             ``name.type`` falls back to the chemical name. -->
                        <fn:string key="type"><xsl:value-of select="(fn:string[@key='type']/text(), 'cn')[1]"/></fn:string>
                        <xsl:if test="local:deprecated(fn:map[@key='status'])">
                            <fn:boolean key="deprecated">true</fn:boolean>
                        </xsl:if>
                        <!-- A name is restricted on its own account when its
                             classification says so, so it carries the same
                             ``access`` list the substance does. An
                             unrestricted name gets the empty list rather
                             than no key at all, which is how GSRS tells a
                             published name from one the server had to
                             decide about. -->
                        <fn:array key="access">
                            <xsl:for-each select="local:access(.)">
                                <fn:string><xsl:value-of select="."/></fn:string>
                            </xsl:for-each>
                        </fn:array>
                        <xsl:if test="fn:boolean[@key='preferred']/text()">
                            <fn:boolean key="displayName"><xsl:value-of select="fn:boolean[@key='preferred']/text()"/></fn:boolean>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="languages">
                                <xsl:for-each select="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <xsl:variable name="langCode" select="string(.)"/>
                                    <fn:string><xsl:value-of select="($ema-language-terms($langCode)?value, $langCode)[1]"/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <!-- The references this name cites: the default
                             reference first, then one uuid per list term the
                             name names. The default is unconditional so no
                             name can be stored without a citation. -->
                        <xsl:variable name="terms" as="xs:string*"
                                      select="local:cited-terms(.)"/>
                        <xsl:variable name="cited" as="xs:string*"
                                      select="($default-citation-uuid,
                                          for $term in $terms return $citation-uuids($term))"/>
                        <fn:array key="references">
                            <xsl:for-each select="$cited">
                                <fn:string><xsl:value-of select="."/></fn:string>
                            </xsl:for-each>
                        </fn:array>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="codes">
        <xsl:param name="smsId" as="xs:string?"/>
        <xsl:if test="$root/fn:array[@key='code']/fn:map or $smsId">
            <fn:array key="codes">
                <xsl:if test="$smsId">
                    <fn:map>
                        <fn:string key="codeSystem">SMS_ID</fn:string>
                        <fn:string key="code"><xsl:value-of select="$smsId"/></fn:string>
                    </fn:map>
                </xsl:if>
                <xsl:for-each select="$root/fn:array[@key='code']/fn:map">
                    <fn:map>
                        <xsl:variable name="termId" select="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='system']/text()"/>
                        <xsl:variable name="termCode" select="substring-after($termId, 'terms/')"/>
                        <fn:string key="codeSystem"><xsl:value-of select="($ema-code-system-terms($termCode)?value, $termCode)[1]"/></fn:string>
                        <fn:string key="code"><xsl:value-of select="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="references">
        <!-- The default reference always leads the list, because every name
             cites it and the first entry is then the one they all share. -->
        <fn:array key="references">
            <fn:map>
                <fn:string key="uuid"><xsl:value-of select="$default-citation-uuid"/></fn:string>
                <fn:string key="docType">EMA LIST</fn:string>
                <fn:string key="citation"><xsl:value-of select="$default-citation"/></fn:string>
                <fn:boolean key="publicDomain">true</fn:boolean>
                <fn:array key="tags">
                    <fn:string><xsl:value-of select="$public-domain-tag"/></fn:string>
                </fn:array>
            </fn:map>
            <xsl:for-each select="$citation-terms">
                <xsl:variable name="term" select="."/>
                <fn:map>
                    <fn:string key="uuid"><xsl:value-of select="$citation-uuids($term)"/></fn:string>
                    <fn:string key="docType">EMA LIST</fn:string>
                    <!-- The list's own display name is the citation GSRS
                         shows, so a term the shared table names is quoted
                         by its label. A term it does not name has no
                         label to quote, and the term id stands in so the
                         citation is still traceable. -->
                    <fn:string key="citation"><xsl:value-of select="($ema-code-system-terms($term)?label, $term)[1]"/></fn:string>
                </fn:map>
            </xsl:for-each>
        </fn:array>
    </xsl:template>

</xsl:stylesheet>
