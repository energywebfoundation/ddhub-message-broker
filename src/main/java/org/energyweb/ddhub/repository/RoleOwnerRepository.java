package org.energyweb.ddhub.repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;

import org.energyweb.ddhub.Role.TYPE;
import org.energyweb.ddhub.model.RoleOwner;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import io.quarkus.mongodb.panache.PanacheMongoRepository;

@ApplicationScoped
public class RoleOwnerRepository implements PanacheMongoRepository<RoleOwner> {

	public void save(String did, String verifiedRoles) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONArray jsonArray = (JSONArray) parser.parse(verifiedRoles);

		RoleOwner roleOwner = find("did", did).firstResult();
		if (Optional.ofNullable(roleOwner).isEmpty()) {
			roleOwner = new RoleOwner();
		}
		roleOwner.setDid(did);
		roleOwner.getClaimType().addAll(jsonArray);

		this.persistOrUpdate(roleOwner);

	}

	
	public boolean hasRoles(String did, String[] roles) {
		Optional<RoleOwner> role = find("did = ?1 and claimType in ?2", did, List.of(roles)).firstResultOptional();
		if(!role.isPresent()) return role.isPresent();
		Set<String> ruleMatch = new HashSet<String>();
		for (String _role : roles) {
			if(role.get().getClaimType().toString().contains(_role)) {
				ruleMatch.add(_role);
			}
		}
		return ruleMatch.size() == roles.length;
	}

	public List<String> queryDidsByRoles(TYPE searchType, String[] roles) {
		List<RoleOwner> role = find("claimType in ?1", List.of(roles)).list();

		List<String> dids = new ArrayList<String>();
		role.forEach(entity -> {
			if (searchType.equals(TYPE.ANY)) {
				dids.add(entity.getDid());
			}else {
				Set<String> ruleMatch = new HashSet<String>();
				for (String _role : roles) {
					if(entity.getClaimType().toString().contains(_role)) {
						ruleMatch.add(_role);
					}
				}
				if(ruleMatch.size() == roles.length) {
					dids.add(entity.getDid());
				}
			}
		});
		return dids;
	}

}
