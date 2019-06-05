package cz.muni.ics.oidc.server;

import org.mitre.oauth2.model.ClientDetailsEntity;
import org.mitre.oauth2.service.ClientDetailsEntityService;
import org.mitre.openid.connect.model.UserInfo;
import org.mitre.openid.connect.repository.UserInfoRepository;
import org.mitre.openid.connect.service.UserInfoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service called from UserInfoEndpoint and other places to get UserInfo.
 *
 * @author Martin Kuba makub@ics.muni.cz
 */
public class PerunUserInfoService implements UserInfoService {

	private static final Logger log = LoggerFactory.getLogger(PerunUserInfoService.class);

	@Autowired
	private UserInfoRepository userInfoRepository;

	@Autowired
	private ClientDetailsEntityService clientService;

	@Override
	public UserInfo getByUsername(String username) {
		log.debug("getByUsername(username={})", username);
		return userInfoRepository.getByUsername(username);
	}

	@Override
	public UserInfo getByUsernameAndClientId(String username, String clientId) {
		log.debug("getByUsernameAndClientId(username={},clientId={})", username, clientId);
		ClientDetailsEntity client = clientService.loadClientByClientId(clientId);
		UserInfo userInfo = userInfoRepository.getByUsername(username);
		if (client == null || userInfo == null) {
			return null;
		}
		log.debug("getByUsernameAndClientId(user={},client={}) returns userInfo", userInfo.getName(), client.getClientName());
		return userInfo;
	}

	@Override
	public UserInfo getByEmailAddress(String email) {
		return userInfoRepository.getByEmailAddress(email);
	}

}
