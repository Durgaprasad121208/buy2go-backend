package com.buy2go.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemCustomizationDTO {

    private String variant;
    private String color;
    private String textToPrint;
    private List<String> photos;
    private String printNotes;
}
