package org.scribble.main;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.Module;
import org.scribble.model.endpoint.AutParser;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.global.GMState;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.LProtocolName;
import org.scribble.sesstype.name.ModuleName;
import org.scribble.sesstype.name.Role;
import org.scribble.util.ScribUtil;
import org.scribble.visit.context.EndpointGraphBuilder;
import org.scribble.visit.context.Projector;

// Global "static" context information for a Job -- single instance per Job and should never be shared
// Mutable: projections, graphs, etc are added mutably later -- replaceModule also mutable setter
public class JobContext
{
	private final Job job;

	public final ModuleName main;
	
	// ModuleName keys are full module names -- currently the modules read from file, distinguished from the generated projection modules
	private final Map<ModuleName, Module> parsed;// = new HashMap<>();
	
	// LProtocolName is the full local protocol name (module name is the prefix)
	private final Map<LProtocolName, Module> projected = new HashMap<>();

	private final Map<LProtocolName, EGraph> graphs = new HashMap<>();
	//private final Map<GProtocolName, GModel> gmodels = new HashMap<>();
	private final Map<GProtocolName, GMState> gmodels = new HashMap<>();
	
	private final Map<LProtocolName, EGraph> unfair = new HashMap<>();
	private final Map<GProtocolName, GMState> unfairGModels = new HashMap<>();
	
	private final Map<LProtocolName, EGraph> minimised = new HashMap<>();  // Toolchain currently depends on single instance of each graph (state id equality), e.g. cannot re-build or re-minimise, would not be the same graph instance
			// FIXME: currently only minimising "fair" graph, need to consider minimisation orthogonally to fairness -- NO: minimising (of fair) is for API gen only, unfair-transform does not use minimisation (regardless of user flag) for WF
	
	protected JobContext(Job job, Map<ModuleName, Module> parsed, ModuleName main)
	{
		this.job = job;

		this.parsed = new HashMap<ModuleName, Module>(parsed);
		this.main = main;
	}
	
	// Used by Job for pass running, includes projections (e.g. for reachability checking)
	// Safer to get module names and require user to re-fetch the module by the getter each time (after replacing), to make sure the latest is used
	public Set<ModuleName> getFullModuleNames()
	{
		Set<ModuleName> modnames = new HashSet<>();
		modnames.addAll(getParsedFullModuleNames());
		modnames.addAll(getProjectedFullModuleNames());
		return modnames;
	}

	public Set<ModuleName> getParsedFullModuleNames()
	{
		Set<ModuleName> modnames = new HashSet<>();
		modnames.addAll(this.parsed.keySet());
		return modnames;
	}

	public Set<ModuleName> getProjectedFullModuleNames()
	{
		return this.projected.keySet().stream().map((lpn) -> lpn.getPrefix()).collect(Collectors.toSet());
	}

	/*public boolean hasModule(ModuleName fullname)
	{
		return isParsedModule(fullname) || isProjectedModule(fullname);
	}*/
	
	private boolean isParsedModule(ModuleName fullname)
	{
		return this.parsed.containsKey(fullname);
	}
	
	private boolean isProjectedModule(ModuleName fullname)
	{
		//return this.projected.keySet().stream().filter((lpn) -> lpn.getPrefix().equals(fullname)).count() > 0;
		return getProjectedFullModuleNames().contains(fullname);
	}

	public Module getModule(ModuleName fullname)
	{
		if (isParsedModule(fullname))
		{
			return this.parsed.get(fullname);
		}
		else if (isProjectedModule(fullname))
		{
			return this.projected.get(
					this.projected.keySet().stream().filter((lpn) -> lpn.getPrefix().equals(fullname)).collect(Collectors.toList()).get(0));
		}
		else
		{
			throw new RuntimeException("Unknown module: " + fullname);
		}
	}

	protected void replaceModule(Module module)
	{
		ModuleName fullname = module.getFullModuleName(); 
		if (isParsedModule(fullname))
		{
			this.parsed.put(fullname, module);
		}
		else if (isProjectedModule(fullname))
		{
			addProjection(module);
		}
		else
		{
			throw new RuntimeException("Unknown module: " + fullname);
		}
	}
	
	// Make context immutable? (will need to assign updated context back to Job) -- will also need to do for Module replacing
	public void addProjections(Map<GProtocolName, Map<Role, Module>> projections)
	{
		for (GProtocolName gpn : projections.keySet())
		{
			Map<Role, Module> mods = projections.get(gpn);
			for (Role role : mods.keySet())
			{
				addProjection(mods.get(role));
			}
		}

		/*// Doesn't work for external subprotocols now that Projector doesn't record Module-specific dependencies itself
		try
		{
			ContextBuilder builder = new ContextBuilder(this.job);
			for (ProtocolName lpn : this.projections.keySet())
			{
				Module mod = this.projections.get(lpn);
				mod = (Module) mod.accept(builder);
				replaceModule(mod);
			}
		}
		catch (ScribbleException e)
		{
			throw new RuntimeException("Shouldn't get in here: " + e);
		}*/
	}

	private void addProjection(Module mod)
	{
		LProtocolName lpn = (LProtocolName) mod.getProtocolDecls().get(0).getFullMemberName(mod);
		this.projected.put(lpn, mod);
	}
	
