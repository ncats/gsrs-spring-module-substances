<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                xmlns:local="urn:local"
                exclude-result-prefixes="fn xs local">

    <xsl:output method="json" encoding="UTF-8" indent="yes"/>

    <!--
      Reverse the transformations applied by export-gsrsp.xsl:
        * re-generate a UUID for every entry in the root "references" array;
        * replace numeric reference indices found in child "references" arrays
          with the corresponding generated UUID.
      The generated UUIDs are deterministic (same index = same UUID) and valid
      UUIDv4 strings, so references inside a single imported document stay consistent.
    -->

    <xsl:param name="raw-input" as="xs:string" required="yes"/>

    <xsl:variable name="json-doc" select="fn:json-to-xml($raw-input)"/>
    <xsl:variable name="root" select="$json-doc/fn:map"/>

    <xsl:function name="local:int-to-hex" as="xs:string">
        <xsl:param name="n" as="xs:integer"/>
        <xsl:variable name="digits" select="'0123456789ABCDEF'"/>
        <xsl:choose>
            <xsl:when test="$n lt 16">
                <xsl:value-of select="substring($digits, $n + 1, 1)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat(local:int-to-hex($n idiv 16), substring($digits, ($n mod 16) + 1, 1))"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>

    <xsl:function name="local:index-to-uuid" as="xs:string">
        <xsl:param name="index" as="xs:integer"/>
        <xsl:variable name="hex" select="local:int-to-hex($index)"/>
        <xsl:variable name="padded" select="concat(substring('000000000000', 1, 12 - string-length($hex)), $hex)"/>
        <xsl:value-of select="concat('00000000-0000-4000-8000-', $padded)"/>
    </xsl:function>

    <xsl:template match="/">
        <xsl:variable name="restored">
            <xsl:apply-templates select="$root" mode="restore"/>
        </xsl:variable>
        <xsl:value-of select="fn:xml-to-json($restored/fn:map) =&gt; fn:parse-json() =&gt; fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <!-- Identity transform -->
    <xsl:template match="*" mode="restore">
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="restore"/>
            <xsl:apply-templates select="node()" mode="restore"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*" mode="restore">
        <xsl:copy/>
    </xsl:template>

    <!-- Add a generated uuid back to every root reference entry. -->
    <xsl:template match="fn:array[@key='references']/fn:map" mode="restore">
        <fn:map>
            <fn:string key="uuid">
                <xsl:value-of select="local:index-to-uuid(position() - 1)"/>
            </fn:string>
            <xsl:apply-templates select="@*" mode="restore"/>
            <xsl:apply-templates select="node()" mode="restore"/>
        </fn:map>
    </xsl:template>

    <!-- Replace numeric reference indices with generated UUID strings. -->
    <xsl:template match="fn:array[@key='references'][fn:number]" mode="restore">
        <fn:array key="references">
            <xsl:for-each select="fn:number">
                <fn:string>
                    <xsl:value-of select="local:index-to-uuid(xs:integer(.))"/>
                </fn:string>
            </xsl:for-each>
        </fn:array>
    </xsl:template>

</xsl:stylesheet>
