package properties;

import com.bulletphysics.collision.shapes.CapsuleShape;

import physics.AbstractCollisionBody;

public class CapsuleCollisionBody extends AbstractCollisionBody {
	
	private static final long serialVersionUID = -8107749273619503327L;
	private CapsuleShape capsuleShape;

	public CapsuleCollisionBody(float radius, float height) {
		super();
		capsuleShape = new CapsuleShape(radius, height);
		shape = capsuleShape;
	}

}
