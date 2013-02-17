package yajhfc.faxcover.fop;
/*
 * YAJHFC - Yet another Java Hylafax client
 * Copyright (C) 2005-2011 Jonas Wolz <info@yajhfc.de>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.Map;
import java.util.zip.ZipFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
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

import yajhfc.faxcover.Faxcover;
import yajhfc.file.FileConverter;
import yajhfc.file.FileConverterSource;
import yajhfc.file.FileConverters;
import yajhfc.file.FileFormat;
import yajhfc.launch.Launcher2;
import yajhfc.plugin.PluginManager;
import yajhfc.plugin.PluginUI;
import yajhfc.util.ExampleFileFilter;
import yajhfc.util.ExcDialogAbstractAction;
import yajhfc.util.ExceptionDialog;
import yajhfc.util.MsgBundle;

public class EntryPoint {
    
    public static final String AppShortName = "YajHFC FOP and ODT plugin";
    public static final String AppCopyright = "Copyright © 2008-2009 by Jonas Wolz";
    public static final String AppVersion = "0.1.11";
    public static final String AuthorEMail = "info@yajhfc.de";
    public static final String HomepageURL = "http://www.yajhfc.de/"; 
    
    private static FopFactory fopFactory;
    
    public static boolean init() {
        Faxcover.supportedCoverFormats.put(FileFormat.FOP, FOPFaxcover.class);
        Faxcover.supportedCoverFormats.put(FileFormat.ODT, ODTFaxcover.class);
        
        FileConverters.addFileConverterSource(new FileConverterSource() {
            @Override
            public void addFileConvertersTo(Map<FileFormat, FileConverter> converters) {
                converters.put(FileFormat.FOP, FOPFileConverter.SHARED_INSTANCE);
            }
        });
        FileConverters.invalidateFileConverters();
        
        final Action ODTtoFOAction = new ExcDialogAbstractAction() {
            @Override
            protected void actualActionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new yajhfc.util.SafeJFileChooser();
                File odt, fo;
                
                fileChooser.setDialogTitle(_("Select ODT file to convert..."));
                configureFileChooserForFileFormats(fileChooser, FileFormat.ODT);
                if (fileChooser.showOpenDialog(Launcher2.application.getFrame()) == JFileChooser.APPROVE_OPTION) {
                    odt = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                fileChooser.setDialogTitle(_("Select FO file to save the converted ODT to"));
                configureFileChooserForFileFormats(fileChooser, FileFormat.FOP);
                
                // Exchange the extension
                String newName = odt.getName();
                int idx = newName.lastIndexOf('.');
                if (idx >= 0) {
                    newName = newName.substring(0, idx) + ".fo";
                    fileChooser.setSelectedFile(new File(odt.getParentFile(), newName));
                }
                
                if (fileChooser.showSaveDialog(Launcher2.application.getFrame()) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    ODTFaxcover.transformOdtToFO(new ZipFile(odt), new FileOutputStream(fo));
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher2.application.getFrame(), _("Error converting the ODT file:"), ex);
                }
            }
        };
        ODTtoFOAction.putValue(Action.NAME, _("Convert ODT to XSL:FO..."));
       
        final Action ViewFOAction = new ExcDialogAbstractAction() {
            @Override
            protected void actualActionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new yajhfc.util.SafeJFileChooser();
                File fo;
                
                fileChooser.setDialogTitle(_("Select FO file to view"));
                configureFileChooserForFileFormats(fileChooser, FileFormat.FOP);
                if (fileChooser.showOpenDialog(Launcher2.application.getFrame()) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    viewFOFile(fo, null);
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher2.application.getFrame(), _("Error viewing the FO file:"), ex);
                }
            }
        };
        ViewFOAction.putValue(Action.NAME, _("View XSL:FO file..."));
        
        final Action ViewODTAction = new ExcDialogAbstractAction() {
            @Override
            protected void actualActionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new yajhfc.util.SafeJFileChooser();
                File odt, fo = null;
                
                fileChooser.setDialogTitle(_("Select ODT file to view"));
                configureFileChooserForFileFormats(fileChooser, FileFormat.ODT);
                if (fileChooser.showOpenDialog(Launcher2.application.getFrame()) == JFileChooser.APPROVE_OPTION) {
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
                    ExceptionDialog.showExceptionDialog(Launcher2.application.getFrame(), _("Error viewing the ODT file:"), ex);
                } finally {
                    if (fo != null)
                        fo.delete();
                }
            }
        };
        ViewODTAction.putValue(Action.NAME, _("View ODT as XSL:FO file..."));
        
        final Action AboutFOPAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                final String aboutString = String.format(
                        "%s\nVersion %s\n\n%s\n\nHomepage: %s\nE-Mail: %s",
                        AppShortName, AppVersion, AppCopyright, HomepageURL, AuthorEMail);
                JOptionPane.showMessageDialog(Launcher2.application.getFrame(), aboutString, MessageFormat.format(_("About {0}"), AppShortName), JOptionPane.INFORMATION_MESSAGE);
            }
        };
        AboutFOPAction.putValue(Action.NAME, _("About FOP Plugin..."));
        
        PluginManager.pluginUIs.add(new PluginUI() {
            public JMenuItem[] createMenuItems() {
                JMenu pluginMenu = new JMenu(_("XSL:FO and ODT utilities"));
                pluginMenu.add(new JMenuItem(ViewFOAction));
                pluginMenu.add(new JMenuItem(ViewODTAction));
                pluginMenu.add(new JMenuItem(ODTtoFOAction));
                pluginMenu.add(new JMenuItem(AboutFOPAction));
                return new JMenuItem[] { pluginMenu };
            };
        });

        return true;
    }
    
    protected static void configureFileChooserForFileFormats(JFileChooser fileChooser, FileFormat... formats) {
        fileChooser.resetChoosableFileFilters();
        FileFilter allFiles = fileChooser.getAcceptAllFileFilter();
        fileChooser.removeChoosableFileFilter(allFiles);
        
        FileFilter firstFilter = null;
        for (FileFormat ff : formats) {
            FileFilter newFilter = new ExampleFileFilter(ff.getPossibleExtensions(), ff.getDescription());
            if (firstFilter == null) {
                firstFilter = newFilter;
            }
            fileChooser.addChoosableFileFilter(newFilter);
        }
        fileChooser.addChoosableFileFilter(allFiles);
        fileChooser.setFileFilter(firstFilter);
    }
    
    static FopFactory getFopFactory() {
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
    
    public static final MsgBundle msgBundle  = new MsgBundle("yajhfc.faxcover.fop.i18n.FOPMessages");
    
    /**
     * Returns the translation of key. If no translation is found, the
     * key is returned.
     * @param key
     * @return
     */
    public static String _(String key) {
        return msgBundle._(key, key);
    }
    
    /**
     * Returns the translation of key. If no translation is found, the
     * defaultValue is returned.
     * @param key
     * @param defaultValue
     * @return
     */
    public static String _(String key, String defaultValue) {
        return msgBundle._(key, defaultValue);
    }
}
