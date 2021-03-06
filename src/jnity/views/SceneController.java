package jnity.views;

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

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;

import base.Camera;
import base.Object3d;
import base.Scene;
import io.ResourceController;
import jnity.properties.SelectionOverlay;
import jnity.views.editor.ClassLoaderObjectInputStream;
import jnity.views.editor.ProjectResourseListener;
import materials.HashMapMaterialLibrary;
import materials.MaterialLibrary;
import materials.SimpleMaterial;
import physics.PhysicController;
import properties.Property3d;

public class SceneController {
	private Object3d selected;
	private Object3d underCursor;
	private Scene scene = new Scene();
	private Camera camera = new Camera();
	private IFolder textureFolder;
	private IFolder modelFolder;
	private IFolder prefabFolder;
	private IProject project;
	private ProjectResourseListener projectResourseListener;
	private boolean playing = false;
	private InputStream baseState;
	private byte[] previousContent;
	private IFolder soundFolder;

	public boolean isPlaying() {
		return playing;
	}

	public synchronized void play() {
		playing = true;
		baseState = openContentStream();
	}

	public synchronized void pause() {
		playing = !playing;
	}

	public synchronized void stop() {
		playing = false;
		loadSceneWithoutCamera(baseState);
	}

	public synchronized void rebase() {
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
		PhysicController.getDefault().clear();
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
		PhysicController.getDefault().clear();
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



	public InputStream openContentStream() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			//printChildren("", scene.getRoot());
			oos.writeObject(getScene());
			oos.writeObject(getCamera());
			oos.flush();
			oos.close();
			oos.flush();
			oos.close();
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
		prefabFolder = project.getFolder("prefabs");
		if (!prefabFolder.exists())
			prefabFolder.create(IResource.NONE, true, null);
		soundFolder = project.getFolder("sounds");
		if (!soundFolder.exists())
			soundFolder.create(IResource.NONE, true, null);
	}

	public Object3d getSelectedObject() {
		return selected;
	}

	public void setSelectedObject(Object3d object) {
		if (selected != null) {
			selected.removeAll(SelectionOverlay.class);
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

	public IFolder getPrefabFolder() {
		return prefabFolder;
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

	public IFolder getSoundFolder() {
		return soundFolder;
	}

}
