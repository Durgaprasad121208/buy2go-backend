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
    private String photo;         // Single photo URL stored in MinIO
    private String printNotes;

    // Backward compatibility if array of photos was sent
    public void setPhotos(List<String> photos) {
        if (photos != null && !photos.isEmpty()) {
            this.photo = photos.get(0);
        }
    }
}
