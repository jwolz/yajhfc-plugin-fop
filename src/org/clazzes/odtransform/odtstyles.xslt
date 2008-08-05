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
<xsl:stylesheet version="2.0"
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

<xsl:variable name="automatic_styles" select="/office:document/office:automatic-styles/*"/>

<xsl:template match="/office:document">
<office:document-styles>
  <xsl:copy-of select="/office:document/office:font-face-decls" />
  <xsl:copy-of select="/office:document/office:styles" />
  
  <!-- Assemble automatic-styles from the used styles in master-styles -->
  <office:automatic-styles>
    <xsl:variable name="used_styles">
     <xsl:element name="styles">
      <xsl:for-each select="/office:document/office:master-styles//*">
       <xsl:for-each select="@*">
        <xsl:if test="local-name()='style-name' or local-name()='page-layout-name'">
         <xsl:attribute name="{string()}"><xsl:value-of select="string()"/></xsl:attribute>
        </xsl:if>
       </xsl:for-each> 
      </xsl:for-each>
     </xsl:element>
    </xsl:variable>

    <xsl:for-each select="exslt:node-set($used_styles)//*">
     <xsl:for-each select="@*">
      <xsl:variable name="style" select="string()"/>
      <xsl:copy-of select="$automatic_styles[@style:name=$style]"/>
     </xsl:for-each>
    </xsl:for-each>

  </office:automatic-styles>
  
  <xsl:copy-of select="/office:document/office:master-styles" />
</office:document-styles>
</xsl:template>

</xsl:stylesheet>