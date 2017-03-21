package utils;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class MyMouse {

	public static float getX() {
		return ((float)Mouse.getX())/Display.getWidth()-0.5f;
	}
	public static float getY() {
		return 0.5f-((float)Mouse.getY())/Display.getHeight();
	}
	public static void center() {
		Mouse.setCursorPosition(Display.getWidth()/2, Display.getHeight()/2);
	}
}
