package com.massivecraft.factions.cmd;

import java.util.ArrayList;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Perm;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.req.ReqHasntFaction;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.event.EventFactionsCreate;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.primitive.TypeString;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.store.MStore;

public class CmdFactionsCreate extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsCreate()
	{
		// Aliases
		this.addAliases("create", "new");

		// Parameters
		this.addParameter(TypeString.get(), "name");

		// Requirements
		this.addRequirements(ReqHasntFaction.get());
		this.addRequirements(RequirementHasPerm.get(Perm.CREATE.node));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		String newName = this.readArg();
		
		// Verify
		if (FactionColl.get().isNameTaken(newName))
		{
			msg("<b>Ce nom est déjà utilisé.");
			return;
		}
		
		ArrayList<String> nameValidationErrors = FactionColl.get().validateName(newName);
		if (nameValidationErrors.size() > 0)
		{
			message(nameValidationErrors);
			return;
		}

		// Pre-Generate Id
		String factionId = MStore.createId();
		
		// Event
		EventFactionsCreate createEvent = new EventFactionsCreate(sender, factionId, newName);
		createEvent.run();
		if (createEvent.isCancelled()) return;
		
		// Apply
		Faction faction = FactionColl.get().create(factionId);
		faction.setName(newName);
		
		msender.setRole(Rel.LEADER);
		msender.setFaction(faction);
		
		EventFactionsMembershipChange joinEvent = new EventFactionsMembershipChange(sender, msender, faction, MembershipChangeReason.CREATE);
		joinEvent.run();
		// NOTE: join event cannot be cancelled or you'll have an empty faction
		
		// Inform
		msg("<i>Vous avez créé(e) la faction %s", faction.getName(msender));
		message(Mson.mson(mson("Vous devez maintenant: ").color(ChatColor.YELLOW), CmdFactions.get().cmdFactionsDescription.getTemplate()));

		// Log
		if (MConf.get().logFactionCreate)
		{
			Factions.get().log(msender.getName()+" a créé(e) une nouvelle faction: "+newName);
		}
	}
	
}
