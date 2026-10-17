package com.nourtech.ecareplus.messaging.api.dto; import jakarta.validation.constraints.*;
public record EnvoyerMessageRequest(@NotBlank @Size(max=2000) String contenu){}
