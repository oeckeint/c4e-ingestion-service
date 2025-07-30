package com.com4energy.processor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadRequest {

    @NotBlank
    private String filename;

    @NotNull
    private Long userId;

}
