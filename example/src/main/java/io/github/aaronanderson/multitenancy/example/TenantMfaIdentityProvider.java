package io.github.aaronanderson.multitenancy.example;

import javax.annotation.Priority;
import javax.enterprise.inject.Alternative;
import javax.inject.Singleton;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

//@Alternative
//@Priority(1) 
//@Singleton
//public class TenantMfaIdentityProvider extends MfaIdentityProvider{
//
//	@Inject
//	Instance<Tenant> tenant;
//	
//  //Perform a security check to ensure claim is constrained to the target tenant and avoid cross tenant access
//	@Override
//	public Uni<SecurityIdentity> authenticate(MfaAuthenticationRequest request, AuthenticationRequestContext context) {
//		String claimTenant = request.getClaims().getClaimValueAsString("tenant");
//      String currentTenant =;
//		if (currentTenant!=null && !currentTenant.equals(tenant.name())) {
//			return Uni.createFrom().nullItem(); 
//		}
//		return super.authenticate(request, context);
//
//	}
//}
