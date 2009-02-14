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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;

import yajhfc.PaperSize;
import yajhfc.faxcover.Faxcover;
import yajhfc.faxcover.MarkupFaxcover;
import yajhfc.file.FileConverter.ConversionException;
import yajhfc.file.FormattedFile.FileFormat;
import yajhfc.phonebook.PBEntryField;
import yajhfc.phonebook.convrules.DefaultPBEntryFieldContainer;

public class FOPFaxcover extends MarkupFaxcover {

    public FOPFaxcover(URL coverTemplate) {
        super(coverTemplate); 
        //newLineReplacement = "\u2028";
    }
    
    @Override
    protected void convertMarkupToHyla(File tempFile, OutputStream out)
            throws IOException, ConversionException {
        FOPFileConverter.SHARED_INSTANCE.convertToHylaFormat(tempFile, out, pageSize, FileFormat.PDF);
    }
    
    // Testing code:
    public static void main(String[] args) throws Exception {
        System.out.println("Creating cover page...");
        Faxcover cov = new FOPFaxcover(new File("/home/jonas/test/directory with a space/test.fo").toURI().toURL());

        cov.comments = "foo\niniun iunuini uinini ninuin iuniuniu 9889hz h897h789 bnin uibiubui ubuib uibub ubiu bib bib ib uib i \nbar";
        cov.fromData = new DefaultPBEntryFieldContainer();
        cov.toData = new DefaultPBEntryFieldContainer();
        cov.fromData.setField(PBEntryField.Company, "foo Ü&Ö OHG");
        cov.fromData.setField(PBEntryField.FaxNumber, "989898");
        cov.fromData.setField(PBEntryField.Location, "Bardorf");
        cov.fromData.setField(PBEntryField.VoiceNumber, "515616");
        cov.fromData.setField(PBEntryField.EMailAddress, "a@bc.de");

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
        cov.pageSize = PaperSize.A4;
        cov.regarding = "Test fax";
        cov.fromData.setField(PBEntryField.Name, "Werner Meißner");

        cov.toData.setField(PBEntryField.Company, "Bâr GmbH & Co. KGaA");
        cov.toData.setField(PBEntryField.FaxNumber, "87878787");
        cov.toData.setField(PBEntryField.Location, "Foostädtle");
        cov.toData.setField(PBEntryField.Name, "Otto Müller");
        cov.toData.setField(PBEntryField.VoiceNumber, "4545454");

        try {
            String outName = "/tmp/test.pdf";
            cov.makeCoverSheet(new FileOutputStream(outName));
            Runtime.getRuntime().exec(new String[] { "xpdf", outName } );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

