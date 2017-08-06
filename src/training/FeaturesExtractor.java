package training;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import model.FrameFeatures;

public class FeaturesExtractor {

	private String srcDir;
	private List<String> utterances;
	private List<List<FrameFeatures>> allFeatures;
	
	public FeaturesExtractor(String dir) {
		this.srcDir = dir;
	}
	
	public List<String> getUtterances() {
		return this.utterances;
	}
	
	public List<List<FrameFeatures>> getAllFeatures() {
		return this.allFeatures;
	}
	
	public void extractFeaturesAndUtterances() throws FileNotFoundException {
		utterances = new ArrayList<String>();
		for(File speakerDir : new File(this.srcDir).listFiles()) {
			File ref = new File(speakerDir, "ref.txt");
			if(!ref.exists())
				throw new FileNotFoundException("Every subfolder of trainPath must contain a file named ref.txt");
			Scanner refScanner = new Scanner(ref);
			while(refScanner.hasNextLine()) {
				String line = refScanner.nextLine();
				String[] parts = line.split(".wav");
				utterances.add(parts[1] + " ");
				File featuresFile = new File(speakerDir, parts[0] + ".ftr");
				// TODO read features
			}
			refScanner.close();
		}
	}
}
