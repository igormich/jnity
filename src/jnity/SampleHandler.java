package jnity;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.Document;

public class SampleHandler  {

        public Object execute(ExecutionEvent event) throws ExecutionException {
                // Get the root of the workspace
                IWorkspace workspace = ResourcesPlugin.getWorkspace();
                IWorkspaceRoot root = workspace.getRoot();
                // Get all projects in the workspace
                IProject[] projects = root.getProjects();
                // Loop over all projects
                for (IProject project : projects) {
                        try {
                                printProjectInfo(project);
                        } catch (CoreException e) {
                                e.printStackTrace();
                        }
                }
                return null;
        }

        public static void printProjectInfo(IProject project) throws CoreException,
                        JavaModelException {
                System.out.println("Working in project " + project.getName());
                // check if we have a Java project
                if (project.isNatureEnabled("org.eclipse.jdt.core.javanature")) {
                        IJavaProject javaProject = JavaCore.create(project);
                        printPackageInfos(javaProject);
                }
        }

        private static void printPackageInfos(IJavaProject javaProject)
                        throws JavaModelException {
                IPackageFragment[] packages = javaProject.getPackageFragments();
                for (IPackageFragment mypackage : packages) {
                        // Package fragments include all packages in the
                        // classpath
                        // We will only look at the package from the source
                        // folder
                        // K_BINARY would include also included JARS, e.g.
                        // rt.jar
                        if (mypackage.getKind() == IPackageFragmentRoot.K_SOURCE) {
                                System.out.println("Package " + mypackage.getElementName());
                                printICompilationUnitInfo(mypackage);

                        }

                }
        }

        private static void printICompilationUnitInfo(IPackageFragment mypackage)
                        throws JavaModelException {
                for (ICompilationUnit unit : mypackage.getCompilationUnits()) {
                	IType[] types = unit.getAllTypes();
                	for (IType type : types) {
                		System.out.println("Class  "+ type);
                		ITypeHierarchy th= type.newTypeHierarchy(null);
                		for (IType type1 : th.getAllSuperclasses(type)) {
                			System.out.println("Super class  "+ type1);
                		}
                   		for (IType type1 : th.getAllInterfaces()) {
                			System.out.println("Interface  "+ type1);
                		}
                	  // do something with the hierarchy
                	}
                        printCompilationUnitDetails(unit);
                }
        }

        private static void printIMethods(ICompilationUnit unit) throws JavaModelException {
                IType[] allTypes = unit.getAllTypes();
                for (IType type : allTypes) {
                        printIMethodDetails(type);
                }
        }

        private static void printCompilationUnitDetails(ICompilationUnit unit)
                        throws JavaModelException {
                System.out.println("Source file " + unit.getElementName());
                Document doc = new Document(unit.getSource());
                System.out.println("Has number of lines: " + doc.getNumberOfLines());
                printIMethods(unit);
        }

        private static void printIMethodDetails(IType type) throws JavaModelException {
                IMethod[] methods = type.getMethods();
                
                for (IMethod method : methods) {
                	
                        System.out.println("Method name " + method.getElementName());
                        System.out.println("Signature " + method.getSignature());
                        System.out.println("Return Type " + method.getReturnType());
                        System.out.println(method.getSource());

                }
        }
}