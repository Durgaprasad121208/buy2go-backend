package com.buy2go.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PhotoDetailsDTO {
    private String id;
    private String orderNumber;
    private String photoId;
    private String photoUrl;
    private List<String> photos;
}
