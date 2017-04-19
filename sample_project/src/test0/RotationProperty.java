package test0;

import java.io.Serializable;

import base.Object3d;
import properties.Property3d;

public class RotationProperty implements Property3d, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2116980322546962002L;

	@Override
	public RotationProperty fastClone() {
		return this;
	}

	@Override
	public void tick(float deltaTime, float time, Object3d owner) {
		owner.getPosition().turn(deltaTime*10);
	}
	
}
