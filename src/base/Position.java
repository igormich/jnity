package base;

import static org.lwjgl.opengl.GL11.*;

import java.io.Serializable;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

public class Position implements Applyable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9028483454077986045L;
	public static final Vector3f VECTOR_X = new Vector3f(1, 0, 0);
	public static final Vector3f VECTOR_Y = new Vector3f(0, 1, 0);
	public static final Vector3f VECTOR_Z = new Vector3f(0, 0, 1);

	public static float gradToRad(float angle) {
		return (float) (angle * Math.PI / 180);
	}

	public static float radToGrad(float angle) {
		return (float) (angle * 180 / Math.PI);
	}

	public static float dist(Vector3f v1, Vector3f v2) {
		return (float) Math
				.sqrt((v1.x - v2.x) * (v1.x - v2.x) + (v1.y - v2.y) * (v1.y - v2.y) + (v1.z - v2.z) * (v1.z - v2.z));
	}

	private transient boolean cashed = false;
	private transient FloatBuffer cash = BufferUtils.createFloatBuffer(16);

	private float turn, roll, pitch;
	private Vector3f scale = new Vector3f(1, 1, 1);
	private Vector3f position = new Vector3f(0, 0, 0);
	private boolean absolyte;
	private Object3d owner;

	public boolean isAbsolyte() {
		return absolyte;
	}

	public void setAbsolyte(boolean absolyte) {
		this.absolyte = absolyte;
	}

	public Position(Object3d owner) {
		this.owner = owner;
		cache();
	}

	private void cache() {
		if (cash == null)
			cash = BufferUtils.createFloatBuffer(16);
		Matrix4f tempMatrix = getMatrix();
		tempMatrix.store(cash);
		cash.flip();
		cashed = true;
	}

	public Matrix4f getMatrix() {
		Matrix4f tempMatrix = new Matrix4f();
		Matrix4f.setIdentity(tempMatrix);
		tempMatrix = tempMatrix.translate(position);
		toRotationMatrix(tempMatrix);
		//System.out.println(owner +"\n"+ tempMatrix);
		tempMatrix = tempMatrix.scale(scale);
		return tempMatrix;
	}

	public void invalidate() {
		cashed = false;
	}

	public synchronized Position turn(float angle) {
		turn += angle;
		invalidate();
		return this;
	}

	public synchronized Position roll(float angle) {
		roll += angle;
		invalidate();
		return this;
	}

	public synchronized Position pitch(float angle) {
		pitch += angle;
		invalidate();
		return this;
	}

	public synchronized Position setTurn(float angle) {
		turn = angle;
		invalidate();
		return this;
	}

	public synchronized Position setRoll(float angle) {
		roll = angle;
		invalidate();
		return this;
	}

	public synchronized Position setPitch(float angle) {
		pitch = angle;
		invalidate();
		return this;
	}

	public float getPitch() {
		return pitch;
	}

	public float getRoll() {
		return roll;
	}

	public float getTurn() {
		return turn;
	}

	public synchronized Position setTranslation(float x, float y, float z) {
		position.x = x;
		position.y = y;
		position.z = z;
		invalidate();
		return this;
	}

	public Position setTranslation(Vector3f pos) {

		return setTranslation(pos.x, pos.y, pos.z);
	}

	public synchronized Position move(float x, float y, float z) {
		position.x += x;
		position.y += y;
		position.z += z;
		invalidate();
		return this;
	}

	public Position move(Vector3f pos) {
		return move(pos.x, pos.y, pos.z);
	}

	public Vector3f getTranslation() {
		return new Vector3f(position);
	}

	public Position setScale(float x, float y, float z) {
		return setScale(new Vector3f(x, y, z));
	}

	public synchronized Position setScale(Vector3f scale) {
		this.scale = scale;
		invalidate();
		return this;
	}

	public Position setScale(float f) {
		setScale(f, f, f);
		return this;
	}

	public Position mulScale(float f) {
		Vector3f s = getScale();
		setScale(s.x * f, s.y * f, s.z * f);
		return this;
	}

	public Vector3f getScale() {
		return new Vector3f(scale);
	}

	public float gistanceTo(Object3d object) {
		return gistanceTo(object.getPosition());
	}

	public float gistanceTo(Position position) {
		return dist(getAbsoluteTranslation(), position.getAbsoluteTranslation());
	}

	public Vector3f getAbsoluteTranslation() {
		Matrix4f m = getAbsoluteMatrix();
		return new Vector3f(m.m30, m.m31, m.m32);
	}

	public float[] getAbsoluteMatrixAsArray() {
		Matrix4f matrix = getAbsoluteMatrix();
		FloatBuffer buf = BufferUtils.createFloatBuffer(16);
		matrix.store(buf);
		buf.flip();
		float[] dst = new float[16];
		buf.get(dst, 0, 15);
		return dst;
	}

	public synchronized void setAbsoluteMatrixAsArray(float[] array) {
		throw new UnsupportedOperationException();
	}

	public Matrix4f getAbsoluteMatrix() {
		if (isAbsolyte()) {
			return getMatrix();
		}
		if (owner == null)
			return getMatrix();
		Object3d parent = owner.getParent();
		if (parent == null)
			return getMatrix();
		Matrix4f m2 = parent.getPosition().getAbsoluteMatrix();
		Matrix4f result = new Matrix4f();
		Matrix4f.mul(m2, getMatrix(), result);
		return result;
	}

	public Object3d getOwner() {
		return owner;
	}

	@Override
	public synchronized void apply() {
		if (!isCashed())
			cache();
		glPushMatrix();
		if (isAbsolyte())
			glLoadIdentity();
		glMultMatrix(cash);
	}

	public synchronized void applyAbsolyte() {
		if (!isCashed())
			cache();
		glPushMatrix();
		glLoadIdentity();
		if (isAbsolyte()) {
			glMultMatrix(cash);
		} else {
			Matrix4f matrix = getAbsoluteMatrix();
			FloatBuffer data = BufferUtils.createFloatBuffer(16);
			matrix.store(data);
			data.flip();
			glMultMatrix(data);
		}
	}

	@Override
	public void unApply() {
		glPopMatrix();
	}

	public void loadFromOpenGL() {
		absolyte = true;
		cashed = true;
		glGetFloat(GL_MODELVIEW_MATRIX, cash);
	}

	// need fix
	public Vector3f getLeftVector() {
		Matrix4f matrix = getMatrix();
		return new Vector3f(matrix.m20, matrix.m21, matrix.m22);
	}

	// need fix
	public Vector3f getUpVector() {
		Matrix4f matrix = getMatrix();
		return new Vector3f(matrix.m10, matrix.m11, matrix.m12);
	}

	// need fix
	public Vector3f getFrontVector() {
		// left
		Matrix4f matrix = getMatrix();
		return new Vector3f(matrix.m00, matrix.m01, matrix.m02);
	}

	// need fix
	public Vector3f getAbsoluteFrontVector() {
		Matrix4f matrix = getAbsoluteMatrix();
		return new Vector3f(new Vector3f(matrix.m01, -matrix.m11, matrix.m21));
	}

	public boolean isCashed() {
		return cashed && cash != null;
	}

	public Position fastClone(Object3d owner) {
		Position result = new Position(owner);
		result.position = new Vector3f(position);
		result.turn = turn;
		result.roll = roll;
		result.pitch = pitch;
		result.scale = new Vector3f(scale);
		result.absolyte = absolyte;
		return result;
	}

	public Vector3f getEulerAngles() {
		return new Vector3f(turn, roll, pitch);
	}

	public Position setEulerAngles(float turn, float roll, float pitch) {
		this.turn = turn;
		this.roll = roll;
		this.pitch = pitch;
		invalidate();
		return this;
	}

	public Position setEulerAngles(Vector3f angles) {
		return setEulerAngles(angles.x, angles.y, angles.z);
	}

	public Matrix4f toRotationMatrix(Matrix4f dest) {
		float pitch = gradToRad(this.turn) / 2;
		float yaw = gradToRad(this.roll) / 2;
		float roll = gradToRad(this.pitch) / 2;

		float sinp = (float) Math.sin(pitch);
		float siny = (float) Math.sin(yaw);
		float sinr = (float) Math.sin(roll);
		float cosp = (float) Math.cos(pitch);
		float cosy = (float) Math.cos(yaw);
		float cosr = (float) Math.cos(roll);

		Quaternion q = new Quaternion();
		q.x = sinr * cosp * cosy - cosr * sinp * siny;
		q.y = cosr * sinp * cosy + sinr * cosp * siny;
		q.z = cosr * cosp * siny - sinr * sinp * cosy;
		q.w = cosr * cosp * cosy + sinr * sinp * siny;

		q.normalise();
		if (dest == null)
			dest = new Matrix4f();

		// The length of the quaternion
		float s = 2f / q.length();
		// Convert the quaternion to matrix
		dest.m00 = 1 - s * (q.y * q.y + q.z * q.z);
		dest.m10 = s * (q.x * q.y + q.w * q.z);
		dest.m20 = s * (q.x * q.z - q.w * q.y);

		dest.m01 = s * (q.x * q.y - q.w * q.z);
		dest.m11 = 1 - s * (q.x * q.x + q.z * q.z);
		dest.m21 = s * (q.y * q.z + q.w * q.x);

		dest.m02 = s * (q.x * q.z + q.w * q.y);
		dest.m12 = s * (q.y * q.z - q.w * q.x);
		dest.m22 = 1 - s * (q.x * q.x + q.y * q.y);

		return dest;
	}

	public Position rebuildRelativeTo(Position parent) {
		Matrix4f absoluteMatrix = getAbsoluteMatrix();
		Matrix4f parentAbsoluteMatrix = parent.getAbsoluteMatrix();
		Matrix4f matrix = Matrix4f.mul((Matrix4f)parentAbsoluteMatrix.invert(), absoluteMatrix, new Matrix4f());
		matrix.store(cash);
		cash.flip();
		cashed = true;
		this.setMatrix(matrix);
		return this;
	}
	public Vector3f getEulerAnglesZYX(Matrix4f m) {
		Vector3f dest = new Vector3f();
        dest.x = -(float) Math.atan2(m.m12, m.m22);
        dest.y = -(float) Math.atan2(-m.m02, (float) Math.sqrt(m.m12 * m.m12 + m.m22 * m.m22));
        dest.z = -(float) Math.atan2(m.m01, m.m00);
        return dest;
    }
	private void setMatrix(Matrix4f matrix) {
		position.x = matrix.m30;
		position.y = matrix.m31;
		position.z = matrix.m32;

        Vector3f angles = getEulerAnglesZYX(matrix);
		
		this.turn = radToGrad(angles.y);
		this.pitch = radToGrad(angles.x);
		this.roll = radToGrad(angles.z);
		

		scale = new Vector3f(new Vector3f(matrix.m00, matrix.m10, matrix.m20).length(),
							 new Vector3f(matrix.m01, matrix.m11, matrix.m21).length(),
							 new Vector3f(matrix.m02, matrix.m12, matrix.m22).length());
		
		invalidate();
		
	}
}
