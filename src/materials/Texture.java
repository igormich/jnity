package materials;

import static org.lwjgl.opengl.GL11.glDeleteTextures;

import base.Applyable;

public interface Texture extends Applyable{
	
	public static void releaseTexture(int id){
		glDeleteTextures(id);
	}
	
	boolean isTransparent();
	void applyAs(int i);

	String getFileName();
}
