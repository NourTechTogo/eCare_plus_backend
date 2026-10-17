package com.nourtech.ecareplus.messaging.domain.entity;
import com.nourtech.ecareplus.identity.domain.entity.*; import com.nourtech.ecareplus.shared.domain.entity.AbstractEntity;
import jakarta.persistence.*; import lombok.*; import java.util.Objects;
@Getter @Entity @Table(name="conversation", uniqueConstraints=@UniqueConstraint(name="uk_conversation_patient_prof",columnNames={"patient_id","professionnel_id"}))
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class Conversation extends AbstractEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="patient_id") private Patient patient;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="professionnel_id") private ProfessionnelSante professionnel;
 public Conversation(Patient p,ProfessionnelSante pro){patient=Objects.requireNonNull(p);professionnel=Objects.requireNonNull(pro);}
 public boolean membre(Utilisateur u){return patient.memeIdentiteQue(u)||professionnel.memeIdentiteQue(u);}
}
