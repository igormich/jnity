package jnity.views.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;

import base.Object3d;
import jnity.views.SceneController;
import materials.Material;
import properties.Property3d;

public class SceneResourseController implements IResourceChangeListener {

	private Map<String, String> materials = new HashMap<>();
	private Map<String, Class<? extends Property3d>> properties = new HashMap<>();
	private SceneController sceneController;

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		scan(event.getDelta());
	}

	private void scan(IResourceDelta delta) {
		try {
			IResource resource = delta.getResource();
			if (resource.getProject() != sceneController.getProject())
				return;
			if (delta.getKind() == IResourceDelta.CHANGED)
				System.out.println("CHANGED");
			;
			if (materials.containsKey(resource.getProjectRelativePath().toString())) {
				String materialName = materials.get(resource);
				@SuppressWarnings("unchecked")
				Class<? extends Material> clazz = (Class<? extends Material>) sceneController.getClassFrom(resource);
				sceneController.getScene().getMaterialLibrary().addMaterial(materialName, clazz.newInstance());
			} else if (properties.containsKey(resource.getProjectRelativePath().toString())) {
				Class<? extends Property3d> oldClazz = properties.get(resource);
				@SuppressWarnings("unchecked")
				Class<? extends Property3d> clazz = (Class<? extends Property3d>) sceneController.getClassFrom(resource);
				replaceProperty(sceneController.getScene().getRoot(),oldClazz,clazz);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		for (IResourceDelta resourceDelta : delta.getAffectedChildren()) {
			scan(resourceDelta);
		}

	}

	private void replaceProperty(Object3d object3d, Class<? extends Property3d> oldClazz,
			Class<? extends Property3d> clazz) {
		Property3d property3d = null;
		do {
			property3d = object3d.get(oldClazz);
			if(property3d!=null) {
				object3d.remove(property3d);
				//add fields copy (by reflection?)
				try {
					object3d.add(clazz.newInstance());
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
		}while(property3d!=null);
		for(Object3d child:object3d.getChildren())
			replaceProperty(child, oldClazz, clazz);
	}

	public void registerMaterial(String path, String materialName) {
		materials.put(path, materialName);
	}

	public void registerProperty(String path, Class<? extends Property3d> property) {
		properties.put(path, property);
	}

	public void setSceneController(SceneController sceneController) {
		this.sceneController = sceneController;
	}

}
