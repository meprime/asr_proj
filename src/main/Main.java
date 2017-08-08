package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import testing.ErrorCalculator;
import testing.StateDecoder;
import training.FeaturesExtractor;
import training.Trainer;

public class Main {

	public static void main(String[] args) throws FileNotFoundException {
		Trainer trainer = new Trainer(MyProperties.getInstance().getProperty(MyProperties.PROP_TRAIN_PATH));
		trainer.train(2);
		System.out.println("Training is finished!");
//		if(MyProperties.getInstance().isDebug())
//			HMMFactory.getInstance().writeTransitions();
		StateDecoder decoder = new StateDecoder();
//		Scanner scanner = Debugger.getInstance().getSysinScanner();
		String result = decoder.decode(FeaturesExtractor.readFeatures("database/test/1/3.ftr"));
		Scanner refScanner = new Scanner("database/test/1/ref.txt");
		String ref = refScanner.nextLine().split("wav")[1];
		refScanner.close();
		System.out.println(result);
		System.out.println("Phone Error Rate: " + ErrorCalculator.getInstance().phoneErrorRate(ref, result));
		System.out.println("Word Error Rate: " + ErrorCalculator.getInstance().wordErrorRate(ref, result));
//		while(scanner.hasNext()) {
//			String line = scanner.nextLine();
//			if(line.equals("end"))
//				break;
//		}
	}
}
