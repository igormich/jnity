package properties;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Vector3f;

import base.Object3d;
import base.RenderContex;
import io.ResourceController;
import physics.TriangleStrip;

public class MultiMesh implements Property3d, TriangleStrip,Externalizable {

	private static final long serialVersionUID = 435157154947697078L;
	private List<Mesh> meshes=new ArrayList<Mesh>();
	private String pathToFile = "";

	public Mesh addMesh(){
		Mesh result = new Mesh();
		meshes.add(result);
		return result;
	}
	
	public Mesh addMesh(Mesh mesh){
		meshes.add(mesh);
		return mesh;
	}
	//maybe not actual
	public void setMaterialNameForAll(String materialName){
		meshes.forEach(mesh -> mesh.setMaterialName(materialName));
	}
	@Override
	public void render(RenderContex renderContex,Object3d owner) {
		meshes.forEach(mesh -> mesh.render(renderContex,owner));
	}

	@Override
	public Iterator<Vector3f> iterator() {
		return new MultiMeshTriangleStrip();
	}
	private class MultiMeshTriangleStrip implements Iterator<Vector3f>{
		private Iterator<Mesh> currentMesh;
		private Iterator<Vector3f> currentIterator;
		
		public MultiMeshTriangleStrip() {
			super();
			currentMesh=meshes.iterator();
			currentIterator=currentMesh.next().iterator();
		}

		@Override
		public boolean hasNext() {
			if(currentIterator.hasNext())
				return true; 
			while(!currentIterator.hasNext())
				if(currentMesh.hasNext())
					currentIterator=currentMesh.next().iterator();
				else
					return false;
			return true;
		}
		@Override
		public Vector3f next() {
			return currentIterator.next();
		}
		
	}
	@Override
	public int getSize() {
		return meshes.stream().mapToInt(m ->m.getSize()).sum();
	}

	public List<Mesh> getMeshes(){
		return Collections.unmodifiableList(meshes);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(pathToFile);
		for(Mesh mesh:meshes)
			out.writeUTF(mesh.getMaterialName());
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		pathToFile = in.readUTF();
		ResourceController.getOrCreate().getOrLoadMesh(this, pathToFile);		
		for(Mesh mesh:meshes)
			mesh.setMaterialName(in.readUTF());
	}

	public void setFileName(String pathToFile) {
		this.pathToFile = pathToFile;	
	}

	public String getFileName() {
		return pathToFile;
	}

	public MultiMesh fastClone(MultiMesh result) {
		result.pathToFile = this.pathToFile;
		result.meshes.clear();
		for(Mesh mesh:meshes)
			result.meshes.add(mesh.fastClone());
		return result;
	}
	@Override
	public MultiMesh fastClone() {
		return fastClone(new MultiMesh());
	}

	public void prepare() {
		for(Mesh mesh:meshes)
			mesh.prepare();		
	}
}
