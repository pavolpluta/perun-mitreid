package cz.muni.ics.oidc.configurations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of Facility attributes
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
@SuppressWarnings("SpringAutowiredFieldsWarningInspection")
public class FacilityAttrsConfig {

	private final static Logger log = LoggerFactory.getLogger(FacilityAttrsConfig.class);

	private String checkGroupMembershipAttr;
	private String registrationURLAttr;
	private String allowRegistrationAttr;
	private String dynamicRegistrationAttr;
	private String voShortNamesAttr;

	public String getCheckGroupMembershipAttr() {
		return checkGroupMembershipAttr;
	}

	public void setCheckGroupMembershipAttr(String checkGroupMembershipAttr) {
		this.checkGroupMembershipAttr = checkGroupMembershipAttr;
	}

	public String getRegistrationURLAttr() {
		return registrationURLAttr;
	}

	public void setRegistrationURLAttr(String registrationURLAttr) {
		this.registrationURLAttr = registrationURLAttr;
	}

	public String getAllowRegistrationAttr() {
		return allowRegistrationAttr;
	}

	public void setAllowRegistrationAttr(String allowRegistrationAttr) {
		this.allowRegistrationAttr = allowRegistrationAttr;
	}

	public String getDynamicRegistrationAttr() {
		return dynamicRegistrationAttr;
	}

	public void setDynamicRegistrationAttr(String dynamicRegistrationAttr) {
		this.dynamicRegistrationAttr = dynamicRegistrationAttr;
	}

	public String getVoShortNamesAttr() {
		return voShortNamesAttr;
	}

	public void setVoShortNamesAttr(String voShortNamesAttr) {
		this.voShortNamesAttr = voShortNamesAttr;
	}

	public List<String> getAttrNamesAsList() {
		List<String> res = new ArrayList<>();
		if (checkGroupMembershipAttr != null && !checkGroupMembershipAttr.isEmpty()) {
			res.add(checkGroupMembershipAttr);
		}
		if (allowRegistrationAttr != null && !allowRegistrationAttr.isEmpty()) {
			res.add(allowRegistrationAttr);
		}
		if (registrationURLAttr != null && !registrationURLAttr.isEmpty()) {
			res.add(registrationURLAttr);
		}
		if (dynamicRegistrationAttr != null && !dynamicRegistrationAttr.isEmpty()) {
			res.add(dynamicRegistrationAttr);
		}
		if (voShortNamesAttr != null && !voShortNamesAttr.isEmpty()) {
			res.add(voShortNamesAttr);
		}

		return res;
	}

	@PostConstruct
	public void postInit() {
		log.info("Facility attributes initialized");
		log.info("Check group membership attr mapped to urn: {}", checkGroupMembershipAttr);
		log.info("Allow registration attr mapped to urn: {}", allowRegistrationAttr);
		log.info("Registration URL attr mapped to urn: {}", registrationURLAttr);
		log.info("Allow dynamic registration attr mapped to urn: {}", dynamicRegistrationAttr);
		log.info("Vo short names attr mapped to urn: {}", voShortNamesAttr);
	}

}
