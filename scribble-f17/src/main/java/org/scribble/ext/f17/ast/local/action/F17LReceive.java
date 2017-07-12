package org.scribble.ext.f17.ast.local.action;

import org.scribble.ext.f17.ast.F17AstFactory;
import org.scribble.ext.f17.ast.F17MessageAction;
import org.scribble.sesstype.Payload;
import org.scribble.sesstype.name.Op;
import org.scribble.sesstype.name.Role;

public class F17LReceive extends F17LInput implements F17MessageAction
{
	public final Op op;
	public final Payload pay;
	
	public F17LReceive(Role self, Role peer, Op op, Payload pay)
	{
		super(self, peer);
		this.op = op;
		this.pay = pay;
	}
	
	@Override
	public boolean isMessageAction()
	{
		return true;
	}
	
	@Override
	public F17LSend toDual()
	{
		return F17AstFactory.FACTORY.LSend(this.peer, this.self, this.op, this.pay);
	}
	
	@Override
	public String toString()
	{
		return super.toString() + "?" + this.op + this.pay;
	} 

	@Override
	public int hashCode()
	{
		int hash = 37;
		hash = 31 * hash + super.hashCode();
		hash = 31 * hash + this.op.hashCode();
		hash = 31 * hash + this.pay.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!(obj instanceof F17LReceive))
		{
			return false;
		}
		F17LReceive them = (F17LReceive) obj;
		return super.equals(obj)  // super does canEquals (and checks self/peer)
				&& this.op.equals(them.op) && this.pay.equals(them.pay);
	}
	
	@Override
	protected boolean canEquals(Object o)
	{
		return o instanceof F17LReceive;
	}

	@Override
	public Op getOp()
	{
		return this.op;
	}

	@Override
	public Payload getPayload()
	{
		return this.pay;
	}
}