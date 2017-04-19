package properties;

import java.io.BufferedInputStream;
import java.io.Externalizable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import base.Object3d;
import io.ResourceController;

public class SoundSourse implements Property3d, Externalizable, LineListener {

	
	
	private String fileName;
	private float volume = 1;
	private boolean loaded = false;
	private boolean playing = false;
	private boolean loop = false;
	private long position;
	
	private Clip clip;
	private FloatControl volumeControl;


	@Override
	public SoundSourse fastClone() {	
		SoundSourse result =  new SoundSourse();
		result.fileName = this.fileName;
		result.volume = this.volume;
		result.loop = this.loop;
		result.position = this.position;
		result.loop = this.loop;
		return result;
	}

	@Override
	public void tick(float deltaTime, float time, Object3d owner) {
		if (playing) {
			position = clip.getMicrosecondPosition();
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeUTF(fileName);
		out.writeFloat(getVolume());
		out.writeBoolean(isLoop());
		out.writeBoolean(isPlaying());
		out.writeLong(position);

	}
	
	public boolean isLoop() {
		return loop;
	}
	
	public void setLoop(boolean loop) {
		this.loop = loop;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		setFileName(in.readUTF());
		setVolume(in.readFloat());
		setLoop(in.readBoolean());
		boolean playing = in.readBoolean();
		position = in.readLong();
		if(playing){
			play();
		}
		
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	private void load() {
	    try {
		    AudioInputStream stream;
		    AudioFormat format;
		    DataLine.Info info;
		    stream = AudioSystem.getAudioInputStream(new BufferedInputStream(
		    		new FileInputStream(ResourceController.getOrCreate().getSoundPath()+fileName)));
		    format = stream.getFormat();
		    info = new DataLine.Info(Clip.class, format);
		    clip = (Clip) AudioSystem.getLine(info);   
		    clip.open(stream);
		    volumeControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
		    volumeControl.setValue(volume);
			clip.addLineListener(this);
		    loaded = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	public boolean isLoaded() {
		return loaded;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
		if(volumeControl!=null)
			volumeControl.setValue(volume);
	}

	public boolean isPlaying() {
		return playing;
	}

	public void setPlaying(boolean playing) {
		if(playing && !this.playing)
			play();
		else 
			stop();
			
	}
	
	private void stop() {
		if (clip != null) {
			stop();
		}	
	}

	protected void play() {
		if (clip == null) {
			load();
		}
		clip.setMicrosecondPosition(position);
		clip.start();
	}
	public static void main(String[] args) throws InterruptedException {
		SoundSourse soundSourse = new SoundSourse();
		soundSourse.setFileName("fall.wav");
		soundSourse.play();
		Thread.sleep(10);
		while(soundSourse.isPlaying()){
			soundSourse.tick(0, 0, null);
		}
	}

	@Override
	public void update(LineEvent event) {
		if(event.getType() == LineEvent.Type.START){
			playing = true;
		}
		if(event.getType() == LineEvent.Type.STOP){
			playing = false;
		}
	}


}
