<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:d="urn:ietf:params:xml:ns:netconf:base:1.0"
xmlns:n="urn:ietf:params:xml:ns:netmod:notification"
xmlns:ood="http://org/openroadm/device"
xmlns:ocp="http://openconfig.net/yang/platform" >

  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="d:data/ocp:components">
    <xsl:element name="config" namespace="urn:ietf:params:xml:ns:netconf:base:1.0">
    <xsl:element name="components" namespace="http://openconfig.net/yang/platform">
      <xsl:for-each select="*">
        <xsl:apply-templates select="." />
      </xsl:for-each>
    </xsl:element>
    </xsl:element>
  </xsl:template>

  <xsl:template match="*">
    <xsl:if test="name(.) != 'state'">
    <xsl:copy>
      <xsl:for-each select="@*"><xsl:copy-of select="."></xsl:copy-of></xsl:for-each>
        <xsl:choose>
          <xsl:when test="name(.) = 'state'"></xsl:when>
          <xsl:otherwise><xsl:apply-templates select="node()" /></xsl:otherwise>
        </xsl:choose>
    </xsl:copy>
    </xsl:if>
  </xsl:template>

  <xsl:template match="d:data | n:netconf">
    <xsl:apply-templates />
  </xsl:template>

  </xsl:stylesheet>
