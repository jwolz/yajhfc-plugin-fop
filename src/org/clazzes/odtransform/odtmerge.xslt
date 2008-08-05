<?xml version="1.0" encoding="UTF-8"?>
<!-- 
  $Id$
  
  OpenDocument transformation tool of the clazzes.org project
  http://www.clazzes.org
  
  Copyright (C) 2006-2007 ev-i Informationstechnologie GmbH
  
  Created: 2006-12-18
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
      http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  -->
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:fn="http://www.w3.org/2005/xpath-functions"
xmlns:exslt="http://exslt.org/common"
xmlns:config="urn:oasis:names:tc:opendocument:xmlns:config:1.0"
xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
xmlns:style="urn:oasis:names:tc:opendocument:xmlns:style:1.0"
xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
xmlns:table="urn:oasis:names:tc:opendocument:xmlns:table:1.0"
xmlns:draw="urn:oasis:names:tc:opendocument:xmlns:drawing:1.0"
xmlns:fo="urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0"
xmlns:xlink="http://www.w3.org/1999/xlink"
xmlns:dc="http://purl.org/dc/elements/1.1/"
xmlns:meta="urn:oasis:names:tc:opendocument:xmlns:meta:1.0"
xmlns:number="urn:oasis:names:tc:opendocument:xmlns:datastyle:1.0"
xmlns:svg="urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0"
xmlns:chart="urn:oasis:names:tc:opendocument:xmlns:chart:1.0"
xmlns:dr3d="urn:oasis:names:tc:opendocument:xmlns:dr3d:1.0"
xmlns:math="http://www.w3.org/1998/Math/MathML"
xmlns:form="urn:oasis:names:tc:opendocument:xmlns:form:1.0"
xmlns:script="urn:oasis:names:tc:opendocument:xmlns:script:1.0"
xmlns:ooo="http://openoffice.org/2004/office"
xmlns:ooow="http://openoffice.org/2004/writer"
xmlns:oooc="http://openoffice.org/2004/calc"
xmlns:dom="http://www.w3.org/2001/xml-events"
xmlns:xforms="http://www.w3.org/2002/xforms"
xmlns:xsd="http://www.w3.org/2001/XMLSchema"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
>

<!-- sub-xmls from the zipped .odt file, which will be imported. -->
<xsl:variable name="styles_xml" select="document('styles.xml')" />
<xsl:variable name="meta_xml" select="document('meta.xml')" />
<xsl:variable name="settings_xml" select="document('settings.xml')" />

<!-- node sets which are going to be searched from wihtin another sub-xmls' context. -->
<xsl:variable name="doc_font_nodes" select="/office:document-content/office:font-face-decls/*" />
<xsl:variable name="doc_style_nodes" select="/office:document-content/office:automatic-styles/*" />

<!-- This template replaces refs to automatic styles in the master-styles. -->
<xsl:template name="replace_style_style_refs">
 <xsl:for-each select="node()">
  <xsl:copy>
   <xsl:for-each select="@*">
    <xsl:variable name="value" select="string()"/>
    <xsl:choose>
     <xsl:when test="local-name()='style-name' and boolean($doc_style_nodes[@style:name=$value])">
      <xsl:attribute name="{name()}"><xsl:value-of select="concat('__styles__',string())"/></xsl:attribute>
     </xsl:when>
     <xsl:otherwise>
      <xsl:copy-of select ="."/>
     </xsl:otherwise>
    </xsl:choose>
   </xsl:for-each>
   <xsl:call-template name="replace_style_style_refs"/>
  </xsl:copy>
 </xsl:for-each>
</xsl:template>

<xsl:template match="/office:document-content">
<office:document>
  <xsl:copy-of select="$meta_xml/office:document-meta/office:meta" />
  <xsl:copy-of select="$settings_xml/office:document-settings/office:settings" />

  <xsl:copy-of select="/office:document-content/office:script" />

  <!-- Assemble font-face-decls from document and fill in missing fonts from styles.xml -->
  <office:font-face-decls>
   <xsl:copy-of select="$doc_font_nodes"/>
    
   <xsl:for-each select="$styles_xml/office:document-styles/office:font-face-decls/*">
    <xsl:variable name="font_name" select="@style:name"/>
    <xsl:if test="not($doc_font_nodes[@style:name=$font_name])">
     <xsl:copy-of select="." />
    </xsl:if>
   </xsl:for-each>
  </office:font-face-decls>
   
  <xsl:copy-of select="$styles_xml/office:document-styles/office:styles" />

  <!-- Assemble automatic-styles from document and fill in missing fonts from styles.xml -->
  <office:automatic-styles>
   <xsl:copy-of select="$doc_style_nodes"/>
  
   <xsl:for-each select="$styles_xml/office:document-styles/office:automatic-styles/*">
    <xsl:variable name="style_name" select="@style:name"/>
    <xsl:choose>
     <xsl:when test="$doc_style_nodes[@style:name=$style_name]">
      <xsl:copy>
       <xsl:for-each select="@*">
        <xsl:choose>
         <xsl:when test="name() != 'style:name'">
          <xsl:copy-of select="."/>  
         </xsl:when>
         <xsl:otherwise>
          <xsl:attribute name="style:name"><xsl:value-of select="concat('__styles__',$style_name)"/></xsl:attribute>
         </xsl:otherwise>
        </xsl:choose>
       </xsl:for-each>
       <xsl:copy-of select="node()"/>
      </xsl:copy>
     </xsl:when>
     <xsl:otherwise>
      <xsl:copy-of select="." />
     </xsl:otherwise>
    </xsl:choose>
   </xsl:for-each>
  </office:automatic-styles>
 
 
  <office:master-styles>
   <xsl:for-each select="$styles_xml/office:document-styles/office:master-styles">
    <xsl:call-template name="replace_style_style_refs"/>
   </xsl:for-each>
  </office:master-styles>
 
  <xsl:copy-of select="/office:document-content/office:body" />
</office:document>
</xsl:template>

</xsl:stylesheet>