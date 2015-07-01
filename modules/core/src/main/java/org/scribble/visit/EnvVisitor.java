package org.scribble.visit;

import java.util.Stack;

import org.scribble.ast.ProtocolDecl;
import org.scribble.ast.ScribNode;
import org.scribble.main.ScribbleException;
import org.scribble.sesstype.kind.ProtocolKind;
import org.scribble.visit.env.Env;

public abstract class EnvVisitor<T extends Env> extends ModuleVisitor
{
	private Stack<T> envs = new Stack<T>();
	
	public EnvVisitor(Job job)
	{
		super(job);
	}
	
	@Override
	protected final void enter(ScribNode parent, ScribNode child) throws ScribbleException
	{
		super.enter(parent, child);
		if (child instanceof ProtocolDecl)  // Only the root ProtocolDecl is visited: subprotocols visit the body directly
		{
			ProtocolDecl<? extends ProtocolKind> pd = (ProtocolDecl<?>) child;
			pushEnv(makeRootProtocolDeclEnv(pd));
		}
		envEnter(parent, child);
	}
	
	@Override
	protected final ScribNode leave(ScribNode parent, ScribNode child, ScribNode visited) throws ScribbleException
	{
		ScribNode n = envLeave(parent, child, visited); 
		if (n instanceof ProtocolDecl)  // Only the root ProtocolDecl is visited by SubprotocolVisitor (subprotocols visit the body directly)
		{
			this.envs.pop();
		}
		return super.leave(parent, child, n);
	}
	
	protected abstract T makeRootProtocolDeclEnv(ProtocolDecl<? extends ProtocolKind> pd);

	protected void envEnter(ScribNode parent, ScribNode child) throws ScribbleException
	{
		//... HERE: push copy of parent Env onto visitor stack for use by visitor pass (del env-leave routine should pop and push back the final result)
		//... only if want an env for every node (unless restrict to specific nodes types -- e.g. interaction nodes) -- as opposed to only e.g. compound nodes, as done via del
		//... so either do base env management here or via del -- here, need to do instanceof; in delegates, need to duplicate base push/pop for each pass
		//... no: should be only compound interaction nodes, as typing environments
		//return this;
	}
	
	protected ScribNode envLeave(ScribNode parent, ScribNode child, ScribNode visited) throws ScribbleException
	{
		return visited;
	}
	
	// Hack? e.g. for ModuleDecl
	public boolean hasEnv()
	{
		return !this.envs.isEmpty();
	}

	public T peekEnv()
	{
		return this.envs.peek();
	}

	public T peekParentEnv()
	{
		return this.envs.get(this.envs.size() - 2);
	}
	
	public void pushEnv(T env)
	{
		this.envs.push(env);
	}
	
	public T popEnv()
	{
		return this.envs.pop();
	}
}
