package com.pandaismyname1.origin_visuals;

import com.pandaismyname1.origin_visuals.client.OriginalFurClient;
import io.github.edwinmindcraft.origins.api.origin.Origin;

public interface IPlayerMixins {
    public default boolean betterCombat$isSwinging() {return false;}
    public default void betterCombat$setSwinging(boolean value) {}
    public default boolean originalFur$isPlayerInvisible() {return false;};
    public default Origin[] originalFur$currentOrigins() {return new Origin[]{Origin.EMPTY};}
    public default OriginFurModel originalFur$getCurrentModel() {
        var originalFur = originalFur$getCurrentFur();
        System.out.println("originalFur$getCurrentFur() = " + originalFur);
        if (originalFur == OriginalFurClient.OriginFur.NULL_OR_DEFAULT_FUR) {
            System.out.println("originalFur$getCurrentFur() == NULL_OR_DEFAULT_FUR");
            return null;
        }
        System.out.println("originalFur.getGeoModel() = " + originalFur.getGeoModel());
        return (OriginFurModel) originalFur.getGeoModel();
    }
    public default OriginalFurClient.OriginFur originalFur$getCurrentFur() {return OriginalFurClient.OriginFur.NULL_OR_DEFAULT_FUR;}
}
