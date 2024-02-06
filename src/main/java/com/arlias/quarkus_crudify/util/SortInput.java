package com.arlias.quarkus_crudify.util;

import io.quarkus.panache.common.Sort;

public class SortInput {

    public String[] by;

    public boolean desc;

    public Sort.Direction getDirection() {
        return desc ? Sort.Direction.Descending : Sort.Direction.Ascending;
    }

    public static Sort getSortOrDefault(SortInput sort){
        if(sort == null || sort.by == null || sort.by.length <= 0) {
            return Sort.by("id", Sort.Direction.Descending);
        } else {
            return Sort.by(sort.by).direction(sort.getDirection());
        }
    }

    public SortInput() {
    }

    private SortInput(String[] by, boolean desc) {
        this.by = by;
        this.desc = desc;
    }

    public static SortInput of(String[] by, boolean desc){
        return new SortInput(by, desc);
    }

}
