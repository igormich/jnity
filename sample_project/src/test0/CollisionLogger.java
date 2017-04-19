package test0;

import java.io.Serializable;

import base.Object3d;
import properties.Property3d;
import properties.SoundSourse;

public class CollisionLogger implements Property3d, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7018359988085255196L;

	@Override
	public Property3d fastClone() {
		return this;
	}

	@Override
	public void collision(Object3d owner, Object3d otherObject) {
		System.out.println("coolision with "+otherObject);
		owner.get(SoundSourse.class).setPlaying(true);
	}
	

}
