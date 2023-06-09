package neo.reducecognitivecomplexity.refactoringcache;

import java.util.Stack;

import neo.mase.patterns.yieldreturn.IteratorYield;
import neo.mase.patterns.yieldreturn.Yield;

public class ConsecutiveSequenceIterator {
	public interface SentenceSequenceInfo {
		int numberOfSentences();

		int cognitiveComplexityOfSentence(int sentence);

		boolean validSequence(int from, int to);
	}

	public enum APPROACH {
		LONG_SEQUENCE_FIRST, SHORT_SEQUENCE_FIRST
	};

	public int[] nextWithCC;
	public int lastWithCC;

	private SentenceSequenceInfo sequence;

	private Stack<Integer> los;

	private APPROACH approach;

	public ConsecutiveSequenceIterator(SentenceSequenceInfo sequence, APPROACH approach) {
		this.sequence = sequence;
		this.approach = approach;
	}

	private void initializaDataStructures() {
		int last = sequence.numberOfSentences();
		lastWithCC = 0;
		nextWithCC = new int[last + 2];
		int lastCC = last + 1;

		nextWithCC[last + 1] = lastCC;
		for (int i = last; i > 0; i--) {
			if (sequence.cognitiveComplexityOfSentence(i) > 0) {
				lastCC = i;
				if (lastWithCC == 0) {
					lastWithCC = i;
				}
			}
			nextWithCC[i] = lastCC;
		}

		los = new Stack<>();
	}

	private void generateElementsRecursive(Yield<Stack<Integer>> yield) {
		initializaDataStructures();
		generateElementsRecursive(yield, 1);
	}

	private void generateElementsRecursive(Yield<Stack<Integer>> yield, int first) {
		int last = sequence.numberOfSentences();

		yield.Return(los);
		for (int i = first; i <= lastWithCC; i++) {
			int startIndex, endIndex;

			if (approach.equals(APPROACH.LONG_SEQUENCE_FIRST)) {
				startIndex = last;
				endIndex = nextWithCC[i];
			} else {
				startIndex = nextWithCC[i];
				endIndex = last;
			}

			for (int j = startIndex; j <= endIndex; j++) {
				if (sequence.validSequence(i, j)) {
					los.push(i);
					los.push(j);
					generateElementsRecursive(yield, j + 1);
					los.pop();
					los.pop();
				}
			}
		}
	}

	public Iterable<Stack<Integer>> getIterable() {
		return IteratorYield.getIterable(this::generateElementsRecursive);
	}

}