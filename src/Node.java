/* Ru Ferguson
 * 4 November 2020
 * 
 * This class creates Node objects which each represent one node in the prediction suffix tree.
 * Nodes can also be removed using Pmin elimination.
 */

import java.util.ArrayList;

public class Node<T, E> extends ProbabilityGenerator<T> {
	// you’ll now inherit from your ProbabilityGenerator class. You will use the ProbabilityGenerator
	// class to keep track of all the tokens that come after your node & the counts for each. In the
	// PST generate, you will use it to generate
	
	ArrayList<T> tokenSequence; // the sequence at this node
	ArrayList<Node> children; // an array of the child nodes
	int count; // count – the number of times the node appears in the input, initially set to 1
	//(NOTE: the name of this class variable MUST be different than the class variable keeping track of the total # of input tokens in ProbabilityGenerator)
	boolean hasSeqAtEndOfDataset; // Is the tokenSequence of this node at the end? – can be in constructor or create a set method to assign the value from the tree
	float this_r; // the r value of the node. I make this a class variable so it is easy to print out in the print() for testing
	E nextToken; // the token that comes after this node’s tokenSequence. Set in a parameter in the constructor.
			
	
	Node() {
		children = new ArrayList<Node>();
		tokenSequence = new ArrayList<T>();
		count = 1;
	}
	
	Node(ArrayList<T> curSequence) {
		children = new ArrayList<Node>();
		tokenSequence = curSequence;
		count = 1;
	}
	
	ArrayList<T> getTokenSeq() {
		return tokenSequence;
	}
	
	ArrayList<Node> getChildren() {
		return children;
	}
	
	
	// Adds a child node. Will only add a child node if the input node contains this node as a suffix.
	boolean addNode(Node node) {
		boolean found = false; // whether the node has been added or not yet
		
		if ((node.getTokenSeq()).equals(tokenSequence)) {	// the tokenSequence of this node is the same as the token sequence of the added node
			found = true;
			count++; // add one to count 
			//if () {  // if the node’s tokenSequence is not the last sequence in the input
				//trainViaProbGen(node);
			//}
		} else if(amIASuffix(node) || (tokenSequence.size() == 0)) { 
			// try to add the node to all the children nodes
			for (int i = 0; i < children.size(); i++) {
				if ((children.get(i)).amIASuffix(node)) {
					(children.get(i)).addNode(node);
					found = true;
				}
			}
			// Did one your child nodes add the node?
			if (!found && (node.getTokenSeq()).size() - 1 == tokenSequence.size()) { //	 If NOT found and the length of node’s tokenSequence is one less than this tokenSequence
				children.add(node);	//	Add the node to our children array.
				found = true;
				node.trainViaProbGen(node);
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
	
	
	// determines whether the tokenSequence of this node is a suffix of the tokenSequence of the input node
	boolean amIASuffix(Node node) {	
		ArrayList<T> input = node.getTokenSeq();
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
	boolean rElimination(float r, Node<T> myRoot) {
		boolean shouldRemove = tokenSequence.size() > 1; // the size of the tokenSequence is greater than 1
			if(shouldRemove) {
				//Find the r of this node
				//1. myRatio = find the max of the alphabet_counts / total (the total # input tokens from the super class)
				// HINT: You can use Collections.Max() to find the max of an ArrayList
				// 2. rootRatio = ( find the corresponding count in myRoot of the token with the max counts from above ) / myRoot’s total (the total # input tokens from myRoot’s super class)
				// HINT: use the getCountsAtToken() method to help get the correct counts in the numerator. This is one part of the process.
				// 3. this_r = myRatio / rootRatio;
				// 4. shouldRemove = this_r < r;
				// 5. if we should NOT remove this node
				// for each node (start from the end & go to the front of each array)
				// {
				// call rElimination on all the children nodes, hand it r & this node (ie. keyword this) if they return true (ie, we should remove the node)
				// then remove the entire node (which incl. its children)
				// you may use the ArrayList method .remove()
				// }
			return shouldRemove;
			}
	} 
	
	//Traverse through the tree to find the correct node to generate from
	T generate(ArrayList initSeq) {
		E newToken = null; //the new token to return
		// 1. If the tokenSequence equals the initSeq, then return the result the super class ProbabilityGenerator generate().
		// 2. Else if the tokenSequence is a suffix of the initSeq, make the pseudo-recursive call to generate(initSeq) via your children. If the return value is not null, return that value.
		// 3. If none of your children have generated a token (generate(ArraList initSeq)) keeps on
		// returning null), then return the result of the super class ProbabilityGenerator generate().
	}
	ArrayList<T> generate(ArrayList initSeq, int length) {
		// call generate(initSeq) for length times and return an ArrayList with the result
	}
	
	//***you should fill in these methods yourself, so they won’t appear below***
	// keeps track of probability distribution of the next tokens 
	void trainViaProbGen(Node<T, E> node) {
		
	}
	
	// given a token, return the # of times it has appeared after the node’s tokenSequence
	float getCountsAtToken(E token) {
		
	}
}