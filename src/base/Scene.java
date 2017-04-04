package base;

import static org.lwjgl.opengl.EXTFramebufferObject.GL_FRAMEBUFFER_EXT;
import static org.lwjgl.opengl.EXTFramebufferObject.glBindFramebufferEXT;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_COMPONENT;
import static org.lwjgl.opengl.GL11.GL_MODELVIEW_MATRIX;
import static org.lwjgl.opengl.GL11.GL_RGB;
import static org.lwjgl.opengl.GL11.GL_STENCIL_TEST;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glGetFloat;
import static org.lwjgl.opengl.GL11.glLoadIdentity;
import static org.lwjgl.opengl.GL11.glMultMatrix;
import static org.lwjgl.opengl.GL11.glReadPixels;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.opengl.GL30.GL_COLOR_ATTACHMENT0;
import static org.lwjgl.opengl.GL30.GL_DEPTH_ATTACHMENT;
import static org.lwjgl.opengl.GL30.GL_FRAMEBUFFER;
import static org.lwjgl.opengl.GL30.GL_RENDERBUFFER;
import static org.lwjgl.opengl.GL30.glBindFramebuffer;
import static org.lwjgl.opengl.GL30.glBindRenderbuffer;
import static org.lwjgl.opengl.GL30.glDeleteFramebuffers;
import static org.lwjgl.opengl.GL30.glDeleteRenderbuffers;
import static org.lwjgl.opengl.GL30.glFramebufferRenderbuffer;
import static org.lwjgl.opengl.GL30.glFramebufferTexture2D;
import static org.lwjgl.opengl.GL30.glGenFramebuffers;
import static org.lwjgl.opengl.GL30.glGenRenderbuffers;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.opengl.GL30.glRenderbufferStorage;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

import materials.HashMapMaterialLibrary;
import materials.Material;
import materials.MaterialLibrary;
import materials.SimpleMaterial;
import materials.Texture2D;
import properties.Property3d;

