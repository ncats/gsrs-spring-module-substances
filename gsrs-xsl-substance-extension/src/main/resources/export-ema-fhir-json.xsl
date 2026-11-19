<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:local="urn:local"
                exclude-result-prefixes="fn xs local">

    <xsl:output method="json" encoding="UTF-8"/>

    <xsl:param name="json-input" as="xs:string" required="yes"/>
    
    <!-- Current timestamp in ISO format for lastUpdated -->
    <xsl:variable name="current-timestamp" select="'2024-10-08T13:37:23.468+00:00'"/>

    <xsl:function name="local:epoch-to-iso" as="xs:string">
        <xsl:param name="epoch-ms" as="xs:string"/>
        <xsl:sequence select="fn:format-dateTime(
            xs:dateTime('1970-01-01T00:00:00Z')
                + xs:dayTimeDuration(concat('PT', xs:string(xs:integer(number($epoch-ms) div 1000)), 'S')),
            '[Y0001]-[M01]-[D01]T[H01]:[m01]:[s01]Z')"/>
    </xsl:function>

    <xsl:template match="/">
        <!-- Parse the JSON input string -->
        <xsl:variable name="json-doc" select="fn:json-to-xml($json-input)"/>
        
        <!-- Extract key values from source -->
        <xsl:variable name="root" select="$json-doc/fn:map"/>
        <xsl:variable name="uuid" select="string($root/fn:string[@key='uuid'])"/>
        <xsl:variable name="version" select="string($root/fn:string[@key='version'])"/>
        <xsl:variable name="status" select="string($root/fn:string[@key='status'])"/>
        <xsl:variable name="changeReason" select="string($root/fn:string[@key='changeReason'])"/>
        <xsl:variable name="approvedBy" select="string($root/fn:string[@key='approvedBy'])"/>
        <xsl:variable name="smsid" select="string($root/fn:array[@key='codes']/fn:map[fn:string[@key='codeSystem'] = 'SMS_ID']/fn:string[@key='code'])"/>
        <xsl:variable name="substanceClass" select="string($root/fn:string[@key='substanceClass'])"/>
        <xsl:variable name="created" select="local:epoch-to-iso(string($root/fn:number[@key='created']))"/>
        <xsl:variable name="lastEdited" select="local:epoch-to-iso(string($root/fn:number[@key='lastEdited']))"/>

        <!-- Transform the GSRS substance to FHIR SubstanceDefinition -->
        <xsl:variable name="transformed-xml">
            <fn:map xmlns:fn="http://www.w3.org/2005/xpath-functions">
                <!-- Resource metadata -->
                <fn:string key="resourceType">SubstanceDefinition</fn:string>
                <fn:string key="id">
                    <xsl:value-of select="$uuid"/>
                </fn:string>
                
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
                <fn:map key="identifier">
                    <fn:string key="system">https://spor.azure-api.net/sms/api/v2/SubstanceDefinition</fn:string>
                    <fn:string key="value">
                        <xsl:value-of select="$smsid"/>
                    </fn:string>
                </fn:map>
                
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
                            <fn:string key="system">https://spor.ema.europa.eu/v1/lists/100000000004</fn:string>
                            <fn:string key="code">100000000012</fn:string>
                            <fn:string key="display">Human use</fn:string>
                        </fn:map>
                    </fn:array>
                </fn:map>
                
                <!-- Names -->
                <fn:array key="name">
                    <xsl:for-each select="$root/fn:array[@key='names']/fn:map">
                        <xsl:variable name="name-value" select="fn:string[@key='name']/text()"/>
                        <xsl:variable name="type" select="fn:string[@key='type']/text()"/>
                        <xsl:variable name="preferred" select="fn:boolean[@key='displayName']/text()"/>
                        <fn:map>
                            <fn:string key="id"><xsl:value-of select="fn:string[@key='uuid']/text()"/></fn:string>
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
                            <xsl:if test="$code-system = ('EVMPD', 'CAS', 'SIAMED', 'FDA UNII', 'SVG', 'ECHA (EC/EINECS)')">
                            <fn:map>
                                <fn:string key="id">
                                    <xsl:value-of select="fn:string[@key='uuid']/text()"/>
                                </fn:string>
                                <fn:map key="code">
                                    <fn:array key="coding">
                                        <fn:map>
                                            <fn:string key="system">
                                                <xsl:text>https://spor.ema.europa.eu/v1/lists/100000000009/terms/</xsl:text>
                                                <xsl:choose>
                                                    <xsl:when test="$code-system eq 'EVMPD'">100000075665</xsl:when>
                                                    <xsl:when test="$code-system eq 'CAS'">100000075787</xsl:when>
                                                    <xsl:when test="$code-system eq 'SIAMED'">100000146035</xsl:when>
                                                    <xsl:when test="$code-system eq 'FDA UNII'">200000018817</xsl:when>
                                                    <xsl:when test="$code-system eq 'SVG'">200000025197</xsl:when>
                                                    <xsl:when test="$code-system eq 'ECHA (EC/EINECS)'">200000032418</xsl:when>
                                                </xsl:choose>
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

                <!-- Relationships -->
                <xsl:if test="$root/fn:array[@key='relationships']">
                    <fn:array key="relationship">
                        <xsl:for-each select="$root/fn:array[@key='relationships']/fn:map">
                            <xsl:variable name="rel-type" select="fn:string[@key='type']/text()"/>
                            <xsl:variable name="rel-ref" select="fn:map[@key='relatedSubstance']/fn:string[@key='refuuid']/text()"/>
                            <fn:map>
                                <fn:string key="id"><xsl:value-of select="fn:string[@key='uuid']/text()"/></fn:string>
                                <fn:map key="substanceDefinitionReference">
                                    <fn:string key="reference">
                                        <xsl:value-of select="concat('SubstanceDefinition/', $rel-ref)"/>
                                    </fn:string>
                                </fn:map>
                                <fn:map key="type">
                                    <xsl:call-template name="relationship">
                                        <xsl:with-param name="relationship-type" select="$rel-type"/>
                                    </xsl:call-template>
                                </fn:map>
                            </fn:map>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
                
                <!-- Property -->
                <xsl:variable name="props" select="$root/fn:array[@key='properties']/fn:map"/>
                <xsl:if test="$props">
                    <fn:array key="property">
                        <xsl:for-each select="$props">
                            <xsl:call-template name="property">
                                <xsl:with-param name="prop" select="."/>
                            </xsl:call-template>
                        </xsl:for-each>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:variable>
        
        <!-- Convert the transformed XML back to JSON -->
        <xsl:value-of select="fn:xml-to-json($transformed-xml) => fn:parse-json() => fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <xsl:template name="access">
        <xsl:param name="access-field" as="element()?"/>
        <fn:map>
            <fn:string key="url">https://ema.europa.eu/fhir/dataClassification</fn:string>
            <fn:map key="valueCoding">
                <fn:string key="system">https://spor.ema.europa.eu/v1/lists/200000004983</fn:string>
                <xsl:choose>
                    <xsl:when test="$access-field/fn:item or $access-field/*">
                        <fn:string key="code">200000004984</fn:string>
                        <fn:string key="display">Confidential</fn:string>
                    </xsl:when>
                    <xsl:otherwise>
                        <fn:string key="code">200000004985</fn:string>
                        <fn:string key="display">Public</fn:string>
                    </xsl:otherwise>
                </xsl:choose>
            </fn:map>
        </fn:map>
    </xsl:template>

    <xsl:template name="status">
        <xsl:param name="deprecated-field" as="element()?"/>
        <fn:array key="coding">
            <fn:map>
                <fn:string key="system">https://spor.ema.europa.eu/v1/lists/200000005003</fn:string>
                <xsl:choose>
                    <xsl:when test="$deprecated-field/text() eq 'true'">
                        <fn:string key="code">200000005006</fn:string>
                        <fn:string key="display">Non-Current</fn:string>
                    </xsl:when>
                    <xsl:otherwise>
                        <fn:string key="code">200000005004</fn:string>
                        <fn:string key="display">Current</fn:string>
                    </xsl:otherwise>
                </xsl:choose>
            </fn:map>
        </fn:array>
    </xsl:template>

    <xsl:template name="relationship">
        <xsl:param name="relationship-type"/>
        <fn:array key="coding">
            <fn:map>
                <fn:string key="system">https://spor.ema.europa.eu/v1/lists/200000004946</fn:string>
                <fn:string key="code">
                    <xsl:choose>
                        <xsl:when test="$relationship-type eq 'Authorised to Development'">200000004958</xsl:when>
                        <xsl:otherwise>200000004958</xsl:otherwise>
                    </xsl:choose>
                </fn:string>
                <fn:string key="display">
                    <xsl:value-of select="$relationship-type"/>
                </fn:string>
            </fn:map>
        </fn:array>
    </xsl:template>

    <xsl:template name="category">
        <xsl:param name="substanceClass-value"/>
        <fn:array key="coding">
            <fn:map>
                <fn:string key="system">https://spor.ema.europa.eu/v1/lists/100000075826</fn:string>
                <fn:string key="code">
                    <xsl:choose>
                        <xsl:when test="$substanceClass-value eq 'chemical'">100000075670</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'mixture'">200000005023</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'nucleicAcid'">200000005035</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'polymer'">200000005022</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'protein'">200000005020</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG1'">200000005031</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG2'">200000005032</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG3'">200000005033</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG4'">200000005034</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - allergen'">200000005026</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - cell therapy'">200000005029</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - herbal'">200000005025</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurallyDiverse'">200000005030</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - plasma derived'">200000005024</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - vaccine'">200000005027</xsl:when>
                        <xsl:otherwise>100000075670</xsl:otherwise>
                    </xsl:choose>
                </fn:string>
                <fn:string key="display">
                    <xsl:choose>
                        <xsl:when test="$substanceClass-value eq 'chemical'">Chemical</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'mixture'">Mixture</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'nucleicAcid'">Nucleic acid</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'polymer'">Polymer</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'protein'">Protein</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG1'">Specified Substance Group 1</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG2'">Specified Substance Group 2</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG3'">Specified Substance Group 3</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'specifiedSubstanceG4'">Specified Substance Group 4</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - allergen'">Structurally Diverse - Allergen</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - cell therapy'">Structurally Diverse - Cell therapy</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - herbal'">Structurally Diverse - Herbal</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurallyDiverse'">Structurally Diverse - Other</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - plasma derived'">Structurally Diverse - Plasma derived</xsl:when>
                        <xsl:when test="$substanceClass-value eq 'structurally diverse - vaccine'">Structurally Diverse - Vaccine</xsl:when>
                        <xsl:otherwise>Other</xsl:otherwise>
                    </xsl:choose>
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

    <xsl:template name="property">
        <xsl:param name="prop" as="element(fn:map)"/>
        <fn:map>
            <fn:map key="code">
                <fn:array key="coding">
                    <fn:map>
                        <fn:string key="system">https://spor.ema.europa.eu/v1/lists/200000029949</fn:string>
                        <fn:string key="code">
                            <xsl:value-of select="$prop/fn:string[@key='name']/text()"/>
                        </fn:string>
                        <fn:string key="display">
                            <xsl:value-of select="$prop/fn:string[@key='name']/text()"/>
                        </fn:string>
                    </fn:map>
                </fn:array>
            </fn:map>
            <xsl:if test="$prop/fn:map[@key='value']">
                <fn:string key="valueString">
                    <xsl:call-template name="build-value-string">
                        <xsl:with-param name="value-node" select="$prop/fn:map[@key='value']"/>
                    </xsl:call-template>
                </fn:string>
            </xsl:if>
            <xsl:if test="exists($prop/fn:array[@key='parameters']/fn:map)">
                <fn:string key="parameters">
                    <xsl:for-each select="$prop/fn:array[@key='parameters']/fn:map">
                        <xsl:if test="position() > 1">; </xsl:if>
                        <xsl:value-of select="fn:string[@key='name']/text()"/>
                        <xsl:text>: </xsl:text>
                        <xsl:call-template name="build-value-string">
                            <xsl:with-param name="value-node" select="fn:map[@key='value']"/>
                        </xsl:call-template>
                    </xsl:for-each>
                </fn:string>
            </xsl:if>
        </fn:map>
    </xsl:template>

    <xsl:template name="build-value-string">
        <xsl:param name="value-node" as="element(fn:map)?"/>
        <xsl:if test="$value-node">
            <xsl:variable name="non-num" select="$value-node/fn:string[@key='nonNumericValue']/text()"/>
            <xsl:variable name="high" select="$value-node/fn:number[@key='high']/text()"/>
            <xsl:variable name="low" select="$value-node/fn:number[@key='low']/text()"/>
            <xsl:variable name="avg" select="$value-node/fn:number[@key='average']/text()"/>
            <xsl:variable name="units" select="$value-node/fn:string[@key='units']/text()"/>
            <xsl:value-of select="string-join((
                $non-num,
                if ($high and $low) then concat($low, ' - ', $high, if ($units) then concat(' ', $units) else ()) else (),
                if ($avg) then concat($avg, if ($units) then concat(' ', $units) else ()) else ()
            ), ' ')"/>
        </xsl:if>
    </xsl:template>

    <xsl:template name="language">
        <xsl:param name="language-ident"/>
        <fn:map>
            <fn:array key="coding">
                <fn:map>
                    <fn:string key="system">https://spor.ema.europa.eu/v1/lists/100000072057</fn:string>
                    <fn:string key="code">
                        <xsl:choose>
                            <xsl:when test="$language-ident eq 'aa'">100000072124</xsl:when>
                            <xsl:when test="$language-ident eq 'ab'">100000072115</xsl:when>
                            <xsl:when test="$language-ident eq 'ae'">100000072118</xsl:when>
                            <xsl:when test="$language-ident eq 'af'">100000072125</xsl:when>
                            <xsl:when test="$language-ident eq 'ak'">100000072126</xsl:when>
                            <xsl:when test="$language-ident eq 'am'">100000072127</xsl:when>
                            <xsl:when test="$language-ident eq 'an'">100000072129</xsl:when>
                            <xsl:when test="$language-ident eq 'ar'">100000072128</xsl:when>
                            <xsl:when test="$language-ident eq 'as'">100000072130</xsl:when>
                            <xsl:when test="$language-ident eq 'av'">100000072131</xsl:when>
                            <xsl:when test="$language-ident eq 'ay'">100000072132</xsl:when>
                            <xsl:when test="$language-ident eq 'az'">100000072133</xsl:when>
                            <xsl:when test="$language-ident eq 'ba'">100000072119</xsl:when>
                            <xsl:when test="$language-ident eq 'be'">100000072136</xsl:when>
                            <xsl:when test="$language-ident eq 'bg'">100000072142</xsl:when>
                            <xsl:when test="$language-ident eq 'bh'">100000072137</xsl:when>
                            <xsl:when test="$language-ident eq 'bi'">100000072138</xsl:when>
                            <xsl:when test="$language-ident eq 'bm'">100000072134</xsl:when>
                            <xsl:when test="$language-ident eq 'bn'">100000072120</xsl:when>
                            <xsl:when test="$language-ident eq 'bo'">100000072293</xsl:when>
                            <xsl:when test="$language-ident eq 'br'">100000072139</xsl:when>
                            <xsl:when test="$language-ident eq 'bs'">100000072141</xsl:when>
                            <xsl:when test="$language-ident eq 'ca'">100000072161</xsl:when>
                            <xsl:when test="$language-ident eq 'ce'">100000072162</xsl:when>
                            <xsl:when test="$language-ident eq 'ch'">100000072143</xsl:when>
                            <xsl:when test="$language-ident eq 'co'">100000072166</xsl:when>
                            <xsl:when test="$language-ident eq 'cr'">100000072145</xsl:when>
                            <xsl:when test="$language-ident eq 'cs'">100000072167</xsl:when>
                            <xsl:when test="$language-ident eq 'cu'">100000072164</xsl:when>
                            <xsl:when test="$language-ident eq 'cv'">100000072144</xsl:when>
                            <xsl:when test="$language-ident eq 'cy'">100000072304</xsl:when>
                            <xsl:when test="$language-ident eq 'da'">100000072168</xsl:when>
                            <xsl:when test="$language-ident eq 'de'">100000072178</xsl:when>
                            <xsl:when test="$language-ident eq 'dv'">100000072146</xsl:when>
                            <xsl:when test="$language-ident eq 'dz'">100000072170</xsl:when>
                            <xsl:when test="$language-ident eq 'ee'">100000072148</xsl:when>
                            <xsl:when test="$language-ident eq 'el'">100000072181</xsl:when>
                            <xsl:when test="$language-ident eq 'en'">100000072147</xsl:when>
                            <xsl:when test="$language-ident eq 'eo'">100000072171</xsl:when>
                            <xsl:when test="$language-ident eq 'es'">100000072264</xsl:when>
                            <xsl:when test="$language-ident eq 'et'">100000072172</xsl:when>
                            <xsl:when test="$language-ident eq 'eu'">100000072135</xsl:when>
                            <xsl:when test="$language-ident eq 'fa'">100000072249</xsl:when>
                            <xsl:when test="$language-ident eq 'ff'">100000072150</xsl:when>
                            <xsl:when test="$language-ident eq 'fi'">100000072149</xsl:when>
                            <xsl:when test="$language-ident eq 'fj'">100000072174</xsl:when>
                            <xsl:when test="$language-ident eq 'fo'">100000072173</xsl:when>
                            <xsl:when test="$language-ident eq 'fr'">100000072175</xsl:when>
                            <xsl:when test="$language-ident eq 'fy'">100000072176</xsl:when>
                            <xsl:when test="$language-ident eq 'ga'">100000072179</xsl:when>
                            <xsl:when test="$language-ident eq 'gd'">100000072151</xsl:when>
                            <xsl:when test="$language-ident eq 'gl'">100000072180</xsl:when>
                            <xsl:when test="$language-ident eq 'gn'">100000072182</xsl:when>
                            <xsl:when test="$language-ident eq 'gu'">100000072153</xsl:when>
                            <xsl:when test="$language-ident eq 'gv'">100000072152</xsl:when>
                            <xsl:when test="$language-ident eq 'ha'">100000072184</xsl:when>
                            <xsl:when test="$language-ident eq 'he'">100000072154</xsl:when>
                            <xsl:when test="$language-ident eq 'hi'">100000072186</xsl:when>
                            <xsl:when test="$language-ident eq 'ho'">100000072155</xsl:when>
                            <xsl:when test="$language-ident eq 'hr'">100000072258</xsl:when>
                            <xsl:when test="$language-ident eq 'ht'">100000072183</xsl:when>
                            <xsl:when test="$language-ident eq 'hu'">100000072187</xsl:when>
                            <xsl:when test="$language-ident eq 'hy'">100000072117</xsl:when>
                            <xsl:when test="$language-ident eq 'hz'">100000072185</xsl:when>
                            <xsl:when test="$language-ident eq 'ia'">100000072192</xsl:when>
                            <xsl:when test="$language-ident eq 'id'">100000072158</xsl:when>
                            <xsl:when test="$language-ident eq 'ie'">100000072191</xsl:when>
                            <xsl:when test="$language-ident eq 'ig'">100000072188</xsl:when>
                            <xsl:when test="$language-ident eq 'ii'">100000072190</xsl:when>
                            <xsl:when test="$language-ident eq 'ik'">100000072193</xsl:when>
                            <xsl:when test="$language-ident eq 'io'">100000072189</xsl:when>
                            <xsl:when test="$language-ident eq 'is'">100000072156</xsl:when>
                            <xsl:when test="$language-ident eq 'it'">100000072194</xsl:when>
                            <xsl:when test="$language-ident eq 'iu'">100000072157</xsl:when>
                            <xsl:when test="$language-ident eq 'ja'">100000072195</xsl:when>
                            <xsl:when test="$language-ident eq 'jv'">100000072159</xsl:when>
                            <xsl:when test="$language-ident eq 'ka'">100000072177</xsl:when>
                            <xsl:when test="$language-ident eq 'kg'">100000072203</xsl:when>
                            <xsl:when test="$language-ident eq 'ki'">100000072200</xsl:when>
                            <xsl:when test="$language-ident eq 'kj'">100000072224</xsl:when>
                            <xsl:when test="$language-ident eq 'kk'">100000072201</xsl:when>
                            <xsl:when test="$language-ident eq 'kl'">100000072196</xsl:when>
                            <xsl:when test="$language-ident eq 'km'">100000072199</xsl:when>
                            <xsl:when test="$language-ident eq 'kn'">100000072160</xsl:when>
                            <xsl:when test="$language-ident eq 'ko'">100000072223</xsl:when>
                            <xsl:when test="$language-ident eq 'kr'">100000072198</xsl:when>
                            <xsl:when test="$language-ident eq 'ks'">100000072197</xsl:when>
                            <xsl:when test="$language-ident eq 'ku'">100000072204</xsl:when>
                            <xsl:when test="$language-ident eq 'kv'">100000072222</xsl:when>
                            <xsl:when test="$language-ident eq 'kw'">100000072165</xsl:when>
                            <xsl:when test="$language-ident eq 'ky'">100000072221</xsl:when>
                            <xsl:when test="$language-ident eq 'la'">100000072226</xsl:when>
                            <xsl:when test="$language-ident eq 'lb'">100000072229</xsl:when>
                            <xsl:when test="$language-ident eq 'lg'">100000072207</xsl:when>
                            <xsl:when test="$language-ident eq 'li'">100000072227</xsl:when>
                            <xsl:when test="$language-ident eq 'ln'">100000072228</xsl:when>
                            <xsl:when test="$language-ident eq 'lo'">100000072225</xsl:when>
                            <xsl:when test="$language-ident eq 'lt'">100000072206</xsl:when>
                            <xsl:when test="$language-ident eq 'lu'">100000072230</xsl:when>
                            <xsl:when test="$language-ident eq 'lv'">100000072205</xsl:when>
                            <xsl:when test="$language-ident eq 'mg'">100000072235</xsl:when>
                            <xsl:when test="$language-ident eq 'mh'">100000072232</xsl:when>
                            <xsl:when test="$language-ident eq 'mi'">100000072233</xsl:when>
                            <xsl:when test="$language-ident eq 'mk'">100000072231</xsl:when>
                            <xsl:when test="$language-ident eq 'ml'">100000072208</xsl:when>
                            <xsl:when test="$language-ident eq 'mn'">100000072237</xsl:when>
                            <xsl:when test="$language-ident eq 'mo'">100000072210</xsl:when>
                            <xsl:when test="$language-ident eq 'mr'">100000072234</xsl:when>
                            <xsl:when test="$language-ident eq 'ms'">100000072209</xsl:when>
                            <xsl:when test="$language-ident eq 'mt'">100000072236</xsl:when>
                            <xsl:when test="$language-ident eq 'my'">100000072140</xsl:when>
                            <xsl:when test="$language-ident eq 'na'">100000072238</xsl:when>
                            <xsl:when test="$language-ident eq 'nb'">100000072213</xsl:when>
                            <xsl:when test="$language-ident eq 'nd'">100000072240</xsl:when>
                            <xsl:when test="$language-ident eq 'ne'">100000072241</xsl:when>
                            <xsl:when test="$language-ident eq 'ng'">100000072212</xsl:when>
                            <xsl:when test="$language-ident eq 'nl'">100000072169</xsl:when>
                            <xsl:when test="$language-ident eq 'nn'">100000072242</xsl:when>
                            <xsl:when test="$language-ident eq 'no'">100000072243</xsl:when>
                            <xsl:when test="$language-ident eq 'nr'">100000072239</xsl:when>
                            <xsl:when test="$language-ident eq 'nv'">100000072211</xsl:when>
                            <xsl:when test="$language-ident eq 'ny'">100000072214</xsl:when>
                            <xsl:when test="$language-ident eq 'oc'">100000072245</xsl:when>
                            <xsl:when test="$language-ident eq 'oj'">100000072246</xsl:when>
                            <xsl:when test="$language-ident eq 'om'">100000072247</xsl:when>
                            <xsl:when test="$language-ident eq 'or'">100000072215</xsl:when>
                            <xsl:when test="$language-ident eq 'os'">100000072248</xsl:when>
                            <xsl:when test="$language-ident eq 'pa'">100000072216</xsl:when>
                            <xsl:when test="$language-ident eq 'pi'">100000072250</xsl:when>
                            <xsl:when test="$language-ident eq 'pl'">100000072217</xsl:when>
                            <xsl:when test="$language-ident eq 'ps'">100000072252</xsl:when>
                            <xsl:when test="$language-ident eq 'pt'">100000072251</xsl:when>
                            <xsl:when test="$language-ident eq 'qu'">100000072218</xsl:when>
                            <xsl:when test="$language-ident eq 'rm'">100000072253</xsl:when>
                            <xsl:when test="$language-ident eq 'rn'">100000072219</xsl:when>
                            <xsl:when test="$language-ident eq 'ro'">100000072254</xsl:when>
                            <xsl:when test="$language-ident eq 'ru'">100000072255</xsl:when>
                            <xsl:when test="$language-ident eq 'rw'">100000072202</xsl:when>
                            <xsl:when test="$language-ident eq 'sa'">100000072220</xsl:when>
                            <xsl:when test="$language-ident eq 'sc'">100000072285</xsl:when>
                            <xsl:when test="$language-ident eq 'sd'">100000072263</xsl:when>
                            <xsl:when test="$language-ident eq 'se'">100000072262</xsl:when>
                            <xsl:when test="$language-ident eq 'sg'">100000072256</xsl:when>
                            <xsl:when test="$language-ident eq 'si'">100000072261</xsl:when>
                            <xsl:when test="$language-ident eq 'sk'">100000072259</xsl:when>
                            <xsl:when test="$language-ident eq 'sl'">100000072260</xsl:when>
                            <xsl:when test="$language-ident eq 'sm'">100000072281</xsl:when>
                            <xsl:when test="$language-ident eq 'sn'">100000072282</xsl:when>
                            <xsl:when test="$language-ident eq 'so'">100000072283</xsl:when>
                            <xsl:when test="$language-ident eq 'sq'">100000072116</xsl:when>
                            <xsl:when test="$language-ident eq 'sr'">100000072257</xsl:when>
                            <xsl:when test="$language-ident eq 'ss'">100000072286</xsl:when>
                            <xsl:when test="$language-ident eq 'st'">100000072284</xsl:when>
                            <xsl:when test="$language-ident eq 'su'">100000072265</xsl:when>
                            <xsl:when test="$language-ident eq 'sv'">100000072288</xsl:when>
                            <xsl:when test="$language-ident eq 'sw'">100000072287</xsl:when>
                            <xsl:when test="$language-ident eq 'ta'">100000072289</xsl:when>
                            <xsl:when test="$language-ident eq 'te'">100000072267</xsl:when>
                            <xsl:when test="$language-ident eq 'tg'">100000072291</xsl:when>
                            <xsl:when test="$language-ident eq 'th'">100000072268</xsl:when>
                            <xsl:when test="$language-ident eq 'ti'">100000072294</xsl:when>
                            <xsl:when test="$language-ident eq 'tk'">100000072270</xsl:when>
                            <xsl:when test="$language-ident eq 'tl'">100000072292</xsl:when>
                            <xsl:when test="$language-ident eq 'tn'">100000072295</xsl:when>
                            <xsl:when test="$language-ident eq 'to'">100000072269</xsl:when>
                            <xsl:when test="$language-ident eq 'tr'">100000072297</xsl:when>
                            <xsl:when test="$language-ident eq 'ts'">100000072296</xsl:when>
                            <xsl:when test="$language-ident eq 'tt'">100000072290</xsl:when>
                            <xsl:when test="$language-ident eq 'tw'">100000072298</xsl:when>
                            <xsl:when test="$language-ident eq 'ty'">100000072266</xsl:when>
                            <xsl:when test="$language-ident eq 'ug'">100000072271</xsl:when>
                            <xsl:when test="$language-ident eq 'uk'">100000072299</xsl:when>
                            <xsl:when test="$language-ident eq 'ur'">100000072300</xsl:when>
                            <xsl:when test="$language-ident eq 'uz'">100000072272</xsl:when>
                            <xsl:when test="$language-ident eq 've'">100000072301</xsl:when>
                            <xsl:when test="$language-ident eq 'vi'">100000072302</xsl:when>
                            <xsl:when test="$language-ident eq 'vo'">100000072303</xsl:when>
                            <xsl:when test="$language-ident eq 'wa'">100000072273</xsl:when>
                            <xsl:when test="$language-ident eq 'wo'">100000072305</xsl:when>
                            <xsl:when test="$language-ident eq 'xh'">100000072306</xsl:when>
                            <xsl:when test="$language-ident eq 'yi'">100000072307</xsl:when>
                            <xsl:when test="$language-ident eq 'yo'">100000072274</xsl:when>
                            <xsl:when test="$language-ident eq 'za'">100000072308</xsl:when>
                            <xsl:when test="$language-ident eq 'zh'">100000072163</xsl:when>
                            <xsl:when test="$language-ident eq 'zu'">100000072309</xsl:when>
                            <xsl:otherwise>100000072147</xsl:otherwise>
                        </xsl:choose>
                    </fn:string>
                    <fn:string key="display">
                        <xsl:choose>
                            <xsl:when test="$language-ident eq 'aa'">Afar</xsl:when>
                            <xsl:when test="$language-ident eq 'ab'">Abkhazian</xsl:when>
                            <xsl:when test="$language-ident eq 'ae'">Avestan</xsl:when>
                            <xsl:when test="$language-ident eq 'af'">Afrikaans</xsl:when>
                            <xsl:when test="$language-ident eq 'ak'">Akan</xsl:when>
                            <xsl:when test="$language-ident eq 'am'">Amharic</xsl:when>
                            <xsl:when test="$language-ident eq 'an'">Aragonese</xsl:when>
                            <xsl:when test="$language-ident eq 'ar'">Arabic</xsl:when>
                            <xsl:when test="$language-ident eq 'as'">Assamese</xsl:when>
                            <xsl:when test="$language-ident eq 'av'">Avaric</xsl:when>
                            <xsl:when test="$language-ident eq 'ay'">Aymara</xsl:when>
                            <xsl:when test="$language-ident eq 'az'">Azerbaijani</xsl:when>
                            <xsl:when test="$language-ident eq 'ba'">Bashkir</xsl:when>
                            <xsl:when test="$language-ident eq 'be'">Belarusian</xsl:when>
                            <xsl:when test="$language-ident eq 'bg'">Bulgarian</xsl:when>
                            <xsl:when test="$language-ident eq 'bh'">Bihari</xsl:when>
                            <xsl:when test="$language-ident eq 'bi'">Bislama</xsl:when>
                            <xsl:when test="$language-ident eq 'bm'">Bambara</xsl:when>
                            <xsl:when test="$language-ident eq 'bn'">Bengali</xsl:when>
                            <xsl:when test="$language-ident eq 'bo'">Tibetan</xsl:when>
                            <xsl:when test="$language-ident eq 'br'">Breton</xsl:when>
                            <xsl:when test="$language-ident eq 'bs'">Bosnian</xsl:when>
                            <xsl:when test="$language-ident eq 'ca'">Catalan</xsl:when>
                            <xsl:when test="$language-ident eq 'ce'">Chechen</xsl:when>
                            <xsl:when test="$language-ident eq 'ch'">Chamorro</xsl:when>
                            <xsl:when test="$language-ident eq 'co'">Corsican</xsl:when>
                            <xsl:when test="$language-ident eq 'cr'">Cree</xsl:when>
                            <xsl:when test="$language-ident eq 'cs'">Czech</xsl:when>
                            <xsl:when test="$language-ident eq 'cu'">Church Slavic</xsl:when>
                            <xsl:when test="$language-ident eq 'cv'">Chuvash</xsl:when>
                            <xsl:when test="$language-ident eq 'cy'">Welsh</xsl:when>
                            <xsl:when test="$language-ident eq 'da'">Danish</xsl:when>
                            <xsl:when test="$language-ident eq 'de'">German</xsl:when>
                            <xsl:when test="$language-ident eq 'dv'">Maldivian</xsl:when>
                            <xsl:when test="$language-ident eq 'dz'">Dzongkha</xsl:when>
                            <xsl:when test="$language-ident eq 'ee'">Ewe</xsl:when>
                            <xsl:when test="$language-ident eq 'el'">Greek, Modern (1453-)</xsl:when>
                            <xsl:when test="$language-ident eq 'en'">English</xsl:when>
                            <xsl:when test="$language-ident eq 'eo'">Esperanto</xsl:when>
                            <xsl:when test="$language-ident eq 'es'">Spanish</xsl:when>
                            <xsl:when test="$language-ident eq 'et'">Estonian</xsl:when>
                            <xsl:when test="$language-ident eq 'eu'">Basque</xsl:when>
                            <xsl:when test="$language-ident eq 'fa'">Persian</xsl:when>
                            <xsl:when test="$language-ident eq 'ff'">Fulah</xsl:when>
                            <xsl:when test="$language-ident eq 'fi'">Finnish</xsl:when>
                            <xsl:when test="$language-ident eq 'fj'">Fijian</xsl:when>
                            <xsl:when test="$language-ident eq 'fo'">Faroese</xsl:when>
                            <xsl:when test="$language-ident eq 'fr'">French</xsl:when>
                            <xsl:when test="$language-ident eq 'fy'">Western Frisian</xsl:when>
                            <xsl:when test="$language-ident eq 'ga'">Irish</xsl:when>
                            <xsl:when test="$language-ident eq 'gd'">Gaelic</xsl:when>
                            <xsl:when test="$language-ident eq 'gl'">Galician</xsl:when>
                            <xsl:when test="$language-ident eq 'gn'">Guarani</xsl:when>
                            <xsl:when test="$language-ident eq 'gu'">Gujarati</xsl:when>
                            <xsl:when test="$language-ident eq 'gv'">Manx</xsl:when>
                            <xsl:when test="$language-ident eq 'ha'">Hausa</xsl:when>
                            <xsl:when test="$language-ident eq 'he'">Hebrew</xsl:when>
                            <xsl:when test="$language-ident eq 'hi'">Hindi</xsl:when>
                            <xsl:when test="$language-ident eq 'ho'">Hiri Motu</xsl:when>
                            <xsl:when test="$language-ident eq 'hr'">Croatian</xsl:when>
                            <xsl:when test="$language-ident eq 'ht'">Haitian</xsl:when>
                            <xsl:when test="$language-ident eq 'hu'">Hungarian</xsl:when>
                            <xsl:when test="$language-ident eq 'hy'">Armenian</xsl:when>
                            <xsl:when test="$language-ident eq 'hz'">Herero</xsl:when>
                            <xsl:when test="$language-ident eq 'ia'">Interlingua (International Auxiliary Language Association)</xsl:when>
                            <xsl:when test="$language-ident eq 'id'">Indonesian</xsl:when>
                            <xsl:when test="$language-ident eq 'ie'">Interlingue</xsl:when>
                            <xsl:when test="$language-ident eq 'ig'">Igbo</xsl:when>
                            <xsl:when test="$language-ident eq 'ii'">Sichuan Yi</xsl:when>
                            <xsl:when test="$language-ident eq 'ik'">Inupiaq</xsl:when>
                            <xsl:when test="$language-ident eq 'io'">Ido</xsl:when>
                            <xsl:when test="$language-ident eq 'is'">Icelandic</xsl:when>
                            <xsl:when test="$language-ident eq 'it'">Italian</xsl:when>
                            <xsl:when test="$language-ident eq 'iu'">Inuktitut</xsl:when>
                            <xsl:when test="$language-ident eq 'ja'">Japanese</xsl:when>
                            <xsl:when test="$language-ident eq 'jv'">Javanese</xsl:when>
                            <xsl:when test="$language-ident eq 'ka'">Georgian</xsl:when>
                            <xsl:when test="$language-ident eq 'kg'">Kongo</xsl:when>
                            <xsl:when test="$language-ident eq 'ki'">Kikuyu</xsl:when>
                            <xsl:when test="$language-ident eq 'kj'">Kuanyama</xsl:when>
                            <xsl:when test="$language-ident eq 'kk'">Kazakh</xsl:when>
                            <xsl:when test="$language-ident eq 'kl'">Greenlandic</xsl:when>
                            <xsl:when test="$language-ident eq 'km'">Central Khmer</xsl:when>
                            <xsl:when test="$language-ident eq 'kn'">Kannada</xsl:when>
                            <xsl:when test="$language-ident eq 'ko'">Korean</xsl:when>
                            <xsl:when test="$language-ident eq 'kr'">Kanuri</xsl:when>
                            <xsl:when test="$language-ident eq 'ks'">Kashmiri</xsl:when>
                            <xsl:when test="$language-ident eq 'ku'">Kurdish</xsl:when>
                            <xsl:when test="$language-ident eq 'kv'">Komi</xsl:when>
                            <xsl:when test="$language-ident eq 'kw'">Cornish</xsl:when>
                            <xsl:when test="$language-ident eq 'ky'">Kirghiz</xsl:when>
                            <xsl:when test="$language-ident eq 'la'">Latin</xsl:when>
                            <xsl:when test="$language-ident eq 'lb'">Luxembourgish</xsl:when>
                            <xsl:when test="$language-ident eq 'lg'">Ganda</xsl:when>
                            <xsl:when test="$language-ident eq 'li'">Limburgan</xsl:when>
                            <xsl:when test="$language-ident eq 'ln'">Lingala</xsl:when>
                            <xsl:when test="$language-ident eq 'lo'">Lao</xsl:when>
                            <xsl:when test="$language-ident eq 'lt'">Lithuanian</xsl:when>
                            <xsl:when test="$language-ident eq 'lu'">Luba-Katanga</xsl:when>
                            <xsl:when test="$language-ident eq 'lv'">Latvian</xsl:when>
                            <xsl:when test="$language-ident eq 'mg'">Malagasy</xsl:when>
                            <xsl:when test="$language-ident eq 'mh'">Marshallese</xsl:when>
                            <xsl:when test="$language-ident eq 'mi'">Maori</xsl:when>
                            <xsl:when test="$language-ident eq 'mk'">Macedonian</xsl:when>
                            <xsl:when test="$language-ident eq 'ml'">Malayalam</xsl:when>
                            <xsl:when test="$language-ident eq 'mn'">Mongolian</xsl:when>
                            <xsl:when test="$language-ident eq 'mo'">Moldavian</xsl:when>
                            <xsl:when test="$language-ident eq 'mr'">Marathi</xsl:when>
                            <xsl:when test="$language-ident eq 'ms'">Malay</xsl:when>
                            <xsl:when test="$language-ident eq 'mt'">Maltese</xsl:when>
                            <xsl:when test="$language-ident eq 'my'">Burmese</xsl:when>
                            <xsl:when test="$language-ident eq 'na'">Nauru</xsl:when>
                            <xsl:when test="$language-ident eq 'nb'">Bokmål, Norwegian</xsl:when>
                            <xsl:when test="$language-ident eq 'nd'">North Ndebele</xsl:when>
                            <xsl:when test="$language-ident eq 'ne'">Nepali</xsl:when>
                            <xsl:when test="$language-ident eq 'ng'">Ndonga</xsl:when>
                            <xsl:when test="$language-ident eq 'nl'">Dutch</xsl:when>
                            <xsl:when test="$language-ident eq 'nn'">Norwegian Nynorsk</xsl:when>
                            <xsl:when test="$language-ident eq 'no'">Norwegian</xsl:when>
                            <xsl:when test="$language-ident eq 'nr'">South Ndebele</xsl:when>
                            <xsl:when test="$language-ident eq 'nv'">Navaho</xsl:when>
                            <xsl:when test="$language-ident eq 'ny'">Chichewa</xsl:when>
                            <xsl:when test="$language-ident eq 'oc'">Occitan (post 1500)</xsl:when>
                            <xsl:when test="$language-ident eq 'oj'">Ojibwa</xsl:when>
                            <xsl:when test="$language-ident eq 'om'">Oromo</xsl:when>
                            <xsl:when test="$language-ident eq 'or'">Oriya</xsl:when>
                            <xsl:when test="$language-ident eq 'os'">Ossetian</xsl:when>
                            <xsl:when test="$language-ident eq 'pa'">Panjabi</xsl:when>
                            <xsl:when test="$language-ident eq 'pi'">Pali</xsl:when>
                            <xsl:when test="$language-ident eq 'pl'">Polish</xsl:when>
                            <xsl:when test="$language-ident eq 'ps'">Pushto</xsl:when>
                            <xsl:when test="$language-ident eq 'pt'">Portuguese</xsl:when>
                            <xsl:when test="$language-ident eq 'qu'">Quechua</xsl:when>
                            <xsl:when test="$language-ident eq 'rm'">Romansh</xsl:when>
                            <xsl:when test="$language-ident eq 'rn'">Rundi</xsl:when>
                            <xsl:when test="$language-ident eq 'ro'">Romanian</xsl:when>
                            <xsl:when test="$language-ident eq 'ru'">Russian</xsl:when>
                            <xsl:when test="$language-ident eq 'rw'">Kinyarwanda</xsl:when>
                            <xsl:when test="$language-ident eq 'sa'">Sanskrit</xsl:when>
                            <xsl:when test="$language-ident eq 'sc'">Sardinian</xsl:when>
                            <xsl:when test="$language-ident eq 'sd'">Sindhi</xsl:when>
                            <xsl:when test="$language-ident eq 'se'">Northern Sami</xsl:when>
                            <xsl:when test="$language-ident eq 'sg'">Sango</xsl:when>
                            <xsl:when test="$language-ident eq 'si'">Sinhala</xsl:when>
                            <xsl:when test="$language-ident eq 'sk'">Slovak</xsl:when>
                            <xsl:when test="$language-ident eq 'sl'">Slovenian</xsl:when>
                            <xsl:when test="$language-ident eq 'sm'">Samoan</xsl:when>
                            <xsl:when test="$language-ident eq 'sn'">Shona</xsl:when>
                            <xsl:when test="$language-ident eq 'so'">Somali</xsl:when>
                            <xsl:when test="$language-ident eq 'sq'">Albanian</xsl:when>
                            <xsl:when test="$language-ident eq 'sr'">Serbian</xsl:when>
                            <xsl:when test="$language-ident eq 'ss'">Swati</xsl:when>
                            <xsl:when test="$language-ident eq 'st'">Sotho, Southern</xsl:when>
                            <xsl:when test="$language-ident eq 'su'">Sundanese</xsl:when>
                            <xsl:when test="$language-ident eq 'sv'">Swedish</xsl:when>
                            <xsl:when test="$language-ident eq 'sw'">Swahili</xsl:when>
                            <xsl:when test="$language-ident eq 'ta'">Tamil</xsl:when>
                            <xsl:when test="$language-ident eq 'te'">Telugu</xsl:when>
                            <xsl:when test="$language-ident eq 'tg'">Tajik</xsl:when>
                            <xsl:when test="$language-ident eq 'th'">Thai</xsl:when>
                            <xsl:when test="$language-ident eq 'ti'">Tigrinya</xsl:when>
                            <xsl:when test="$language-ident eq 'tk'">Turkmen</xsl:when>
                            <xsl:when test="$language-ident eq 'tl'">Tagalog</xsl:when>
                            <xsl:when test="$language-ident eq 'tn'">Tswana</xsl:when>
                            <xsl:when test="$language-ident eq 'to'">Tonga (Tonga Islands)</xsl:when>
                            <xsl:when test="$language-ident eq 'tr'">Turkish</xsl:when>
                            <xsl:when test="$language-ident eq 'ts'">Tsonga</xsl:when>
                            <xsl:when test="$language-ident eq 'tt'">Tatar</xsl:when>
                            <xsl:when test="$language-ident eq 'tw'">Twi</xsl:when>
                            <xsl:when test="$language-ident eq 'ty'">Tahitian</xsl:when>
                            <xsl:when test="$language-ident eq 'ug'">Uighur</xsl:when>
                            <xsl:when test="$language-ident eq 'uk'">Ukrainian</xsl:when>
                            <xsl:when test="$language-ident eq 'ur'">Urdu</xsl:when>
                            <xsl:when test="$language-ident eq 'uz'">Uzbek</xsl:when>
                            <xsl:when test="$language-ident eq 've'">Venda</xsl:when>
                            <xsl:when test="$language-ident eq 'vi'">Vietnamese</xsl:when>
                            <xsl:when test="$language-ident eq 'vo'">Volapük</xsl:when>
                            <xsl:when test="$language-ident eq 'wa'">Walloon</xsl:when>
                            <xsl:when test="$language-ident eq 'wo'">Wolof</xsl:when>
                            <xsl:when test="$language-ident eq 'xh'">Xhosa</xsl:when>
                            <xsl:when test="$language-ident eq 'yi'">Yiddish</xsl:when>
                            <xsl:when test="$language-ident eq 'yo'">Yoruba</xsl:when>
                            <xsl:when test="$language-ident eq 'za'">Zhuang</xsl:when>
                            <xsl:when test="$language-ident eq 'zh'">Chinese</xsl:when>
                            <xsl:when test="$language-ident eq 'zu'">Zulu</xsl:when>
                            <xsl:otherwise>English</xsl:otherwise>
                        </xsl:choose>
                    </fn:string>
                </fn:map>
            </fn:array>
        </fn:map>
    </xsl:template>

</xsl:stylesheet>
