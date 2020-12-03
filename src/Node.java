/* Ru Ferguson
 * 3 December 2020
 * 
 * This class creates Node objects which each represent one node in the prediction suffix tree.
 * Nodes can also be removed using Pmin or R elimination. This class extends the Probability Generator
 * and uses its functionality to keep track of all the tokens that come after each node and the counts for each.
 * It is also used to generate new tokens.
 */

import java.util.ArrayList;
import java.util.Collections;

public class Node<T> extends ProbabilityGenerator<T> {
	
	ArrayList<T> tokenSequence; // the sequence at this node
	ArrayList<Node> children; // an array of the child nodes
	int count; // the number of times the node appears in the input, initially set to 1
	boolean hasSeqAtEndOfDataset; // is the tokenSequence of this node at the end?
	double this_r; // the r value of the node
	T nextToken; // the token that comes after this node’s tokenSequence
	
	Node() {
		children = new ArrayList<Node>();
		tokenSequence = new ArrayList<T>();
		count = 1;
	}
	
	Node(ArrayList<T> curSequence, boolean atEnd, T nextT) {
		children = new ArrayList<Node>();
		tokenSequence = curSequence;
		count = 1;
		hasSeqAtEndOfDataset = atEnd;
		nextToken = nextT;
	}
	
	ArrayList<T> getTokenSeq() {
		return tokenSequence;
	}
	
	ArrayList<Node> getChildren() {
		return children;
	}
	
	public double getTotal() {
		double total = 0;
		for (int i = 0; i < alphabet_counts.size(); i++) {
			total = total + alphabet_counts.get(i);
		}
		return total;
	}
	
	boolean isSeqAtEndOfDataset() {
		return hasSeqAtEndOfDataset;
	}
	
	public ArrayList<Integer> getAlphabetCounts() {
		return alphabet_counts;
	}
	
	public ArrayList<T> getAlphabet() {
		return alphabet;
	}
	
	public T getToken(int index) {
		return alphabet.get(index);
	}
	
	public ArrayList<T> getNextToken() {
		ArrayList<T> nextTokenArray = new ArrayList<T>();
		nextTokenArray.add(nextToken);
		return nextTokenArray;
	}
	
	// Adds a child node. Will only add a child node if the input node contains this node as a suffix.
	boolean addNode(Node node) {		
		boolean found = false; // whether the node has been added or not yet
				
		if ((node.getTokenSeq()).equals(tokenSequence)) {	// the tokenSequence of this node is the same as the token sequence of the added node
			found = true;
			count++; // add one to count 
			if (!node.isSeqAtEndOfDataset()) {  // if the node’s tokenSequence is not the last sequence in the input
				trainViaProbGen(node);
			}
		} else if(amIASuffix(node) || (tokenSequence.size() == 0)) { 
			// try to add the node to all the children nodes
			for (int i = 0; i < children.size(); i++) {
				if ((children.get(i)).amIASuffix(node)) {
					(children.get(i)).addNode(node);
					found = true;
				}
			}
			// Did one your child nodes add the node?
			if (!found && (node.getTokenSeq()).size() - 1 == tokenSequence.size() && !node.isSeqAtEndOfDataset()) { //	 If NOT found and the length of node’s tokenSequence is one less than this tokenSequence
				children.add(node);	// add the node to our children array.
				found = true;
				if (!node.isSeqAtEndOfDataset()) {
					node.trainViaProbGen(node);
				}
			}
		}	
		return found;
	}
	
	
	// prints the tokenSequence, then prints its children
	void print() {
		System.out.println(tokenSequence);	// print the token sequence
		for (int i = 0; i < children.size(); i ++) {	// for each node in the children
			(children.get(i)).print(1);	//see the next function
		}
	}
	
	//this will produce nicely formatted trees that make sense
	void print(int numSpacesBefore) {
		for (int i = 1; i < numSpacesBefore; i++) {	//for 1 to numSpacesBefore
			System.out.print("   "); 
		}
		System.out.print("  ––> "); //print an arrow (-->)
		System.out.println(this.getTokenSeq());	// print the token
		for (int i = 0; i < children.size(); i++) {	//for each node in the children
			// each time you call this from the next child the number of spaces will increase by 1
			(children.get(i)).print(numSpacesBefore + 1);
		}
	}
	
	// determines whether this node is a suffix of the the input node
	boolean amIASuffix(Node node) {	
		ArrayList<T> input = node.getTokenSeq();
		boolean isSuffix = amIASuffix(input);
		return isSuffix;
	}
	
