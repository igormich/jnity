package jnity.views.editor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.ui.part.ResourceTransfer;
import org.lwjgl.util.vector.Vector3f;

import base.Object3d;
import base.Position;
import base.Scene;
import io.MeshLoaderOBJ;
import io.MeshLoaderSMD;
import io.ResourceController;
import jnity.views.SceneEditor;
import materials.Material;
import materials.TexturedMaterial;
import properties.MultiMesh;
import properties.Property3d;

public class SceneDropTarget extends DropTargetAdapter {
	enum DropType {
		TEXTURE, MODEL, JAVA_CLASS, PREFAB, UNDEFINED
	}

	private SceneController sceneController;
	private SceneEditor sceneEditor;

	public SceneDropTarget(SceneEditor sceneEditor, SceneController sceneController) {
		this.sceneEditor = sceneEditor;
		this.sceneController = sceneController;
	}

	@Override
	public void drop(DropTargetEvent event) {
		try {
			event.detail = DND.DROP_NONE;
			synchronized (sceneController) {
				DropType dropType = getDropType(event);
				switch (dropType) {
				case TEXTURE:
					dropTexture(event);
					break;
				case JAVA_CLASS:
					dropClass(event);
					break;
				case MODEL:
					dropModel(event);
					break;
				case PREFAB:
					dropPrefab(event);
					break;
				default:
					break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void dropClass(DropTargetEvent event) throws MalformedURLException, ClassNotFoundException, CoreException {
		Object3d target = sceneEditor.getObjectFromScreenPoint(event.x, event.y);
		if (target == null)
			return;
		IResource[] resources = (IResource[]) event.data;
		IResource resource = resources[0];
		try{
			Class<?>  clazz = sceneController.seachForProperty(resource);
			if(clazz == null) {
				clazz = sceneController.getClassFrom(resource);;
			}
			if (Material.class.isAssignableFrom(clazz)) {
				@SuppressWarnings("unchecked")
				Class<? extends Material> materialClazz = (Class<? extends Material>) clazz;
				dropMaterial(materialClazz, target, resource);
			} else if (Property3d.class.isAssignableFrom(clazz)) {
				@SuppressWarnings("unchecked")
				Class<? extends Property3d> propertyClazz = (Class<? extends Property3d>) clazz;
				dropProperty(propertyClazz, target, resource);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void dropMaterial(Class<? extends Material> clazz, Object3d target, IResource resource)
			throws InstantiationException, IllegalAccessException {
		String materialName = clazz.getCanonicalName();
		sceneController.registerMaterial(resource, materialName);
		sceneController.getScene().getMaterialLibrary().addMaterial(materialName, clazz.newInstance());
		MultiMesh multiMesh = target.get(MultiMesh.class);
		multiMesh.setMaterialNameForAll(materialName);
		sceneEditor.makeDirty("add Material");
	}

	private void dropProperty(Class<? extends Property3d> clazz, Object3d target, IResource resource)
			throws InstantiationException, IllegalAccessException {
		sceneController.registerProperty(resource, clazz);
		Property3d property3d = clazz.newInstance();
		target.add(property3d);
		sceneEditor.makeDirty("add Propert");
	}
	private void dropPrefab(DropTargetEvent event) throws CoreException, FileNotFoundException, IOException, ClassNotFoundException {
		File f = null;
		if (ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
			IResource[] resources = (IResource[]) event.data;
			IResource resource = resources[0];
			if (!resource.getProject().equals(sceneController.getProject()))
				return;
			f = resource.getLocation().toFile();
		}
		
		ClassLoader classLoader = sceneController.getClassLoader();
		try(ClassLoaderObjectInputStream ois = new ClassLoaderObjectInputStream(classLoader, new FileInputStream(f))){
			Object3d object3d  = (Object3d) ois.readObject();
			String shortName = f.getName();
			object3d.setName(shortName.substring(0,shortName.lastIndexOf('.')) + object3d.getID());
			sceneController.getScene().add(object3d);
			Position position = object3d.resetPosition();
			Position cameraPosition = sceneController.getCamera().getPosition();
			position.setTranslation(cameraPosition.getTranslation());
			position.move((Vector3f) cameraPosition.getFrontVector().negate().scale(5));
		}
	}
	
	private void dropModel(DropTargetEvent event) throws FileNotFoundException, CoreException {
		ResourceController.getOrCreate().setModelPath(sceneController.getModelFolder().getLocation().toString() + "/");
		File f = null;
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			String[] fileNames = (String[]) event.data;
			String fileName = fileNames[0];
			sceneEditor.makeDirty("add Model");
			f = new File(fileName);
			if (!copyFile(f, sceneController.getModelFolder()))
				return;
		}
		if (ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
			IResource[] resources = (IResource[]) event.data;
			IResource resource = resources[0];
			if (!resource.getProject().equals(sceneController.getProject()))
				return;
			f = resource.getLocation().toFile();
		}
		String shortName = f.getName();
		MultiMesh multiMesh = null;
		if (shortName.endsWith(".smd"))
			multiMesh = MeshLoaderSMD.loadMultiMesh(shortName);
		if (shortName.endsWith(".obj"))
			multiMesh = MeshLoaderOBJ.loadMultiMesh(shortName);
		Object3d object3d = sceneController.getScene().add(multiMesh);
		object3d.setName(shortName.substring(0,shortName.lastIndexOf('.')) + object3d.getID());
		Position position = object3d.getPosition();
		Position cameraPosition = sceneController.getCamera().getPosition();
		position.setTranslation(cameraPosition.getTranslation());
		position.move((Vector3f) cameraPosition.getFrontVector().negate().scale(5));// replace
																					// to
																					// boundingboxsize
	}

	private boolean copyFile(File f,IFolder folder) throws FileNotFoundException, CoreException {
		String shortName = f.getName();
		IFile newFile = folder.getFile(new Path(shortName));
		if (newFile.exists()) {
			MessageDialog dialog = new MessageDialog(null, "Replace", null, "Replace existing file " + shortName + " ?",
					MessageDialog.QUESTION, new String[] { "Yes", "No" }, 0);
			int result = dialog.open();
			if (result == 1)
				return false;
			newFile.setContents(new FileInputStream(f), IFile.FORCE, null);
		} else {
			newFile.create(new FileInputStream(f), true, null);
		}
		return true;
	}

	private void dropTexture(DropTargetEvent event) throws CoreException, IOException {
		ResourceController.getOrCreate()
				.setTexturesPath(sceneController.getTextureFolder().getLocation().toString() + "/");
		File f = null;
		Object3d target = sceneEditor.getObjectFromScreenPoint(event.x, event.y);
		if (target != null) {
			if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
				String[] fileNames = (String[]) event.data;
				String fileName = fileNames[0];
				f = new File(fileName);
				if (!copyFile(f, sceneController.getTextureFolder()))
					return;
			}
			if (ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
				IResource[] resources = (IResource[]) event.data;
				IResource resource = resources[0];
				if (!resource.getProject().equals(sceneController.getProject()))
					return;
				f = resource.getLocation().toFile();
			}
			String shortName = f.getName();
			MultiMesh multiMesh = target.get(MultiMesh.class);
			String materialName = shortName;//.substring(0, shortName.lastIndexOf('.'));
			if (sceneController.getMaterialLibrary().getMaterial(materialName) == null) {
				Material material = new TexturedMaterial(shortName);
				sceneController.getMaterialLibrary().addMaterial(materialName, material);
			}
			if (multiMesh != null) {
				multiMesh.setMaterialNameForAll(materialName);
			}
			sceneEditor.makeDirty("add Texture");
		}
	}

	private DropType getDropType(DropTargetEvent event) {
		if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
			String[] fileNames = (String[]) event.data;
			String fileName = fileNames[0];
			if (fileName.endsWith(".png") || fileName.endsWith(".jpg"))
				return DropType.TEXTURE;
			if (fileName.endsWith(".obj") || fileName.endsWith(".smd"))
				return DropType.MODEL;
		}
		if (ResourceTransfer.getInstance().isSupportedType(event.currentDataType)) {
			IResource[] resources = (IResource[]) event.data;
			IResource resource = resources[0];

			String resourceName = resource.getFileExtension();
			if (resourceName.equals("png") || resourceName.equals("jpg"))
				return DropType.TEXTURE;
			if (resourceName.equals("obj") || resourceName.equals("smd"))
				return DropType.MODEL;
			if (resourceName.equals("java"))
				return DropType.JAVA_CLASS;
			if (resourceName.equals("prefab"))
				return DropType.PREFAB;
		}
		return DropType.UNDEFINED;
	}
}
