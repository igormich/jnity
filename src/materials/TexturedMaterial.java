package materials;

import static org.lwjgl.opengl.GL11.GL_CLAMP;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glTexParameteri;

import base.Object3d;
import base.RenderContex;
import io.ResourceController;

public class TexturedMaterial extends SimpleMaterial {

	private static final long serialVersionUID = -6662677082519463646L;
	private String fileName = "";
	private boolean repeatW = true;
	private boolean repeatH = true;

	public TexturedMaterial() {
	}
	public TexturedMaterial(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public boolean isTransparent() {
		return ResourceController.getOrCreate().getOrLoadTexture(fileName).isTransparent();
	}

	@Override
	public void apply(RenderContex renderContex, Object3d owner) {
		super.apply(renderContex, owner);
		ResourceController.getOrCreate().getOrLoadTexture(fileName).apply();
		if(repeatW)
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		else
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP);
		if(repeatH)
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		else	
			glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP);
	}

	@Override
	public void unApply() {
		ResourceController.getOrCreate().getOrLoadTexture(fileName).unApply();
	}


}
