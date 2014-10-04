package com.worldcretornica.plotme_core.commands;

import com.worldcretornica.plotme_core.Plot;
import com.worldcretornica.plotme_core.PlotMapInfo;
import com.worldcretornica.plotme_core.PlotMe_Core;
import com.worldcretornica.plotme_core.api.*;
import com.worldcretornica.plotme_core.api.event.InternalPlotOwnerChangeEvent;

import net.milkbowl.vault.economy.EconomyResponse;

public class CmdSetOwner extends PlotCommand {

    public CmdSetOwner(PlotMe_Core instance) {
        super(instance);
    }

    public boolean exec(IPlayer p, String[] args) {
        if (plugin.cPerms(p, "PlotMe.admin.setowner")) {
            if (!plugin.getPlotMeCoreManager().isPlotWorld(p)) {
                p.sendMessage(RED + C("MsgNotPlotWorld"));
            } else {
                String id = plugin.getPlotMeCoreManager().getPlotId(p.getLocation());
                if (id.isEmpty()) {
                    p.sendMessage(RED + C("MsgNoPlotFound"));
                } else if (args.length < 2 || args[1].isEmpty()) {
                    p.sendMessage(C("WordUsage") + ": " + RED + "/plotme " + C("CommandSetowner") + " <" + C("WordPlayer") + ">");
                } else {
                    String newowner = args[1];
                    String oldowner = "<" + C("WordNotApplicable") + ">";
                    String playername = p.getName();

                    if (!plugin.getPlotMeCoreManager().isPlotAvailable(id, p)) {
                        Plot plot = plugin.getPlotMeCoreManager().getPlotById(p, id);

                        PlotMapInfo pmi = plugin.getPlotMeCoreManager().getMap(p);
                        oldowner = plot.getOwner();
                        
                        InternalPlotOwnerChangeEvent event;

                        if (plugin.getPlotMeCoreManager().isEconomyEnabled(p)) {
                            if (pmi.isRefundClaimPriceOnSetOwner() && newowner != oldowner) {
                                event = sob.getEventFactory().callPlotOwnerChangeEvent(plugin, p.getWorld(), plot, p, newowner);

                                if (event.isCancelled()) {
                                    return true;
                                } else {
                                    if (plot.getOwnerId() != null) {
                                        IOfflinePlayer playeroldowner = sob.getOfflinePlayer(plot.getOwnerId());
                                        EconomyResponse er = sob.depositPlayer(playeroldowner, pmi.getClaimPrice());
    
                                        if (!er.transactionSuccess()) {
                                            p.sendMessage(RED + er.errorMessage);
                                            Util().warn(er.errorMessage);
                                            return true;
                                        } else {
                                            IPlayer player = sob.getPlayer(playeroldowner.getUniqueId());
                                            if (player != null) {
                                                player.sendMessage(C("MsgYourPlot") + " " + id + " " + C("MsgNowOwnedBy") + " " + newowner + ". " + Util().moneyFormat(pmi.getClaimPrice()));
                                            }
                                        }
                                    }
                                }
                            } else {
                                event = sob.getEventFactory().callPlotOwnerChangeEvent(plugin, p.getWorld(), plot, p, newowner);
                            }

                            if (plot.getCurrentBidderId() != null) {
                                IOfflinePlayer playercurrentbidder = sob.getOfflinePlayer(plot.getCurrentBidderId());
                                EconomyResponse er = sob.depositPlayer(playercurrentbidder, plot.getCurrentBid());

                                if (!er.transactionSuccess()) {
                                    p.sendMessage(er.errorMessage);
                                    Util().warn(er.errorMessage);
                                } else {
                                    IPlayer player = sob.getPlayer(playercurrentbidder.getUniqueId());
                                    if (player != null) {
                                        player.sendMessage(C("WordPlot") + " " + id + " " + C("MsgChangedOwnerFrom") + " " + oldowner + " " + C("WordTo") + " " + newowner + ". " + Util().moneyFormat(plot.getCurrentBid()));
                                    }
                                }
                            }
                        } else {
                            event = sob.getEventFactory().callPlotOwnerChangeEvent(plugin, p.getWorld(), plot, p, newowner);
                        }

                        if (!event.isCancelled()) {
                            plot.setCurrentBidder("");
                            plot.setCurrentBidderId(null);
                            plot.setCurrentBid(0);
                            plot.setAuctionned(false);
                            plot.setForSale(false);

                            plugin.getPlotMeCoreManager().setSellSign(p.getWorld(), plot);

                            plot.updateField("currentbidder", "");
                            plot.updateField("currentbid", 0);
                            plot.updateField("auctionned", false);
                            plot.updateField("forsale", false);
                            plot.updateField("currentbidderid", null);

                            plot.setOwner(newowner);

                            plugin.getPlotMeCoreManager().setOwnerSign(p.getWorld(), plot);

                            plot.updateField("owner", newowner);
                        }
                    } else {
                        plugin.getPlotMeCoreManager().createPlot(p.getWorld(), id, newowner, null);
                    }

                    p.sendMessage(C("MsgOwnerChangedTo") + " " + RED + newowner);

                    if (isAdvancedLogging()) {
                        plugin.getLogger().info(LOG + playername + " " + C("MsgChangedOwnerOf") + " " + id + " " + C("WordFrom") + " " + oldowner + " " + C("WordTo") + " " + newowner);
                    }
                }
            }
        } else {
            p.sendMessage(RED + C("MsgPermissionDenied"));
        }
        return true;
    }
}
