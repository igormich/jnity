package materials;

import java.io.Serializable;
import java.util.Collection;

public interface MaterialLibrary extends Serializable{
	Material getMaterial(String materialName);
	Material addMaterial(String s, Material load);
	Collection<String> getMaterialNames();
}
