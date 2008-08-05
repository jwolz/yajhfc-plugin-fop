/***********************************************************
 * $Id$
 * 
 * OpenDocument transformation tool of the clazzes.org project
 * http://www.clazzes.org
 *
 * Copyright (C) 2006-2007 ev-i Informationstechnologie GmbH
 *
 * Created: 2006-12-18
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 ***********************************************************/

package org.clazzes.odtransform;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * This implementation of a SAXTransformerFactory is used to translate
 * an ODT file to SAX events suitable for applying an OOo export filter.
 * The resulting documents of generated TarnsformerHandlers
 * will have office:document as its root tag.
 * 
 * @author wglas
 */
public class OdtTransformer
{
    public OdtTransformer()
    {}
    
    /**
     * This method allows overriding of the creation of the URIResolver
     * used to resolve parts of the ODT file.
     * 
     * @param odtFile  The zip file object of the given odtFile.
     * @return An URI resolver, which resolve relative URLs to entries in the
     *         zip file.
     */
    public URIResolver makeURIResolver(ZipFile odtFile)
    {
        return new ZipFileURIResolver(odtFile);
    }
    
    /**
     * Trransform an ODT file to an XML result.
     * 
     * @param odtFile The zip file object of the given odtFile.
     * @param result The result to which the resulting OOo XML document will be
     *               written.
     * @throws IOException 
     * @throws SAXException 
     */
    public void transform(ZipFile odtFile, Result result) throws TransformerConfigurationException, IOException, SAXException
    {
        TransformerFactory transformerFactory = SAXTransformerFactory.newInstance();
        
        if (!(transformerFactory instanceof SAXTransformerFactory))
            throw new TransformerConfigurationException("TransformerFactory.newInstance() did not return an instance of javax.xml.transform.sax.SAXTransformerFactory.");
        
        SAXTransformerFactory saxTransformerFactory =
            (SAXTransformerFactory) transformerFactory;
        
        // use the odt file for resolving dependant files.
        saxTransformerFactory.setURIResolver(this.makeURIResolver(odtFile));
        
        // open the odt merge stylesheet, which assembles the full office:document content.
        InputStream ooomergeIS =
            OdtTransformer.class.getClassLoader().getResourceAsStream("org/clazzes/odtransform/odtmerge.xslt");

        Source ooomergeSource =
            new StreamSource(ooomergeIS);

        // open content xml.
        InputSource odtSource =
            new InputSource(odtFile.getInputStream(new ZipEntry("content.xml")));
        
        TransformerHandler ooomergeHandler =
            saxTransformerFactory.newTransformerHandler(ooomergeSource);
 
        ooomergeHandler.setResult(result);
        
        // set up the content reader.
        XMLReader odtReader = XMLReaderFactory.createXMLReader();
 
        odtReader.setContentHandler(ooomergeHandler);
        
        odtReader.parse(odtSource);
     }
}
