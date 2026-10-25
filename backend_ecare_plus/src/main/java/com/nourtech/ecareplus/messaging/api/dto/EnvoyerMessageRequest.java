package com.nourtech.ecareplus.messaging.api.dto; import jakarta.validation.constraints.*;
public record EnvoyerMessageRequest(@Size(max=2000) String contenu,@Size(max=5) java.util.List<java.util.UUID> piecesJointes,java.util.UUID sourceRef){
 public EnvoyerMessageRequest(String contenu){this(contenu,java.util.List.of(),null);}
 public EnvoyerMessageRequest(String contenu,java.util.List<java.util.UUID> pieces){this(contenu,pieces,null);}
}
