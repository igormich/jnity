package physics;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector3f;

import properties.Mesh;
import properties.MultiMesh;
import properties.Property3d;
import base.Object3d;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class AbstractCollisionBody implements Property3d{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1842281453921179153L;

	public static final Vector3f ZERO_VECTOR = new javax.vecmath.Vector3f(0,0,0);
	
	public static final Vector3f FULL_INERTIA = new javax.vecmath.Vector3f(1,1,1);
	public static final Vector3f NO_INERTIA = ZERO_VECTOR;
	
	
	protected CollisionShape shape = null;
	protected float mass = 0;
	protected float friction = 1;
	protected boolean grost = false;
	protected Vector3f inertia=FULL_INERTIA;
	private boolean gravity = true;
	private Map<Object3d, RigidBody> rigidBody=new HashMap<Object3d,RigidBody>();
	private PhysicController physicController;
	
	@Override
	public boolean isUnique() {
		return true;
	}
	@Override
	public void register(Object3d owner) {
		init(owner);			
	}
	@Override
	public void tick(float deltaTime,float time,Object3d owner) {
		float[] matrix = owner.getPosition().getAbsoluteMatrixAsArray();
		Transform transform = new Transform();
		transform.setFromOpenGLMatrix(matrix);
		rigidBody.get(owner).getWorldTransform(transform);
	}
	public AbstractCollisionBody(PhysicController physicController) {
		this.physicController = physicController;
	}
	public AbstractCollisionBody() {
		this(PhysicController.getDefault());
	}
	public float getMass() {
		return mass;
	}
	public AbstractCollisionBody setMass(float mass) {
		this.mass = mass;
		rigidBody.values().forEach(rb ->rb.setMassProps(mass, inertia));		
		return this;
	}
	public float getFriction() {
		return friction;
	}
	public AbstractCollisionBody setFriction(float friction) {
		this.friction = friction;
		return this;
	}
	public boolean isGrost() {
		return grost;
	}
	public AbstractCollisionBody setGrost(boolean grost) {
		this.grost = grost;
		if(rigidBody!=null)
		{
			if(grost)
				rigidBody.values().forEach(rb ->rb.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE));
			else
				rigidBody.values().forEach(rb ->rb.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT));
		}
		return this;
	}
	public Vector3f getInertia() {
		return inertia;
	}
	public AbstractCollisionBody setInertia(Vector3f inertia) {
		this.inertia = inertia;
		rigidBody.values().forEach(rb ->rb.setMassProps(mass, inertia));	
		return this;
	}
	public CollisionShape getShape() {
		return shape;
	}
	public boolean useGravity() {
		return gravity;
	}
	public AbstractCollisionBody setGravity(boolean gravity) {
		this.gravity = gravity;
		if(rigidBody!=null)
		{
			if(gravity)
				rigidBody.values().forEach(rb ->rb.setGravity(physicController.getGravity()));
		    else
		    	rigidBody.values().forEach(rb ->rb.setGravity(PhysicController.ZERO_GRAVIRY));
		}
		return this;
	}
	protected void init(Object3d owner) {
		rigidBody.put(owner, physicController.addBody(this, owner));
	}
	public void stopLinearVelocity(Object3d owner) {
		rigidBody.get(owner).setLinearVelocity(ZERO_VECTOR);
	}
	public void stopAngularVelocity(Object3d owner) {
		rigidBody.get(owner).setAngularVelocity(ZERO_VECTOR);
	}
	
	public void applyCentralImpulse(Object3d owner,Vector3f impulse) {
		rigidBody.get(owner).applyCentralImpulse(impulse);
	}
	public void applyCentralImpulse(Object3d owner,
			org.lwjgl.util.vector.Vector3f vec) {
		applyCentralImpulse(owner,new Vector3f(vec.x,vec.y,vec.z));
		
		
	}
	public void applyCentralForce(Object3d owner,Vector3f force) {
		rigidBody.get(owner).applyCentralForce(force);
	}
	public void applyCentralForce(Object3d owner,
			org.lwjgl.util.vector.Vector3f vec) {
		applyCentralImpulse(owner,new Vector3f(vec.x,vec.y,vec.z));	
	}
	public org.lwjgl.util.vector.Vector3f getSpeed(Object3d owner){
		Vector3f temp = rigidBody.get(owner).getLinearVelocity(new Vector3f());
		return new org.lwjgl.util.vector.Vector3f(temp.x, temp.y, temp.z);
	}
	@Override
	public Property3d fastClone() {
		return null;
	}
}
