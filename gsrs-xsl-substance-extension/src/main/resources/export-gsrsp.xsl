<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:fn="http://www.w3.org/2005/xpath-functions"
                exclude-result-prefixes="fn xs">

    <xsl:output method="json" encoding="UTF-8"/>
    <xsl:param name="input" as="xs:string" required="yes"/>

    <!-- The code system the provenance entry cites. ``FDA UNII`` is what GSRS
         records by default; a caller working with another registry sets the
         parameter rather than editing the stylesheet. -->
    <xsl:param name="code-system" as="xs:string" select="'FDA UNII'"/>

    <xsl:template match="/">
        <xsl:variable name="json-doc" select="fn:json-to-xml($input)"/>
        <xsl:variable name="root-refs" select="$json-doc/fn:map/fn:array[@key='references']/fn:map"/>

        <!-- The substance's own link, read before the clean pass drops ``_self``
             from every map it copies. -->
        <xsl:variable name="self-link" as="xs:string"
                      select="string($json-doc/fn:map/fn:string[@key='_self'])"/>

        <!-- The moment of the export, in the epoch milliseconds GSRS writes its
             own timestamps in. Counting from the Unix epoch rather than
             formatting a date keeps the reference entry in the same vocabulary
             as the rest of a GSRS document. -->
        <xsl:variable name="export-epoch-ms" as="xs:integer"
                      select="xs:integer(
                          (fn:current-dateTime() - xs:dateTime('1970-01-01T00:00:00Z'))
                          div xs:dayTimeDuration('PT0.001S'))"/>

        <xsl:variable name="cleaned">
            <xsl:apply-templates select="$json-doc/fn:map" mode="clean">
                <xsl:with-param name="root-refs" select="$root-refs" tunnel="yes"/>
            </xsl:apply-templates>
        </xsl:variable>

        <!-- The provenance entry a portable export adds to the substance's own
             reference list: it says which code system the document is written
             for and where the substance was read from. It is written here
             rather than through the ``clean`` templates because those strip
             ``_self`` from every map they copy, and this entry is the one place
             where a link still belongs.

             ``url`` is written only when the substance carries a link: an empty
             ``url`` would send a reader nowhere, and GSRS treats the key as
             optional. No ``uuid`` is written either, because the export drops
             the uuids it cannot carry and the import derives new ones. No
             ``id`` is written because the entry identifies a code system rather
             than a record in one, and a citation is where GSRS keeps that name.

             ``deprecated`` and ``publicDomain`` are false because the entry
             describes the export itself, not a citation a curator retired or
             released; ``tags`` and ``access`` are empty for the same reason.
             ``documentDate`` is the instant the export ran, in the epoch
             milliseconds GSRS stores, so a reader can tell how old the copy
             is. -->
        <xsl:variable name="export-reference" as="element()">
            <fn:map>
                <fn:string key="docType">GSRS Portable</fn:string>
                <fn:string key="citation">
                    <xsl:value-of select="$code-system"/>
                </fn:string>
                <xsl:if test="$self-link">
                    <fn:string key="url">
                        <xsl:value-of select="$self-link"/>
                    </fn:string>
                </xsl:if>
                <fn:boolean key="deprecated">false</fn:boolean>
                <fn:boolean key="publicDomain">false</fn:boolean>
                <fn:array key="tags"/>
                <fn:array key="access"/>
                <fn:number key="documentDate">
                    <xsl:value-of select="$export-epoch-ms"/>
                </fn:number>
            </fn:map>
        </xsl:variable>

        <!-- Append the provenance entry to the substance's reference list and
             leave every other key where it was. A substance that cites nothing
             still gets the list, because it is the only place the entry can
             live; appending leaves the indices the child ``references`` arrays
             already hold untouched. -->
        <xsl:variable name="exported">
            <fn:map>
                <xsl:copy-of select="$cleaned/fn:map/@*"/>
                <xsl:for-each select="$cleaned/fn:map/node()">
                    <xsl:choose>
                        <xsl:when test="self::fn:array[@key='references']">
                            <fn:array key="references">
                                <xsl:copy-of select="node()"/>
                                <xsl:copy-of select="$export-reference"/>
                            </fn:array>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:copy-of select="."/>
                        </xsl:otherwise>
                    </xsl:choose>
                </xsl:for-each>
                <xsl:if test="not($cleaned/fn:map/fn:array[@key='references'])">
                    <fn:array key="references">
                        <xsl:copy-of select="$export-reference"/>
                    </fn:array>
                </xsl:if>
            </fn:map>
        </xsl:variable>

        <xsl:value-of select="fn:xml-to-json($exported) =&gt; fn:parse-json() =&gt; fn:serialize(map {'method':'json', 'use-character-maps': map{'/':'/'}})"/>
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

    <!-- Row keys and audit stamps that mean nothing outside the database the
         substance was read from: a uuid names one row, ``_self`` one url, and
         the ``createdBy``/``lastEditedBy``/``approvedBy`` names one user
         profile. ``created`` and ``lastEdited`` are kept: they are epoch
         milliseconds, which a reader of the portable document can place on a
         timeline without knowing the source deployment. -->
    <xsl:template match="*[@key=('id','uuid','createdBy','lastEditedBy','approved','approvedBy','_self','refuuid','originatorUuid')]" mode="clean"/>

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
