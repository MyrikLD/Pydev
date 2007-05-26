package com.python.pydev;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.PyEdit;

import com.python.pydev.license.ClientEncryption;
import com.python.pydev.util.PydevExtensionNotifier;

/**
 * The main plugin class to be used in the desktop.
 */
public class PydevPlugin extends AbstractUIPlugin {

    public static final String version = "REPLACE_VERSION";

	//The shared instance.
	private static PydevPlugin plugin;
	private PydevExtensionNotifier notifier;
	private boolean validated;
    public static final String ANNOTATIONS_CACHE_KEY = "MarkOccurrencesJob Annotations";
    public static final String OCCURRENCE_ANNOTATION_TYPE = "com.python.pydev.occurrences";
	
	/**
	 * The constructor.
	 */
	public PydevPlugin() {
		plugin = this;
	}

	/**
	 * This method is called upon plug-in activation
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		if(!version.equals(org.python.pydev.plugin.PydevPlugin.version)){
			String msg = StringUtils.format("Error: the pydev com plugin version (%s) differs from the org plugin version (%s)", version, org.python.pydev.plugin.PydevPlugin.version);
			org.python.pydev.plugin.PydevPlugin.log(msg);
		}
		checkValid();
	}

    public boolean isValidated(){
        return validated;
    }
    
	public String checkValidStr() {
	    String result = loadLicense();
        if(notifier == null){
            notifier = new PydevExtensionNotifier();
            notifier.start();
        }
        return result;
        
    }
    
    private boolean checkedValidOnce = false;
	public boolean checkValid() {
        if(!checkedValidOnce){
            checkValidStr();
            checkedValidOnce = true;
        }
	    return validated;
	}

	/**
	 * This method is called when the plug-in is stopped
	 */
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
		plugin = null;
	}

	/**
	 * Returns the shared instance.
	 */
	public static PydevPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin("com.python.pydev", path);
	}
	
	
	public void saveLicense( String data ) {	
		Bundle bundle = Platform.getBundle("com.python.pydev");
		IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
			FileOutputStream file = new FileOutputStream(path.toFile());
			file.write( data.getBytes() );
			file.close();
		} catch (FileNotFoundException e) {		
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}   			
    }
	
	private String loadLicense() {
        if(loadFromPluginLocation()){
            return null;
        }
        
        
    	Bundle bundle = Platform.getBundle("com.python.pydev");
    	IPath path = Platform.getStateLocation( bundle );		
    	path = path.addTrailingSeparator();
    	path = path.append("license");
    	try {
            File f = path.toFile();
            if(!f.exists()){
                throw new FileNotFoundException("File not found.");
            }
            
            String encLicense = REF.getFileContents(f);
			if( isLicenseValid(encLicense) ) {
				validated = true;
			} else {
				validated = false;
			}
		} catch (FileNotFoundException e) {
			validated = false;
			String ret = "The license file: "+path.toOSString()+" was not found.";
            return ret;
            
		} catch (Exception e) {
		    validated = false;
            return e.getMessage();
		}
        return null;
	}

    private boolean loadFromPluginLocation() {
        try{
            Location configurationLocation = Platform.getInstallLocation();
            URL url = configurationLocation.getURL();
            String path = url.getPath();
            File file = new File(path);
            if(!file.exists()){
                return false;
            }
            
            file = new File(file, "features");
            if(!file.exists()){
                return false;
            }
            
            file = new File(file, "com.python.pydev");
            if(!file.exists()){
                return false;
            }
            
            File fileLicense = new File(file, "pydev_ext");
            File fileEmail = new File(file, "pydev_ext_email");
            
            if(fileLicense != null && fileEmail != null && fileLicense.exists() && fileEmail.exists()){
                String encLicense = REF.getFileContents(fileLicense).replaceAll("\n", "").replaceAll("\r", "").replaceAll(" ", "");
                String enteredEmail = REF.getFileContents(fileEmail).trim();
                
                if( isLicenseValid(encLicense, enteredEmail, true) ) {
                    validated = true;
                    return true;
                }
            }
            return false;
        }catch(Exception e){
            return false;
        }
    }

    private boolean isLicenseValid(String encLicense) {
        return isLicenseValid(encLicense, getPreferenceStore().getString(PydevExtensionInitializer.USER_EMAIL), false);
    }
    
    private boolean isLicenseValid(String encLicense, String enteredEmail, boolean hideDetails) {
        //already decrypted
        getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME, "");
        getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TIME, "");
        getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TYPE, "");
        String license = ClientEncryption.getInstance().decrypt(encLicense);
        try {
            Properties properties = new Properties();
            properties.load(new ByteArrayInputStream(license.getBytes()));
            
            String eMail = (String) properties.remove("e-mail");
            String name  = (String) properties.remove("name");
            String time = (String) properties.remove("time");
            String licenseType = (String) properties.remove("licenseType");
            String devs = (String) properties.remove("devs");
            
            if(eMail == null || name == null || time == null || licenseType == null || devs == null || enteredEmail == null){
                throw new RuntimeException("The license is not correct, please re-paste it. If this error persists, please request a new license.");
            }
            if(!enteredEmail.equalsIgnoreCase(eMail)){
                throw new RuntimeException("The e-mail specified is different from the e-mail this license was generated for.");
            }
            
            
            Calendar currentCalendar = Calendar.getInstance();
            Calendar licenseCalendar = getExpTime(time);
            if(currentCalendar.after(licenseCalendar)){
                throw new RuntimeException("The current license has already expired.");
            }
            
            
            getPreferenceStore().setValue(PydevExtensionInitializer.USER_NAME, name);
            getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TIME, time);
            getPreferenceStore().setValue(PydevExtensionInitializer.LIC_TYPE, licenseType);
            getPreferenceStore().setValue(PydevExtensionInitializer.LIC_DEVS, devs);
            if(hideDetails){
                getPreferenceStore().setValue(PydevExtensionInitializer.USER_EMAIL, "Install validated for: "+name);
                getPreferenceStore().setValue(PydevExtensionInitializer.LICENSE, "Install validated for: "+name);
            }
            
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        
        //if it got here, everything is ok...
        return true;
    }

    /**
     * @return the list of occurrence annotations in the pyedit
     */
    @SuppressWarnings("unchecked")
    public static final List<Annotation> getOccurrenceAnnotationsInPyEdit(final PyEdit pyEdit) {
    	List<Annotation> toRemove = new ArrayList<Annotation>();
    	final Map<String, Object> cache = pyEdit.cache;
    	
    	if(cache == null){
    		return toRemove;
    	}
    	
    	List<Annotation> inEdit = (List<Annotation>) cache.get(ANNOTATIONS_CACHE_KEY);
    	if(inEdit != null){
    	    Iterator<Annotation> annotationIterator = inEdit.iterator();
    	    while(annotationIterator.hasNext()){
    	        Annotation annotation = annotationIterator.next();
    	        if(annotation.getType().equals(OCCURRENCE_ANNOTATION_TYPE)){
    	            toRemove.add(annotation);
    	        }
    	    }
    	}
    	return toRemove;
    }

    public static Calendar getExpTime(String time) {
        return getExpTime(Long.parseLong(time));
    }

    public static Calendar getExpTime(long lTime) {
        Calendar licenseCalendar = Calendar.getInstance();
        licenseCalendar.setTimeInMillis(lTime);
        licenseCalendar.add(Calendar.YEAR, 1);
        return licenseCalendar;
    }

}
