<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:template match="*/*/*" name="nodes">
        <xsl:element name="{local-name()}">
            <xsl:apply-templates select="@*|node()" />
        </xsl:element>
    </xsl:template>
    <xsl:template match="*/*/*/*">
        <xsl:if test="not(name(.)='type' or name(.)='lldp' or name(.)='ethernet' or name(.)='ots')">
            <xsl:element name="{local-name()}">
                <xsl:apply-templates select="@*|node()" />
            </xsl:element>
        </xsl:if>
        <xsl:if test="name(.)='type'" xmlns="http://org/openroadm/device">
            <type xmlns:openROADM-if="http://org/openroadm/interfaces">
                <xsl:apply-templates select="@*|node()"/>
            </type>
        </xsl:if>
        <xsl:if test="name(.)='ots'">
            <ots xmlns="http://org/openroadm/optical-transport-interfaces">
                <xsl:apply-templates select="@*|node()"/>
            </ots>
        </xsl:if>
        <xsl:if test="name(.)='lldp'">
            <lldp xmlns="http://org/openroadm/lldp">
                <xsl:apply-templates select="@*|node()"/>
            </lldp>
        </xsl:if>
        <xsl:if test="name(.)='ethernet'">
            <ethernet xmlns="http://org/openroadm/ethernet-interfaces">
                <xsl:apply-templates select="@*|node()"/>
            </ethernet>
        </xsl:if>
    </xsl:template>
    <xsl:template match="/">
        <org-openroadm-device xmlns="http://org/openroadm/device">
            <xsl:apply-templates select="*"/>
        </org-openroadm-device>
    </xsl:template>
</xsl:stylesheet>