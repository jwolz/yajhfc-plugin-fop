package yajhfc.faxcover.fop;
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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.Date;
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

import yajhfc.utils;
import yajhfc.faxcover.Faxcover;

public class FOPFaxcover extends Faxcover {

    private static final Logger log = Logger.getLogger(FOPFaxcover.class.getName());

    public FOPFaxcover(URL coverTemplate) {
        super(coverTemplate); 
        fopFactory.setStrictValidation(false);
    }

    // configure fopFactory as desired
    private FopFactory fopFactory = FopFactory.newInstance();


    /**
     * Converts an FO file to a PDF file using FOP
     * @param fo the FO file
     * @param pdf the target PDF file
     * @throws IOException In case of an I/O problem
     * @throws FOPException In case of a FOP problem
     * @throws TransformerException 
     */
    @SuppressWarnings("unchecked")
    public void convertFO2PDF(File fo, OutputStream pdf) throws IOException, FOPException, TransformerException {

        OutputStream out = null;

        try {
            fopFactory.setPageHeight(String.format(Locale.US, "%.2fmm", pageSize.size.height));
            fopFactory.setPageWidth(String.format(Locale.US, "%.2fmm", pageSize.size.width));

            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            configureFO2PDFUserAgent(foUserAgent);
            
            // Setup output stream.  Note: Using BufferedOutputStream
            // for performance reasons (helpful with FileOutputStreams).
            out = new BufferedOutputStream(pdf);

            // Construct fop with desired output format
            Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent, out);

            // Setup JAXP using identity transformer
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(); // identity transformer
            
            // Setup input stream
            Source src = new StreamSource(fo);

            // Resulting SAX events (the generated FO) must be piped through to FOP
            Result res = new SAXResult(fop.getDefaultHandler());

            // Start XSLT transformation and FOP processing
            transformer.transform(src, res);

            if (utils.debugMode) {
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
        } finally {
            if (out != null) out.close();
        }
    }
    
    /**
     * Allows configuration of the transformer used in convertFO2PDF in sub classes.
     * @param trans
     */
    protected void configureFO2PDFUserAgent(FOUserAgent userAgent) throws IOException {
        // Do nothing here
    }

    // Tag names. MUST be lower case to allow case insensitive comparison
    private static final String NAME_TAG =            "name";
    private static final String LOCATION_TAG =        "location";
    private static final String COMPANY_TAG =         "company";
    private static final String FAXNUMBER_TAG =       "faxnumber";
    private static final String VOICENUMBER_TAG =     "voicenumber";
    private static final String FROMNAME_TAG =        "fromname";
    private static final String FROMLOCATION_TAG =    "fromlocation";
    private static final String FROMCOMPANY_TAG =     "fromcompany";
    private static final String FROMFAXNUMBER_TAG =   "fromfaxnumber";
    private static final String FROMVOICENUMBER_TAG = "fromvoicenumber";
    private static final String FROMEMAIL_TAG =       "fromemail";
    private static final String SUBJECT_TAG =         "subject";
    private static final String COMMENT_TAG =         "comments";
    private static final String DATE_TAG =            "date";
    private static final String NUMPAGES_TAG =        "pagecount";
    private static final int MAXTAGLENGTH = 16;
    private static final char TAGCHAR = '@';


    protected void createCoverSheet(InputStream in, OutputStream out) throws IOException {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("cover", ".fo");
            Reader inReader = new InputStreamReader(in, "UTF-8");
            Writer outWriter = new OutputStreamWriter(new FileOutputStream(tempFile), "UTF-8");
            replaceTags(inReader, outWriter);
            inReader.close();
            outWriter.close();
            try {
                convertFO2PDF(tempFile, out);
            } catch (FOPException e) {
                throw new RuntimeException(e);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        } finally {
            if (tempFile != null) tempFile.delete();
        }
    }

