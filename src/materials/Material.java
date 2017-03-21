package materials;

import java.io.Serializable;

import base.Object3d;
import base.RenderContex;



public interface Material extends Serializable{
	boolean isTransparent();
	void apply(RenderContex renderContex, Object3d owner);
	void unApply();
}
