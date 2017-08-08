package testing;

public class ErrorCalculator {

	private static ErrorCalculator instance;
	public static ErrorCalculator getInstance() {
		if(instance == null)
			instance = new ErrorCalculator();
		return instance;
	}
	
	public double phoneErrorRate(String ref, String test) {
		int[][] dist = new int[ref.length()+1][test.length()+1];
		for(int i = 0; i < ref.length(); i++)
			dist[i][0] = i;
		for(int i = 0; i < test.length(); i++)
			dist[0][i] = i;
		for(int i = 1; i <= ref.length(); i++) {
			for(int j = 1; j <= test.length(); j++) {
				if(ref.charAt(i-1) == test.charAt(j-1))
					dist[i][j] = dist[i-1][j-1];
				else {
					dist[i][j] = dist[i-1][j] + 1;
					if(dist[i][j-1] + 1 < dist[i][j])
						dist[i][j] = dist[i][j-1] + 1;
					if(dist[i-1][j-1] + 1 < dist[i][j])
						dist[i][j] = dist[i-1][j-1] + 1;
				}
			}
		}
		return (double)dist[ref.length()][test.length()] / ref.length();
	}
	
	public double wordErrorRate(String ref, String test) {
		String[] refWords = ref.split(" ");
		String[] testWords = test.split(" ");
		int[][] dist = new int[refWords.length+1][testWords.length+1];
		for(int i = 0; i < refWords.length; i++)
			dist[i][0] = i;
		for(int i = 0; i < testWords.length; i++)
			dist[0][i] = i;
		for(int i = 1; i <= refWords.length; i++) {
			for(int j = 1; j <= testWords.length; j++) {
				if(refWords[i-1].equals(testWords[j-1]))
					dist[i][j] = dist[i-1][j-1];
				else {
					dist[i][j] = dist[i-1][j] + 1;
					if(dist[i][j-1] + 1 < dist[i][j])
						dist[i][j] = dist[i][j-1] + 1;
					if(dist[i-1][j-1] + 1 < dist[i][j])
						dist[i][j] = dist[i-1][j-1] + 1;
				}
			}
		}
		return (double)dist[refWords.length][testWords.length] / refWords.length;
	}
}
