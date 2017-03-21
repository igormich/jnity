package materials;

public interface TextureLibrary {

	Texture getTexture(String materialName);

	Texture addTexture(String s, Texture load);

}
