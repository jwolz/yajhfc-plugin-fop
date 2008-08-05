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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
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
import org.apache.fop.apps.MimeConstants;
import org.clazzes.odtransform.ZipFileURIResolver;

import yajhfc.ExampleFileFilter;
import yajhfc.ExceptionDialog;
import yajhfc.Launcher;
import yajhfc.PluginManager;
import yajhfc.FormattedFile.FileFormat;
import yajhfc.faxcover.Faxcover;

public class EntryPoint {
    
    private static FopFactory fopFactory;
    
    public static boolean init() {
        Faxcover.supportedCoverFormats.put(FileFormat.FOP, FOPFaxcover.class);
        Faxcover.supportedCoverFormats.put(FileFormat.ODT, ODTFaxcover.class);
        
        Action ODTtoFOAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                File odt, fo;
                
                fileChooser.setDialogTitle(_("Select ODT file to convert..."));
                configureFileChooserForFileFormats(fileChooser, FileFormat.ODT);
                if (fileChooser.showOpenDialog(Launcher.application) == JFileChooser.APPROVE_OPTION) {
                    odt = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                fileChooser.setDialogTitle(_("Select FO file to save the converted ODT."));
                configureFileChooserForFileFormats(fileChooser, FileFormat.FOP);
                
                // Exchange the extension
                String newName = odt.getName();
                int idx = newName.lastIndexOf('.');
                if (idx >= 0) {
                    newName = newName.substring(0, idx) + ".fo";
                    fileChooser.setSelectedFile(new File(odt.getParentFile(), newName));
                }
                
                if (fileChooser.showSaveDialog(Launcher.application) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    ODTFaxcover.transformOdtToFO(new ZipFile(odt), new FileOutputStream(fo));
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher.application, _("Error converting the ODT file:"), ex);
                }
            }
        };
        ODTtoFOAction.putValue(Action.NAME, _("Covert ODT to XSL:FO..."));
       
        Action ViewFOAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                File fo;
                
                fileChooser.setDialogTitle(_("Select FO file to view"));
                configureFileChooserForFileFormats(fileChooser, FileFormat.FOP);
                if (fileChooser.showOpenDialog(Launcher.application) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    viewFOFile(fo, null);
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher.application, _("Error viewing the FO file:"), ex);
                }
            }
        };
        ViewFOAction.putValue(Action.NAME, _("View XSL:FO file..."));
        
        Action ViewODTAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                File odt, fo = null;
                
                fileChooser.setDialogTitle(_("Select ODT file to view"));
                configureFileChooserForFileFormats(fileChooser, FileFormat.ODT);
                if (fileChooser.showOpenDialog(Launcher.application) == JFileChooser.APPROVE_OPTION) {
                    odt = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    fo = File.createTempFile("fromodt", ".fo");
                    ZipFile odtZIP = new ZipFile(odt);
                    ODTFaxcover.transformOdtToFO(odtZIP, new FileOutputStream(fo));
                    
                    FOUserAgent userAgent = getFopFactory().newFOUserAgent();
                    userAgent.setURIResolver(new ZipFileURIResolver(odtZIP));                   
                    viewFOFile(fo, userAgent);;
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher.application, _("Error viewing the ODT file:"), ex);
                } finally {
                    if (fo != null)
                        fo.delete();
                }
            }
        };
        ViewODTAction.putValue(Action.NAME, _("View ODT as XSL:FO file..."));
        
        JMenu pluginMenu = new JMenu(_("XSL:FO and ODT utilities"));
        pluginMenu.add(new JMenuItem(ViewFOAction));
        pluginMenu.add(new JMenuItem(ViewODTAction));
        pluginMenu.add(new JMenuItem(ODTtoFOAction));
        
        PluginManager.pluginMenuEntries.add(pluginMenu);
        
        return true;
    }
    
    protected static void configureFileChooserForFileFormats(JFileChooser fileChooser, FileFormat... formats) {
        fileChooser.resetChoosableFileFilters();
        FileFilter allFiles = fileChooser.getAcceptAllFileFilter();
        fileChooser.removeChoosableFileFilter(allFiles);
        
        FileFilter firstFilter = null;
        for (FileFormat ff : formats) {
            FileFilter newFilter = new ExampleFileFilter(ff.getPossibleExtension(), ff.getDescription());
            if (firstFilter == null) {
                firstFilter = newFilter;
            }
            fileChooser.addChoosableFileFilter(newFilter);
        }
        fileChooser.addChoosableFileFilter(allFiles);
        fileChooser.setFileFilter(firstFilter);
    }
    
    private static FopFactory getFopFactory() {
        // configure fopFactory as desired
        if (fopFactory == null) {
            fopFactory = FopFactory.newInstance();
            fopFactory.setStrictValidation(false);
        }
        return fopFactory;
    }
    
    public static void viewFOFile(File foFile, FOUserAgent userAgent) throws FOPException, TransformerException {


        //Setup FOP
        if (userAgent == null) {
            userAgent = getFopFactory().newFOUserAgent();
        }
        Fop fop = getFopFactory().newFop(MimeConstants.MIME_FOP_AWT_PREVIEW, userAgent);


        //Load XSL-FO file (you can also do an XSL transformation here)
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        Source src = new StreamSource(foFile);
        Result res = new SAXResult(fop.getDefaultHandler());
        transformer.transform(src, res);
    }
    
    public static String _(String key) {
        // TODO: Replace with non dummy implementation...
        return key;
    }
}
