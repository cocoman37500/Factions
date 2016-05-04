package com.massivecraft.factions.cmd;

import java.util.Collection;

import org.bukkit.ChatColor;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.cmd.type.TypeMPlayer;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.event.EventFactionsInvitedChange;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.container.TypeSet;
import com.massivecraft.massivecore.mson.Mson;
import com.massivecraft.massivecore.util.Txt;

public class CmdFactionsInviteAdd extends FactionsCommand
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	public CmdFactionsInviteAdd()
	{
		// Aliases
		this.addAliases("add");

		// Parameters
		this.addParameter(TypeSet.get(TypeMPlayer.get()), "players", true);
		
		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.INVITE_ADD.node));
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //	
	
	@Override
	public void perform() throws MassiveException
	{
		// Args
		Collection<MPlayer> mplayers = this.readArg();
		
		// MPerm
		if ( ! MPerm.getPermInvite().has(msender, msenderFaction, true)) return;
		
		for (MPlayer mplayer : mplayers)
		{	
			// Already member?
			if (mplayer.getFaction() == msenderFaction)
			{
				msg("%s<i> est déjà un membre de %s<i>.", mplayer.getName(), msenderFaction.getName());
				continue;
			}
			
			// Already invited?
			boolean isInvited = msenderFaction.isInvited(mplayer);
			
			if ( ! isInvited)
			{
				// Event
				EventFactionsInvitedChange event = new EventFactionsInvitedChange(sender, mplayer, msenderFaction, isInvited);
				event.run();
				if (event.isCancelled()) continue;
				isInvited = event.isNewInvited();
				
				// Inform
				mplayer.msg("%s<i> vous a invité(e) à rejoindre la faction %s<i>.", msender.describeTo(mplayer, true), msenderFaction.describeTo(mplayer));
				msenderFaction.msg("%s<i> a invité(e) %s<i> dans votre faction.", msender.describeTo(msenderFaction, true), mplayer.describeTo(msenderFaction));
				
				// Apply
				msenderFaction.setInvited(mplayer, true);
				msenderFaction.changed();
			}
			else
			{
				// Mson
				String command = CmdFactions.get().cmdFactionsInvite.cmdFactionsInviteRemove.getCommandLine(mplayer.getName());
				String tooltip = Txt.parse("<i>Clique sur <c>%s<i>.", command);
				
				Mson remove = Mson.mson(
					mson("Vous voudrez peut-être le retirer. ").color(ChatColor.YELLOW), 
					mson("Clique sur " + command).color(ChatColor.RED).tooltip(tooltip).suggest(command)
				);
				
				// Inform
				msg("%s <i>est déjà invité(e) à rejoindre %s<i>.", mplayer.getName(), msenderFaction.getName());
				message(remove);
			}
		}
	}
	
}
