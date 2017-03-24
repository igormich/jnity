package properties;

import static org.lwjgl.opengl.GL11.GL_COMPILE;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glCallList;
import static org.lwjgl.opengl.GL11.glColor3f;
import static org.lwjgl.opengl.GL11.glDeleteLists;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glEndList;
import static org.lwjgl.opengl.GL11.glNewList;
import static org.lwjgl.opengl.GL11.glNormal3f;
import static org.lwjgl.opengl.GL11.glTexCoord3f;
import static org.lwjgl.opengl.GL11.glVertex3f;

import java.util.ArrayList;
import java.util.Iterator;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.bulletphysics.util.ObjectArrayList;

import base.Object3d;
import base.RenderContex;

public class Mesh extends AbstaractMesh {

	private static int globalId = 1;
	private ArrayList<Vector3f> vert = new ArrayList<Vector3f>();
	private ArrayList<Vector3f> normals = new ArrayList<Vector3f>();
	private ArrayList<Vector3f> color = new ArrayList<Vector3f>();
	private ArrayList<Vector3f> tex = new ArrayList<Vector3f>();
	private int renderParts = ALL;
	protected int listId = -1;
	boolean isPrepared = false;

	public static void releaseMesh(int listId) {
		glDeleteLists(listId, 1);
	}
	
	@Override
	public void render(RenderContex renderContex, Object3d owner) {
		applyMaterial(renderContex, owner);
		renderMesh();
		unApplyMaterial(renderContex, owner);
	}

	private void renderMesh() {
		if (!isPrepared)
			prepare();
		glCallList(listId);
	}

	private static int getId() {
		return globalId++;
	}

	public ObjectArrayList<Vector3f> trinaglesList() {
		ObjectArrayList<Vector3f> result = new ObjectArrayList<Vector3f>();
		for (Vector3f v : vert) {
			result.add(v);
		}
		return result;
	}

	public void add(Vector3f v, Vector3f n, Vector3f c, Vector3f t) {
		if (v != null)
			vert.add(v);
		if (n != null)
			normals.add(n);
		if (c != null)
			color.add(c);
		if (t != null)
			tex.add(t);
		isPrepared = false;
	}

	public void add(Vector3f v, Vector3f n, Vector3f c, Vector2f t) {
		add(v, n, c, new Vector3f(t.x, t.y, 0));
	}

	protected void renderPoint(int i) {
		if ((renderParts & TEXTURE) > 0)
			glTexCoord3f(tex.get(i).x, tex.get(i).y, tex.get(i).z);
		if ((renderParts & NORMAL) > 0)
			glNormal3f(normals.get(i).x, normals.get(i).y, normals.get(i).z);
		if ((renderParts & COLOR) > 0)
			glColor3f(color.get(i).x, color.get(i).y, color.get(i).z);
		glVertex3f(vert.get(i).x, vert.get(i).y, vert.get(i).z);
	}

	public void prepare() {
		if (listId > -1)
			glDeleteLists(listId, 1);
		listId = getId();
		glNewList(listId, GL_COMPILE);
		glBegin(GL_TRIANGLES);
		for (int i = 0; i < vert.size(); i++) {
			renderPoint(i);
		}
		glEnd();
		glEndList();
		/*vert.clear();
		color.clear();
		normals.clear();
		tex.clear();*/
		isPrepared = true;
	}

	public void setRenderParts(int renderParts) {
		this.renderParts = renderParts;
	}

	@Override
	public Iterator<Vector3f> iterator() {
		return vert.iterator();
	}

	@Override
	public int getSize() {
		return vert.size();
	}

	@Override
	public Mesh fastClone() {
		Mesh result = new Mesh();
		fastClone(result);
		result.listId = listId;
		result.isPrepared = isPrepared;
		result.renderParts = renderParts;
		result.vert = vert;
		result.tex = tex;
		result.normals = normals;
		result.color = color;
		return result;
	}
}
