package org.llk.gcsAdmin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileDescription {
    String name;
    boolean isFile;
}
