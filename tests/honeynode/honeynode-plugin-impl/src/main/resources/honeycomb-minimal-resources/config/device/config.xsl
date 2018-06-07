<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>
    <xsl:template match="/">
        <config xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
            <xsl:apply-templates/>
        </config>
    </xsl:template>
</xsl:stylesheet>