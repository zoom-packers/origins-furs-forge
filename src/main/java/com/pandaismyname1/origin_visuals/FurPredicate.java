package com.pandaismyname1.origin_visuals;

import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class FurPredicate {
    @FunctionalInterface
    public interface Predicate_T  {
        boolean test(OriginFurModel model, ResourceLocation thisid, UUID ent_uuid);
    }
    Predicate_T predicate;
    public FurPredicate(Predicate_T predicate) {
        this.predicate = predicate;
    }
    public final boolean test(OriginFurModel model, ResourceLocation thidid, UUID ent_uuid) {
        return predicate.test(model, thidid, ent_uuid);
    }
}
