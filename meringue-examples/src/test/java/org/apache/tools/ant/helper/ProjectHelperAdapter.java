package org.apache.tools.ant.helper;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.*;
import org.xml.sax.helpers.XMLReaderAdapter;

import java.io.*;
import java.lang.reflect.Field;

public class ProjectHelperAdapter extends ProjectHelper {
    private final Field projectField;
    private final Field parserField;
    private final Field buildFileParentField;
    private final Field buildFileField;

    public ProjectHelperAdapter() throws NoSuchFieldException {
        parserField = ProjectHelperImpl.class.getDeclaredField("parser");
        parserField.setAccessible(true);
        projectField = ProjectHelperImpl.class.getDeclaredField("project");
        projectField.setAccessible(true);
        buildFileField = ProjectHelperImpl.class.getDeclaredField("buildFile");
        buildFileField.setAccessible(true);
        buildFileParentField = ProjectHelperImpl.class.getDeclaredField("buildFileParent");
        buildFileParentField.setAccessible(true);
    }

    public void parse(Project project, InputStream in) throws BuildException, IllegalAccessException {
        ProjectHelperImpl helper = new ProjectHelperImpl();
        try {
            org.xml.sax.Parser parser;
            try {
                parser = JAXPUtils.getParser();
            } catch (BuildException e) {
                parser = new XMLReaderAdapter(JAXPUtils.getXMLReader());
            }
            configure(helper, project, parser);
            InputSource inputSource = new InputSource(in);
            HandlerBase hb = new ProjectHelperImpl.RootHandler(helper);
            parser.setDocumentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(), exc.getLineNumber(), exc.getColumnNumber());
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }
            throw new BuildException(exc.getMessage(), t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
            throw new BuildException("Encoding of project file is invalid.", exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file: " + exc.getMessage(), exc);
        } finally {
            FileUtils.close(in);
        }
    }

    private void configure(ProjectHelperImpl helper, Project project, Parser parser) throws IllegalAccessException {
        projectField.set(helper, project);
        parserField.set(helper, parser);
        buildFileParentField.set(helper, new File("/tmp"));
        buildFileField.set(helper, new File("/tmp/build.xml"));
    }
}

