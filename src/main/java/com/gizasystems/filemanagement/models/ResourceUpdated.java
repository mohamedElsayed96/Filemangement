package com.gizasystems.filemanagement.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResourceUpdated {
    private String id;
    private boolean updated;
}
