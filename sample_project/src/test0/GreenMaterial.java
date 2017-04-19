package test0;

import base.Object3d;
import base.RenderContex;
import io.ResourceController;
import materials.SimpleMaterial;

public class GreenMaterial extends SimpleMaterial {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4762925173854982380L;

	public GreenMaterial() {
		super(1,0,0);	
	}

	@Override
	public void apply(RenderContex renderContex, Object3d owner) {
		super.apply(renderContex, owner);
		ResourceController.getOrCreate().getOrLoadTexture("stone0.jpg").apply();
	}


}
