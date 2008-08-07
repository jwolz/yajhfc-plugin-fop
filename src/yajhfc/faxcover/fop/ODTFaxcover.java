package yajhfc.faxcover.fop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.ZipFile;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOUserAgent;
import org.clazzes.odtransform.OdtTransformer;
import org.clazzes.odtransform.ZipFileURIResolver;
import org.xml.sax.SAXException;

import yajhfc.faxcover.Faxcover;
import yajhfc.utils;

public class ODTFaxcover extends FOPFaxcover {
    private static Logger log = Logger.getLogger(ODTFaxcover.class.getName());
    
    protected ZipFile odtZipfile;
    protected File foTempFile;
    
    public ODTFaxcover(URL coverTemplate) throws IOException, TransformerException, SAXException {
        super(coverTemplate);
        createTempFO();
    }

    protected void createTempFO() throws IOException, TransformerException, SAXException {
        foTempFile = File.createTempFile("fromodt", ".fo");
        foTempFile.deleteOnExit();
        FileOutputStream outStream = new FileOutputStream(foTempFile);
        transformOdtToFO(getODTZipFile(), outStream);
        outStream.close();
    }
    
    protected ZipFile getODTZipFile() throws IOException {
        if (odtZipfile == null) {
            if (coverTemplate.getProtocol().equals("file")) {
                if (utils.debugMode) {
                    log.info("Creating ZipFile for URL " + coverTemplate);
                }
                String path = coverTemplate.getPath();
                if (utils.debugMode) {
                    log.info("Path is " + path);
                }
                odtZipfile = new ZipFile(path);
            } else {
                throw new RuntimeException("Unsupported URL type: " + coverTemplate);
            }
        }
        return odtZipfile;
    }
    
    public static void transformOdtToFO(ZipFile odtFile, OutputStream os) throws IOException, TransformerException, SAXException
    {
        InputStream xsltFile = ODTFaxcover.class.getResourceAsStream("/de/systemconcept/ooo/ooo2xslfo.xslt");
        
        SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();

        // set up xslt transformation.
        Source xsltSource = new StreamSource(xsltFile);

        TransformerHandler xsltHandler = transformerFactory.newTransformerHandler(xsltSource);

        xsltHandler.setResult(new StreamResult(os));

        OdtTransformer odtTransformer = new OdtTransformer();

        odtTransformer.transform(odtFile, new SAXResult(xsltHandler));
    }

    @Override
    protected void configureFO2PDFUserAgent(FOUserAgent userAgent) throws IOException {
        userAgent.setURIResolver(new ZipFileURIResolver(getODTZipFile()));
    }

    @Override
    public void makeCoverSheet(OutputStream out) throws IOException {
        createCoverSheet(new FileInputStream(foTempFile), out);
    }

    // Testing code:
    public static void main(String[] args) throws Exception {
        System.out.println("Creating cover page...");
        Faxcover cov = new ODTFaxcover(new URL("file:/home/jonas/java/yajhfc/test.odt"));

        cov.comments = "foo\niniun iunuini uinini ninuin iuniuniu 9889hz h897h789 bnin uibiubui ubuib uibub ubiu bib bib ib uib i \nbar";
        cov.fromCompany = "foo Ü&Ö OHG";
        cov.fromFaxNumber = "989898";
        cov.fromLocation = "Bardorf";
        cov.fromVoiceNumber = "515616";
        cov.fromMailAddress = "a@bc.de";


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
