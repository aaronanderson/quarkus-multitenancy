package io.github.aaronanderson.multitenancy.example;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import io.github.aaronanderson.quarkus.multitenancy.runtime.TenantId;
import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

/**
 * Trust all basic or form based auth credentials 
 */
@ApplicationScoped
public class CredentialsIdentityProvider implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

	private static final Logger log = Logger.getLogger(CredentialsIdentityProvider.class);

	@Inject
	@TenantId
	Instance<String> tenantId;

	private String subject(String userName) {
		try {
			return userName + "@" + tenantId.get();
		} catch (ContextNotActiveException ce) {
			return userName;
		}
	}

	@Override
	public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
		return UsernamePasswordAuthenticationRequest.class;
	}

	@Override
	public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request, AuthenticationRequestContext context) {
		//TODO Due to asynchronous Mutiny handling the TenantResolverHandler is terminating the tenant scope before the identity is authenticating. Using addEndhandler runs on another worker thread so that will not work.
		//Research how to invoke the context termination after the Vert.x end is called. See if Context Propagation is applicable https://download.eclipse.org/microprofile/microprofile-context-propagation-1.2/apidocs/index.html?org/eclipse/microprofile/context/spi/ContextManagerExtension.html
		String subject = subject(request.getUsername());		
		//String subject = request.getUsername();
		return Uni.createFrom().item(() -> {
			log.infof("CredentialsIdentityProvider authenticate: %s", subject);
			QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder();
			builder.setPrincipal(new QuarkusPrincipal(subject));
			return builder.build();

		});

	}

}
