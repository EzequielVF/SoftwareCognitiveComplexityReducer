package test.neo.reducecognitivecomplexity.refactoringcache;

import java.util.Stack;

import org.junit.Test;

import neo.reducecognitivecomplexity.refactoringcache.ConsecutiveSequenceIterator;
import neo.reducecognitivecomplexity.refactoringcache.ConsecutiveSequenceIterator.APPROACH;
import neo.reducecognitivecomplexity.refactoringcache.ConsecutiveSequenceIterator.SentenceSequenceInfo;


class SentenceSequenceIteratorTest {

	@Test
	void test() {
		SentenceSequenceInfo info = new SentenceSequenceInfo() {

			@Override
			public int numberOfSentences() {
				return 1;
			}

			@Override
			public int cognitiveComplexityOfSentence(int sentence) {
				return 0;
			}

			@Override
			public boolean validSequence(int from, int to) {
				return true;
			}
			
		};
		
		ConsecutiveSequenceIterator ssi = new ConsecutiveSequenceIterator(info, APPROACH.LONG_SEQUENCE_FIRST);
		
		for (Stack<Integer> s: ssi.getIterable()) {
			System.out.println(s);
		}
	}

}
