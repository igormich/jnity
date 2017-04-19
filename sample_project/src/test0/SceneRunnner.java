package test0;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.lwjgl.opengl.Display;

import base.Camera;
import base.Scene;
import io.ResourceController;

public class SceneRunnner {

	private static final int WIDTH = 512;
	private static final int HEIGHT = WIDTH;
	private static Scene scene;
	private static Camera camera;

	public static void main(String[] args) throws IOException, ClassNotFoundException {
		Utils.initDisplay(WIDTH, HEIGHT, false);
		ResourceController.getOrCreate().setTexturesPath("./textures/");
		ResourceController.getOrCreate().setModelPath("./models/");
		FileInputStream fis = new FileInputStream(new File("new.scene"));
		ObjectInputStream ois = new ObjectInputStream(fis);
		scene = (Scene) ois.readObject();
		camera = (Camera) ois.readObject();
		camera.width = WIDTH;
		camera.height = HEIGHT;
		ois.close();
		while (!Display.isCloseRequested()) {
			scene.tick(1/60f);
			scene.render(camera);
			Display.update();
			Display.sync(60);
		}
		Display.destroy();
	}

}
