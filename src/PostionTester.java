

import org.lwjgl.util.vector.Matrix4f;

import base.Object3d;
import base.Position;

public class PostionTester {
	  public static void main(String[] args) {
		  Object3d o1= new Object3d();
		  Position p1 = o1.getPosition();
		  p1.move(1,2,3);
		  p1.roll(30);
		  p1.setScale(0.5f);
		  Object3d o2= new Object3d();
		  Position p2 = o2.getPosition();
		  p2.roll(0);
		  p2.turn(0);
		  p2.pitch(45);
		  p2.move(1,2,3);
		  p2.setScale(2);
		 // System.out.println(p2.getAbsoluteMatrix());
		  System.out.println(p1.getAbsoluteMatrix());
		  //p1.rebuildRelativeTo(p2);
		  o2.addChild(o1);
		  System.out.println(Matrix4f.mul(p2.getMatrix(), p1.getMatrix(), new Matrix4f()));
		  
		  System.out.println();
		  System.out.println(p1.getTurn()+ " " + p1.getRoll() +" " + p1.getPitch());
		  System.out.println();
		  
		  Object3d o3= new Object3d();
		  o3.addChild(o1);
		  System.out.println(p1.getAbsoluteMatrix());
		  }
}
