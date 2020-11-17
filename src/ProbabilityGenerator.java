/* Ru Ferguson
 * 28 September 2020
 * 
 * This is the probability generator from Project 1. Trains and generates values from a
 * probability distribution. */

import java.util.ArrayList;

public class ProbabilityGenerator<T> {
	
	
	ArrayList<T> alphabet;
	ArrayList<Integer> alphabet_counts;
	ArrayList<Double> probabilities;
	ArrayList<Double> probDist;

	
	ProbabilityGenerator() {
		alphabet = new ArrayList<T>();
		alphabet_counts = new ArrayList<Integer>();
		probabilities = new ArrayList<Double>();
		probDist = new ArrayList<Double>();
	}
	
	
	// returns the size of the alphabet ArrayList
	public int getAlphabetSize() {
		return alphabet.size();
	}
	
	
	// returns the ArrayList containing the counts of occurrences for each note
	public ArrayList<Integer> getAlphabetCounts() {
		return alphabet_counts;
	}
	
	
	// returns the ArrayList containing the counts of occurrences for each note
	public ArrayList<T> getAlphabet() {
		return alphabet;
	}
	
	
	// returns the token at a given index in the ArrayList alphabet
	public T getToken(int index) {
		return alphabet.get(index);
	}
	
	
	// returns the probability from the probabilities ArrayList at a specific index 
	public double getProbability(int index) {
		getProbabilities();
		return probabilities.get(index);
	}
	
	
	// returns the total number of notes
	public double getTotal() {
		double total = 0;
		for (int i = 0; i < alphabet_counts.size(); i++) {
			total = total + alphabet_counts.get(i);
		}
		return total;
	}
	
	
	// adds the probabilities to an ArrayList called probabilities
	public ArrayList<Double> getProbabilities() { 
		for (int i = 0; i < alphabet_counts.size(); i++) {
			while (probabilities.isEmpty() || probabilities.size() != alphabet_counts.size()) {
				probabilities.add((double) alphabet_counts.get(i) / getTotal()); 
			}
			probabilities.set(i, (double) alphabet_counts.get(i) / getTotal()); 
		}
		
		return probabilities;
	}
	
	
	// uses the probabilities calculated in getProbabilities() to create a distribution to generate from
	public ArrayList<Double> getProbDist(ArrayList<Double> probs) { 
		double temp = 0;
		if (probDist.isEmpty()) {
			for (int i = 0; i < probs.size(); i++) {
				temp = temp + probs.get(i);
				probDist.add(temp); 
			} 
		} else {
			for (int i = 0; i < probs.size(); i++) {
				temp = temp + probs.get(i);
				probDist.set(i, temp);
			} 
		}
		return probDist;
	}
	
	
	// adds new tokens to the alphabet ArrayList and counts number of occurrences in the alphabet_counts ArrayList
	void train(ArrayList<T> newTokens) {
		int i = 0;
		while (i < newTokens.size()) {
			int index = alphabet.indexOf(newTokens.get(i));
			
			// if new token is not in the alphabet array, add a new ArrayList item with the new token
			if (index == -1) {
				alphabet.add(newTokens.get(i));
				alphabet_counts.add(0);
			}   
			// if token already exists, count it
			for (int j = 0; j < alphabet.size(); j++) {
				if (alphabet.get(j).equals(newTokens.get(i))) {
					int tempCount = alphabet_counts.get(j) + 1;
					alphabet_counts.set(j, tempCount);
				}
			}
			i++;
		}
	}
	
	
	T generate() {
		getProbDist(getProbabilities());
		T newToken = null;
		
		double rIndex = (double) Math.random(); // generate a random double between 0 and 1
		boolean found = false; // a boolean to notify program if the note is found and returned
		int i = 0; // to allow exit from while()
		
		while ((i <= probabilities.size() - 1) && (!found)) { 
			if (rIndex < probDist.get(i + 1)) {
				newToken = alphabet.get(i);
				found = true;
			}
			i++;
		}		
		return newToken;
	} 
	
	
	// this function adds the new token from the function above to the new melody
	ArrayList<T> generate(int length) {
		ArrayList<T> newSequence = new ArrayList<T>();
		
		for (int i = 0; i < length; i++) {
			newSequence.add(generate());
		}
		
		return newSequence;
	}
	
}
