package org.energyweb.ddhub.dto;

import java.util.Set;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.energyweb.ddhub.helper.constraint.ValueOfEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicDTOGet {

	public enum SchemaType {
		JSD7("JSD7"),
		XSD6("XSD6"),
		XML("XML"),
		CSV("CSV"),
		TSV("TSV");

		private String name;

		SchemaType(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

	private String id;
	@NotNull
	@NotEmpty
	@Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.")
	private String name;
	@NotNull
	@ValueOfEnum(enumClass = SchemaType.class)
	private String schemaType;
	@NotNull
	@NotEmpty
	@Pattern(regexp = "^.+\\.ewc$", message = "Required format .*.ewc")
	private String owner;
	@Valid
	private Set<@NotEmpty @Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.") String> tags;
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private String did;
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private boolean isOwnerValid;

}