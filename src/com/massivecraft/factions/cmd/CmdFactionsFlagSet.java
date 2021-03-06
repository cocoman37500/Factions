package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.cmd.type.TypeMFlag;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.event.EventFactionsFlagChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.primitive.TypeBoolean;

public class CmdFactionsFlagSet extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsFlagSet()
	{
		// Aliases
		this.addAliases("set");
		
		// Parameters
		this.addParameter(TypeMFlag.get(), "flag");
		this.addParameter(TypeBoolean.getYes(), "yes/no");
		this.addParameter(TypeFaction.get(), "faction", "you");
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.FLAG_SET.node));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		MFlag flag = this.readArg();
		boolean value = this.readArg();
		Faction faction = this.readArg(msenderFaction);
		
		// Do the sender have the right to change flags for this faction?
		if ( ! MPerm.getPermFlags().has(msender, faction, true)) return;
		
		// Is this flag editable?
		if (!msender.isOverriding() && ! flag.isEditable())
		{
			msg("<b>Le flag <h>%s <b>n'est pas modifiable.", flag.getName());
			return;
		}
		
		// Event
		EventFactionsFlagChange event = new EventFactionsFlagChange(sender, faction, flag, value);
		event.run();
		if (event.isCancelled()) return;
		value = event.isNewValue();
		
		// No change 
		if (faction.getFlag(flag) == value)
		{
			msg("%s <i>a déjà %s <i>défini sur %s<i>.", faction.describeTo(msender), flag.getStateDesc(value, false, true, true, false, true), flag.getStateDesc(value, true, true, false, false, false));
			return;
		}
		
		// Apply
		faction.setFlag(flag, value);
		
		// Inform
		String stateInfo = flag.getStateDesc(faction.getFlag(flag), true, false, true, true, true);
		if (msender.getFaction() != faction)
		{
			// Send message to sender
			msg("<h>%s <i>a modifié(e) un flag pour <h>%s<i>.", msender.describeTo(msender, true), faction.describeTo(msender, true));
			message(stateInfo);
		}
		faction.msg("<h>%s <i>a modifié(e) un flag pour <h>%s<i>.", msender.describeTo(faction, true), faction.describeTo(faction, true));
		faction.sendMessage(stateInfo);
	}
	
}
