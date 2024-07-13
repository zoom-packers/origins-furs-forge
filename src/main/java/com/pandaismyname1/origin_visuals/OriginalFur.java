package com.pandaismyname1.origin_visuals;

import mod.azure.azurelib.AzureLib;
import net.minecraftforge.fml.ModList;

public class OriginalFur {
    public void onInitialize() {
        AzureLib.initialize();
        if (ModList.get().isLoaded("bettercombat")) {

        }
    }
}
