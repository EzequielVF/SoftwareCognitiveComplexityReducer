package neo.reducecognitivecomplexity.refactoringcache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;

import neo.reducecognitivecomplexity.Constants;
import neo.reducecognitivecomplexity.algorithms.Sequence;

public class SentencesSelectorVisitor extends ASTVisitor {

	private CompilationUnit compilationUnit;
	
	private List<Sequence> sentencesToIterate;

	public SentencesSelectorVisitor(CompilationUnit cu) {
		compilationUnit = cu;
		sentencesToIterate = new ArrayList<>();
	}

	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Block) {
			Block block = (Block) node;
			Sequence sequence = new Sequence(this.compilationUnit, (List<ASTNode>) block.statements());
			getSentencesToIterate().add(sequence);
		} else if (node instanceof Statement) {
			switch (node.getParent().getNodeType()) {
			case ASTNode.DO_STATEMENT:
			case ASTNode.ENHANCED_FOR_STATEMENT:
			case ASTNode.FOR_STATEMENT:
			case ASTNode.IF_STATEMENT:
			case ASTNode.WHILE_STATEMENT:
				Integer acc = (Integer) node.getProperty(Constants.ACCUMULATED_COMPLEXITY);
				if (acc != null && acc > 0) {
					getSentencesToIterate().add(new Sequence(this.compilationUnit, Arrays.asList(node)));
				}
				break;
			}
		}

		if (node instanceof SwitchStatement) {
			SwitchStatement sswitch = (SwitchStatement) node;

			List<ASTNode> currentBlock = new ArrayList<>();
			for (Statement stmt : (List<Statement>) sswitch.statements()) {
				if (stmt instanceof SwitchCase) {
					if (!currentBlock.isEmpty()) {
						getSentencesToIterate().add(new Sequence(this.compilationUnit,currentBlock));
						currentBlock = new ArrayList<>();
					}
				} else {
					currentBlock.add(stmt);
				}
			}
			if (!currentBlock.isEmpty()) {
				getSentencesToIterate().add(new Sequence(this.compilationUnit,currentBlock));
			}
		}

		super.preVisit(node);
	}

	@Override
	public void postVisit(ASTNode node) {
		super.postVisit(node);
	}

	public List<Sequence> getSentencesToIterate() {
		return sentencesToIterate;
	}
}