package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.api.*;
import com.worldcretornica.plotme_core.api.event.InternalPlotCreateEvent;

import net.milkbowl.vault.economy.EconomyResponse;

public class CmdAuto extends PlotCommand {

    public CmdAuto(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(IPlayer p, String[] args) {
        if (plugin.cPerms(p, "PlotMe.use.auto")) {
            if (!plugin.getPlotMeCoreManager().isPlotWorld(p) && !sob.getConfig().getBoolean("allowWorldTeleport")) {
                p.sendMessage(RED + C("MsgNotPlotWorld"));
            } else {
                IWorld w;

                if (!plugin.getPlotMeCoreManager().isPlotWorld(p) && sob.getConfig().getBoolean("allowWorldTeleport")) {
                    if (args.length == 2) {
                        w = sob.getWorld(args[1]);
                    } else {
                        w = plugin.getPlotMeCoreManager().getFirstWorld();
                    }

                    if (w == null || !plugin.getPlotMeCoreManager().isPlotWorld(w)) {
                        p.sendMessage(RED + args[1] + " " + C("MsgWorldNotPlot"));
                        return true;
                    }
                } else {
                    w = p.getWorld();
                }

                if (w == null) {
                    p.sendMessage(RED + C("MsgNoPlotworldFound"));
                } else {
                    
                    int playerlimit = plugin.getPlotLimit(p);
                    
                    if (playerlimit != -1 && plugin.getPlotMeCoreManager().getNbOwnedPlot(p, w) >= playerlimit && !plugin.cPerms(p, "PlotMe.admin")) {
                        p.sendMessage(RED + C("MsgAlreadyReachedMaxPlots") + " ("
                                + plugin.getPlotMeCoreManager().getNbOwnedPlot(p, w) + "/" + playerlimit + "). " + C("WordUse") + " " + RED + "/plotme " + C("CommandHome") + RESET + " " + C("MsgToGetToIt"));
                    } else {
                        PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(w);
                        int limit = pmi.getPlotAutoLimit();

                        String next = pmi.getNextFreed();
                        String id = "";

                        if (plugin.getPlotMeCoreManager().isPlotAvailable(next, w)) {
                            id = next;
                        } else {
                            int x = plugin.getPlotMeCoreManager().getIdX(next);
                            int z = plugin.getPlotMeCoreManager().getIdZ(next);

                            toploop:
                            for (int i = Math.max(Math.abs(x), Math.abs(z)); i < limit;) {
                                for (; x <= i; x++) {
                                    for (; z <= i; z++) {
                                        id = "" + x + ";" + z;

                                        if (plugin.getPlotMeCoreManager().isPlotAvailable(id, w)) {
                                            pmi.setNextFreed(id);
                                            break toploop;
                                        }
                                    }
                                }

                                i++;
                                x = -i;
                                z = -i;

                                if (i >= limit) {
                                    p.sendMessage(RED + C("MsgNoPlotFound1") + " " + (limit ^ 2) + " " + C("MsgNoPlotFound2"));
                                    return false;
                                }
                            }
                        }

                        double price = 0;

                        InternalPlotCreateEvent event;

                        if (plugin.getPlotMeCoreManager().isEconomyEnabled(w)) {
                            price = pmi.getClaimPrice();
                            double balance = sob.getBalance(p);

                            if (balance >= price) {
                                event = sob.getEventFactory().callPlotCreatedEvent(plugin, w, id, p);

                                if (event.isCancelled()) {
                                    return true;
                                } else {
                                    EconomyResponse er = sob.withdrawPlayer(p, price);

                                    if (!er.transactionSuccess()) {
                                        p.sendMessage(RED + er.errorMessage);
                                        Util().warn(er.errorMessage);
                                        return true;
                                    }
                                }
                            } else {
                                p.sendMessage(RED + C("MsgNotEnoughAuto") + " " + C("WordMissing") + " " + RESET + Util().moneyFormat(price - balance, false));
                                return true;
                            }
                        } else {
                            event = sob.getEventFactory().callPlotCreatedEvent(plugin, w, id, p);
                        }

                        if (!event.isCancelled()) {
                            plugin.getPlotMeCoreManager().createPlot(w, id, p.getName(), p.getUniqueId());
                            pmi.removeFreed(id);

                            //plugin.getPlotMeCoreManager().adjustLinkedPlots(id, w);
                            p.teleport(plugin.getPlotMeCoreManager().getPlotHome(w, id));

                            p.sendMessage(C("MsgThisPlotYours") + " " + C("WordUse") + " " + RED + "/plotme " + C("CommandHome") + RESET + " " + C("MsgToGetToIt") + " " + Util().moneyFormat(-price));

                            if (isAdvancedLogging()) {
                                plugin.getLogger().info(LOG + p.getName() + " " + C("MsgClaimedPlot") + " " + id + ((price != 0) ? " " + C("WordFor") + " " + price : ""));
                            }

                        }
                        return true;
                    }
                }
            }
        } else {
            p.sendMessage(RED + C("MsgPermissionDenied"));
        }
        return true;
    }
}
