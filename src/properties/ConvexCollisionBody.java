package properties;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.ConvexHullShape;
import com.bulletphysics.util.ObjectArrayList;

import base.Object3d;
import physics.TriangleStrip;

public class ConvexCollisionBody extends MeshCollisionBody {
	
	private static final long serialVersionUID = -8107749273619503327L;

	public void init(TriangleStrip triangleMesh,Object3d owner) {
		ObjectArrayList<Vector3f> data=new ObjectArrayList<Vector3f>(triangleMesh.getSize());
		for(Vector3f v:triangleMesh)
			data.add(v);
		shape = new ConvexHullShape(data);
		super.init(owner);
	}


}
