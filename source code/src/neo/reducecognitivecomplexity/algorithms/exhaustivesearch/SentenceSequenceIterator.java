package neo.reducecognitivecomplexity.algorithms.exhaustivesearch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EmptyStatement;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.Utils;
import neo.reducecognitivecomplexity.algorithms.RefactoringCache;
import neo.reducecognitivecomplexity.algorithms.Sequence;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.ConsecutiveSequenceIterator.SentenceSequenceInfo;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;

public class SentenceSequenceIterator implements Iterable<List<Sequence>> {
	// TODO: efficiency

	private Sequence sentences;
	private ConsecutiveSequenceIterator csi;
	private Iterable<List<Sequence>> iterable = null;

	public SentenceSequenceIterator(Sequence sentences, RefactoringCache refactoringCache,
			ConsecutiveSequenceIterator.APPROACH approach) {
		this.sentences = sentences;
		csi = new ConsecutiveSequenceIterator(new SentenceSequenceInfo() {
			@Override
			public int numberOfSentences() {
				return sentences.getSiblingNodes().size();
			}

			@Override
			public int cognitiveComplexityOfSentence(int sentence) {
				Integer acc = (Integer) sentences.getSiblingNodes().get(sentence - 1)
						.getProperty(Constants.ACCUMULATED_COMPLEXITY);
				if (acc == null) {
					acc = 0;
				}
				return acc;
			}

			@Override
			public boolean validSequence(int from, int to) {
				if (isEmptyStatement(from) || isEmptyStatement(to)) {
					return false;
				}
				CodeExtractionMetrics cem = refactoringCache
						.getMetrics(new Sequence(sentences.getSiblingNodes().subList(from - 1, to)));
				return cem.isFeasible();
			}

			private boolean isEmptyStatement(int sentence) {
				ASTNode node = sentences.getSiblingNodes().get(sentence - 1);
				return (node instanceof EmptyStatement);
			}
		}, approach);
	}

	public Iterable<List<Sequence>> getIterable() {
		if (iterable == null) {
			iterable = Utils.adapt(csi.getIterable(), this::adapt);
		}
		return iterable;
	}

	private List<Sequence> adapt(Stack<Integer> stack) {
		List<Sequence> result = new ArrayList<>();
		for (int i = 0; i < stack.size(); i = i + 2) {
			ArrayList<ASTNode> nodes = new ArrayList<>(
					sentences.getSiblingNodes().subList(stack.get(i) - 1, stack.get(i + 1)));
			result.add(new Sequence(nodes));
		}
		return result;
	}

	@Override
	public Iterator<List<Sequence>> iterator() {
		return getIterable().iterator();
	}

}
