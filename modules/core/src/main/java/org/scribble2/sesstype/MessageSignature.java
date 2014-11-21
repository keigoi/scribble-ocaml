package org.scribble2.sesstype;

import java.util.List;

import org.scribble2.sesstype.name.Kind;
import org.scribble2.sesstype.name.Operator;
import org.scribble2.sesstype.name.PayloadTypeOrParameter;

// FIXME: rename to UnscopedMessageSignature
public class MessageSignature implements Message
{
	public final Operator op;
	public final List<PayloadTypeOrParameter> payload;
	
	public MessageSignature(Operator op, List<PayloadTypeOrParameter> payload)
	{
		this.op = op;
		this.payload = payload;
	}

	/*@Override
	public ScopedMessage toScopedMessage(Scope scope)
	{
		return new ScopedMessageSignature(scope, this.op, this.payload);
	}*/

	@Override
	public Kind getKind()
	{
		return Kind.SIG;
	}

	@Override
	public int hashCode()
	{
		int hash = 3187;
		hash = 31 * hash + this.op.hashCode();
		hash = 31 * hash + this.payload.hashCode();
		return hash;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o)
		{
			return true;
		}
		if (o == null || this.getClass() != o.getClass())
		{
			return false;
		}
		MessageSignature ms = (MessageSignature) o;
		return this.op.equals(ms.op) && this.payload.equals(ms.payload);
	}
	
	@Override
	public String toString()
	{
		String payload = "";
		if (!this.payload.isEmpty())
		{
			payload += this.payload.get(0).toString();
			for (PayloadTypeOrParameter ptpn : this.payload.subList(1, this.payload.size()))
			{
				payload+= ", " + ptpn;
			}
		}
		return this.op.toString() + "(" + payload + ")";
	}
	
	@Override
	public boolean isParameter()
	{
		return false;
	}
}
