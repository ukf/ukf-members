<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ukfxMembers="xalan://uk.org.ukfederation.members.Members"
    xmlns:shibmd="urn:mace:shibboleth:metadata:1.0"
    extension-element-prefixes="ukfxMembers"
    exclude-result-prefixes="ukfxMembers shibmd"
    version="1.0">
    
    <!--
        Pick up the members.xml document, and create a Members class instance.
    -->
    <xsl:variable name="memberDocument" select="document('src/test/resources/pushedScopes.xml')"/>
    <xsl:variable name="members" select="ukfxMembers:new($memberDocument)"/>

    <xsl:template match="values">
        <results>
            <xsl:apply-templates/>
        </results>
    </xsl:template>
    
    <xsl:template match="value">
        <result>
            <xsl:attribute name="id"><xsl:value-of select="."/></xsl:attribute>
            <xsl:text>&#10;</xsl:text>
            <xsl:apply-templates select="ukfxMembers:scopesForEntity($members, .)"/>
            <xsl:text>    </xsl:text>
        </result>
    </xsl:template>

    <xsl:template match="shibmd:Scope">
        <xsl:text>        </xsl:text>
        <Scope>
            <xsl:apply-templates select="@regex"/>
            <xsl:value-of select="."/>
        </Scope>
        <xsl:text>&#10;</xsl:text>
    </xsl:template>
    
    <xsl:template match="@regex">
        <xsl:copy/>
    </xsl:template>
</xsl:stylesheet>