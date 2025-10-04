package com.example.cellex.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Province {
    private String code;
    private String name;
    private String codeName;
    private String divisionType;
    private Integer phoneCode;
}
