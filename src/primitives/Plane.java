package primitives;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import properties.Mesh;

public class Plane {

	public static Mesh build(float w, float h) {
		Mesh result = new Mesh();
		result.setRenderParts(Mesh.TEXTURE + Mesh.NORMAL + Mesh.COLOR);
		
		addPart(0f,0f,result,w,h);
		
		return result;
	}

	private static void addPart(float posX, float posY, Mesh result,float w,float h) {
		Vector3f red =new Vector3f(1, 0, 0);
		Vector3f green =new Vector3f(0, 1, 0);
		Vector3f blue =new Vector3f(0, 0, 1);
		Vector3f yellow =new Vector3f(1, 1, 0);
		
		Vector3f centerV=new Vector3f(posX+0.0f, 0, posY+0.0f);
		Vector3f leftDownV=new Vector3f(posX-w/2, 0, posY-h/2);
		Vector3f rightDownV=new Vector3f(posX+w/2, 0, posY-h/2);
		Vector3f leftUpV=new Vector3f(posX-w/2, 0, posY+h/2);
		Vector3f rightUpV=new Vector3f(posX+w/2, 0, posY+h/2);
		
		Vector3f normal = new Vector3f(0, 1, 0);
		/*Vector3f centerV=new Vector3f(0.0f, 0, 0.0f);
		Vector3f leftDownV=new Vector3f(-0.5f, -0.5f, 0);
		Vector3f rightDownV=new Vector3f(0.5f, -0.5f, 0);
		Vector3f leftUpV=new Vector3f(-0.5f, -0.5f, 0);
		Vector3f rightUpV=new Vector3f(0.5f, -0.5f, 0);
		
		Vector3f normal = new Vector3f(0, 0, 1);//*/
		
		result.add(leftDownV, normal, red , new Vector2f(0, 0));
		result.add(centerV, normal, red, new Vector2f(0.5f, 0.5f));
		result.add(rightDownV, normal, red, new Vector2f(1, 0));
		
		result.add(leftUpV, normal, green, new Vector2f(0, 1));
		result.add(centerV, normal, green, new Vector2f(0.5f, 0.5f));
		result.add(leftDownV, normal, green, new Vector2f(0, 0));
		
		result.add(rightUpV, normal, blue , new Vector2f(1, 1));
		result.add(centerV, normal, blue, new Vector2f(0.5f, 0.5f));
		result.add(leftUpV, normal, blue, new Vector2f(0, 1));
		
		result.add(rightDownV, normal, yellow, new Vector2f(1, 0));
		result.add(centerV, normal, yellow, new Vector2f(0.5f, 0.5f));
		result.add(rightUpV, normal, yellow, new Vector2f(1, 1));	
	}

	public static Mesh buildMultiPlane(int w,int h) {
		Mesh result = new Mesh();
		result.setRenderParts(Mesh.TEXTURE + Mesh.NORMAL);
		for(int x=0;x<w;x++)
			for(int y=0;y<h;y++)
				addPart(x-w/2f,y-h/2f,result,1,1);
		
		return result;
	}

}
