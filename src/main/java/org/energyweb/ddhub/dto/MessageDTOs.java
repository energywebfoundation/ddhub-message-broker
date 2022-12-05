package org.energyweb.ddhub.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.validation.Valid;
import javax.validation.constraints.Size;

import org.energyweb.ddhub.helper.ReturnErrorMessage;
import org.energyweb.ddhub.helper.ReturnMessage;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageDTOs extends MessageDTO {
    @Valid
	private List<@Size(max = 200, message = "The maximum length is 200 characters")  String> fqcns;
    
    @Valid
    @Size(max = 25, message = "The array size 25 maximum.")
	private List<@Size(min = 1, max = 255, message = "The length between 1-255 characters")  String> anonymousRecipient;

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

	public List<ReturnMessage> validateFqcnParam() {
		List<ReturnMessage> failed = new ArrayList<ReturnMessage>();
		if((fqcns != null && !fqcns.isEmpty())) {
			final String regex = "^(?!(did|DID|dID|dId|DiD|DId|Did):).+(\\w*)";
			List<String> failedFqcn = new ArrayList<String>();
			this.getFqcns().forEach(fqcn -> {
				if(Pattern.compile(regex).matcher(fqcn).matches()) {
					ReturnMessage errorMessage = new ReturnMessage();
					errorMessage.setDid(fqcn);
					errorMessage.setStatusCode(400);
					errorMessage.setErr(new ReturnErrorMessage("MB::INVALID_FQCN", "fqcn:" + fqcn + " DIDs format invalid"));
					failed.add(errorMessage);
					failedFqcn.add(fqcn);
				}
			});
			this.getFqcns().removeAll(failedFqcn);
		}
		return failed;
	}
	
	public List<ReturnMessage> validateAnonymousRecipientParam() {
		List<ReturnMessage> failed = new ArrayList<ReturnMessage>();
		if((anonymousRecipient != null && !anonymousRecipient.isEmpty())) {
			final String regex = "^((did|DID|dID|dId|DiD|DId|Did):).+(\\w*)";
			List<String> failedAnonymous = new ArrayList<String>();
			this.getAnonymousRecipient().forEach(anonymous -> {
				if(Pattern.compile(regex).matcher(anonymous).matches()) {
					ReturnMessage errorMessage = new ReturnMessage();
					errorMessage.setDid(anonymous);
					errorMessage.setStatusCode(400);
					errorMessage.setErr(new ReturnErrorMessage("MB::INVALID_ANONYMOUSKEY", "anonymousKey:" + anonymous + " DIDs format identify"));
					failed.add(errorMessage);
					failedAnonymous.add(anonymous);
				}
			});
			this.getAnonymousRecipient().removeAll(failedAnonymous);
		}
		return failed;
	}

}
