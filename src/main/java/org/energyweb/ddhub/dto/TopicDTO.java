package org.energyweb.ddhub.dto;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
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

import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.xml.sax.SAXException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaException;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.SpecVersionDetector;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class TopicDTO {

	public enum SchemaType {
		JSD7,
		XSD6,
		XML
	}

	private String id;
	@NotNull
	@NotEmpty
	private String name;
	@NotNull
	private SchemaType schemaType;
	@NotNull
	@NotEmpty
	@Getter(AccessLevel.NONE)
	private String schema;
	@NotNull
	@Pattern(regexp = "^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)(?:-((?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\\.(?:0|[1-9]\\d*|\\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\\+([0-9a-zA-Z-]+(?:\\.[0-9a-zA-Z-]+)*))?$", message = "Required Semantic Versions")
	private String version;
	@NotNull
	@NotEmpty
	@Pattern(regexp = "\\w*.*.iam.ewc", message = "Required format .iam.ewc")
	private String owner;
	@Valid
	private Set<@NotEmpty String> tags;
	@JsonIgnore
	private String did;
	@JsonIgnore
	@Getter(AccessLevel.NONE)
	private boolean isOwnerValid;
	
	public boolean validOwner() {
		return isOwnerValid;
	}

	
	public HashMap getSchema() {
		return jsonParser();
	}

	private HashMap stringParser() {
		HashMap map = new HashMap();
		if(schemaType == SchemaType.XML) {
			map.put("XML", schema);
		}
		else{
			map.put("STRING", schema);
		}
		schema = JsonbBuilder.create().toJson(map);
		return jsonParser();
	}

	private HashMap jsonParser() {
		try {
			JSONParser parser = new JSONParser();
			return (HashMap)parser.parse(schema);
		} catch (ParseException e) {
			return stringParser();
		}
	}

	public void validateOwner(String roles) {
		List<?> _roles = JsonbBuilder.create().fromJson(roles, ArrayList.class);
    	_roles.forEach(role ->{
    		String[] namespace = role.toString().split(".roles.");
    		Optional.ofNullable(namespace[1]).ifPresent(item ->{
    			if(!isOwnerValid) {
    				isOwnerValid = owner.contains(item);
    			}
    		});
    	});
	}

	public boolean validateSchemaType() {
		boolean isValid = false;
		if(schemaType == SchemaType.XML) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				factory.newSchema(new StreamSource(new StringReader(schema)));
				isValid = true;
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}else if(schemaType == SchemaType.JSD7) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode schemaNode = mapper.readTree(schema);
				JsonSchemaFactory factory = JsonSchemaFactory.builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)).objectMapper(mapper).build();
				JsonSchema schema = factory.getSchema(schemaNode);
				schema.initializeValidators(); 
				isValid = true;
			} catch (JsonSchemaException e) {
				e.printStackTrace();
			} catch (JsonMappingException e) {
				e.printStackTrace();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
		}else if(schemaType == SchemaType.XSD6) {
			SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
			try {
				factory.newSchema(new StreamSource(new StringReader(schema)));
				isValid = true;
			} catch (SAXException e) {
				e.printStackTrace();
			}
		}
		return isValid;
	}
}