	public Module getProjection(GProtocolName fullname, Role role) throws ScribbleException
	{
		Module proj = this.projected.get(Projector.projectFullProtocolName(fullname, role));
		if (proj == null)
		{
			throw new ScribbleException("Projection not found: " + fullname + ", " + role);  // E.g. disamb/enabling error before projection passes (e.g. CommandLine -fsm arg)
		}
		return proj;
	}
	
	//public void addGlobalModel(GProtocolName fullname, GModel model)
	public void addGlobalModel(GProtocolName fullname, GMState model)
	{
		this.gmodels.put(fullname, model);
	}

	public void addUnfairGlobalModel(GProtocolName fullname, GMState model)
	{
		this.unfairGModels.put(fullname, model);
	}
	
	//public GModel getGlobalModel(GProtocolName fullname)
	public GMState getGlobalModel(GProtocolName fullname)
	{
		return this.gmodels.get(fullname);
	}
	
	protected void addEndpointGraph(LProtocolName fullname, EGraph graph)
	{
		this.graphs.put(fullname, graph);
	}
	
	public EGraph getEndpointGraph(GProtocolName fullname, Role role) throws ScribbleException
	{
		////return this.graphs.get(Projector.projectFullProtocolName(fullname, role));
		//return getEndpointGraph(Projector.projectFullProtocolName(fullname, role));

		LProtocolName fulllpn = Projector.projectFullProtocolName(fullname, role);
		// Moved form LProtocolDecl
		EGraph graph = this.graphs.get(fulllpn);
		if (graph == null)
		{
			Module proj = getProjection(fullname, role);  // Projected module contains a single protocol
			EndpointGraphBuilder builder = new EndpointGraphBuilder(this.job);
			proj.accept(builder);
			graph = builder.builder.finalise();  // Projected module contains a single protocol
			addEndpointGraph(fulllpn, graph);
		}
		return graph;
	}

  /*// Full projected name
	protected EndpointGraph getEndpointGraph(LProtocolName fullname) throws ScribbleException
	{
		EndpointGraph graph = this.graphs.get(fullname);
		if (graph == null)
		{
			//throw new RuntimeException("FIXME: " + fullname);
			throw new ScribbleException(": " + fullname);  // FIXME checked exception for CommandLine
		}
		return graph;
	}*/
	
	protected void addMinimisedEndpointGraph(LProtocolName fullname, EGraph graph)
	{
		this.minimised.put(fullname, graph);
	}
	
	public EGraph getMinimisedEndpointGraph(GProtocolName fullname, Role role) throws ScribbleException
	{
		//return getMinimisedEndpointGraphAux(Projector.projectFullProtocolName(fullname, role));
		return getMinimisedEndpointGraphAux(fullname, role);
	}

  // Full projected name
	//protected EndpointGraph getMinimisedEndpointGraphAux(LProtocolName fullname)
	protected EGraph getMinimisedEndpointGraphAux(GProtocolName fullname, Role role) throws ScribbleException
	{
		LProtocolName fulllpn = Projector.projectFullProtocolName(fullname, role);

		EGraph minimised = this.minimised.get(fulllpn);
		if (minimised == null)
		{
			String aut = runAut(getEndpointGraph(fullname, role).init.toAut(), fulllpn + ".aut");
			minimised = new AutParser().parse(aut);
			addMinimisedEndpointGraph(fulllpn, minimised);
		}
		return minimised;
	}
	
	protected void addUnfairEndpointGraph(LProtocolName fullname, EGraph graph)
	{
		this.unfair.put(fullname, graph);
	}
	
	public EGraph getUnfairEndpointGraph(GProtocolName fullname, Role role) throws ScribbleException
	{
		//return getUnfairEndpointGraphAux(Projector.projectFullProtocolName(fullname, role));
		return getUnfairEndpointGraphAux(fullname, role);
	}

  // Full projected name
	//protected EndpointGraph getUnfairEndpointGraphAux(LProtocolName fullname)
	protected EGraph getUnfairEndpointGraphAux(GProtocolName fullname, Role role) throws ScribbleException
	{
		LProtocolName fulllpn = Projector.projectFullProtocolName(fullname, role);

		EGraph unfair = this.unfair.get(fulllpn);
		if (unfair == null)
		{
			unfair = getEndpointGraph(fullname, role).init.unfairTransform().toGraph();
			addUnfairEndpointGraph(fulllpn, unfair);
		}
		return unfair;
	}

	public Module getMainModule()
	{
		return getModule(this.main);
	}

	// Duplicated from CommandLine.runDot
	// Minimises the FSM up to bisimulation
	// N.B. ltsconvert will typically re-number the states
	private static String runAut(String fsm, String aut) throws ScribbleException
	{
		String tmpName = aut + ".tmp";
		File tmp = new File(tmpName);
		if (tmp.exists())  // Factor out with CommandLine.runDot (file exists check)
		{
			throw new RuntimeException("Cannot overwrite: " + tmpName);
		}
		try
		{
			ScribUtil.writeToFile(tmpName, fsm);
			String[] res = ScribUtil.runProcess("ltsconvert", "-ebisim", "-iaut", "-oaut", tmpName);
			if (!res[1].isEmpty())
			{
				throw new RuntimeException(res[1]);
			}
			return res[0];
		}
		finally
		{
			tmp.delete();
		}
	}
}