public class Scene implements Externalizable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1385416712044074843L;
	private class SceneRenderContex implements RenderContex {
		private boolean useMaterial=true;
		private boolean skipTransparent=true;
		private boolean selectMode;
		private Camera camera;
		private Vector3f camDir;
		private Vector3f camPos;
		private Material defaultMaterial = new SimpleMaterial();
		
		@Override
		public boolean useMaterial() {
			return useMaterial;
		}

		@Override
		public boolean skipTransparent() {
			return skipTransparent;
		}

		@Override
		public boolean storeTransparent() {
			return skipTransparent;
		}

		@Override
		public void store(Property3d transparentObject,Object3d owner) {
			if(!storeTransparent())
				return;		
			Scene.this.storeTransparent(transparentObject,owner);
		}

		@SuppressWarnings("unused")
		public void setUseMaterial(boolean useMaterial) {
			this.useMaterial = useMaterial;
		}

		public void setSkipTransparent(boolean skipTransparent) {
			this.skipTransparent = skipTransparent;
		}
		@Override
		public boolean selectMode() {
			return selectMode;
		}

		@SuppressWarnings("unused")
		public void setSelectMode(boolean selectMode) {
			this.selectMode = selectMode;
		}
		@Override
		public Camera getCamera() {
			return camera;
		}

		public void setCamera(Camera camera) {
			this.camera = camera;
			Vector3f camPos0 = camera.getPosition().getAbsoluteTranslation();
			camDir = camera.getPosition().getAbsoluteFrontVector();
			camDir.y = -camDir.x;
			camDir.x = camDir.z;
			camDir.z=0;
			float shift = 3;
			camPos = new Vector3f(camPos0.x-camDir.x*shift ,
								  camPos0.y-camDir.y*shift,
								  camPos0.z-camDir.z*shift);
		}
		@Override
		public Material getMaterial(String materialName) {
			Material result = null;
			if (getMaterialLibrary() != null)
				result = getMaterialLibrary().getMaterial(materialName);
			if (result == null)
				result = defaultMaterial;
			return result;
		}
		@Override
		public float isVisible(Vector3f absoluteTranslation) {
			Vector3f objcetDir = new Vector3f(absoluteTranslation.x-camPos.x,
											  absoluteTranslation.y-camPos.y,
											  0).normalise(null);
			objcetDir.z=0;
			if(Vector3f.dot(camDir, objcetDir)>0.6)
				return Vector3f.sub(absoluteTranslation, camPos, null).length();
			else
				return Float.MAX_VALUE;
		}
		

	}

	private static class TransparentContainer implements Comparable<TransparentContainer>{

		private Property3d transparentObject;
		private Object3d owner;
		private FloatBuffer positionBuffer;
		private Vector3f translation;
		private float distance;
		
		public TransparentContainer(Property3d transparentObject, Object3d owner) {
			this.transparentObject = transparentObject;
			this.owner = owner;
			positionBuffer=BufferUtils.createFloatBuffer(16);
			glGetFloat(GL_MODELVIEW_MATRIX,positionBuffer);
			translation=new Vector3f(positionBuffer.get(12),positionBuffer.get(13),positionBuffer.get(14));
		}

		public void calcDistanceTo(Vector3f cameraTranslation) {
			distance=Position.dist(translation, cameraTranslation);
		}

		@Override
		public int compareTo(TransparentContainer arg0) {
			return Float.compare(arg0.distance,distance);
		}
		
	}
	private Object3d root=new Object3d();
	private transient SceneRenderContex renderContex=new SceneRenderContex();
	private transient List<TransparentContainer> transparentPool=new ArrayList<TransparentContainer>();
	private float time;
	private Vector3f backColor = new Vector3f();
	private MaterialLibrary materialLibrary = new HashMapMaterialLibrary();
	
	public Object3d add(Object3d object3d){
		Object3d parent = object3d.getParent();
		if ((parent == null) || !root.contains(parent))
			return root.addChild(object3d);
		return object3d;
	}
	public void remove(Object3d object3d){
		root.removeChild(object3d);
	}
	public void render(Camera camera){
		glViewport (0, 0, camera.width, camera.height);
		glClearColor (backColor.x, backColor.y, backColor.z, 1.0f);	
		glClear (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity();	
		renderObjects(camera);
	}
	private void renderObjects(Camera camera) {
		camera.apply();
		renderContex.setCamera(camera);
		renderContex.setSkipTransparent(true);
		root.render(renderContex);
		renderContex.setSkipTransparent(false);
		renderTransparent(camera);
		transparentPool.clear();		
	}
	private void renderTransparent(Camera camera) {
		Vector3f cameraTranslation = camera.getPosition().getAbsoluteTranslation();
		transparentPool.stream().peek(tc -> tc.calcDistanceTo(cameraTranslation)).sorted().forEach(tc -> {
			glLoadIdentity();
			glMultMatrix(tc.positionBuffer);
			tc.transparentObject.render(renderContex, tc.owner);
		});
	}
	private void storeTransparent(Property3d transparentObject,
			Object3d owner) {
		transparentPool.add(new TransparentContainer(transparentObject,owner));
	}
	public Object3d add(Property3d ... properties) {
		Object3d result=new Object3d();
		for(Property3d property:properties)
			result.add(property);
		add(result);
		return result;		
	}
	public void tick(float deltaTime) {
		time+=deltaTime;
		root.tick(deltaTime, time);
	}
	public Object3d getByID(int id) {
		return root.getByID(id);
	}
	public void renderToTexture(Camera camera, Texture2D screenShot) {  

		int rboId = glGenRenderbuffers();
		glBindRenderbuffer(GL_RENDERBUFFER, rboId);
		glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT,screenShot.getWidth(), screenShot.getHeight());
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
		int fbo=glGenFramebuffers();             //Генерируем буфер
		glBindFramebuffer(GL_FRAMEBUFFER, fbo);

		glViewport (0, 0, screenShot.getWidth(), screenShot.getHeight());
		glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0,  GL_TEXTURE_2D, screenShot.getID() , 0);
		glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT,  GL_RENDERBUFFER, rboId );
		glClearColor (backColor.x, backColor.y, backColor.z, 1.0f);	
		glDisable(GL_STENCIL_TEST);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity ();	//*/
        renderObjects(camera);
        glEnable(GL_TEXTURE_2D);                                        // enable texturing
        glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0); 
        glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, screenShot.getID());
		glGenerateMipmap(GL_TEXTURE_2D);
		glDisable(GL_TEXTURE_2D);
		glBindFramebuffer(GL_FRAMEBUFFER, 0);     //Деактивируем
		glDeleteFramebuffers(fbo);         //Удаляем	*/
		glBindRenderbuffer(GL_RENDERBUFFER, 0);
		glDeleteRenderbuffers(rboId);
		
	}
	public Vector3f getBackColor() {
		return backColor;
	}
	public void setBackColor(Vector3f backColor) {
		this.backColor = backColor;
	}
	public Object3d getObject(int x, int y,Camera camera) {
		glViewport (0, 0, camera.width, camera.height);
		glClearColor (0.0f, 0.0f, 0.0f, 1.0f);	
		glClear (GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glLoadIdentity ();
		
		camera.apply();
		renderContex.setCamera(camera);
		renderContex.useMaterial = false;
		renderContex.selectMode = true;
		root.render(renderContex);
		ByteBuffer buffer = BufferUtils.createByteBuffer(3);
		glReadPixels(x,y,1,1,GL_RGB,GL_UNSIGNED_BYTE, buffer);
		int r = buffer.get();
		int g = buffer.get();
		int b = buffer.get();
		renderContex.useMaterial = true;
		renderContex.selectMode = false;
		//return null;	
		int id=r + g * 255 + b * 255 * 255;
		return getByID(id);
	}
	public void renderSingle(Camera camera, Object3d selected, boolean useMaterial) {
		glViewport (0, 0, camera.width, camera.height);
		glLoadIdentity ();	
		camera.apply();
		renderContex.useMaterial = useMaterial;
		selected.renderSingle(renderContex);
		renderContex.useMaterial = true;
	}
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeFloat(time);
		out.writeObject(backColor);
		out.writeObject(root);
		out.writeObject(materialLibrary);
		out.writeInt(Object3d.idCounter.get());
	}
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		time = in.readFloat();
		backColor = (Vector3f) in.readObject();
		root = (Object3d) in.readObject();
		materialLibrary = (MaterialLibrary) in.readObject();
		Object3d.idCounter.set(in.readInt()+1);
		root.postLoad();
	}
	public void setMaterialLibrary(MaterialLibrary materialLibrary) {
		this.materialLibrary = materialLibrary;
	}
	public MaterialLibrary getMaterialLibrary() {
		return materialLibrary;
	}
	public Object3d getRoot() {
		return root;
	}
}
