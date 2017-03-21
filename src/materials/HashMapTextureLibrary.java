package materials;

import java.util.HashMap;


public class HashMapTextureLibrary implements TextureLibrary {

	private HashMap<String, Texture> map=new HashMap<String, Texture>();
	@Override
	public Texture getTexture(String materialName) {
		return map.get(materialName);
	}

	@Override
	public Texture addTexture(String s, Texture load) {
		map.put(s, load);
		return load;
	}

	public String[] getMaterialsAsArray() {
		return map.keySet().toArray((new String[map.size()]));
	}

}
