package com.gizasystems.filemanagement.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceCreated {
    private String id;
    private boolean created;
    public ResourceCreated(boolean created){
        this.created = created;
    }
    public ResourceCreated(String id){
        this.id = id;
        this.created =true;
    }
}