    protected void replaceTags(Reader in, Writer out) throws IOException {
        final char[] buf = new char[8000];
        int readLen;
        int readOffset = 0;
        int numRead;
        out = new BufferedWriter(out);
        do {
            readLen = buf.length - readOffset;
            numRead = in.read(buf, readOffset, readLen);

            int lastTag = -1;
            int writePointer = 0;
            int loopEnd = numRead + readOffset;
            for (int i = 0; i < loopEnd; i++) {
                if (buf[i] == TAGCHAR && (i+1) < loopEnd && buf[i+1] == TAGCHAR) {
                    // Found start/end of a tag
                    if (lastTag != -1 && i - lastTag < MAXTAGLENGTH) {
                        String replacement = null;
                        if (matchesTag(buf, lastTag, NAME_TAG)) {
                            //tagLen = NAME_TAG.length();
                            replacement = this.toName;
                        } else if (matchesTag(buf, lastTag, LOCATION_TAG)) {
                            //tagLen = LOCATION_TAG.length();
                            replacement = this.toLocation;
                        } else if (matchesTag(buf, lastTag, COMPANY_TAG)) {
                            //tagLen = COMPANY_TAG.length();
                            replacement = this.toCompany;
                        } else if (matchesTag(buf, lastTag, FAXNUMBER_TAG)) {
                            //tagLen = FAXNUMBER_TAG.length();
                            replacement = this.toFaxNumber;
                        } else if (matchesTag(buf, lastTag, VOICENUMBER_TAG)) {
                            //tagLen = VOICENUMBER_TAG.length();
                            replacement = this.toVoiceNumber;
                        } else if (matchesTag(buf, lastTag, FROMNAME_TAG)) {
                            //tagLen = FROMNAME_TAG.length();
                            replacement = this.sender;
                        } else if (matchesTag(buf, lastTag, FROMCOMPANY_TAG)) {
                            //tagLen = FROMCOMPANY_TAG.length();
                            replacement = this.fromCompany;
                        } else if (matchesTag(buf, lastTag, FROMLOCATION_TAG)) {
                            //tagLen = FROMLOCATION_TAG.length();
                            replacement = this.fromLocation;
                        } else if (matchesTag(buf, lastTag, FROMFAXNUMBER_TAG)) {
                            //tagLen = FROMFAXNUMBER_TAG.length();
                            replacement = this.fromFaxNumber;
                        } else if (matchesTag(buf, lastTag, FROMVOICENUMBER_TAG)) {
                            //tagLen = FROMVOICENUMBER_TAG.length();
                            replacement = this.fromVoiceNumber;
                        } else if (matchesTag(buf, lastTag, FROMEMAIL_TAG)) {
                            //tagLen = FROMEMAIL_TAG.length();
                            replacement = this.fromMailAddress;
                        } else if (matchesTag(buf, lastTag, SUBJECT_TAG)) {
                            //tagLen = SUBJECT_TAG.length();
                            replacement = this.regarding;
                        } else if (matchesTag(buf, lastTag, COMMENT_TAG)) {
                            //tagLen = COMMENT_TAG.length();
                            replacement = this.comments;
                        } else if (matchesTag(buf, lastTag, DATE_TAG)) {
                            //tagLen = DATE_TAG.length();
                            replacement = this.dateFmt.format(new Date());
                        } else if (matchesTag(buf, lastTag, NUMPAGES_TAG)) {
                            //tagLen = NUMPAGES_TAG.length();
                            replacement = String.valueOf(this.pageCount);
                        }
                        if (replacement == null) { // Doesn't match any tag -> copy unmodified
                            lastTag = i+2;
                            i++; // skip second @
                        } else {
                            // Write the unmodified part
                            out.write(buf, writePointer, lastTag - writePointer - 2);
                            for (int j = 0; j < replacement.length(); j++) {
                                char c = replacement.charAt(j);
                                // Escape &, <, >, " and '
                                switch (c) {
                                case '&':
                                    out.write("&amp;");
                                    break;
                                case '<':
                                    out.write("&lt;");
                                    break;
                                case '>':
                                    out.write("&gt;");
                                    break;
                                case '\"':
                                    out.write("&quot;");
                                    break;
                                case '\'':
                                    out.write("&apos;");
                                    break;
                                default:
                                    out.write(c);
                                }
                            }
                            // Set the write pointer behind the replaced tag
                            writePointer = i = i+2;
                            lastTag = -1;
                        }
                    } else {
                        lastTag = i+2;
                        i++; // skip second @
                    }
                }
            }
            if (numRead < readLen) {
                readOffset = 0;
            } else if (lastTag >= 0 && loopEnd - lastTag < MAXTAGLENGTH + 4) { // There might be an incomplete tag at the end
                readOffset = loopEnd - lastTag + 2;
            } else {
                readOffset = 2;
            }

            out.write(buf, writePointer, loopEnd - readOffset - writePointer);

            if (readOffset > 0) { // Copy stuff that may be the start of a tag for processing in the next iteration
                System.arraycopy(buf, loopEnd - readOffset, buf, 0, readOffset);
            }
        } while (numRead == readLen);

        out.close();
    }


    private boolean matchesTag(char[] buffer, int offset, String tag) {
        int tagLen = tag.length();
        for (int i = 0; i < tagLen; i++) {
            if (Character.toLowerCase(buffer[i + offset]) != tag.charAt(i)) {
                return false;
            }
        }
        return (buffer[offset+tagLen] == TAGCHAR && buffer[offset+tagLen+1] == TAGCHAR);
    }

    @Override
    public void makeCoverSheet(OutputStream out) throws IOException {
        createCoverSheet(coverTemplate.openStream(), out);
    }
    
    // Testing code:
    public static void main(String[] args) throws Exception {
        System.out.println("Creating cover page...");
        Faxcover cov = new FOPFaxcover(new URL("file:/home/jonas/java/yajhfc/test.fo"));

        cov.comments = "foo\niniun iunuini uinini ninuin iuniuniu 9889hz h897h789 bnin uibiubui ubuib uibub ubiu bib bib ib uib i \nbar";
        cov.fromCompany = "foo Ü&Ö OHG";
        cov.fromFaxNumber = "989898";
        cov.fromLocation = "Bardorf";
        cov.fromVoiceNumber = "515616";
        cov.fromMailAddress = "a@bc.de";

        //cov.pageCount = 10;
//      String[] docs = { "/home/jonas/mozilla.ps", "/home/jonas/nssg.pdf" };
//      for (int i=0; i<docs.length; i++)
//      try {
//      System.out.println(docs[i] + " pages: " + cov.estimatePostscriptPages(new FileInputStream(docs[i])));
//      } catch (FileNotFoundException e) {
//      e.printStackTrace();
//      } catch (IOException e) {
//      e.printStackTrace();
//      }

        cov.pageCount = 55;
        cov.pageSize = utils.papersizes[0];
        cov.regarding = "Test fax";
        cov.sender = "Werner Meißner";

        cov.toCompany = "Bâr GmbH & Co. KGaA";
        cov.toFaxNumber = "87878787";
        cov.toLocation = "Foostädtle";
        cov.toName = "Otto Müller";
        cov.toVoiceNumber = "4545454";

        try {
            cov.makeCoverSheet(new FileOutputStream("/tmp/test.pdf"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

