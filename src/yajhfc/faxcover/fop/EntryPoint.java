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
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
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

import yajhfc.Launcher2;
import yajhfc.Utils;
import yajhfc.faxcover.Faxcover;
import yajhfc.file.FormattedFile;
import yajhfc.file.FormattedFile.FileFormat;
import yajhfc.plugin.PluginManager;
import yajhfc.plugin.PluginUI;
import yajhfc.util.ExampleFileFilter;
import yajhfc.util.ExcDialogAbstractAction;
import yajhfc.util.ExceptionDialog;

public class EntryPoint {
    
    public static final String AppShortName = "YajHFC FOP and ODT plugin";
    public static final String AppCopyright = "Copyright © 2008-2009 by Jonas Wolz";
    public static final String AppVersion = "0.1.3";
    public static final String AuthorEMail = "jwolz@freenet.de";
    public static final String HomepageURL = "http://yajhfc.berlios.de/"; 
    
    private static FopFactory fopFactory;
    
    public static boolean init() {
        Faxcover.supportedCoverFormats.put(FileFormat.FOP, FOPFaxcover.class);
        Faxcover.supportedCoverFormats.put(FileFormat.ODT, ODTFaxcover.class);
        
        FormattedFile.fileConverters.put(FileFormat.FOP, FOPFileConverter.SHARED_INSTANCE);
        
        final Action ODTtoFOAction = new ExcDialogAbstractAction() {
            @Override
            protected void actualActionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new yajhfc.util.SafeJFileChooser();
                File odt, fo;
                
                fileChooser.setDialogTitle(_("Select ODT file to convert..."));
                configureFileChooserForFileFormats(fileChooser, FileFormat.ODT);
                if (fileChooser.showOpenDialog(Launcher2.application) == JFileChooser.APPROVE_OPTION) {
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
                
                if (fileChooser.showSaveDialog(Launcher2.application) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    ODTFaxcover.transformOdtToFO(new ZipFile(odt), new FileOutputStream(fo));
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher2.application, _("Error converting the ODT file:"), ex);
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
                if (fileChooser.showOpenDialog(Launcher2.application) == JFileChooser.APPROVE_OPTION) {
                    fo = fileChooser.getSelectedFile();
                } else {
                    return;
                }
                
                try {
                    viewFOFile(fo, null);
                } catch (Exception ex) {
                    ExceptionDialog.showExceptionDialog(Launcher2.application, _("Error viewing the FO file:"), ex);
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
                if (fileChooser.showOpenDialog(Launcher2.application) == JFileChooser.APPROVE_OPTION) {
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
                    ExceptionDialog.showExceptionDialog(Launcher2.application, _("Error viewing the ODT file:"), ex);
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
                JOptionPane.showMessageDialog(Launcher2.application, aboutString, MessageFormat.format(_("About {0}"), AppShortName), JOptionPane.INFORMATION_MESSAGE);
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
    
    private static boolean triedMsgLoad = false;
    private static ResourceBundle msgs = null;
    public static String _(String key) {
        if (msgs == null)
            if (triedMsgLoad)
                return key;
            else {
                LoadMessages();
                return _(key);
            }                
        else
            try {
                return msgs.getString(key);
            } catch (Exception e) {
                return key;
            }
    }
    
    private static void LoadMessages() {
        triedMsgLoad = true;
        
        // Use special handling for english locale as we don't use
        // a ResourceBundle for it
        if (Utils.getLocale().equals(Locale.ENGLISH)) {
            msgs = null;
        } else {
            try {
                msgs = ResourceBundle.getBundle("yajhfc.faxcover.fop.i18n.FOPMessages", Utils.getLocale());
            } catch (Exception e) {
                msgs = null;
            }
        }
    }
}
