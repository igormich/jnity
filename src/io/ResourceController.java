package io;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import materials.Texture;
import materials.Texture2D;
import properties.MultiMesh;

public class ResourceController {
	static ResourceController resourceController = new ResourceController();
	private static Map<Thread, ResourceController> controllers = new HashMap<>();
	private String texturesPath = "";
	private String modelPath = "";
	
	private HashMap<String, MultiMesh> meshes=new HashMap<>();
	private HashMap<String, Texture> textures=new HashMap<>();
	public static synchronized ResourceController getOrCreate(){
		ResourceController result = controllers.get(Thread.currentThread());
		if (result == null) {
			result = new ResourceController();
			controllers.put(Thread.currentThread(), result);
		}
		return result;
	}

	public void setTexturesPath(String path) {
		this.texturesPath = path;	
	}
	public String getTexturesPath() {
		return texturesPath;
	}
	public void setModelPath(String path) {
		this.modelPath = path;	
	}
	public String getModelPath() {
		return modelPath;
	}
	public void getOrLoadMesh(MultiMesh multiMesh, String pathToFile) {
		MultiMesh mesh = meshes.get(pathToFile);
		if(mesh == null) {
			mesh = new MultiMesh();
			if(pathToFile.endsWith(".smd")) {
				MeshLoaderSMD.loadMultiMesh(mesh ,pathToFile);
			}
			if(pathToFile.endsWith(".obj")) {
				MeshLoaderOBJ.loadMultiMesh(mesh ,pathToFile);
			}
		}
		mesh.fastClone(multiMesh);
	}
	public Texture getOrLoadTexture(String pathToFile) {
		Texture result = textures.get(pathToFile);
		if(result == null) {
			try {
				result = new Texture2D(pathToFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
			textures.put(pathToFile, result);
		}
		return result;
	}
	public void emptyCache(){
		meshes.clear();
		textures.clear();
	}
}
