package com.massivecraft.factions.cmd;

import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.FactionColl;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.event.EventFactionsDisband;
import com.massivecraft.factions.event.EventFactionsMembershipChange;
import com.massivecraft.factions.event.EventFactionsMembershipChange.MembershipChangeReason;
import com.massivecraft.factions.Factions;
import com.massivecraft.factions.Perm;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.util.IdUtil;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsDisband extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsDisband()
	{
		// Aliases
		this.addAliases("disband");

		// Parameters
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.DISBAND.node));
	}

	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Faction faction = this.readArg(msenderFaction);
		
		// MPerm
		if ( ! MPerm.getPermDisband().has(msender, faction, true)) return;

		// Verify
		if (faction.getFlag(MFlag.getFlagPermanent()))
		{
			msg("<i>Cette faction est désignée comme permanente, donc vous ne pouvez pas la dissoudre.");
			return;
		}

		// Event
		EventFactionsDisband event = new EventFactionsDisband(me, faction);
		event.run();
		if (event.isCancelled()) return;

		// Merged Apply and Inform
		
		// Run event for each player in the faction
		for (MPlayer mplayer : faction.getMPlayers())
		{
			EventFactionsMembershipChange membershipChangeEvent = new EventFactionsMembershipChange(sender, mplayer, FactionColl.get().getNone(), MembershipChangeReason.DISBAND);
			membershipChangeEvent.run();
		}

		// Inform
		for (MPlayer mplayer : faction.getMPlayersWhereOnline(true))
		{
			mplayer.msg("<h>%s<i> a dissous votre faction.", msender.describeTo(mplayer));
		}
		
		if (msenderFaction != faction)
		{
			msender.msg("<i>Vous avez dissous <h>%s<i>." , faction.describeTo(msender));
		}
		
		// Log
		if (MConf.get().logFactionDisband)
		{
			Factions.get().log(Txt.parse("<i>La faction <h>%s <i>(<h>%s<i>) a était dissoute par <h>%s<i>.", faction.getName(), faction.getId(), msender.getDisplayName(IdUtil.getConsole())));
		}		
		
		// Apply
		faction.detach();
	}
	
}
