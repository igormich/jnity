package properties;

import base.Object3d;

public class CollisionSound extends SoundSourse{

	@Override
	public void collision(Object3d owner, Object3d otherObject) {
		super.collision(owner, otherObject);
		play();
	}

}
