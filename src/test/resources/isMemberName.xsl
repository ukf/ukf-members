<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:ukfxMembers="xalan://uk.org.ukfederation.members.Members"
    version="1.0">
    
    <!--
        Pick up the members.xml document, and create a Members class instance.
    -->
    <xsl:variable name="memberDocument" select="document('src/test/resources/oneOfEach.xml')"/>
    <xsl:variable name="members" select="ukfxMembers:new($memberDocument)"/>

    <xsl:template match="values">
        <results>
            <xsl:apply-templates/>
        </results>
    </xsl:template>
    
    <xsl:template match="value">
        <result><xsl:value-of select="ukfxMembers:isOwnerName($members, .)"/></result>
    </xsl:template>

</xsl:stylesheet>