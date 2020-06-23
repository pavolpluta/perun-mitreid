package cz.muni.ics.oidc.server.connectors;

import com.google.common.base.Strings;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.apache.directory.ldap.client.template.EntryMapper;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;

/**
 * Connector for calling Perun LDAP
 *
 * @author Dominik Frantisek Bucik <bucik@ics.muni.cz>
 */
public class PerunConnectorLdap implements DisposableBean {

	private static final Logger log = LoggerFactory.getLogger(PerunConnectorLdap.class);

	private final String baseDN;
	private final LdapConnectionPool pool;
	private final LdapConnectionTemplate ldap;

	public PerunConnectorLdap(String ldapHost, String ldapUser, String ldapPassword, long timeoutSecs, String baseDN) {
		if (ldapHost == null || ldapHost.trim().isEmpty()) {
			throw new IllegalArgumentException("Host cannot be null or empty");
		} else if (baseDN == null || baseDN.trim().isEmpty()) {
			throw new IllegalArgumentException("baseDN cannot be null or empty");
		}

		this.baseDN = baseDN;
		LdapConnectionConfig config = getConfig(ldapHost);
		if (ldapUser != null && !ldapUser.isEmpty()) {
			log.debug("setting ldap user to {}", ldapUser);
			config.setName(ldapUser);
		}
		if (ldapPassword != null && !ldapPassword.isEmpty()) {
			log.debug("setting ldap password");
			config.setCredentials(ldapPassword);
		}
		DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(config);
		factory.setTimeOut(timeoutSecs * 1000L);

		GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setTestOnBorrow(true);

		pool = new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory), poolConfig);
		ldap = new LdapConnectionTemplate(pool);
		log.debug("initialized LDAP connector");
	}

	public String getBaseDN() {
		return baseDN;
	}

	private LdapConnectionConfig getConfig(String host) {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(host);
		config.setLdapPort(636);
		config.setUseSsl(true);

		return config;
	}

	/**
	 * Invoked by a BeanFactory on destruction of a Spring bean.
	 */
	@Override
	public void destroy() {
		log.trace("destroy()");
		if (!pool.isClosed()) {
			pool.close();
		}
	}

	/**
	 * Search for the first entry that satisfies criteria.
	 * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People)
	 *                 ! DO NOT END WITH A COMMA !
	 * @param filter Filter for entries
	 * @param scope Seearch scope
	 * @param attributes Attributes to be fetch for entry
	 * @param entryMapper Mapper of entries to the target class T
	 * @param <T> Class that the result should be mapped to.
	 * @return Found entry mapped to target class
	 */
	public <T> T searchFirst(String dnPrefix, FilterBuilder filter, SearchScope scope, String[] attributes, EntryMapper<T> entryMapper) {
		log.trace("searchFirst({}, {}, {}, {} , {})", dnPrefix, filter, scope, attributes, entryMapper);
		Dn fullDn = getFullDn(dnPrefix);
		T result = ldap.searchFirst(fullDn, filter, scope, attributes, entryMapper);

		log.trace("searchFirst({}, {}, {}, {} , {}) returns: {}", dnPrefix, filter, scope, attributes, entryMapper, result);
		return result;
	}

	/**
	 * Perform lookup for the entry that satisfies criteria.
	 * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People)
	 *                 ! DO NOT END WITH A COMMA !
	 * @param attributes Attributes to be fetch for entry
	 * @param entryMapper Mapper of entries to the target class T
	 * @param <T> Class that the result should be mapped to.
	 * @return Found entry mapped to target class
	 */
	public <T> T lookup(String dnPrefix, String[] attributes, EntryMapper<T> entryMapper) {
		log.trace("lookup({}, {}, {})", dnPrefix, attributes, entryMapper);
		Dn fullDn = getFullDn(dnPrefix);

		T result = ldap.lookup(fullDn, attributes, entryMapper);
		log.trace("lookup({}, {}, {}) returns: {}", dnPrefix, attributes, entryMapper, result);
		return result;
	}

	/**
	 * Search for the entries satisfy criteria.
	 * @param dnPrefix Prefix to be added to the base DN. (i.e. ou=People)
	 *                 ! DO NOT END WITH A COMMA !
	 * @param filter Filter for entries
	 * @param scope Seearch scope
	 * @param attributes Attributes to be fetch for entry
	 * @param entryMapper Mapper of entries to the target class T
	 * @param <T> Class that the result should be mapped to.
	 * @return List of found entries mapped to target class
	 */
	public <T> List<T> search(String dnPrefix, FilterBuilder filter, SearchScope scope, String[] attributes, EntryMapper<T> entryMapper) {
		log.trace("search({}, {}, {}, {} , {})", dnPrefix, filter, scope, attributes, entryMapper);
		Dn fullDn = getFullDn(dnPrefix);

		List<T> result = ldap.search(fullDn, filter, scope, attributes, entryMapper);
		log.trace("searchFirst({}, {}, {}, {} , {}) returns: {}", dnPrefix, filter, scope, attributes, entryMapper, result);
		return result;
	}

	private Dn getFullDn(String prefix) {
		String dn = baseDN;
		if (!Strings.isNullOrEmpty(prefix)) {
			dn = prefix + "," + baseDN;
		}

		Dn newDn = ldap.newDn(dn);
		log.trace("getFullDn ({}) returns: {}", prefix, newDn);
		return newDn;
	}
}
