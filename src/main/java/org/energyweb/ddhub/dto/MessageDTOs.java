package org.energyweb.ddhub.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageDTOs extends MessageDTO {
    @Valid
	private List<@Size(max = 200, message = "The maximum length is 200 characters") String> fqcns;
    
    @Valid
    @Size(max = 25, message = "The array size 25 maximum.")
	private List<@Size(min = 1, max = 255, message = "The length between 1-255 characters") String> anonymousRecipient;

	public boolean anonymousRule() {
		if(anonymousRecipient == null  && fqcns == null) return true;
		if((anonymousRecipient != null && anonymousRecipient.isEmpty()) || (fqcns != null && fqcns.isEmpty())) return true;
		if((anonymousRecipient != null && anonymousRecipient.isEmpty()) && (fqcns != null && fqcns.isEmpty())) return true;
		return false;
	}

	public String anonymousRuleErrorMsg() {
		if(anonymousRecipient == null  && fqcns == null) return "Both fqcns and anonymousRecipient cannot empty.";
		if((anonymousRecipient != null && anonymousRecipient.isEmpty()) || (fqcns != null && fqcns.isEmpty())) return "Both fqcns and anonymousRecipient cannot empty.";
		if((anonymousRecipient != null && anonymousRecipient.isEmpty()) && (fqcns != null && fqcns.isEmpty())) return "Both fqcns and anonymousRecipient cannot empty.";
		return null;
	}

	public List<String> findFqcnList() {
		List<String> both = new ArrayList<String>();
		if((fqcns != null && !fqcns.isEmpty())) {
			both.addAll(fqcns);
		}
		if((anonymousRecipient != null && !anonymousRecipient.isEmpty())) {
			both.addAll(anonymousRecipient);
		}
		return both;
	}

}
