package com.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {
    @Size(max = 50, message = "Username must be at most 50 characters")
    private String username;

    @Size(max = 100, message = "Profile name must be at most 100 characters")
    private String profileName;

    @Size(max = 255, message = "Bio must be at most 255 characters")
    private String bio;
}
