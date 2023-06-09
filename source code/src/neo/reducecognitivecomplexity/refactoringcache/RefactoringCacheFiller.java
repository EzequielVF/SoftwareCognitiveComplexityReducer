package neo.reducecognitivecomplexity.refactoringcache;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.EmptyStatement;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.algorithms.Sequence;
import neo.reducecognitivecomplexity.jdt.CodeExtractionMetrics;
import neo.reducecognitivecomplexity.refactoringcache.ConsecutiveSequenceIterator.SentenceSequenceInfo;

public class RefactoringCacheFiller {

	private static void exploreSentenceSequence(Sequence sentences, RefactoringCache refactoringCache) {
		new ValidSentenceSequencesExplorer(new SentenceSequenceInfo() {
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
						.getMetrics(new Sequence(refactoringCache.getCompilationUnit(), sentences.getSiblingNodes().subList(from - 1, to)));
				return cem.isFeasible();
			}

			private boolean isEmptyStatement(int sentence) {
				ASTNode node = sentences.getSiblingNodes().get(sentence - 1);
				return (node instanceof EmptyStatement);
			}
		}).exploreSequence();
	}

	public static void exhaustiveEnumerationAlgorithm(RefactoringCache refactoringCache, ASTNode method) {
		SentencesSelectorVisitor sentencesSelectorVisitor = new SentencesSelectorVisitor(refactoringCache.getCompilationUnit());
		method.accept(sentencesSelectorVisitor);

		sentencesSelectorVisitor.getSentencesToIterate().stream()
				.forEach(sequence -> exploreSentenceSequence(sequence, refactoringCache));
	}

}