	// determines whether the given tokenSequence is a suffix of the tokenSequence of the input node
	boolean amIASuffix(ArrayList input) {	
		boolean isSuffix = false;
		if (tokenSequence.isEmpty()) {	// empty string is suffix of everything
			isSuffix = true;		
		} else {
			for (int i = 0; i < input.size(); i++) {
				ArrayList<T> checkSublist = new ArrayList<T>(input.subList(i, input.size()));	//curSequence = find the current sequence of size i		

				int inputIsInTokenSeq = tokenSequence.indexOf(checkSublist); // find checkSublist in tokenSequence			
				if (inputIsInTokenSeq != -1) {
					isSuffix = true;
				} else if (checkSublist.equals(tokenSequence)) { // are they the same?
					isSuffix = true;
				}
			}
		}
		return isSuffix;
	}
	
	// Returns whether to delete this node or not. The parent node performs the deletion.
	boolean pMinElimination(int totalTokens, double pMin) {
		// find the number of times that the sequence could have occurred (dependent on tokenSequence.size())
		double empProb = (double) this.count / (totalTokens - (tokenSequence.size() - 1));

		// shouldRemove = empirical probability of the token sequence < pMin (note: handle the empty sequence / root )
		boolean shouldRemove = (empProb < pMin) && (!this.getTokenSeq().isEmpty());

		if (!shouldRemove) {	// if we should NOT remove this node
			for (int i = children.size() - 1; i >= 0; i--) {	// for each node (start from the end & go to the front of each array)
				boolean shouldRemoveChild = (children.get(i)).pMinElimination(totalTokens, pMin); // call pMinElimination on all the children nodes
				if (shouldRemoveChild) {	// if we should remove the node
					children.remove(i);	// remove the entire node (incl. its children)
				}	
			}
		}
		return shouldRemove;
	}
	
	// performs elimination based on the R-values. Returns whether to delete this node or not. The parent node performs the deletion
	boolean rElimination(double r, Node<T> myRoot) {
		boolean shouldRemove = tokenSequence.size() > 1; // the size of the tokenSequence is greater than 1
		if(shouldRemove) {
			// find the r of this node
			double myRatio = (double) Collections.max(super.getAlphabetCounts()) / super.getTotal();

			// find the conditional probabilities for the root.
			int index = alphabet_counts.indexOf(Collections.max(getAlphabetCounts()));
			double rootRatio = (double) myRoot.getCountsAtToken(alphabet.get(index)) / myRoot.getTotal();		
			this_r = myRatio / rootRatio;
			shouldRemove = this_r < r;
		}
		if (!shouldRemove) { // if we should NOT remove this node 
			for (int i = children.size() - 1; i >= 0; i--) { // for each node (start from the end & go to the front of each array)
				boolean shouldRemoveChild = (children.get(i)).rElimination(r, this); // call rElimination on all the children nodes, hand it r & 'this' node
				if (shouldRemoveChild) { // if we should remove the node
					children.remove(i);	// remove the entire node incl. children
				}
			}
		}
		return shouldRemove;
	} 
	
	// traverse through the tree to find the correct node to generate from
	T generate(ArrayList initSeq) {
		T newToken = null; //the new token to return
		
		if (getTokenSeq().equals(initSeq)) {	// if the tokenSequence equals the initSeq
			newToken = (T) super.generate();	// return the result the super class ProbabilityGenerator generate()
		} else if (this.amIASuffix(initSeq)) {	// else if the tokenSequence is a suffix of the initSeq	
			int i = 0;
			while (i < children.size()) {
				T tempToken = (T) (children.get(i)).generate(initSeq);	// make the pseudo-recursive call to generate(initSeq) via your children
				if (tempToken != null) {	// if the return value is not null, return that value
					newToken = tempToken;
					i = children.size();
				}
				i++;
			}
			if (newToken == null) { // if generate(ArrayList initSeq)) keeps returning null)
				newToken = (T) super.generate(); // return the result of the super class ProbabilityGenerator generate()
			}
		}
		return newToken;
	}
	
	
	ArrayList<T> generate(ArrayList initSeq, int length, int L) {
		// call generate(initSeq) for length times and return an ArrayList with the result
		ArrayList<T> newSequence = new ArrayList<T>();
		for (int i = 0; i < length; i++) {			
			newSequence.add(generate(initSeq));
			// update initSeq with sequences of size L from the end of the newSequence
			int m = newSequence.size() - 1;
			int k = 0;
			while (k < L && initSeq.size() < newSequence.size()) {
				if (initSeq.size() < L || initSeq.size() == 0) { 
					initSeq.add(newSequence.get(m));
				} else {
					initSeq.set(k, newSequence.get(m));
					Collections.reverse(initSeq);
				}
				m--;
				k++;
			}
		}
		return newSequence;
	}
	
	// keeps track of probability distribution of the next tokens 
	void trainViaProbGen(Node<T> node) {
		super.train(node.getNextToken());
	}
	
	// given a token, return the # of times it has appeared after the node’s tokenSequence
	double getCountsAtToken(T token) {
		int index = alphabet.indexOf(token);
		return alphabet_counts.get(index);
	}
}
