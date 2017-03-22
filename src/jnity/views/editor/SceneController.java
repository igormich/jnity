package jnity.views.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;

import base.Camera;
import base.Object3d;
import base.Scene;
import io.ResourceController;
import materials.HashMapMaterialLibrary;
import materials.MaterialLibrary;
import materials.SimpleMaterial;
import properties.MultiMesh;
import properties.Property3d;
import properties.SelectionOverlay;

public class SceneController {
	private Object3d selected;
	private Object3d underCursor;
	private Scene scene;
	private Camera camera;
	private IFolder textureFolder;
	private IFolder modelFolder;
	private IProject project;
	private ProjectResourseListener projectResourseListener;
	private boolean playing = false;
	private InputStream baseState;
	private byte[] previousContent;

	public boolean isPlaying() {
		return playing;
	}

	public void play() {
		playing = true;
		baseState = openContentStream();
	}

	public void pause() {
		playing = false;
	}

	public void stop() {
		playing = false;
		loadScene(baseState);
	}
	
	public void rebase() {
		playing = false;
		baseState = openContentStream();
	}
	public SceneController(ProjectResourseListener sceneResourseController) {
		this.projectResourseListener = sceneResourseController;
		this.projectResourseListener.setSceneController(this);
	}

	public Scene getScene() {
		return scene;
	}

	public Camera getCamera() {
		return camera;
	}
	public void loadSceneWithoutCamera(InputStream inputStream) {
		ObjectInputStream ois;
		try {
			ResourceController.getOrCreate().setTexturesPath(textureFolder.getLocation().toString() + "/");
			ResourceController.getOrCreate().setModelPath(modelFolder.getLocation().toString() + "/");
			ClassLoader classLoader = getClassLoader();
			ois = new ClassLoaderObjectInputStream(classLoader, inputStream);
			scene = (Scene) ois.readObject();
			ois.close();
			if (selected != null) {
				int id = selected.getID();
				selected = scene.getByID(id);
			}
			openContentStream();
		} catch (Exception e) {
			scene = new Scene();
			e.printStackTrace();
		}
	}
	public void loadScene(InputStream inputStream) {
		ObjectInputStream ois;
		try {
			ResourceController.getOrCreate().setTexturesPath(textureFolder.getLocation().toString() + "/");
			ResourceController.getOrCreate().setModelPath(modelFolder.getLocation().toString() + "/");
			ClassLoader classLoader = getClassLoader();
			ois = new ClassLoaderObjectInputStream(classLoader, inputStream);
			scene = (Scene) ois.readObject();
			camera = (Camera) ois.readObject();
			ois.close();
			if (selected != null) {
				int id = selected.getID();
				selected = scene.getByID(id);
			}
			openContentStream();
		} catch (Exception e) {
			scene = new Scene();
			camera = new Camera();
			e.printStackTrace();
		}
	}

	private void printChildren(String tabs, Object3d root) {
		System.out.println(tabs + root.getID());
		for (Property3d property : root.getProperties()) {
			System.out.println(tabs + property.getClass());
			if (property instanceof MultiMesh) {
				MultiMesh mesh = (MultiMesh) property;
				System.out.println(tabs + mesh.getFileName());
			}
		}
		for (Object3d object3d : root.getChildren())
			printChildren(tabs + "\t", object3d);

	}

