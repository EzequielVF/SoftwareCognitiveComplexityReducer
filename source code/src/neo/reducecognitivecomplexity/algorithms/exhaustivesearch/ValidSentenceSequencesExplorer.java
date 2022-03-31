package neo.reducecognitivecomplexity.algorithms.exhaustivesearch;

import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.ConsecutiveSequenceIterator.SentenceSequenceInfo;

public class ValidSentenceSequencesExplorer {

	private int[] nextWithCC;
	private int lastWithCC;

	private SentenceSequenceInfo sequence;

	public ValidSentenceSequencesExplorer(SentenceSequenceInfo sequence) {
		this.sequence = sequence;
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
	}

	public void exploreSequence() {
		initializaDataStructures();
		int last = sequence.numberOfSentences();

		for (int i = 1; i <= lastWithCC; i++) {
			for (int j = last; j >= nextWithCC[i]; j--) {
				sequence.validSequence(i, j);
			}
		}
	}

}