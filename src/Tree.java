/* Ru Ferguson
 * 4 November 2020
 * 
 * This class holds the root node and interacts with the tree via the root node. The tree does not
 * perform actions of adding nodes or traversing the tree beyond its calls to the root node. */

import java.util.ArrayList;
import java.util.List;

public class Tree<T> { 
	
	Node<T> root; // the root of the tree
	int L; // maximum token sequence length (or order length) –> chosen by artist/composer/engineer
	Node<T> newNode;
	double Pmin; // Pmin - set in constructor
	int totalInputTokens; // the total number of input tokens
	double r; // the threshold r value of the tree. (nodes with r-values less than this will be deleted)
	
	Tree() {
		root = new Node<T>();
		L = 3; // 3 is used for unit tests
		Pmin = 0.1;
	}
	
	Tree(int i, double k, double rVal) {
		root = new Node<T>();
		L = i; // input parameter chooses L
		Pmin = k;
		r = rVal;
	}
		
	// creates the unique token sequences from the input & using the root, adds them to the tree
	void train(ArrayList<T> input) {
		for(int i = 1; i <= L ; i++) { // i = order #
			for(int j = 0; j < input.size() - (i - 1); j++) { // j = index into input
				boolean hasSeqAtEndOfDataset = j + i == input.size(); // set the hasSeqAtEndOfDataset of theNewNode (also can be in constructor like above)
				ArrayList<T> curSequence = new ArrayList<T>(input.subList(j, j + i));	// curSequence = find the current sequence of size i		
				T nextToken = null;
				if (!hasSeqAtEndOfDataset) {
					nextToken = input.get(j + i);
				}
				newNode = new Node<T>(curSequence, hasSeqAtEndOfDataset, nextToken); // create a new node with the current sequence & the next token
				root.addNode(newNode);
			}
		}
		root.train(input);
		// sum the total tokens in the input here (totalInputTokens) -- this is most easily
		// done similarly to how we summed our total tokens in the ProbabilityGenerator.
		totalInputTokens = input.size();
		root.pMinElimination(totalInputTokens, Pmin);
		root.rElimination(r, root);
	}

	// calls the root to print
	void print() {
		root.print();
	}
	
	ArrayList<T> generate(int length) {
		return root.generate(new ArrayList(), length, L);
	}
}
