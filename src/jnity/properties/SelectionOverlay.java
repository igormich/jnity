package jnity.properties;

import static org.lwjgl.opengl.GL11.*;


import base.Object3d;
import base.RenderContex;
import properties.Property3d;

public class SelectionOverlay implements EditorProperty {

	@Override
	public Property3d fastClone() {
		return null;
	}
	public void render(RenderContex renderContex,Object3d owner) {
		glDisable(GL_DEPTH_TEST);
		glLineWidth(3); 
		glBegin(GL_LINES);
		glColor3f(1, 0, 0);
		glVertex3f(0, 0, 0);
		glVertex3f(1, 0, 0);
		
		glColor3f(0, 1, 0);
		glVertex3f(0, 0, 0);
		glVertex3f(0, 1, 0);
		
		glColor3f(0, 0, 1);
		glVertex3f(0, 0, 0);
		glVertex3f(0, 0, 1);
		glEnd();
		glEnable(GL_DEPTH_TEST);
	}
}
