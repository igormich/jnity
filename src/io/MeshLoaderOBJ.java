package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import materials.MaterialLibrary;
import properties.Mesh;
import properties.MultiMesh;

public class MeshLoaderOBJ {
	private static Vector3f defNormal = new Vector3f();
	private static Vector3f defTexture = new Vector3f();
	
	public static MultiMesh loadMultiMesh(String pathToFile) {
		MultiMesh result = new MultiMesh();
		loadMultiMesh(result, pathToFile);
		result.prepare();
		return result;
	}
	public static void loadMultiMesh(MultiMesh result, String pathToFile){
		result.setFileName(pathToFile);
		pathToFile = ResourceController.getOrCreate().getModelPath() + pathToFile;
		File f=new File(pathToFile);
		List<Vector3f> vertex = new ArrayList<Vector3f>();
		List<Vector3f> texture = new ArrayList<Vector3f>();
		List<Vector3f> normals = new ArrayList<Vector3f>();
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String s = br.readLine().trim();
			Mesh mesh = new Mesh();
			mesh.setRenderParts(Mesh.TEXTURE+Mesh.NORMAL);
			result.addMesh(mesh);
			while(br.ready()){
				if(s.startsWith("v ")){
					String[] ss = s.substring(2).trim().split(" ");
					vertex.add(new Vector3f(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2])));
				}
				if(s.startsWith("vt ")){
					String[] ss = s.substring(3).trim().split(" ");
					texture.add(new Vector3f(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2])));
				}
				if(s.startsWith("vn ")){
					String[] ss = s.substring(3).trim().split(" ");
					normals.add(new Vector3f(Float.parseFloat(ss[0]), Float.parseFloat(ss[1]), Float.parseFloat(ss[2])));
				}
				if(s.startsWith("f ")){
					String[] face = s.substring(2).trim().split(" ");
					for(String vert:face){
						String[] ss = vert.split("/");
						int vInd=Integer.parseInt(ss[0]);
						if(vInd<0)
							vInd=vertex.size()+vInd;
						else
							vInd=vInd-1;
						Vector3f text=defTexture;
						if((ss.length==3)&&(ss[1].length()>0)){
							int tInd=Integer.parseInt(ss[1]);
							if(tInd<0)
								tInd=texture.size()+tInd;
							else
								tInd=tInd-1;
							text=texture.get(vInd);
							text.normalize();
						}
						Vector3f normal=defNormal;
						if((ss.length>1)){
							int nInd=0;
							if(ss.length==3)
								nInd=Integer.parseInt(ss[2]);
							else 
								nInd=Integer.parseInt(ss[1]);
							if(nInd<0)
								nInd=normals.size()+nInd;
							else
								nInd=nInd-1;
							normal=normals.get(vInd);
							normal.normalize();
						}
						
						Vector3f text2d = new Vector3f(text.x,text.y,text.z);
						mesh.add(vertex.get(vInd), normal, null, text2d);
					}
				}
				s = br.readLine().trim();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
