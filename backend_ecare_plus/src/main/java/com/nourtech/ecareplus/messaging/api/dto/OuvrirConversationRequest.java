package com.nourtech.ecareplus.messaging.api.dto; import java.util.UUID;
public record OuvrirConversationRequest(UUID professionnelId,UUID patientId){ public OuvrirConversationRequest(UUID professionnelId){this(professionnelId,null);} }
