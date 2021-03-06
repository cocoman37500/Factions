package com.massivecraft.factions.cmd;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.massivecraft.factions.Perm;
import com.massivecraft.factions.Rel;
import com.massivecraft.factions.cmd.type.TypeFaction;
import com.massivecraft.factions.entity.BoardColl;
import com.massivecraft.factions.entity.MConf;
import com.massivecraft.factions.entity.MFlag;
import com.massivecraft.factions.entity.MPerm;
import com.massivecraft.factions.entity.MPlayer;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.factions.event.EventFactionsHomeTeleport;
import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.requirement.RequirementIsPlayer;
import com.massivecraft.massivecore.mixin.Mixin;
import com.massivecraft.massivecore.mixin.TeleporterException;
import com.massivecraft.massivecore.ps.PS;
import com.massivecraft.massivecore.teleport.Destination;
import com.massivecraft.massivecore.teleport.DestinationSimple;
import com.massivecraft.massivecore.util.MUtil;


public class CmdFactionsHome extends FactionsCommandHome
{
	// -------------------------------------------- //
	// CONSTRUCT
	// -------------------------------------------- //
	
	public CmdFactionsHome()
	{
		// Aliases
		this.addAliases("home");
		
		// Parameters
		this.addParameter(TypeFaction.get(), "faction", "you");

		// Requirements
		this.addRequirements(RequirementHasPerm.get(Perm.HOME.node));
		this.addRequirements(RequirementIsPlayer.get());
	}
	
	// -------------------------------------------- //
	// OVERRIDE
	// -------------------------------------------- //
	
	@Override
	public void perform() throws MassiveException
	{
		if ( ! MConf.get().homesTeleportCommandEnabled)
		{
			msender.msg("<b>Désolé, la capacité à se téléporter à l'home de la faction est désactivé sur le serveur.");
			return;
		}
		
		// Args
		Faction faction = this.readArg(msenderFaction);
		PS home = faction.getHome();
		String homeDesc = "home pour " + faction.describeTo(msender, false);
		
		// Any and MPerm
		if ( ! MPerm.getPermHome().has(msender, faction, true)) return;
		
		if (home == null)
		{
			msender.msg("<b>%s <b>n'a pas de home.", faction.describeTo(msender, true));
			
			if (MPerm.getPermSethome().has(msender, faction, false))
			{
				msender.msg("<i>Vous devez:");
				msender.message(CmdFactions.get().cmdFactionsSethome.getTemplate());
			}
			
			return;
		}
		
		if ( ! MConf.get().homesTeleportAllowedFromEnemyTerritory && msender.isInEnemyTerritory())
		{
			msender.msg("<b>Vous ne pouvez téléporter sur %s <b>tant qu'il se trouve sur le territoire d'une faction.", homeDesc);
			return;
		}
		
		if ( ! MConf.get().homesTeleportAllowedFromDifferentWorld && !me.getWorld().getName().equalsIgnoreCase(home.getWorld()))
		{
			msender.msg("<b>Vous ne pouvez téléporter sur %s <b>tant qu'il se trouve dans un monde différent.", homeDesc);
			return;
		}
		
		
		Faction factionHere = BoardColl.get().getFactionAt(PS.valueOf(me.getLocation()));
		Location locationHere = me.getLocation().clone();
		
		// if player is not in a safe zone or their own faction territory, only allow teleport if no enemies are nearby
		if
		(
			MConf.get().homesTeleportAllowedEnemyDistance > 0
			&&
			factionHere.getFlag(MFlag.getFlagPvp())
			&&
			(
				! msender.isInOwnTerritory()
				||
				(
					msender.isInOwnTerritory()
					&&
					! MConf.get().homesTeleportIgnoreEnemiesIfInOwnTerritory
				)
			)
		)
		{
			World w = locationHere.getWorld();
			double x = locationHere.getX();
			double y = locationHere.getY();
			double z = locationHere.getZ();

			for (Player p : me.getServer().getOnlinePlayers())
			{
				if (MUtil.isntPlayer(p)) continue;
				
				if (p == null || !p.isOnline() || p.isDead() || p == me || p.getWorld() != w)
					continue;

				MPlayer fp = MPlayer.get(p);
				if (msender.getRelationTo(fp) != Rel.ENEMY)
					continue;

				Location l = p.getLocation();
				double dx = Math.abs(x - l.getX());
				double dy = Math.abs(y - l.getY());
				double dz = Math.abs(z - l.getZ());
				double max = MConf.get().homesTeleportAllowedEnemyDistance;

				// box-shaped distance check
				if (dx > max || dy > max || dz > max)
					continue;

				msender.msg("<b>Vous ne pouvez téléporter sur %s <b>tant qu'un ennemi se trouve à moins de %f blocs de vous.", homeDesc, MConf.get().homesTeleportAllowedEnemyDistance);
				return;
			}
		}

		// Event
		EventFactionsHomeTeleport event = new EventFactionsHomeTeleport(sender);
		event.run();
		if (event.isCancelled()) return;
		
		// Apply
		try
		{
			Destination destination = new DestinationSimple(home, homeDesc);
			Mixin.teleport(me, destination, sender);
		}
		catch (TeleporterException e)
		{
			me.sendMessage(e.getMessage());
		}
	}
	
}
