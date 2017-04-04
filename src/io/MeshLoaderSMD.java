package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import properties.Mesh;
import properties.MultiMesh;

public class MeshLoaderSMD {
	public static MultiMesh loadMultiMesh(String pathToFile)  {
		MultiMesh result = new MultiMesh();
		loadMultiMesh(result, pathToFile);
		result.prepare();
		return result;
	}

	public static void loadMultiMesh(MultiMesh result, String pathToFile) {
		result.setFileName(pathToFile);
		pathToFile = ResourceController.getOrCreate().getModelPath() + pathToFile;
		Map<String, Mesh> meshes = new HashMap<String, Mesh>();
		File f = new File(pathToFile);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			skipHeader(br);

			String s = br.readLine().trim();
			while (!"end".equals(s)) {
				Mesh mesh = meshes.get(s);
				if (mesh == null) {
					mesh = result.addMesh();
					mesh.setRenderParts(Mesh.NORMAL + Mesh.TEXTURE);
					mesh.setMaterialName(s);
					meshes.put(s, mesh);
				}
				s = br.readLine().trim().replaceAll(" +", " ");
				parsePoint(mesh, s);
				s = br.readLine().trim().replaceAll(" +", " ");
				parsePoint(mesh, s);
				s = br.readLine().trim().replaceAll(" +", " ");
				parsePoint(mesh, s);
				s = br.readLine().trim();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void parsePoint(Mesh mesh, String s) {
		String[] ss = s.split(" ");
		Vector3f v = new Vector3f(Float.parseFloat(ss[1]), Float.parseFloat(ss[3]), Float.parseFloat(ss[2]));
		v.z = -v.z;
		Vector3f n = new Vector3f(Float.parseFloat(ss[4]), Float.parseFloat(ss[6]), Float.parseFloat(ss[5]));
		n.z = -n.z;
		Vector2f t = new Vector2f(Float.parseFloat(ss[7]), 1 - Float.parseFloat(ss[8]));
		mesh.add(v, n, null, t);
	}

	private static void skipHeader(BufferedReader br) throws IOException {
		String s = br.readLine();
		while (!"nodes".equals(s))
			s = br.readLine();
		s = br.readLine().trim().replaceAll(" +", " ");
		while (!"end".equals(s)) {
			s = br.readLine().trim().replaceAll(" +", " ");
		}
		s = br.readLine();

		while (!"skeleton".equals(s))
			s = br.readLine();
		s = br.readLine().trim();
		s = br.readLine().trim().replaceAll(" +", " ");
		while (!"end".equals(s)) {
			s = br.readLine().trim().replaceAll(" +", " ");
		}
		s = br.readLine();
		while (!"triangles".equals(s))
			s = br.readLine();
	}
}
