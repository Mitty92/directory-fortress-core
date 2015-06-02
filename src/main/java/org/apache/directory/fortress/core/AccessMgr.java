/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core;


import java.util.List;
import java.util.Set;

import org.apache.directory.fortress.core.model.Permission;
import org.apache.directory.fortress.core.model.User;
import org.apache.directory.fortress.core.model.Session;
import org.apache.directory.fortress.core.model.UserRole;


/**
 * This object performs runtime access control operations on objects that are provisioned RBAC entities
 * that reside in LDAP directory.  These APIs map directly to similar named APIs specified by ANSI and NIST
 * RBAC system functions.
 * Many of the java doc function descriptions found below were taken directly from ANSI INCITS 359-2004.
 * The RBAC Functional specification describes administrative operations for the creation
 * and maintenance of RBAC element sets and relations; administrative review functions for
 * performing administrative queries; and system functions for creating and managing
 * RBAC attributes on user sessions and making access control decisions.
 * <p/>
 * <hr>
 * <h4>RBAC0 - Core</h4>
 * Many-to-many relationship between Users, Roles and Permissions. Selective role activation into sessions.  API to add, update, delete identity data and perform identity and access control decisions during runtime operations.
 * <p/>
 * <img src="./doc-files/RbacCore.png">
 * <hr>
 * <h4>RBAC1 - General Hierarchical Roles</h4>
 * Simplifies role engineering tasks using inheritance of one or more parent roles.
 * <p/>
 * <img src="./doc-files/RbacHier.png">
 * <hr>
 * <h4>RBAC2 - Static Separation of Duty (SSD) Relations</h4>
 * Enforce mutual membership exclusions across role assignments.  Facilitate dual control policies by restricting which roles may be assigned to users in combination.  SSD provide added granularity for authorization limits which help enterprises meet strict compliance regulations.
 * <p/>
 * <img src="./doc-files/RbacSSD.png">
 * <hr>
 * <h4>RBAC3 - Dynamic Separation of Duty (DSD) Relations</h4>
 * Control allowed role combinations to be activated within an RBAC session.  DSD policies fine tune role policies that facilitate authorization dual control and two man policy restrictions during runtime security checks.
 * <p/>
 * <img src="./doc-files/RbacDSD.png">
 * <hr>
 * <p/>
 * This interface's implementer will NOT be thread safe if parent instance variables ({@link Manageable#setContextId(String)} or {@link Manageable#setAdmin(org.apache.directory.fortress.core.model.Session)}) are set.
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface AccessMgr extends Manageable
{

    /**
     * Perform user authentication only.  It does not activate RBAC roles in session but will evaluate password policies.
     *
     * @param userId   Contains the userid of the user signing on.
     * @param password Contains the user's password.
     * @return Session object will be returned if authentication successful.  This will not contain user's roles.
     * @throws SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    Session authenticate( String userId, char[] password )
        throws SecurityException;


    /**
     * Perform user authentication {@link User#password} and role activations.<br />
     * This method must be called once per user prior to calling other methods within this class.
     * The successful result is {@link org.apache.directory.fortress.core.model.Session} that contains target user's RBAC {@link User#roles} and Admin role {@link User#adminRoles}.<br />
     * In addition to checking user password validity it will apply configured password policy checks {@link org.apache.directory.fortress.core.model.User#pwPolicy}..<br />
     * Method may also store parms passed in for audit trail {@link org.apache.directory.fortress.core.model.FortEntity}.
     * <h4> This API will...</h4>
     * <ul>
     * <li> authenticate user password if trusted == false.
     * <li> perform <a href="http://www.openldap.org/">OpenLDAP</a> <a href="http://tools.ietf.org/html/draft-behera-ldap-password-policy-10">password policy evaluation</a>.
     *
     * <li> fail for any user who is locked by OpenLDAP's policies {@link org.apache.directory.fortress.core.model.User#isLocked()}, regardless of trusted flag being set as parm on API.
     * <li> evaluate temporal {@link org.apache.directory.fortress.core.util.time.Constraint}(s) on {@link User}, {@link UserRole} and {@link org.apache.directory.fortress.core.model.UserAdminRole} entities.
     * <li> process selective role activations into User RBAC Session {@link User#roles}.
     * <li> check Dynamic Separation of Duties {@link org.apache.directory.fortress.core.impl.DSDChecker#validate(org.apache.directory.fortress.core.model.Session, org.apache.directory.fortress.core.util.time.Constraint, org.apache.directory.fortress.core.util.time.Time)} on {@link org.apache.directory.fortress.core.model.User#roles}.
     * <li> process selective administrative role activations {@link User#adminRoles}.
     * <li> return a {@link org.apache.directory.fortress.core.model.Session} containing {@link org.apache.directory.fortress.core.model.Session#getUser()}, {@link org.apache.directory.fortress.core.model.Session#getRoles()} and (if admin user) {@link org.apache.directory.fortress.core.model.Session#getAdminRoles()} if everything checks out good.
     * <li> throw a checked exception that will be {@link SecurityException} or its derivation.
     * <li> throw a {@link SecurityException} for system failures.
     * <li> throw a {@link PasswordException} for authentication and password policy violations.
     * <li> throw a {@link ValidationException} for data validation errors.
     * <li> throw a {@link FinderException} if User id not found.
     * </ul>
     * <h4>
     * The function is valid if and only if:
     * </h4>
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the password is supplied (unless trusted).
     * <li> the (optional) active role set is a subset of the roles authorized for that user.
     * </ul>
     * <h4>
     * The following attributes may be set when calling this method
     * </h4>
     * <ul>
     * <li> {@link User#userId} - required
     * <li> {@link org.apache.directory.fortress.core.model.User#password}
     * <li> {@link org.apache.directory.fortress.core.model.User#roles} contains a list of RBAC role names authorized for user and targeted for activation within this session.  Default is all authorized RBAC roles will be activated into this Session.
     * <li> {@link org.apache.directory.fortress.core.model.User#adminRoles} contains a list of Admin role names authorized for user and targeted for activation.  Default is all authorized ARBAC roles will be activated into this Session.
     * <li> {@link User#props} collection of name value pairs collected on behalf of User during signon.  For example hostname:myservername or ip:192.168.1.99
     * </ul>
     * <h4>
     * Notes:
     * </h4>
     * <ul>
     * <li> roles that violate Dynamic Separation of Duty Relationships will not be activated into session.
     * <li> role activations will proceed in same order as supplied to User entity setter, see {@link User#setRole(String)}.
     * </ul>
     * </p>
     *
     * @param user      Contains {@link User#userId}, {@link org.apache.directory.fortress.core.model.User#password} (optional if {@code isTrusted} is 'true'), optional {@link User#roles}, optional {@link org.apache.directory.fortress.core.model.User#adminRoles}
     * @param isTrusted if true password is not required.
     * @return Session object will contain authentication result code {@link org.apache.directory.fortress.core.model.Session#errorId}, RBAC role activations {@link org.apache.directory.fortress.core.model.Session#getRoles()}, Admin Role activations {@link org.apache.directory.fortress.core.model.Session#getAdminRoles()},OpenLDAP pw policy codes {@link org.apache.directory.fortress.core.model.Session#warnings}, {@link org.apache.directory.fortress.core.model.Session#expirationSeconds}, {@link org.apache.directory.fortress.core.model.Session#graceLogins} and more.
     * @throws SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    Session createSession( User user, boolean isTrusted )
        throws SecurityException;


    /**
     * Perform user RBAC authorization.  This function returns a Boolean value meaning whether the subject of a given session is
     * allowed or not to perform a given operation on a given object. The function is valid if and
     * only if the session is a valid Fortress session, the object is a member of the OBJS data set,
     * and the operation is a member of the OPS data set. The session's subject has the permission
     * to perform the operation on that object if and only if that permission is assigned to (at least)
     * one of the session's active roles. This implementation will verify the roles or userId correspond
     * to the subject's active roles are registered in the object's access control list.
     *
     * @param perm    must contain the object, {@link Permission#objName}, and operation, {@link Permission#opName}, of permission User is trying to access.
     * @param session This object must be instantiated by calling {@link AccessMgr#createSession} method before passing into the method.  No variables need to be set by client after returned from createSession.
     * @return True if user has access, false otherwise.
     * @throws SecurityException
     *          in the event of data validation failure, security policy violation or DAO error.
     */
    boolean checkAccess( Session session, Permission perm )
        throws SecurityException;


    /**
     * This function returns the permissions of the session, i.e., the permissions assigned
     * to its authorized roles. The function is valid if and only if the session is a valid Fortress session.
     *
     * @param session This object must be instantiated by calling {@link AccessMgr#createSession} method before passing into the method.  No variables need to be set by client after returned from createSession.
     * @return List<Permission> containing permissions (op, obj) active for user's session.
     * @throws SecurityException is thrown if runtime error occurs with system.
     */
    List<Permission> sessionPermissions( Session session )
        throws SecurityException;


    /**
     * This function returns the active roles associated with a session. The function is valid if
     * and only if the session is a valid Fortress session.
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @return List<UserRole> containing all roles active in user's session.  This will NOT contain inherited roles.
     * @throws SecurityException is thrown if session invalid or system. error.
     */
    List<UserRole> sessionRoles( Session session )
        throws SecurityException;


    /**
     * This function returns the authorized roles associated with a session based on hierarchical relationships. The function is valid if
     * and only if the session is a valid Fortress session.
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @return Set<String> containing all roles active in user's session.  This will contain inherited roles.
     * @throws SecurityException is thrown if session invalid or system. error.
     */
    Set<String> authorizedRoles( Session session )
        throws SecurityException;


    /**
     * This function adds a role as an active role of a session whose owner is a given user.
     * <p>
     * The function is valid if and only if:
     * <ul>
     * <li> the user is a member of the USERS data set
     * <li> the role is a member of the ROLES data set
     * <li> the role inclusion does not violate Dynamic Separation of Duty Relationships
     * <li> the session is a valid Fortress session
     * <li> the user is authorized to that role
     * <li> the session is owned by that user.
     * </ul>
     * </p>
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @param role    object contains the role name, {@link UserRole#name}, to be activated into session.
     * @throws SecurityException is thrown if user is not allowed to activate or runtime error occurs with system.
     */
    void addActiveRole( Session session, UserRole role )
        throws SecurityException;


    /**
     * This function deletes a role from the active role set of a session owned by a given user.
     * The function is valid if and only if the user is a member of the USERS data set, the
     * session object contains a valid Fortress session, the session is owned by the user,
     * and the role is an active role of that session.
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @param role    object contains the role name, {@link org.apache.directory.fortress.core.model.UserRole#name}, to be deactivated.
     * @throws SecurityException is thrown if user is not allowed to deactivate or runtime error occurs with system.
     */
    void dropActiveRole( Session session, UserRole role )
        throws SecurityException;


    /**
     * This function returns the userId value that is contained within the session object.
     * The function is valid if and only if the session object contains a valid Fortress session.
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @return The userId value
     * @throws SecurityException is thrown if user session not active or runtime error occurs with system.
     */
    String getUserId( Session session )
        throws SecurityException;


    /**
     * This function returns the user object that is contained within the session object.
     * The function is valid if and only if the session object contains a valid Fortress session.
     *
     * @param session object contains the user's returned RBAC session from the createSession method.
     * @return The user value
     *         Sample User data contained in Session object:
     *         <ul> <code>Session</code>
     *         <li> <code>session.getUserId() => demoUser4</code>
     *         <li> <code>session.getInternalUserId() => be2dd2e:12a82ba707e:-7fee</code>
     *         <li> <code>session.getMessage() => Fortress checkPwPolicies userId <demouser4> VALIDATION GOOD</code>
     *         <li> <code>session.getErrorId() => 0</code>
     *         <li> <code>session.getWarningId() => 11</code>
     *         <li> <code>session.getExpirationSeconds() => 469831</code>
     *         <li> <code>session.getGraceLogins() => 0</code>
     *         <li> <code>session.getIsAuthenticated() => true</code>
     *         <li> <code>session.getLastAccess() => 1283623680440</code>
     *         <li> <code>session.getSessionId() => -7410986f:12addeea576:-7fff</code>
     *         <li>  ------------------------------------------
     *         <li> <code>User user = session.getUser();</code>
     *         <ul> <li> <code>user.getUserId() => demoUser4</code>
     *         <li> <code>user.getInternalId() => be2dd2e:12a82ba707e:-7fee</code>
     *         <li> <code>user.getCn() => JoeUser4</code>
     *         <li> <code>user.getDescription() => Demo Test User 4</code>
     *         <li> <code>user.getOu() => test</code>
     *         <li> <code>user.getSn() => User4</code>
     *         <li> <code>user.getBeginDate() => 20090101</code>
     *         <li> <code>user.getEndDate() => none</code>
     *         <li> <code>user.getBeginLockDate() => none</code>
     *         <li> <code>user.getEndLockDate() => none</code>
     *         <li> <code>user.getDayMask() => 1234567</code>
     *         <li> <code>user.getTimeout() => 60</code>
     *         <li> <code>List<UserRole> roles = session.getRoles();</code>
     *         <ul> <li><code>UserRole userRole = roles.get(i);</code>
     *         <li> <code>userRole.getName() => role1</code>
     *         <li> <code>userRole.getBeginTime() => 0000</code>
     *         <li> <code>userRole.getEndTime() => 0000</code>
     *         <li> <code>userRole.getBeginDate() => none</code>
     *         <li> <code>userRole.getEndDate() => none</code>
     *         <li> <code>userRole.getBeginLockDate() => null</code>
     *         <li> <code>userRole.getEndLockDate() => null</code>
     *         <li> <code>userRole.getDayMask() => null</code>
     *         <li> <code>userRole.getTimeout() => 0</code>
     *         </ul>
     *         </ul>
     *         </ul>
     * @throws SecurityException is thrown if user session not active or runtime error occurs with system.
     */
    User getUser( Session session )
        throws SecurityException;
}