	public InputStream openContentStream() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			if (selected != null) {
				selected.remove(SelectionOverlay.class);
			}
			printChildren("", scene.getRoot());
			oos.writeObject(getScene());
			oos.writeObject(getCamera());
			oos.flush();
			oos.close();
			oos.flush();
			oos.close();
			if (selected != null) {
				selected.add(new SelectionOverlay());
			}
			previousContent = baos.toByteArray();
			return new ByteArrayInputStream(previousContent);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new ByteArrayInputStream(new byte[] {});
	}

	public InputStream getPreviousContentStream() {
		if (previousContent != null)
			return new ByteArrayInputStream(previousContent);
		else
			return null;
	}

	public void initDirs(IProject project) throws CoreException {
		this.project = project;
		textureFolder = project.getFolder("textures");
		if (!textureFolder.exists())
			textureFolder.create(IResource.NONE, true, null);
		modelFolder = project.getFolder("models");
		if (!modelFolder.exists())
			modelFolder.create(IResource.NONE, true, null);

	}

	public Object3d getSelectedObject() {
		return selected;
	}

	public void setSelectedObject(Object3d object) {
		if (selected != null) {
			selected.remove(SelectionOverlay.class);
		}
		selected = object;
		if (selected != null) {
			selected.add(new SelectionOverlay());
		}
	}

	public Object3d getObject(int x, int y) {
		return scene.getObject(x, camera.height - y, camera);
	}

	public void render() {
		scene.render(camera);
		if ((underCursor != null) && (underCursor != selected)) {
			SimpleMaterial selectedMateral = new SimpleMaterial();
			selectedMateral.setBlendMode(SimpleMaterial.TRANSPARENCY);
			selectedMateral.setColor(1, 1, 0, 0.5f);
			selectedMateral.apply(null, underCursor);
			scene.renderSingle(camera, underCursor, false);
			selectedMateral.unApply();
		}
		if (selected != null) {
			SimpleMaterial selectedMateral = new SimpleMaterial();
			selectedMateral.setBlendMode(SimpleMaterial.TRANSPARENCY);
			selectedMateral.setColor(1, 0, 0, 0.5f);
			selectedMateral.apply(null, selected);
			scene.renderSingle(camera, selected, false);
			selectedMateral.unApply();
		}

	}

	public IProject getProject() {
		return project;
	}

	public MaterialLibrary getMaterialLibrary() {
		MaterialLibrary result = scene.getMaterialLibrary();
		if (result == null)
			scene.setMaterialLibrary(new HashMapMaterialLibrary());
		return scene.getMaterialLibrary();
	}

	public IFolder getModelFolder() {
		return modelFolder;
	}

	public IFolder getTextureFolder() {
		return textureFolder;
	}

	public void setUnderCursorObject(Object3d object) {
		underCursor = object;
	}

	public void registerMaterial(IResource resource, String materialName) {
		String path = resource.getProjectRelativePath().toString();
		path = path.replaceAll("src/", "bin/");
		path = path.replaceAll(".java", ".class");
		projectResourseListener.registerMaterial(path, materialName);
	}

	public void registerProperty(IResource resource, Class<? extends Property3d> clazz) {
		String path = resource.getProjectRelativePath().toString();
		path = path.replaceAll("src/", "bin/");
		path = path.replaceAll(".java", ".class");
		projectResourseListener.registerProperty(path, clazz);
	}

	public URLClassLoader getClassLoader() throws CoreException, MalformedURLException {
		IJavaProject javaProject = JavaCore.create(project);
		String[] classPathEntries = JavaRuntime.computeDefaultRuntimeClassPath(javaProject);
		List<URL> urlList = new ArrayList<URL>();
		for (int i = 0; i < classPathEntries.length; i++) {
			String entry = classPathEntries[i];
			IPath path = new Path(entry);
			URL url = path.toFile().toURI().toURL();
			urlList.add(url);
		}
		URL[] urls = (URL[]) urlList.toArray(new URL[urlList.size()]);
		// return new URLClassLoader(urls, scene.getClass().getClassLoader());
		// return new URLClassLoader(urls, project.getClass().getClassLoader());
		return new URLClassLoader(urls, this.getClass().getClassLoader());
	}

	public Class<?> seachForProperty(IResource resource) {
		String path = resource.getProjectRelativePath().toString();
		path = path.replaceAll("src/", "bin/");
		path = path.replaceAll(".java", ".class");
		return projectResourseListener.seachForProperty(path);
	}

	public Class<?> getClassFrom(IResource resource)
			throws ClassNotFoundException, MalformedURLException, IOException, CoreException {
		String classPath = resource.getProjectRelativePath().toString();
		classPath = classPath.replaceAll("/", ".");
		classPath = classPath.replaceAll("src.", "");
		classPath = classPath.replaceAll("bin.", "");
		classPath = classPath.replaceAll(".java", "");
		classPath = classPath.replaceAll(".class", "");
		try (URLClassLoader classLoader = getClassLoader()) {
			return classLoader.loadClass(classPath);
		}
	}



}
