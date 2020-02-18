<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:d="urn:ietf:params:xml:ns:netconf:base:1.0"
xmlns:oor="http://org/openroadm/device"
exclude-result-prefixes="oor d">

  <xsl:output method="xml" encoding="UTF-8" indent="yes"/>

  <xsl:template match="/">
    <xsl:element name="config" namespace="urn:ietf:params:xml:ns:netconf:base:1.0">
    <xsl:element name="org-openroadm-device" namespace="http://org/openroadm/device">
    <xsl:element name="info" namespace="http://org/openroadm/device">
      <xsl:apply-templates select="d:data/oor:org-openroadm-device/oor:info"/>
    </xsl:element>

    <xsl:copy-of select="d:data/oor:org-openroadm-device/oor:users" />

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:shelves">
      <xsl:element name="shelves" namespace="http://org/openroadm/device">
        <xsl:call-template name="shelve-body"/>
      </xsl:element>
    </xsl:for-each>

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:circuit-packs">
      <xsl:element name="circuit-packs" namespace="http://org/openroadm/device">
        <xsl:call-template name="cp-body"/>
      </xsl:element>
    </xsl:for-each>

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:interface">
        <xsl:element name="interface" namespace="http://org/openroadm/device">
            <xsl:call-template name="inter-body"/>
        </xsl:element>
    </xsl:for-each>

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:protocols">
        <xsl:element name="protocols" namespace="http://org/openroadm/device">
            <xsl:call-template name="proto-body"/>
        </xsl:element>
    </xsl:for-each>

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:degree">
      <xsl:element name="degree" namespace="http://org/openroadm/device">
        <xsl:call-template name="degree-body"/>
      </xsl:element>
    </xsl:for-each>

    <xsl:for-each select="d:data/oor:org-openroadm-device/oor:shared-risk-group">
      <xsl:element name="shared-risk-group" namespace="http://org/openroadm/device">
        <xsl:call-template name="srg-body"/>
      </xsl:element>
    </xsl:for-each>

    </xsl:element>
    </xsl:element>
  </xsl:template>

  <xsl:template match="oor:info">
    <xsl:for-each select="./*">
      <xsl:if test="name(.) = 'node-id' or name(.) = 'node-number' or name(.) = 'node-type' or name(.) = 'clli' or name(.) = 'ipAddress' or name(.) = 'prefix-length' or name(.) = 'defaultGateway' or name(.) = 'template' or name(.) = 'geoLocation'">
        <xsl:copy-of select="." />
      </xsl:if>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="shelve-body">
    <xsl:for-each select="./*">
      <xsl:if test="name(.) ='shelf-name' or name(.) = 'shelf-type' or name(.) = 'rack' or name(.) = 'shelf-position' or name(.) = 'administrative-state' or name(.) = 'equipment-state' or name(.) = 'due-date'">
        <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="cp-body">
    <xsl:for-each select="./*">
      <xsl:if test="name(.) = 'circuit-pack-type' or name(.) = 'circuit-pack-product-code' or name(.) = 'circuit-pack-name' or name(.) = 'administrative-state' or name(.) = 'equipment-state' or name(.) = 'circuit-pack-mode' or name(.) = 'shelf' or name(.) = 'slot' or name(.) = 'subSlot' or name(.) = 'due-date' or name(.) = 'parent-circuit-pack'">
        <xsl:copy-of select="." />
      </xsl:if>
      <xsl:if test="name(.) = 'ports'">
        <xsl:for-each select=".">
          <xsl:element name="ports" namespace="http://org/openroadm/device">
            <xsl:call-template name="ports-body"/>
          </xsl:element>
        </xsl:for-each>
      </xsl:if>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="inter-body">
    <xsl:for-each select="./*">
        <xsl:if test="not(name(.) = 'operational-state' or name(.) = 'ethernet' or name(.) = 'och' or name(.) = 'otu'  or name(.) = 'odu')">
          <xsl:copy-of select="." />
        </xsl:if>
        <xsl:if test="name(.)='ethernet'">
          <xsl:element name="ethernet" xmlns="http://org/openroadm/ethernet-interfaces">
            <xsl:call-template name="eth-body"/>
          </xsl:element>
        </xsl:if>
        <xsl:if test="name(.)='och'">
          <xsl:element name="och" xmlns="http://org/openroadm/optical-channel-interfaces">
            <xsl:call-template name="och-body"/>
          </xsl:element>
        </xsl:if>
        <xsl:if test="name(.)='otu'">
          <xsl:element name="otu" xmlns="http://org/openroadm/otn-otu-interfaces">
            <xsl:call-template name="otu-body"/>
          </xsl:element>
        </xsl:if>
        <xsl:if test="name(.)='odu'">
          <xsl:element name="odu" xmlns="http://org/openroadm/otn-odu-interfaces">
            <xsl:call-template name="odu-body"/>
          </xsl:element>
        </xsl:if>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="eth-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.) = 'curr-speed' or name(.)='curr-duplex') ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="och-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.) = 'width') ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="otu-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.)='accepted-sapi' or name(.) = 'accepted-dapi' or name(.)='accepted-operator' or name(.)='tim-act-enabled') ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="odu-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.)='no-oam-function' or name(.)='accepted-sapi' or name(.) = 'accepted-dapi' or name(.)='accepted-operator' or name(.)='tim-act-enabled' or name(.)='opu') ">
         <xsl:copy-of select="." />
      </xsl:if>
      <xsl:if test="name(.)='opu'">
        <xsl:element name="opu" xmlns="http://org/openroadm/otn-odu-interfaces">
          <xsl:call-template name="opu-body"/>
        </xsl:element>
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

   <xsl:template name="opu-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.)='rx-payload-type' or name(.) = 'msi') ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="proto-body">
    <xsl:for-each select="./*">
      <xsl:if test="name(.) = 'lldp'">
        <xsl:for-each select=".">
          <xsl:element name="lldp" xmlns="http://org/openroadm/lldp">
            <xsl:call-template name="lldp-body"/>
          </xsl:element>
        </xsl:for-each>
      </xsl:if>
     </xsl:for-each>
  </xsl:template>

  <xsl:template name="ports-body">
    <xsl:for-each select="./*">
      <xsl:if test="name(.) = 'port-name' or name(.) = 'port-type' or name(.) = 'port-qual' or name(.) = 'circuit-id' or name(.) = 'administrative-state' or name(.) = 'logical-connection-point' or name(.) = 'roadm-port' or name(.) = 'otdr-port' or name(.) = 'ila-port' ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="lldp-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.) = 'nbr-list') ">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="degree-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.) = 'max-wavelengths')">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

  <xsl:template name="srg-body">
    <xsl:for-each select="./*">
      <xsl:if test="not(name(.) = 'max-add-drop-ports' or name(.) = 'wavelength-duplication' or name(.) = 'current-provisioned-add-drop-ports')">
         <xsl:copy-of select="." />
      </xsl:if>
    </xsl:for-each>
  </xsl:template>

 </xsl:stylesheet>
