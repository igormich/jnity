package materials;

import java.util.Collection;
import java.util.HashMap;


public class HashMapMaterialLibrary implements MaterialLibrary {

	private static final long serialVersionUID = 5908318929747348922L;
	private HashMap<String, Material> map=new HashMap<String, Material>();
	@Override
	public Material getMaterial(String materialName) {
		return map.get(materialName);
	}

	@Override
	public Material addMaterial(String s, Material load) {
		map.put(s, load);
		return load;
	}

	public String[] getMaterialsAsArray() {
		return map.keySet().toArray((new String[map.size()]));
	}
	public Collection<String> getMaterialNames() {
		return map.keySet();
	}

}
