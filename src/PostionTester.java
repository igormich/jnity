import org.lwjgl.util.vector.Matrix4f;

import base.Position;

public class PostionTester {
	  public static void main(String[] args) {
		  Position p1 = new Position(null);
		  p1.move(1,2,3);
		  Position p2 = new Position(null);
		  p2.roll(45);
		  p2.move(1,2,3);
		  
		 // System.out.println(p2.getAbsoluteMatrix());
		  Matrix4f m = p1.rebuildRelativeTo(p2);
		 // System.out.println(m);
		  System.out.println(p1.getAbsoluteMatrix());
		  System.out.println(Matrix4f.mul(p2.getAbsoluteMatrix(), m, new Matrix4f()));
		  }
}
