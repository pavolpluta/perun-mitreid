package cz.muni.ics.oidc.server;

import org.mitre.openid.connect.models.Acr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Date;

@Repository
@Transactional(value="defaultTransactionManager")
public class PerunAcrRepository {

	private static final Logger log = LoggerFactory.getLogger(PerunAcrRepository.class);

	@PersistenceContext(unitName="defaultPersistenceUnit")
	private EntityManager manager;

	public Acr get(String sub, String clientId, String acr, String state) {
		log.trace("get(sub: {}, clientId: {}, acr: {}, state: {})", sub, clientId, acr, state);
		TypedQuery<Acr> query = manager.createNamedQuery(Acr.GET, Acr.class);
		query.setParameter(Acr.PARAM_SUB, sub);
		query.setParameter(Acr.PARAM_CLIENT_ID, clientId);
		query.setParameter(Acr.PARAM_ACR, acr);
		query.setParameter(Acr.PARAM_STATE, state);

		Acr result = query.getSingleResult();
		log.trace("get() returns: {}", result);
		return result;
	}

	public Acr getById(Long id) {
		log.trace("getById({})", id);
		TypedQuery<Acr> query = manager.createNamedQuery(Acr.GET_BY_ID, Acr.class);
		query.setParameter(Acr.PARAM_ID, id);

		Acr result = query.getSingleResult();
		log.trace("get() returns: {}", result);
		return result;
	}

	@Transactional
	public Acr store(Acr acr) {
		log.trace("store({})", acr);
		Acr tmp = manager.merge(acr);
		manager.flush();

		log.trace("store() returns: {}", tmp);
		return tmp;
	}

	@Transactional
	public void remove(Long id) {
		log.trace("remove({})", id);
		Acr acr = getById(id);

		if (acr != null) {
			manager.remove(acr);
		}
	}

	@Transactional
	@Scheduled(fixedDelay = 600000)
	public void deleteExpired() {
		log.trace("deleteExpired()");
		Query query = manager.createNamedQuery(Acr.DELETE_EXPIRED);
		query.setParameter(Acr.PARAM_EXPIRATION, new Date(System.currentTimeMillis()));
		query.executeUpdate();
	}
}