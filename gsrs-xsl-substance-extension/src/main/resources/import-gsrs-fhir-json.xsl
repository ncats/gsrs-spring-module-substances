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

    <xsl:function name="local:iso-to-epoch" as="xs:string">
        <xsl:param name="iso" as="xs:string"/>
        <xsl:variable name="dt" select="xs:dateTime($iso)"/>
        <xsl:sequence select="xs:string(xs:integer(($dt - xs:dateTime('1970-01-01T00:00:00Z')) div xs:dayTimeDuration('PT0.001S')) )"/>
    </xsl:function>

    <xsl:template match="/">
        <xsl:variable name="substance">
            <fn:map>
                <xsl:call-template name="substance"/>
            </fn:map>
        </xsl:variable>
        <xsl:value-of select="fn:xml-to-json($substance) =&gt; fn:parse-json() =&gt; fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <xsl:template name="substance">
        <xsl:variable name="uuid" select="string($root/fn:string[@key='id'])"/>
        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="deprecated" select="local:deprecated($root/fn:map[@key='status'])"/>

        <xsl:variable name="allCodings" select="$root/fn:array[@key='classification']/fn:map/fn:array[@key='coding']/fn:map"/>
        <xsl:variable name="classCoding" select="($allCodings[fn:contains(fn:string[@key='system'], 'SUBSTANCE_CLASS')], $allCodings[1])[1]"/>
        <xsl:variable name="resolvedClass" select="local:classification(($classCoding/fn:string[@key='code'], 'chemical')[1])"/>

        <xsl:variable name="proteinRef" select="$root/fn:map[@key='protein']/fn:string[@key='reference']/text()"/>
        <xsl:variable name="naRef" select="$root/fn:map[@key='nucleicAcid']/fn:string[@key='reference']/text()"/>
        <xsl:variable name="polyRef" select="$root/fn:map[@key='polymer']/fn:string[@key='reference']/text()"/>

        <xsl:variable name="hasFullStructure" select="
            some $rep in $root/fn:map[@key='structure']/fn:array[@key='representation']/fn:map
            satisfies local:is-full-structure-representation($rep)
        "/>

        <xsl:variable name="hasDefinition" select="
            $hasFullStructure or
            exists($root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstanceProtein'][concat('#', fn:string[@key='id']/text()) = $proteinRef]) or
            exists($root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstanceNucleicAcid'][concat('#', fn:string[@key='id']/text()) = $naRef]) or
            exists($root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstancePolymer'][concat('#', fn:string[@key='id']/text()) = $polyRef]) or
            exists($root/fn:map[@key='sourceMaterial']) or
            exists($root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'MIXTURE_COMPONENT_') or
                starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'MIXTURE_COMPONENT_')]) or
            exists($root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'SSG1_CONSTITUENT_') or
                starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'SSG1_CONSTITUENT_')])
        "/>

        <xsl:variable name="finalClass" select="if ($resolvedClass = 'chemical' and not($hasDefinition)) then 'concept' else $resolvedClass"/>

        <fn:string key="substanceClass"><xsl:value-of select="$finalClass"/></fn:string>
        <fn:string key="uuid"><xsl:value-of select="$uuid"/></fn:string>
        <xsl:if test="$version">
            <fn:string key="version"><xsl:value-of select="$version"/></fn:string>
        </xsl:if>
        <xsl:if test="$root/fn:map[@key='status']/fn:array[@key='coding']/fn:map/fn:string[@key='code'] != 'retired'">
            <fn:string key="status">approved</fn:string>
        </xsl:if>
        <xsl:if test="$deprecated != ''">
            <fn:boolean key="deprecated"><xsl:value-of select="$deprecated"/></fn:boolean>
        </xsl:if>

        <xsl:variable name="approvalId" select="$root/fn:array[@key='identifier']/fn:map[fn:string[@key='system']='https://gsrs.ncats.nih.gov/api/v1/approvalID']/fn:string[@key='value']"/>
        <xsl:if test="$approvalId">
            <fn:string key="approvalID"><xsl:value-of select="$approvalId"/></fn:string>
        </xsl:if>

        <xsl:if test="$root/fn:map[@key='meta']/fn:string[@key='lastUpdated']">
            <fn:number key="lastEdited"><xsl:value-of select="local:iso-to-epoch(string($root/fn:map[@key='meta']/fn:string[@key='lastUpdated']))"/></fn:number>
        </xsl:if>

        <xsl:call-template name="names"/>
        <xsl:call-template name="codes"/>
        <xsl:if test="$finalClass != 'concept'">
            <xsl:call-template name="structure"/>
            <xsl:call-template name="moieties"/>
            <xsl:call-template name="protein"/>
            <xsl:call-template name="nucleicAcid"/>
            <xsl:call-template name="polymer"/>
            <xsl:call-template name="structurallyDiverse"/>
            <xsl:call-template name="mixture"/>
            <xsl:call-template name="specifiedSubstance"/>
        </xsl:if>
        <xsl:call-template name="relationships"/>
        <xsl:call-template name="properties"/>
        <xsl:call-template name="notes"/>
        <xsl:call-template name="references"/>
    </xsl:template>

    <xsl:function name="local:deprecated" as="xs:string">
        <xsl:param name="statusNode" as="element()?"/>
        <xsl:choose>
            <xsl:when test="$statusNode/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() = 'retired'">true</xsl:when>
            <xsl:otherwise><xsl:value-of select="''"/></xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="local:classification" as="xs:string">
        <xsl:param name="classCode" as="xs:string?"/>
        <xsl:value-of select="string($classCode)"/>
    </xsl:function>

    <xsl:function name="local:is-inchi-key" as="xs:boolean">
        <xsl:param name="value" as="xs:string?"/>
        <xsl:sequence select="matches($value, '^[A-Z]{14}-[A-Z]{10}-[A-Z]$')"/>
    </xsl:function>

    <xsl:function name="local:is-full-structure-representation" as="xs:boolean">
        <xsl:param name="rep" as="element()?"/>
        <xsl:variable name="format" select="string($rep/fn:string[@key='format']/text())"/>
        <xsl:variable name="value" select="string($rep/fn:string[@key='representation']/text())"/>
        <xsl:sequence select="
            $format = ('MOLFILE', 'SMILES', 'InChI') or
            starts-with($value, 'InChI=1') or
            contains($value, 'V2000') or
            contains($value, 'V3000') or
            ($format = '' and $value != '' and not(local:is-inchi-key($value)) and not(contains($value, ' ')))
        "/>
    </xsl:function>
    <xsl:function name="local:first-public-reference-id" as="xs:string?">
        <xsl:variable name="publicRef" select="
            $root/fn:array[@key='contained']/fn:map[
                fn:string[@key='resourceType'] = 'DocumentReference'
                and fn:array[@key='securityLabel']/fn:map[1]/fn:array[@key='coding']/fn:map[1]/fn:string[@key='code']/text() = 'U'
            ][1]
        " />
        <xsl:value-of select="$publicRef/fn:string[@key='id']/text()"/>
    </xsl:function>

    <xsl:template name="names">
        <xsl:if test="$root/fn:array[@key='name']/fn:map">
            <fn:array key="names">
                <xsl:for-each select="$root/fn:array[@key='name']/fn:map">
                    <fn:map>
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <fn:string key="name"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                        <xsl:if test="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="type"><xsl:value-of select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:boolean[@key='preferred']/text()">
                            <fn:boolean key="displayName"><xsl:value-of select="fn:boolean[@key='preferred']/text()"/></fn:boolean>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="languages">
                                <xsl:for-each select="fn:array[@key='language']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <fn:string><xsl:value-of select="."/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='domain']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="domains">
                                <xsl:for-each select="fn:array[@key='domain']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <fn:string><xsl:value-of select="."/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='jurisdiction']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="nameJurisdiction">
                                <xsl:for-each select="fn:array[@key='jurisdiction']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <fn:string><xsl:value-of select="."/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='official']/fn:map/fn:map[@key='authority']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="nameOrgs">
                                <xsl:for-each select="fn:array[@key='official']/fn:map">
                                    <fn:map>
                                        <fn:string key="nameOrg"><xsl:value-of select="fn:map[@key='authority']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                                    </fn:map>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                            <fn:array key="references">
                                <xsl:for-each select="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                                    <fn:string><xsl:value-of select="substring(., 2)"/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                        <xsl:variable name="sourceRefs" select="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()"/>
                        <xsl:variable name="isUnrestricted" select="
                            some $ref in $sourceRefs
                            satisfies (
                                let $docRef := $root/fn:array[@key='contained']/fn:map[
                                    fn:string[@key='resourceType'] = 'DocumentReference'
                                    and concat('#', fn:string[@key='id']/text()) = $ref
                                ]
                                return $docRef/fn:array[@key='securityLabel']/fn:map[1]/fn:array[@key='coding']/fn:map[1]/fn:string[@key='display']/text() = 'Unrestricted'
                            )
                        "/>
                        <fn:array key="access">
                            <xsl:if test="not($isUnrestricted)">
                                <fn:string>protected</fn:string>
                            </xsl:if>
                        </fn:array>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="codes">
        <xsl:if test="$root/fn:array[@key='code']/fn:map">
            <fn:array key="codes">
                <xsl:for-each select="$root/fn:array[@key='code']/fn:map">
                    <fn:map>
                        <xsl:if test="fn:string[@key='id']">
                            <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:variable name="system" select="string(fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='system']/text())"/>
                        <xsl:variable name="gsrsCodeSystemPrefix" select="concat($cv-base-url, 'CODE_SYSTEM/terms/')"/>
                        <xsl:variable name="codeSystemName" select="replace(substring-after($system, $gsrsCodeSystemPrefix), '%20', ' ')"/>
                        <xsl:variable name="code" select="string(fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text())"/>
                        <fn:string key="codeSystem"><xsl:value-of select="$codeSystemName"/></fn:string>
                        <fn:string key="code"><xsl:value-of select="$code"/></fn:string>
                        <xsl:if test="fn:array[@key='note']/fn:map[1]/fn:string[@key='text']/text()">
                            <fn:string key="comments"><xsl:value-of select="fn:array[@key='note']/fn:map[1]/fn:string[@key='text']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                            <fn:array key="references">
                                <xsl:for-each select="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                                    <fn:string><xsl:value-of select="substring(., 2)"/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="structure">
        <xsl:variable name="structure" select="$root/fn:map[@key='structure']"/>
        <xsl:if test="$structure">
            <fn:map key="structure">
                <xsl:if test="$structure/fn:string[@key='molecularFormula']/text()">
                    <fn:string key="formula"><xsl:value-of select="$structure/fn:string[@key='molecularFormula']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$structure/fn:array[@key='representation']/fn:map/fn:string[@key='representation']/text()">
                    <xsl:for-each select="$structure/fn:array[@key='representation']/fn:map">
                        <xsl:variable name="format" select="string(fn:string[@key='format']/text())"/>
                        <xsl:variable name="value" select="string(fn:string[@key='representation']/text())"/>
                        <xsl:choose>
                            <xsl:when test="$format = 'SMILES' or ($format = '' and not(starts-with($value, 'InChI=')) and not(local:is-inchi-key($value)) and not(contains($value, 'V2000')) and not(contains($value, 'V3000')) and $value != '')">
                                <fn:string key="smiles"><xsl:value-of select="$value"/></fn:string>
                            </xsl:when>
                            <xsl:when test="$format = 'InChI' or starts-with($value, 'InChI=')">
                                <fn:string key="_inchi"><xsl:value-of select="$value"/></fn:string>
                            </xsl:when>
                            <xsl:when test="$format = 'InChI-Key' or local:is-inchi-key($value)">
                                <fn:string key="_inchiKey"><xsl:value-of select="$value"/></fn:string>
                            </xsl:when>
                            <xsl:when test="$format = 'MOLFILE' or contains($value, 'V2000') or contains($value, 'V3000')">
                                <fn:string key="molfile"><xsl:value-of select="$value"/></fn:string>
                            </xsl:when>
                        </xsl:choose>
                    </xsl:for-each>
                </xsl:if>
                <xsl:if test="$structure/fn:map[@key='stereochemistry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="stereochemistry"><xsl:value-of select="$structure/fn:map[@key='stereochemistry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$structure/fn:map[@key='opticalActivity']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="opticalActivity"><xsl:value-of select="$structure/fn:map[@key='opticalActivity']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$structure/fn:array[@key='sourceDocument']/fn:map/fn:string[@key='reference']/text()">
                    <fn:array key="references">
                        <xsl:for-each select="$structure/fn:array[@key='sourceDocument']/fn:map/fn:string[@key='reference']/text()">
                            <fn:string><xsl:value-of select="substring(., 2)"/></fn:string>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="moieties">
        <xsl:if test="$root/fn:array[@key='moiety']/fn:map">
            <fn:array key="moieties">
                <xsl:for-each select="$root/fn:array[@key='moiety']/fn:map">
                    <fn:map>
                        <xsl:variable name="idValue" select="fn:array[@key='identifier']/fn:map/fn:string[@key='value']/text()"/>
                        <xsl:if test="$idValue and starts-with($idValue, 'urn:uuid:')">
                            <fn:string key="uuid"><xsl:value-of select="substring-after($idValue, 'urn:uuid:')"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='role']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="role"><xsl:value-of select="fn:map[@key='role']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:string[@key='molecularFormula']/text()">
                            <fn:string key="formula"><xsl:value-of select="fn:string[@key='molecularFormula']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='stereochemistry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="stereochemistry"><xsl:value-of select="fn:map[@key='stereochemistry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='opticalActivity']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="opticalActivity"><xsl:value-of select="fn:map[@key='opticalActivity']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='amountQuantity']/fn:number[@key='value']/text() or fn:map[@key='amountQuantity']/fn:string[@key='unit']/text()">
                            <fn:map key="countAmount">
                                <xsl:if test="fn:map[@key='amountQuantity']/fn:number[@key='value']/text()">
                                    <fn:number key="average"><xsl:value-of select="fn:map[@key='amountQuantity']/fn:number[@key='value']/text()"/></fn:number>
                                </xsl:if>
                                <xsl:if test="fn:map[@key='amountQuantity']/fn:string[@key='unit']/text()">
                                    <fn:string key="units"><xsl:value-of select="fn:map[@key='amountQuantity']/fn:string[@key='unit']/text()"/></fn:string>
                                </xsl:if>
                            </fn:map>
                        </xsl:if>
                        <xsl:if test="fn:string[@key='name']/text()">
                            <fn:map key="structure">
                                <fn:string key="formula"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                            </fn:map>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
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
                        <xsl:if test="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="type"><xsl:value-of select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                            <fn:array key="references">
                                <xsl:for-each select="fn:array[@key='source']/fn:map/fn:string[@key='reference']/text()">
                                    <fn:string><xsl:value-of select="substring(., 2)"/></fn:string>
                                </xsl:for-each>
                            </fn:array>
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
                        <xsl:variable name="securityCoding" select="fn:array[@key='securityLabel']/fn:map[1]/fn:array[@key='coding']/fn:map[1]"/>
                        <xsl:variable name="securitySystem" select="$securityCoding/fn:string[@key='system']/text()"/>
                        <xsl:variable name="securityCode" select="$securityCoding/fn:string[@key='code']/text()"/>
                        <xsl:choose>
                            <xsl:when test="contains($securitySystem, 'v3-Confidentiality') and ($securityCode = ('N', 'U'))">
                                <fn:boolean key="publicDomain">true</fn:boolean>
                            </xsl:when>
                            <xsl:when test="contains($securitySystem, 'v3-Confidentiality') and $securityCode = 'R'">
                                <fn:boolean key="publicDomain">false</fn:boolean>
                            </xsl:when>
                        </xsl:choose>
                        <xsl:if test="$securityCode = 'U'">
                            <fn:array key="tags">
                                <fn:string>PUBLIC_DOMAIN_RELEASE</fn:string>
                            </fn:array>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="notes">
        <xsl:if test="$root/fn:array[@key='note']/fn:map/fn:string[@key='text']/text()">
            <fn:array key="notes">
                <xsl:for-each select="$root/fn:array[@key='note']/fn:map[fncstring[@key='text']/text()]">
                    <fn:map>
                        <fn:string key="note"><xsl:value-of select="fn:string[@key='text']/text()"/></fn:string>
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
                        <xsl:if test="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="name"><xsl:value-of select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='valueQuantity']/fn:number[@key='value']/text() or fn:map[@key='valueQuantity']/fn:string[@key='unit']/text()">
                            <fn:map key="value">
                                <xsl:if test="fn:map[@key='valueQuantity']/fn:number[@key='value']/text()">
                                    <fn:number key="average"><xsl:value-of select="fn:map[@key='valueQuantity']/fn:number[@key='value']/text()"/></fn:number>
                                </xsl:if>
                                <xsl:if test="fn:map[@key='valueQuantity']/fn:string[@key='unit']/text()">
                                    <fn:string key="units"><xsl:value-of select="fn:map[@key='valueQuantity']/fn:string[@key='unit']/text()"/></fn:string>
                                </xsl:if>
                            </fn:map>
                        </xsl:if>
                        <xsl:if test="fn:map[@key='valueCodeableConcept']/fn:string[@key='text']/text()">
                            <fn:map key="value">
                                <fn:string key="nonNumericValue"><xsl:value-of select="fn:map[@key='valueCodeableConcept']/fn:string[@key='text']/text()"/></fn:string>
                            </fn:map>
                        </xsl:if>
                    </fn:map>
                </xsl:for-each>
            </fn:array>
        </xsl:if>
    </xsl:template>

    <xsl:template name="protein">
        <xsl:variable name="proteinRef" select="$root/fn:map[@key='protein']/fn:string[@key='reference']/text()"/>
        <xsl:variable name="proteinContained" select="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstanceProtein'][concat('#', fn:string[@key='id']/text()) = $proteinRef]"/>
        <xsl:if test="$proteinContained">
            <fn:map key="protein">
                <xsl:if test="$proteinContained/fn:string[@key='id']">
                    <fn:string key="uuid"><xsl:value-of select="$proteinContained/fn:string[@key='id']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$proteinContained/fn:map[@key='sequenceType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="sequenceType"><xsl:value-of select="$proteinContained/fn:map[@key='sequenceType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$proteinContained/fn:array[@key='subunit']/fn:map">
                    <fn:array key="subunits">
                        <xsl:for-each select="$proteinContained/fn:array[@key='subunit']/fn:map">
                            <fn:map>
                                <fn:number key="subunitIndex"><xsl:value-of select="fn:number[@key='subunit']/text()"/></fn:number>
                                <fn:string key="sequence"><xsl:value-of select="fn:string[@key='sequence']/text()"/></fn:string>
                                <fn:number key="length"><xsl:value-of select="fn:number[@key='length']/text()"/></fn:number>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:if test="$proteinContained/fn:array[@key='disulfideLinkage']/fn:string/text()">
                    <fn:array key="disulfideLinks">
                        <xsl:for-each select="$proteinContained/fn:array[@key='disulfideLinkage']/fn:string/text()">
                            <fn:map>
                                <fn:string key="sitesShorthand"><xsl:value-of select="."/></fn:string>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="nucleicAcid">
        <xsl:variable name="naRef" select="$root/fn:map[@key='nucleicAcid']/fn:string[@key='reference']/text()"/>
        <xsl:variable name="naContained" select="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstanceNucleicAcid'][concat('#', fn:string[@key='id']/text()) = $naRef]"/>
        <xsl:if test="$naContained">
            <fn:map key="nucleicAcid">
                <xsl:if test="$naContained/fn:string[@key='id']">
                    <fn:string key="uuid"><xsl:value-of select="$naContained/fn:string[@key='id']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$naContained/fn:map[@key='sequenceType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="sequenceType"><xsl:value-of select="$naContained/fn:map[@key='sequenceType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$naContained/fn:map[@key='oligoNucleotideType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="nucleicAcidType"><xsl:value-of select="$naContained/fn:map[@key='oligoNucleotideType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$naContained/fn:array[@key='subunit']/fn:map">
                    <fn:array key="subunits">
                        <xsl:for-each select="$naContained/fn:array[@key='subunit']/fn:map">
                            <fn:map>
                                <fn:number key="subunitIndex"><xsl:value-of select="fn:number[@key='subunit']/text()"/></fn:number>
                                <fn:string key="sequence"><xsl:value-of select="fn:string[@key='sequence']/text()"/></fn:string>
                                <fn:number key="length"><xsl:value-of select="fn:number[@key='length']/text()"/></fn:number>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:if test="$naContained/fn:array[@key='subunit'][1]/fn:map/fn:array[@key='linkage']/fn:map">
                    <fn:array key="linkages">
                        <xsl:for-each select="$naContained/fn:array[@key='subunit'][1]/fn:map/fn:array[@key='linkage']/fn:map">
                            <fn:map>
                                <fn:string key="linkage"><xsl:value-of select="fn:string[@key='connectivity']/text()"/></fn:string>
                                <fn:string key="sitesShorthand"><xsl:value-of select="fn:string[@key='residueSite']/text()"/></fn:string>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:if test="$naContained/fn:array[@key='subunit'][1]/fn:map/fn:array[@key='sugar']/fn:map">
                    <fn:array key="sugars">
                        <xsl:for-each select="$naContained/fn:array[@key='subunit'][1]/fn:map/fn:array[@key='sugar']/fn:map">
                            <fn:map>
                                <fn:string key="sugar"><xsl:value-of select="fn:string[@key='name']/text()"/></fn:string>
                                <fn:string key="sitesShorthand"><xsl:value-of select="fn:string[@key='residueSite']/text()"/></fn:string>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="polymer">
        <xsl:variable name="polyRef" select="$root/fn:map[@key='polymer']/fn:string[@key='reference']/text()"/>
        <xsl:variable name="polyContained" select="$root/fn:array[@key='contained']/fn:map[fn:string[@key='resourceType']='SubstancePolymer'][concat('#', fn:string[@key='id']/text()) = $polyRef]"/>
        <xsl:if test="$polyContained">
            <fn:map key="polymer">
                <xsl:if test="$polyContained/fn:string[@key='id']">
                    <fn:string key="uuid"><xsl:value-of select="$polyContained/fn:string[@key='id']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$polyContained/fn:map[@key='class']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() or
                             $polyContained/fn:map[@key='geometry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() or
                             $polyContained/fn:array[@key='copolymerConnectivity']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:map key="classification">
                        <xsl:if test="$polyContained/fn:map[@key='class']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="polymerClass"><xsl:value-of select="$polyContained/fn:map[@key='class']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="$polyContained/fn:map[@key='geometry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string key="polymerGeometry"><xsl:value-of select="$polyContained/fn:map[@key='geometry']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                        </xsl:if>
                        <xsl:if test="$polyContained/fn:array[@key='copolymerConnectivity']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:array key="polymerSubclass">
                                <xsl:for-each select="$polyContained/fn:array[@key='copolymerConnectivity']/fn:map/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <fn:string><xsl:value-of select="."/></fn:string>
                                </xsl:for-each>
                            </fn:array>
                        </xsl:if>
                    </fn:map>
                </xsl:if>
                <xsl:if test="$polyContained/fn:array[@key='monomerSet']/fn:map">
                    <fn:array key="monomers">
                        <xsl:for-each select="$polyContained/fn:array[@key='monomerSet']/fn:map">
                            <fn:map>
                                <xsl:variable name="material" select="fn:array[@key='startingMaterial']/fn:map[1]"/>
                                <xsl:if test="$material/fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                    <fn:map key="monomerSubstance">
                                        <fn:string key="approvalID"><xsl:value-of select="$material/fn:map[@key='code']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                                    </fn:map>
                                </xsl:if>
                                <xsl:if test="$material/fn:boolean[@key='isDefining']/text()">
                                    <fn:boolean key="defining"><xsl:value-of select="$material/fn:boolean[@key='isDefining']/text()"/></fn:boolean>
                                </xsl:if>
                                <xsl:if test="fn:map[@key='ratioType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text() or
                                             $material/fn:map[@key='amount']/fn:number[@key='value']/text() or
                                             $material/fn:map[@key='amount']/fn:string[@key='unit']/text()">
                                    <fn:map key="amount">
                                        <xsl:if test="fn:map[@key='ratioType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                                            <fn:string key="type"><xsl:value-of select="fn:map[@key='ratioType']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/></fn:string>
                                        </xsl:if>
                                        <xsl:if test="$material/fn:map[@key='amount']/fn:number[@key='value']/text()">
                                            <fn:number key="average"><xsl:value-of select="$material/fn:map[@key='amount']/fn:number[@key='value']/text()"/></fn:number>
                                        </xsl:if>
                                        <xsl:if test="$material/fn:map[@key='amount']/fn:string[@key='unit']/text()">
                                            <fn:string key="units"><xsl:value-of select="$material/fn:map[@key='amount']/fn:string[@key='unit']/text()"/></fn:string>
                                        </xsl:if>
                                    </fn:map>
                                </xsl:if>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:variable name="repeat" select="$polyContained/fn:array[@key='repeat']/fn:map[1]"/>
                <xsl:variable name="repeatUnit" select="$repeat/fn:array[@key='repeatUnit']/fn:map[1]"/>
                <xsl:variable name="repr" select="$repeatUnit/fn:array[@key='structuralRepresentation']/fn:map[fn:map[@key='format']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()='MOLFILE'][1]"/>
                <xsl:variable name="displayMolfile" select="$repr/fn:string[@key='representation']/text()"/>
                <xsl:variable name="displayFormula" select="$repeatUnit/fn:string[@key='amountSubstanceType']/text()"/>
                <xsl:variable name="displayMwt" select="$repeat/fn:map[@key='averageMolecularFormula']/fn:string[@key='text']/text()"/>
                <xsl:if test="$displayMolfile or $displayFormula or $displayMwt">
                    <fn:map key="displayStructure">
                        <xsl:if test="$displayMolfile">
                            <fn:string key="molfile"><xsl:value-of select="$displayMolfile"/></fn:string>
                        </xsl:if>
                        <xsl:if test="$displayFormula">
                            <fn:string key="formula"><xsl:value-of select="$displayFormula"/></fn:string>
                        </xsl:if>
                        <xsl:if test="$displayMwt">
                            <fn:number key="mwt"><xsl:value-of select="$displayMwt"/></fn:number>
                        </xsl:if>
                    </fn:map>
                </xsl:if>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="structurallyDiverse">
        <xsl:if test="$root/fn:map[@key='sourceMaterial']">
            <fn:map key="structurallyDiverse">
                <xsl:variable name="sm" select="$root/fn:map[@key='sourceMaterial']"/>
                <xsl:if test="$sm/fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:string key="sourceMaterialClass"><xsl:value-of select="$sm/fn:map[@key='type']/fn:array[@key='coding']/fn:map[1]/fn:string[@key='code']/text()"/></fn:string>
                    <xsl:if test="$sm/fn:map[@key='type']/fn:array[@key='coding']/fn:map[2]/fn:string[@key='code']/text()">
                        <fn:string key="sourceMaterialType"><xsl:value-of select="$sm/fn:map[@key='type']/fn:array[@key='coding']/fn:map[2]/fn:string[@key='code']/text()"/></fn:string>
                    </xsl:if>
                </xsl:if>
                <xsl:if test="$sm/fn:map[@key='genus']/fn:string[@key='text']/text()">
                    <fn:string key="organismGenus"><xsl:value-of select="$sm/fn:map[@key='genus']/fn:string[@key='text']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$sm/fn:map[@key='species']/fn:string[@key='text']/text()">
                    <fn:string key="organismSpecies"><xsl:value-of select="$sm/fn:map[@key='species']/fn:string[@key='text']/text()"/></fn:string>
                </xsl:if>
                <xsl:if test="$sm/fn:map[@key='part']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                    <fn:array key="part">
                        <xsl:for-each select="$sm/fn:map[@key='part']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()">
                            <fn:string><xsl:value-of select="."/></fn:string>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="mixture">
        <xsl:if test="$root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'MIXTURE_COMPONENT_')] or
                      $root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'MIXTURE_COMPONENT_')]">
            <fn:map key="mixture">
                <fn:array key="components">
                    <xsl:for-each select="$root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'MIXTURE_COMPONENT_') or
                                                         starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'MIXTURE_COMPONENT_')]">
                        <fn:map>
                            <xsl:if test="fn:string[@key='id']">
                                <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                            </xsl:if>
                            <xsl:variable name="typeCode" select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/>
                            <xsl:variable name="typeDisplay" select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text()"/>
                            <fn:string key="type"><xsl:value-of select="if (starts-with($typeCode, 'MIXTURE_COMPONENT_')) then substring-after($typeCode, 'MIXTURE_COMPONENT_') else substring-after($typeDisplay, 'MIXTURE_COMPONENT_')"/></fn:string>
                            <xsl:variable name="ref" select="fn:map[@key='substanceDefinitionReference']/fn:string[@key='reference']/text()"/>
                            <xsl:if test="$ref and starts-with($ref, 'SubstanceDefinition/')">
                                <fn:map key="substance">
                                    <fn:string key="refuuid"><xsl:value-of select="substring-after($ref, 'SubstanceDefinition/')"/></fn:string>
                                </fn:map>
                            </xsl:if>
                        </fn:map>
                    </xsl:for-each>
                </fn:array>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

    <xsl:template name="specifiedSubstance">
        <xsl:if test="$root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'SSG1_CONSTITUENT_')] or
                      $root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'SSG1_CONSTITUENT_')]">
            <fn:map key="specifiedSubstance">
                <fn:array key="constituents">
                    <xsl:for-each select="$root/fn:array[@key='relationship']/fn:map[starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text(), 'SSG1_CONSTITUENT_') or
                                                         starts-with(fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text(), 'SSG1_CONSTITUENT_')]">
                        <fn:map>
                            <xsl:if test="fn:string[@key='id']">
                                <fn:string key="uuid"><xsl:value-of select="fn:string[@key='id']/text()"/></fn:string>
                            </xsl:if>
                            <xsl:variable name="roleCode" select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='code']/text()"/>
                            <xsl:variable name="roleDisplay" select="fn:map[@key='type']/fn:array[@key='coding']/fn:map/fn:string[@key='display']/text()"/>
                            <fn:string key="role"><xsl:value-of select="if (starts-with($roleCode, 'SSG1_CONSTITUENT_')) then substring-after($roleCode, 'SSG1_CONSTITUENT_') else substring-after($roleDisplay, 'SSG1_CONSTITUENT_')"/></fn:string>
                            <xsl:variable name="ref" select="fn:map[@key='substanceDefinitionReference']/fn:string[@key='reference']/text()"/>
                            <xsl:if test="$ref and starts-with($ref, 'SubstanceDefinition/')">
                                <fn:map key="substance">
                                    <fn:string key="refuuid"><xsl:value-of select="substring-after($ref, 'SubstanceDefinition/')"/></fn:string>
                                </fn:map>
                            </xsl:if>
                            <xsl:variable name="amountNode" select="fn:map[@key='amount']/fn:map[@key='valueQuantity']"/>
                            <xsl:if test="$amountNode/fn:number[@key='value']/text() or $amountNode/fn:string[@key='unit']/text()">
                                <fn:map key="amount">
                                    <xsl:if test="$amountNode/fn:number[@key='value']/text()">
                                        <fn:number key="average"><xsl:value-of select="$amountNode/fn:number[@key='value']/text()"/></fn:number>
                                    </xsl:if>
                                    <xsl:if test="$amountNode/fn:string[@key='unit']/text()">
                                        <fn:string key="units"><xsl:value-of select="$amountNode/fn:string[@key='unit']/text()"/></fn:string>
                                    </xsl:if>
                                </fn:map>
                            </xsl:if>
                        </fn:map>
                    </xsl:for-each>
                </fn:array>
                <xsl:variable name="publicRefId" select="local:first-public-reference-id()"/>
                <xsl:if test="$publicRefId">
                    <fn:array key="references">
                        <fn:string><xsl:value-of select="$publicRefId"/></fn:string>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:if>
    </xsl:template>

</xsl:stylesheet>
