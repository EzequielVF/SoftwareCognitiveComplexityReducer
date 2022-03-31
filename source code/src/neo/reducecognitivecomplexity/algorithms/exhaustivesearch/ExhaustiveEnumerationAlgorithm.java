package neo.reducecognitivecomplexity.algorithms.exhaustivesearch;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTNode;

import neo.reducecognitivecomplexity.algorithms.RefactoringCache;
import neo.reducecognitivecomplexity.algorithms.Sequence;
import neo.reducecognitivecomplexity.algorithms.exhaustivesearch.ConsecutiveSequenceIterator.APPROACH;

public class ExhaustiveEnumerationAlgorithm {
	private ASTNode method;
	private RefactoringCache refactoringCache;
	private SentencesSelectorVisitor sentencesSelectorVisitor;
	private APPROACH approach;

	public ExhaustiveEnumerationAlgorithm(RefactoringCache refactoringCache, ASTNode method, APPROACH approach) {
		this.refactoringCache = refactoringCache;
		this.method = method;
		this.approach = approach;
		sentencesSelectorVisitor = new SentencesSelectorVisitor();
		method.accept(sentencesSelectorVisitor);
	}

	public void run(Consumer<List<Sequence>> consumer, long maxElements) {
		List<Iterable<List<Sequence>>> elementsToIterate = sentencesSelectorVisitor.getSentencesToIterate().stream()
				.map(sequence -> new SentenceSequenceIterator(sequence, refactoringCache, approach))
				.collect(Collectors.toList());

		ExhaustiveEnumeration<List<Sequence>> ee = new ExhaustiveEnumeration<>(elementsToIterate, t -> true);

		ee.run(solution -> {
			List<Sequence> result = solution.stream().reduce(new ArrayList<>(), (a, b) -> {
				a.addAll(b);
				return a;
			});
			consumer.accept(result);
		}, maxElements);

	}

	public BigInteger count() {
		List<Iterable<List<Sequence>>> elementsToIterate = sentencesSelectorVisitor.getSentencesToIterate().stream()
				.map(sequence -> new SentenceSequenceIterator(sequence, refactoringCache, approach))
				.collect(Collectors.toList());

		ExhaustiveEnumeration<List<Sequence>> ee = new ExhaustiveEnumeration<>(elementsToIterate, t -> true);
		return ee.count();
	}
}
