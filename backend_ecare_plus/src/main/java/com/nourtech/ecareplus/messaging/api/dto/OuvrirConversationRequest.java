package com.nourtech.ecareplus.messaging.api.dto; import jakarta.validation.constraints.NotNull; import java.util.UUID;
public record OuvrirConversationRequest(@NotNull UUID professionnelId){}
