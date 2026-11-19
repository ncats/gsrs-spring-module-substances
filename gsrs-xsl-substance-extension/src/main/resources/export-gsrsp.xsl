<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="fn xs">

    <xsl:output method="json" encoding="UTF-8"/>
    <xsl:param name="json-input" as="xs:string" required="yes"/>

    <xsl:template match="/">
        <xsl:variable name="json-doc" select="fn:json-to-xml($json-input)"/>
        <xsl:variable name="root-refs" select="$json-doc/fn:map/fn:array[@key='references']/fn:map"/>

        <xsl:variable name="cleaned">
            <xsl:apply-templates select="$json-doc/fn:map" mode="clean">
                <xsl:with-param name="root-refs" select="$root-refs" tunnel="yes"/>
            </xsl:apply-templates>
        </xsl:variable>

        <xsl:value-of select="fn:xml-to-json($cleaned) => fn:parse-json() => fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
    </xsl:template>

    <xsl:template match="*" mode="clean">
        <xsl:param name="root-refs" tunnel="yes"/>
        <xsl:copy>
            <xsl:apply-templates select="@*" mode="clean"/>
            <xsl:apply-templates select="node()" mode="clean"/>
        </xsl:copy>
    </xsl:template>

    <xsl:template match="@*" mode="clean">
        <xsl:copy/>
    </xsl:template>

    <xsl:template match="*[@key=('id','uuid','created','createdBy','lastEdited','lastEditedBy','approved','approvedBy','_self','refuuid','originatorUuid')]" mode="clean"/>

    <xsl:template match="fn:array[@key='references'][fn:string]" mode="clean">
        <xsl:param name="root-refs" tunnel="yes"/>
        <fn:array key="references">
            <xsl:for-each select="fn:string">
                <xsl:variable name="ref-uuid" select="text()"/>
                <xsl:variable name="index" select="index-of($root-refs/fn:string[@key='uuid']/text(), $ref-uuid)"/>
                <fn:number>
                    <xsl:value-of select="if (exists($index)) then ($index - 1) else ()"/>
                </fn:number>
            </xsl:for-each>
        </fn:array>
    </xsl:template>
</xsl:stylesheet>
