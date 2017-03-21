package properties;

import base.Object3d;
import base.RenderContex;
import materials.Material;
import materials.Textured;
import physics.TriangleStrip;

public abstract class AbstaractMesh implements Property3d, Textured, TriangleStrip {
	
	public static final int NONE=0;
	public static final int TEXTURE=1;
	public static final int NORMAL=2;
	public static final int COLOR=4;
	public static final int ALL=TEXTURE+NORMAL+COLOR;
	
	private String materialName;

	@Override
	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	@Override
	public String getMaterialName() {
		return materialName;
	}
	protected <T extends AbstaractMesh> void fastClone(T abstractMesh) {
		abstractMesh.setMaterialName(materialName);
	}	
	
	protected void applyMaterial(RenderContex renderContex, Object3d owner) {
		Material material = renderContex.getMaterial(getMaterialName());
		if(material!=null)
		{
			if(renderContex.storeTransparent()&&(material.isTransparent()))
				renderContex.store(this, owner);
			if(renderContex.skipTransparent()&&(material.isTransparent()))
				return;
			if(renderContex.useMaterial())
			{
				material.apply(renderContex, owner);
			}
		}
	}
	
	protected void unApplyMaterial(RenderContex renderContex, Object3d owner) {
		Material material = renderContex.getMaterial(getMaterialName());
		if(renderContex.useMaterial() && material!=null)
		{
			material.unApply();
		}
	}
}
