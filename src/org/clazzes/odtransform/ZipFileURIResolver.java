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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An implementation of URIResolver, which resolves zip entries with in a supplied ZipFile.
 * 
 * @author wglas
 */
public class ZipFileURIResolver implements URIResolver
{
    private static Log log = LogFactory.getLog(ZipFileURIResolver.class);
    
    private ZipFile zipFile;
    
    /**
     * @param zipFile
     */
    public ZipFileURIResolver(ZipFile zipFile)
    {
        this.zipFile = zipFile;
    }
    
    /* (non-Javadoc)
     * @see javax.xml.transform.URIResolver#resolve(java.lang.String, java.lang.String)
     */
    public Source resolve(String href, String base) throws TransformerException
    {
        try
        {
            URI uri = new URI(href);
            
            if (uri.isAbsolute()) return null;
            
            log.debug("Resolving relative URI ["+href+"].");
            
            return 
                new StreamSource(this.zipFile.getInputStream(new ZipEntry(href)));
            
        } catch (URISyntaxException e)
        {
            throw new TransformerException("Invalid URI",e);
        } catch (IOException e)
        {
            throw new TransformerException("Unexpected I/O error",e);
        }
    }
}
