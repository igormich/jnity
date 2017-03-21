package materials;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL14;

import io.ResourceController;

public class Texture2D implements Texture, Externalizable{
	
	private static final long serialVersionUID = -460385300096058453L;
	private static final int BYTES_PER_PIXEL = 4;//3 for RGB, 4 for RGBA
    private int ID;
    public boolean hasAlpha;
    public int width;
    public int height;
	private String filename;
	private boolean repeatW;
	private boolean repeatH;
	
    private BufferedImage loadImage(String filename) {
    	filename = ResourceController.getOrCreate().getTexturesPath() + filename;
		System.out.println(filename);
		Image imageX = new javax.swing.ImageIcon(filename).getImage();
	    BufferedImage image = new BufferedImage(imageX.getWidth(null),imageX.getHeight(null), BufferedImage.TYPE_INT_ARGB);
	    try{
	    	Method getColorModel = imageX.getClass().getMethod("getColorModel");
	    	ColorModel colorModel=(ColorModel) getColorModel.invoke(imageX);
	    	hasAlpha = colorModel.hasAlpha();
	    }catch (Exception e) {
	    	e.printStackTrace();
	    	hasAlpha = filename.contains(".png");
		}
	    Graphics2D ig = image.createGraphics();
	    ig.drawImage(imageX, 0, 0, null); 
		return image;
	}
    
    private int loadTexture(BufferedImage image, boolean repeatW,boolean repeatH){
		width=image.getWidth();
		height=image.getHeight();
		
		DataBufferInt dbi=(DataBufferInt) image.getRaster().getDataBuffer(); 
		int[] pixels=dbi.getData();
		
		ByteBuffer buffer = BufferUtils.createByteBuffer(image.getWidth() * image.getHeight() * BYTES_PER_PIXEL); //4 for RGBA, 3 for RGB
		
		for(int y = 0; y < image.getHeight(); y++){
			for(int x = 0; x < image.getWidth(); x++){
				int pixel = pixels[y * image.getWidth() + x];
				buffer.put((byte) ((pixel >> 16) & 0xFF));     // Red component
				buffer.put((byte) ((pixel >> 8) & 0xFF));      // Green component
				buffer.put((byte) (pixel & 0xFF));               // Blue component
				buffer.put((byte) ((pixel >> 24) & 0xFF));    // Alpha component. Only for RGBA
			}
		}//*/
		buffer.flip(); //FOR THE LOVE OF GOD DO NOT FORGET THIS
		ID=glGenTextures(); //Generate texture ID
		glBindTexture(GL_TEXTURE_2D, getID()); //Bind texture ID
		glTexParameteri(GL_TEXTURE_2D, GL14.GL_GENERATE_MIPMAP, GL_TRUE);
		glTexParameteri(GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
		glTexParameteri(GL_TEXTURE_2D,  GL12.GL_TEXTURE_MAX_LEVEL, 6);
		//Setup texture scaling filtering 
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);   
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, image.getWidth(), image.getHeight(), 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);       
		//Return the texture ID so we can bind it later again
		return getID();
    }
    public Texture2D() {		
    }
    public Texture2D(String filename, boolean repeat) throws IOException {		
    	this(filename,repeat,repeat);
    }
	public Texture2D(String filename, boolean repeatW, boolean repeatH) throws IOException {	
		this.filename = filename;
		this.repeatW = repeatW;
		this.repeatH = repeatH;
		System.out.println(filename);	
		BufferedImage image=loadImage(filename);
		loadTexture(image,repeatW,repeatH);	
	}

	public Texture2D(String filename) throws IOException {
		this(filename, true);
	}

	public Texture2D(int width, int height) {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		loadTexture(image,true,true);
	}

	@Override
	public void apply() {
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, getID());
	}

	@Override
	public boolean isTransparent() {
		return hasAlpha;
	}

	@Override
	public void applyAs(int i) {
		glActiveTexture(GL_TEXTURE0+i);
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D,getID());
		glDisable(GL_TEXTURE_2D);
	}
	@Override
	public void unApply() {
		glDisable(GL_TEXTURE_2D);
	}

	public int getID() {
		return ID;
	}

	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(filename);
		out.writeBoolean(repeatH);
		out.writeBoolean(repeatW);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		filename = in.readUTF();

		repeatH = in.readBoolean();
		repeatW = in.readBoolean();
		BufferedImage image= null;
		try{
			image=loadImage(filename);
		} catch (Exception e) {
			System.out.println("Can't be loaded " + e.getClass().getSimpleName()+ " " + e.getMessage());
			image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			Graphics g = image.getGraphics();
			g.setColor(Color.red);
			g.fillRect(0, 0, 16, 16);
		}
		loadTexture(image,repeatW,repeatH);	
	}

	@Override
	public String getFileName() {
		return filename;
		
	}
}
