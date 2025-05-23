package org.energyweb.ddhub.dto;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.json.bind.JsonbBuilder;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;

import org.energyweb.ddhub.helper.constraint.ValueOfEnum;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopicDTO {

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
	@Getter(AccessLevel.NONE)
	private String schema;
	@NotNull
	@Pattern(regexp = "^\\d+\\.\\d+\\.\\d+(-[0-9A-Za-z-.]+)?(\\+[0-9A-Za-z-.]+)?$", message = "Required Semantic Versions")
	private String version;
	@NotNull
	@NotEmpty
	@Pattern(regexp = "^.+\\.ewc$", message = "Required format .*.ewc")
	private String owner;
	@Valid
	@Getter(AccessLevel.NONE)
	private Set<@NotEmpty @Pattern(regexp = "^[^&<>\"'/\\\\\\-\\.\\r\\n\\t]*$", message = "Invalid characters detected.") String> tags = new HashSet<String>();
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private String did;
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private boolean isOwnerValid;

	private boolean deleted;
	@JsonIgnore
	private String createdBy;

	private LocalDateTime createdDate;
	private LocalDateTime updatedDate;
	private LocalDateTime deletedDate = null;

	public boolean validOwner() {
		return isOwnerValid;
	}

	public String schemaValue() {
		return schema;
	}

	public HashMap getSchema() {
		if (schema == null)
			return null;
		return jsonParser();
	}

	private HashMap stringParser() {
		HashMap map = new HashMap();
		map.put(SchemaType.valueOf(schemaType).name(), schema);
		schema = JsonbBuilder.create().toJson(map);
		return jsonParser();
	}

	private HashMap jsonParser() {
		try {
			JSONParser parser = new JSONParser();
			return (HashMap) parser.parse(schema);
		} catch (Exception e) {
			return stringParser();
		}
	}

	public void validateOwner(String roles) {
		List<?> _roles = JsonbBuilder.create().fromJson(roles, ArrayList.class);
		_roles.forEach(role -> {
			String[] namespace = role.toString().split(".roles.");
			Optional.ofNullable(namespace[1]).ifPresent(item -> {
				if (!isOwnerValid) {
					isOwnerValid = owner.contains(item);
				}
			});
		});
	}

	public boolean validateSchemaType() {
		boolean isValid = false;
		if (schemaType.contentEquals(SchemaType.XML.name)) {
			isValid = true;
		} else if (schemaType.contentEquals(SchemaType.CSV.name)) {
			isValid = true;
		} else if (schemaType.contentEquals(SchemaType.TSV.name)) {
			isValid = true;
		} else if (schemaType.contentEquals(SchemaType.JSD7.name)) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode schemaNode = mapper.readTree(schema);
				JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
						.objectMapper(mapper).build();
				JsonSchema schema = factory.getSchema(schemaNode);
				schema.initializeValidators();
				isValid = true;
				this.schema = schemaNode.toString();
			} catch (JsonSchemaException e) {
				// e.printStackTrace();
			} catch (JsonMappingException e) {
				// e.printStackTrace();
			} catch (JsonProcessingException e) {
				// e.printStackTrace();
			}
		} else if (schemaType.contentEquals(SchemaType.XSD6.name)) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				factory.newSchema(new StreamSource(new StringReader(schema)));
				isValid = true;
			} catch (SAXException e) {
				// e.printStackTrace();
			}
		}
		return isValid;
	}

	public String did() {
		return this.did;
	}

	public Set<String> getTags() {
		if (this.tags == null)
			return new HashSet<>();
		return tags;
	}
}