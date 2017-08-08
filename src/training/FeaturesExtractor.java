package training;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import main.Debugger;
import main.MyProperties;
import model.FrameFeatures;
import model.acoustic.HmmState;

public class FeaturesExtractor {

	public static void main(String[] args) throws FileNotFoundException {
		FeaturesExtractor fe = new FeaturesExtractor("database/train");
		fe.extractFeaturesAndUtterances();
		System.out.println(fe.getAllFeatures().size());
		System.out.println(fe.getUtterances().size());
	}
	
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
	
	public static List<FrameFeatures> readFeatures(String featuresFilePath) throws FileNotFoundException {
		File featuresFile = new File(featuresFilePath);
		Scanner featuresScanner = new Scanner(featuresFile);
		List<FrameFeatures> features = new ArrayList<>();
		while(featuresScanner.hasNextDouble()) {
			double[] values = new double[FrameFeatures.FEATURES_COUNT];
			for(int i = 0; i < FrameFeatures.FEATURES_COUNT; i++)
				values[i] = featuresScanner.nextDouble();
			FrameFeatures f = new FrameFeatures();
			f.setValues(values);
			features.add(f);
		}
		featuresScanner.close();
		return features;
	}
	
	public void extractFeaturesAndUtterances() throws FileNotFoundException {
		utterances = new ArrayList<String>();
		allFeatures = new ArrayList<>();
		for(File speakerDir : new File(this.srcDir).listFiles()) {
			File ref = new File(speakerDir, "ref.txt");
			if(!ref.exists())
				throw new FileNotFoundException("Every subfolder of trainPath must contain a file named ref.txt");
			Scanner refScanner = new Scanner(ref);
			List<List<FrameFeatures>> speakerFeatures = new ArrayList<>();
			while(refScanner.hasNextLine()) {
				String line = refScanner.nextLine();
				String[] parts = line.split(".wav");
				utterances.add(parts[1] + " ");
				File featuresFile = new File(speakerDir, parts[0] + ".ftr");
				Scanner featuresScanner = new Scanner(featuresFile);
				List<FrameFeatures> features = new ArrayList<>();
				while(featuresScanner.hasNextDouble()) {
					double[] values = new double[FrameFeatures.FEATURES_COUNT];
					for(int i = 0; i < FrameFeatures.FEATURES_COUNT; i++)
						values[i] = featuresScanner.nextDouble();
					FrameFeatures f = new FrameFeatures();
					f.setValues(values);
					features.add(f);
				}
				features.add(new FrameFeatures()); // Adding the all-zero feature as the last feature of file. 
				speakerFeatures.add(features);
				featuresScanner.close();
			}
			this.normalizeSpeakerFeatures(speakerFeatures);
			allFeatures.addAll(speakerFeatures);
			refScanner.close();
		}
		if(MyProperties.getInstance().isDebug())
			viewFeatureVals();
	}
	
	private void normalizeSpeakerFeatures(List<List<FrameFeatures>> speakerFeatures) {
		int l = FrameFeatures.FEATURES_COUNT;
		double[] mean = new double[l];
		int c = 0;
		for(int i = 0; i < speakerFeatures.size(); i++) {
			for(int j = 0; j < speakerFeatures.get(i).size(); j++) {
				c++;
				for(int k = 0; k < l; k++) {
					mean[k] += speakerFeatures.get(i).get(j).getValue(k);
				}
			}
		}
		for(int i = 0; i < l; i++) {
			mean[i] /= c;
		}
		double[] var = new double[l];
		for(int i = 0; i < speakerFeatures.size(); i++) {
			for(int j = 0; j < speakerFeatures.get(i).size(); j++) {
				for(int k = 0; k < l; k++) {
					var[k] += Math.pow(speakerFeatures.get(i).get(j).getValue(k) - mean[k], 2);
				}
			}
		}
		for(int i = 0; i < l; i++) {
			var[i] /= c-1;
		}
		for(int i = 0; i < speakerFeatures.size(); i++) {
			for(int j = 0; j < speakerFeatures.get(i).size(); j++) {
				for(int k = 0; k < l; k++) {
					speakerFeatures.get(i).get(j).getValues()[k] = (speakerFeatures.get(i).get(j).getValues()[k] - mean[k]) / var[k];
				}
			}
		}
	}
	
	private void viewFeatureVals() {
		Scanner scanner = Debugger.getInstance().getSysinScanner();
		System.out.println("checking feature values...");
		while(scanner.hasNextLine()) {
			String command = scanner.nextLine();
			if(command.equals("end"))
				break;
			if(command.equals("feature")) {
				int fileInd = scanner.nextInt();
				int featureInd = scanner.nextInt();
				String featureStr = "";
				for(int i = 0; i < 13; i++) {
					featureStr += this.allFeatures.get(fileInd).get(featureInd).getValues()[i] + " ";
				}
				System.out.println(featureStr);
			}
		}
		System.out.println("feature values checked.");
	}
}
