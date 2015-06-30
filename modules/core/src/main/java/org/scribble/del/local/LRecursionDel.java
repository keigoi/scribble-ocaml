package org.scribble.del.local;

import java.util.HashSet;
import java.util.Set;

import org.scribble.ast.ScribNode;
import org.scribble.ast.local.LRecursion;
import org.scribble.main.ScribbleException;
import org.scribble.model.local.ProtocolState;
import org.scribble.sesstype.name.RecVar;
import org.scribble.visit.FsmBuilder;
import org.scribble.visit.ReachabilityChecker;
import org.scribble.visit.env.ReachabilityEnv;

public class LRecursionDel extends LCompoundInteractionNodeDel
{
	@Override
	public LRecursion leaveReachabilityCheck(ScribNode parent, ScribNode child, ReachabilityChecker checker, ScribNode visited) throws ScribbleException
	{
		LRecursion lr = (LRecursion) visited;
		ReachabilityEnv env = checker.popEnv().mergeContext((ReachabilityEnv) lr.block.del().env());
		env = env.removeContinueLabel(lr.recvar.toName());
		//merged.contExitable = this.contExitable;
		checker.pushEnv(env);
		return (LRecursion) super.leaveReachabilityCheck(parent, child, checker, visited);  // records the current checker Env to the current del; also pops and merges that env into the parent env*/
	}
	
	@Override
	public void enterFsmBuilder(ScribNode parent, ScribNode child, FsmBuilder conv)
	{
		super.enterFsmBuilder(parent, child, conv);
		LRecursion lr = (LRecursion) child;
		RecVar rv = lr.recvar.toName();
		/*Set<RecVar> labs = new HashSet<>();
		labs.add(rv);*/
		Set<String> labs = new HashSet<>(conv.builder.getEntry().getLabels());
		labs.add(rv.toString());
		ProtocolState s = conv.builder.newState(labs);
		conv.builder.setEntry(s);
		conv.builder.setRecursionEntry(rv);
	}

	@Override
	public LRecursion leaveFsmBuilder(ScribNode parent, ScribNode child, FsmBuilder conv, ScribNode visited)
	{
		LRecursion lr = (LRecursion) visited;
		RecVar rv = lr.recvar.toName();
		conv.builder.removeRecursionEntry(rv);
		return (LRecursion) super.leaveFsmBuilder(parent, child, conv, lr);
	}
}
