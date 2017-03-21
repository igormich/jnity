package jnity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.osgi.framework.Bundle;

/**
 * 
 * @author dzavodnikov
 * 
 */
public class EclipseHelper {

    private static IProgressMonitor monitor = new NullProgressMonitor();

    /**
     * Return object of active editor.
     */
    public static IEditorPart getActiveEditor() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            final IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                return page.getActiveEditor();
            } else {
                throw new RuntimeException("Can not get access to Active page!");
            }
        } else {
            throw new RuntimeException("Can not get access to Workbench window!");
        }
    }

    /**
     * Return ID of current perspective.
     */
    public static String getCurrentPerspective() {
        final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window != null) {
            final IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                final String perspectiveId = page.getPerspective().getId();
                if (perspectiveId != null) {
                    return perspectiveId;
                } else {
                    throw new RuntimeException("Can not get ID of current Perspective!");
                }
            } else {
                throw new RuntimeException("Can not get access to Active page!");
            }
        } else {
            throw new RuntimeException("Can not get access to Workbench window!");
        }
    }

    /**
     * Open some perspective by given ID.
     */
    public static void openPerspective(final String perspectiveId) {
    	final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        try{
        	PlatformUI.getWorkbench().showPerspective(perspectiveId, window);
        } catch (WorkbenchException e) {
        	throw new RuntimeException("Unable to open Perspective " + perspectiveId);
        }
    }

    /**
     * Return View object by view ID.
     */
    public static IViewPart getViewById(final String viewId) {
        final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        if (activePage != null) {
            final IViewPart view = activePage.findView(viewId);
            if (view != null) {
                // View is open.
                return view;
            }
        }
        // View is closed or not exists.
        return null;
    }

    /**
     * Get content of IFile.
     */
    public static String getFileContent(final IFile file) {
        if (file == null) {
            throw new IllegalArgumentException("IFile argument is null!");
        }

        final StringBuilder sb = new StringBuilder();

        try {
            final BufferedReader in = new BufferedReader(new InputStreamReader(file.getContents()));

            String inputLine;
            while((inputLine = in.readLine()) != null) {
                sb.append(inputLine + "\n");
            }

            in.close();
        } catch (final CoreException e) {
            throw new RuntimeException(e.getMessage());
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }

        return sb.toString();
    }

    /**
     * Get content of IFile.
     */
    public static void setFileContent(final IFile file, final String content) {
        if (file == null) {
            throw new IllegalArgumentException("IFile argument is null!");
        }
        if (content == null) {
            throw new IllegalArgumentException("String argument is null!");
        }

        try {
            file.setContents(new ByteArrayInputStream(content.getBytes()), true, false, monitor);
        } catch (final CoreException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Correctly resolve URL to URI.
     */
    public static URI correctURLtoURI(final URL fileURL) {
        if (fileURL == null) {
            throw new IllegalArgumentException("URL argument is null!");
        }

        try {
            // Method 'fileURL.toURI()' will be not working!
            final URL resolved = FileLocator.resolve(fileURL);
            if (resolved != null) {
                return resolved.toURI();
            } else {
                throw new RuntimeException("Can not resolve URL: " + fileURL);
            }
        } catch (final URISyntaxException e) {
            throw new RuntimeException(e.getMessage());
        } catch (final IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Create java.io.File (file or directory) for given path into current Plug-in resource.
     * 
     * <P>
     * To get Bundle use this code:
     * <CODE>
     * Bundle bundle1 = Platform.getBundle("my.plugin.id");
     * // or
     * Bundle bundle2 = MyPlugin.getDefault().getBundle();
     * </CODE>
     * </P>
     */
    public static java.io.File createFileFromPlugin(final Bundle pluginBundle, final String path) {
        if (pluginBundle == null) {
            throw new IllegalArgumentException("Bundle is null!");
        }
        if (path == null) {
            throw new IllegalArgumentException("Path string is null!");
        }

        final URL url = pluginBundle.getEntry(path);
        if (url == null) {
            throw new RuntimeException("Can not get URL value: " + path);
        }

        final URI uri = correctURLtoURI(url);
        if (uri == null) {
            throw new RuntimeException("Can not resolve URI value: " + uri);
        }

        return new java.io.File(uri);
    }

    /**
     * Create IFile for given path into Workspace.
     * 
     * <P>
     * Workspace for current project can be defined as:
     * <CODE>
     * IWorkspace workspace = ResourcesPlugin.getWorkspace();
     * </CODE>
     * </P>
     */
    public static IFile createIFileFromWorkspace(final IWorkspace workspace, final Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path is null!");
        }

        return workspace.getRoot().getFile(path);
    }

    /**
     * Create IFile for given path into Workspace.
     */
    public static IFile createIFileFromWorkspace(final IWorkspace workspace, final String path) {
        return createIFileFromWorkspace(workspace, new Path(path));
    }

    /**
     * Create IFolder for given path into Workspace.
     */
    public static IFolder createIFolderFromWorkspace(final IWorkspace workspace, final Path path) {
        if (path == null) {
            throw new IllegalArgumentException("Path is null!");
        }

        return workspace.getRoot().getFolder(path);
    }

    /**
     * Create IFolder for given path into Workspace.
     */
    public static IFolder createIFolderFromWorkspace(final IWorkspace workspace, final String path) {
        if (path == null) {
            throw new IllegalArgumentException("String is null!");
        }

        return createIFolderFromWorkspace(workspace, new Path(path));
    }

    /**
     * Copy content from file represented as java.io.File to IFile.
     */
    public static void copyFileToIFile(final java.io.File source, final IFile target) {
        if (source == null) {
            throw new IllegalArgumentException("java.io.File is null!");
        }

        if (target == null) {
            throw new IllegalArgumentException("IFile is null!");
        }

        if (source.exists()) {
            try {
                if (target.exists()) {
                    target.delete(true, monitor);
                }
                target.create(new FileInputStream(source), true, monitor);
            } catch (final IOException e) {
                throw new RuntimeException(e.getMessage());
            } catch (final CoreException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
     * Copy resource (File or Folder) represented as java.io.File into IFolder.
     */
    public static void copyFileToIFolder(final java.io.File source, final IFolder parent) {
        if (source == null) {
            throw new IllegalArgumentException("java.io.File is null!");
        }
        if (parent == null) {
            throw new IllegalArgumentException("IFolder is null!");
        }

        if (!parent.exists()) {
            try {
                parent.create(true, true, monitor);
            } catch (final CoreException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        if (source.exists()) {
            if (source.isFile()) {
                copyFileToIFile(source, parent.getFile(source.getName()));
            } else if (source.isDirectory()) {
                final java.io.File[] dirContent = source.listFiles();
                for (java.io.File file: dirContent) {
                    if (file.exists()) {
                        if (file.isFile()) {
                            copyFileToIFolder(file, parent);
                        } else if (file.isDirectory()) {
                            copyFileToIFolder(file, parent.getFolder(file.getName()));
                        }
                    }
                }
            }
        }
    }
    public static IProject getSelectedProject() {
        ISelectionService ss=PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService();
        String projExpID = "org.eclipse.ui.navigator.ProjectExplorer";
        ISelection sel = ss.getSelection(projExpID);
        Object selectedObject=sel;
        if(sel instanceof IStructuredSelection) {
        	selectedObject=((IStructuredSelection)sel).getFirstElement();
	        if (selectedObject instanceof IAdaptable) {
	              IResource res = (IResource) ((IAdaptable) selectedObject).getAdapter(IResource.class);
	              IProject project = res.getProject();
	              System.out.println("Project found: "+project.getName());
	              return project;
	        }
        }
        return null;
      }
}
