<?xml version="1.0" encoding="UTF-8"?>
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

<xsl:template match="/office:document">
<office:document>
  <xsl:copy-of select="/office:document/office:meta" />
  <xsl:copy-of select="/office:document/office:settings" />
  <xsl:copy-of select="/office:document/office:script" />
  <xsl:copy-of select="/office:document/office:font-face-decls" />
  <xsl:copy-of select="/office:document/office:styles" />

  <!-- Assemble automatic-styles by sorting w.r.t. the style name -->
  <office:automatic-styles>
   <xsl:for-each select="/office:document/office:automatic-styles/*">
    <xsl:sort select="@style:name"/>
    <xsl:copy-of select="."/>
   </xsl:for-each>
  </office:automatic-styles>
  
  <xsl:copy-of select="/office:document/office:master-styles" />
  <xsl:copy-of select="/office:document/office:body" />
</office:document>
</xsl:template>

</xsl:stylesheet>