/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2008 Jonas Wolz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package yajhfc.faxcover.fop;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.FormattingResults;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.apps.PageSequenceResults;

import yajhfc.PaperSize;
import yajhfc.Utils;
import yajhfc.file.FileConverter;
import yajhfc.file.FormattedFile.FileFormat;

/**
 * @author jonas
 *
 */
public class FOPFileConverter implements FileConverter {

    private static final Logger log = Logger.getLogger(FOPFileConverter.class.getName());

    public static final FOPFileConverter SHARED_INSTANCE = new FOPFileConverter();
    
    // configure fopFactory as desired
    private FopFactory pFopFactory;

    public FOPFileConverter() {
    }
    
    public FopFactory getFopFactory() {
        if (pFopFactory == null) {
            pFopFactory = FopFactory.newInstance();
            pFopFactory.setStrictValidation(false);
        }
        return pFopFactory;
    }
    
    /* (non-Javadoc)
     * @see yajhfc.FileConverter#convertToHylaFormat(java.io.File, java.io.OutputStream, yajhfc.PaperSize)
     */
    public void convertToHylaFormat(File inFile, OutputStream destination, PaperSize pageSize, FileFormat desiredFormat) throws ConversionException, IOException {
        String fopFormat = (desiredFormat == FileFormat.PostScript) ? MimeConstants.MIME_POSTSCRIPT : MimeConstants.MIME_PDF;
        convertFOToPDF(inFile, destination, pageSize, getFopFactory().newFOUserAgent(), fopFormat);
    }
    
    @SuppressWarnings("unchecked")
    public void convertFOToPDF(File inFile, OutputStream destination, PaperSize pageSize, FOUserAgent foUserAgent, String desiredFormat) throws ConversionException, IOException {
        OutputStream out = null;

        try {
            FopFactory fopFactory = getFopFactory();
            fopFactory.setPageHeight(String.format(Locale.US, "%dmm", pageSize.getSize().height));
            fopFactory.setPageWidth(String.format(Locale.US, "%dmm", pageSize.getSize().width));

            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new BufferedOutputStream(destination);

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(desiredFormat, foUserAgent, out);

            // Setup JAXP using identity transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            // Setup input stream
            Source src = new StreamSource(inFile);

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            if (Utils.debugMode) {
                // Result processing
                FormattingResults foResults = fop.getResults();
                java.util.List pageSequences = foResults.getPageSequences();
                for (java.util.Iterator it = pageSequences.iterator(); it.hasNext();) {
                    PageSequenceResults pageSequenceResults = (PageSequenceResults)it.next();
                    log.info("PageSequence " 
                            + (String.valueOf(pageSequenceResults.getID()).length() > 0 
                                    ? pageSequenceResults.getID() : "<no id>") 
                                    + " generated " + pageSequenceResults.getPageCount() + " pages.");
                }
                log.info("Generated " + foResults.getPageCount() + " pages in total.");
            }
        } catch (FOPException e) {
            throw new ConversionException(e);
        } catch (TransformerException e) {
            throw new ConversionException(e);
        } finally {
            if (out != null) out.close();
        }
    }
    

}
