package base;

import org.lwjgl.util.vector.Vector3f;

import materials.Material;
import properties.Property3d;

public interface RenderContex {
	boolean useMaterial();
	boolean selectMode();
	boolean skipTransparent();
	boolean storeTransparent();
	void store(Property3d transparentObject, Object3d owner);
	float isVisible(Vector3f absoluteTranslation);
	Camera getCamera();
	Material getMaterial(String materialName);
}
