<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:local="urn:local"
                exclude-result-prefixes="fn xs local">

    <xsl:output method="json" encoding="UTF-8" indent="yes"/>

    <xsl:param name="raw-input" as="xs:string" required="yes"/>

    <xsl:variable name="json-doc" select="fn:json-to-xml($raw-input)"/>
    <xsl:variable name="root" select="$json-doc/fn:map"/>

    <xsl:variable name="cv-base-url" as="xs:string"
                  select="'https://gsrs.ncats.nih.gov/api/v1/vocabularies/'"/>

    <xsl:variable name="category-map" as="map(*)" select="map {
        '100000075670': 'chemical',
        '200000005023': 'mixture',
        '200000005035': 'nucleicAcid',
        '200000005022': 'polymer',
        '200000005020': 'protein',
        '200000005031': 'specifiedSubstanceG1',
        '200000005032': 'specifiedSubstanceG2',
        '200000005033': 'specifiedSubstanceG3',
        '200000005034': 'specifiedSubstanceG4',
        '200000005026': 'structurally diverse - allergen',
        '200000005029': 'structurally diverse - cell therapy',
        '200000005025': 'structurally diverse - herbal',
        '200000005030': 'structurallyDiverse',
        '200000005024': 'structurally diverse - plasma derived',
        '200000005027': 'structurally diverse - vaccine'
    }"/>

    <xsl:variable name="code-system-map" as="map(*)" select="map {
        '100000075665': 'EVMPD',
        '100000075787': 'CAS',
        '100000146035': 'SIAMED',
        '200000018817': 'FDA UNII',
        '200000025197': 'SVG',
        '200000032418': 'ECHA (EC/EINECS)'
    }"/>

    <xsl:variable name="language-map" as="map(*)" select="map {
        '100000072147': 'en',
        '100000072181': 'fr',
        '100000072252': 'de',
        '100000072291': 'es',
        '100000072295': 'it',
        '100000072282': 'nl',
        '100000072137': 'da',
        '100000072305': 'pt',
        '100000072317': 'sv',
        '100000072171': 'fi',
        '100000072273': 'no',
        '100000072259': 'is',
        '100000072211': 'mt',
        '100000072231': 'ga',
        '100000072331': 'cs',
        '100000072324': 'et',
        '100000072278': 'hu',
        '100000072296': 'lv',
        '100000072306': 'lt',
        '100000072287': 'pl',
        '100000072318': 'ro',
        '100000072289': 'sk',
        '100000072330': 'sl',
        '100000072346': 'bg',
        '100000072140': 'hr',
        '100000072186': 'el',
        '100000072314': 'ru',
        '100000072183': 'gd',
        '100000072175': 'ga',
        '100000072213': 'mk',
        '100000072312': 'ro',
        '100000072339': 'sr',
        '100000072157': 'tr',
        '100000072233': 'uk',
        '100000072313': 'ru',
        '100000072153': 'ar',
        '100000072188': 'zh',
        '100000072338': 'ja'
    }"/>

    <xsl:function name="local:iso-to-epoch" as="xs:string">
        <xsl:param name="iso" as="xs:string"/>
        <xsl:variable name="dt" select="xs:dateTime(replace($iso, 'Z$', '+00:00'))"/>
        <xsl:sequence select="xs:string(xs:integer(($dt - xs:dateTime('1970-01-01T00:00:00Z')) div xs:dayTimeDuration('PT0.001S')) )"/>
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
        <xsl:variable name="uuid" select="string($root/fn:string[@key='id'])"/>
        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="category" select="string($root/fn:map[@key='category']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text())"/>
        <xsl:variable name="deprecated" select="$root/fn:map[@key='status']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() = '200000005006'"/>

        <fn:string key="substanceClass"><xsl:value-of select="($category-map($category), 'chemical')[1]"/></fn:string>
        <fn:string key="uuid"><xsl:value-of select="$uuid"/></fn:string>
        <xsl:if test="$version">
            <fn:string key="version"><xsl:value-of select="$version"/></fn:string>
        </xsl:if>
        <xsl:if test="not($deprecated)">
            <fn:string key="status">approved</fn:string>
        </xsl:if>
        <xsl:if test="$deprecated">
            <fn:boolean key="deprecated">true</fn:boolean>
        </xsl:if>

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
        <xsl:call-template name="structure"/>
        <xsl:call-template name="relationships"/>
        <xsl:call-template name="properties"/>
        <xsl:call-template name="references"/>
    </xsl:template>

    <xsl:template name="names">
        <xsl:if test="$root/fn:array[@key='name']/fn:map">
            <fn:array key="names">
                <xsl:for-each select="$root/fn:array[@key='name']/fn:map">
                    <fn:map>
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <fn:string key="name"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                        <xsl:if test="fn:map[@key='status']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() = '200000005006'">
                            <fn:boolean key="deprecated">true</fn:boolean>
                        </xsl:if>
                        <xsl:if test="fn:boolean[@key='preferred']/text()">
                            <fn:boolean key="displayName"><xsl:value-of select="fn:boolean[@key='preferred']/text()"/></fn:boolean>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="languages">
                                <xsl:for-each select="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <xsl:variable name="langCode" select="string(.)"/>
                                    <fn:string><xsl:value-of select="($language-map($langCode), $langCode)[1]"/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
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
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:variable name="termId" select="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='system']/text()"/>
                        <xsl:variable name="termCode" select="substring-after($termId, 'terms/')"/>
                        <fn:string key="codeSystem"><xsl:value-of select="($code-system-map($termCode), $termCode)[1]"/></fn:string>
                        <fn:string key="code"><xsl:value-of select="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="structure">
        <xsl:variable name="structure" select="$root/fn:map[@key='structure']"/>
        <xsl:if test="$structure/fn:string[@key='molecularFormula']/text() or
                      $structure/fn:array[@key='representation']/fn:map/fn:string[@key='representation']/text() or
                      $structure/fn:map[@key='molecularWeight']/fn:map[@key='amount']/fn:number[@key='value']/text()">
            <fn:map key="structure">
                <xsl:if test="$structure/fn:string[@key='molecularFormula']/text()">
                    <fn:string key="formula"><xsl:value-of select="$structure/fn:string[@key='molecularFormula']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$structure/fn:map[@key='molecularWeight']/fn:map[@key='amount']/fn:number[@key='value']/text()">
                    <fn:number key="mwt"><xsl:value-of select="$structure/fn:map[@key='molecularWeight']/fn:map[@key='amount']/fn:number[@key='value']/text()"/></fn:number>
                </xsl:if>
                <xsl:if test="$structure/fn:array[@key='representation']/fn:map/fn:string[@key='representation']/text()">
                    <xsl:for-each select="$structure/fn:array[@key='representation']/fn:map">
                        <xsl:variable name="format" select="fn:string[@key='format']/text()"/>
                        <xsl:variable name="value" select="fn:string[@key='representation']/text()"/>
                        <xsl:choose>
                            <xsl:when test="$format = 'SMILES'"><fn:string key="smiles"><xsl:value-of select="$value"/></fn:string></xsl:when>
                            <xsl:when test="$format = 'InChI'"><fn:string key="_inchi"><xsl:value-of select="$value"/></fn:string></xsl:when>
                            <xsl:when test="$format = 'InChI-Key'"><fn:string key="_inchiKey"><xsl:value-of select="$value"/></fn:string></xsl:when>
                            <xsl:when test="$format = 'MOLFILE'"><fn:string key="molfile"><xsl:value-of select="$value"/></fn:string></xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="relationships">
        <xsl:if test="$root/fn:array[@key='relationship']/fn:map">
            <fn:array key="relationships">
                <xsl:for-each select="$root/fn:array[@key='relationship']/fn:map">
                    <fn:map>
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:variable name="ref" select="fn:map[@key='substanceDefinitionReference']/fn:string[@key='reference']/text()"/>
                        <xsl:if test="$ref and starts-with($ref, 'SubstanceDefinition/')">
                            <fn:map key="relatedSubstance">
                                <fn:string key="refuuid"><xsl:value-of select="substring-after($ref, 'SubstanceDefinition/')"/></fn:string>
                            </fn:map>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text()">
                            <fn:string key="type"><xsl:value-of select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text()"/></fn:string>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="properties">
        <xsl:if test="$root/fn:array[@key='property']/fn:map">
            <fn:array key="properties">
                <xsl:for-each select="$root/fn:array[@key='property']/fn:map">
                    <fn:map>
                        <xsl:if test="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="name"><xsl:value-of select="fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:string[@key='valueString']/text()">
                            <fn:map key="value">
                                <fn:string key="nonNumericValue"><xsl:value-of select="fn:string[@key='valueString']/text()"/></fn:string>
                            </fn:map>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="references">
        <xsl:if test="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='DocumentReference']">
            <fn:array key="references">
                <xsl:for-each select="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='DocumentReference']">
                    <fn:map>
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="docType"><xsl:value-of select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:string[@key='description']/text()">
                            <fn:string key="citation"><xsl:value-of select="fn:string[@key='description']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='content']/fn:map/fn:map[@key='attachment']/fn:string[@key='url']/text()">
                            <fn:string key="url"><xsl:value-of select="fn:array[@key='content']/fn:map/fn:map[@key='attachment']/fn:string[@key='url']/text()"/></fn:string>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
