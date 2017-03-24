package physics;

import java.io.Serializable;

import javax.vecmath.Vector3f;

import com.bulletphysics.collision.dispatch.CollisionFlags;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import base.Object3d;
import properties.Property3d;

public abstract class AbstractCollisionBody implements Property3d, Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1842281453921179153L;

	public static final Vector3f ZERO_VECTOR = new javax.vecmath.Vector3f(0,0,0);
	
	public static final Vector3f FULL_INERTIA = new javax.vecmath.Vector3f(1,1,1);
	public static final Vector3f NO_INERTIA = ZERO_VECTOR;
	
	
	protected transient CollisionShape shape = null;
	protected float mass = 0;
	protected float friction = 1;
	protected boolean grost = false;
	protected Vector3f inertia=FULL_INERTIA;
	private boolean gravity = true;
	private transient RigidBody rigidBody;
	
	@Override
	public boolean isUnique() {
		return true;
	}
	@Override
	public void register(Object3d owner) {
		init(owner);			
	}
	@Override
	public void unRegister(Object3d owner) {
		getPhysicController().removeBody(owner);		
	}
	@Override
	public void tick(float deltaTime,float time,Object3d owner) {
		float[] matrix = owner.getPosition().getAbsoluteMatrixAsArray();
		Transform transform = new Transform();
		transform.setFromOpenGLMatrix(matrix);
		rigidBody.getWorldTransform(transform);
	}
	public AbstractCollisionBody() {
	}
	public float getMass() {
		return mass;
	}
	public AbstractCollisionBody setMass(float mass) {
		this.mass = mass;
		if(rigidBody!=null)
			rigidBody.setMassProps(mass, inertia);		
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
				rigidBody.setCollisionFlags(CollisionFlags.NO_CONTACT_RESPONSE);
			else
				rigidBody.setCollisionFlags(CollisionFlags.KINEMATIC_OBJECT);
		}
		return this;
	}
	public Vector3f getInertia() {
		return inertia;
	}
	public AbstractCollisionBody setInertia(Vector3f inertia) {
		this.inertia = inertia;
		if(rigidBody!=null)
			rigidBody.setMassProps(mass, inertia);	
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
				rigidBody.setGravity(getPhysicController().getGravity());
		    else
		    	rigidBody.setGravity(PhysicController.ZERO_GRAVIRY);
		}
		return this;
	}
	private PhysicController getPhysicController() {
		return PhysicController.getDefault();
	}
	protected void init(Object3d owner) {
		rigidBody = getPhysicController().addBody(this, owner);
	}
	public void stopLinearVelocity() {
		if(rigidBody!=null)
			rigidBody.setLinearVelocity(ZERO_VECTOR);
	}
	public void stopAngularVelocity() {
		if(rigidBody!=null)
			rigidBody.setAngularVelocity(ZERO_VECTOR);
	}
	
	public void applyCentralImpulse(Vector3f impulse) {
		if(rigidBody!=null)
			rigidBody.applyCentralImpulse(impulse);
	}
	public void applyCentralImpulse(Object3d owner,
			org.lwjgl.util.vector.Vector3f vec) {
		applyCentralImpulse(new Vector3f(vec.x,vec.y,vec.z));
		
		
	}
	public void applyCentralForce(Vector3f force) {
		if(rigidBody!=null)
			rigidBody.applyCentralForce(force);
	}
	public void applyCentralForce(Object3d owner,
			org.lwjgl.util.vector.Vector3f vec) {
		applyCentralImpulse(new Vector3f(vec.x,vec.y,vec.z));	
	}
	public org.lwjgl.util.vector.Vector3f getSpeed(){
		Vector3f temp = rigidBody.getLinearVelocity(new Vector3f());
		return new org.lwjgl.util.vector.Vector3f(temp.x, temp.y, temp.z);
	}
	@Override
	public Property3d fastClone() {
		return this;
	}
}
