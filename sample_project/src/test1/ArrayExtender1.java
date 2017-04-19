package test1;

import java.io.Serializable;

import base.Object3d;
import properties.MultiMesh;
import properties.Property3d;

public class ArrayExtender1 implements Property3d, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2116980322546962002L;

	@Override
	public ArrayExtender1 fastClone() {
		return this;
	}

	@Override
	public void register(Object3d owner) {
		Object3d object3d = new Object3d(owner.get(MultiMesh.class));
		object3d.resetPosition().setTranslation(0, 0, 5);
		owner.addChild(object3d);
	}

	@Override
	public void tick(float deltaTime, float time, Object3d owner) {
		//owner.getPosition().turn(-deltaTime*10);
	}
	
}
