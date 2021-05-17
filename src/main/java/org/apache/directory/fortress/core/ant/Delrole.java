/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.fortress.core.ant;

import org.apache.directory.fortress.core.model.Role;

import java.util.ArrayList;
import java.util.List;


/**
 * The class is used by {@link FortressAntTask} to load {@link org.apache.directory.fortress.core.model.Role}s used to 
 * drive {@link org.apache.directory.fortress.core.AdminMgr#deleteRole(org.apache.directory.fortress.core.model.Role)}}.
 * It is not intended to be callable by programs outside of the Ant load utility.  The class name itself maps to the xml 
 * tag used by load utility.
 * <p>
 * This class name, 'Delrole', is used for the xml tag in the load script.
 * <pre>
 * {@code
 * <target name="all">
 *     <FortressAdmin>
 *         <delrole>
 *           ...
 *         </delrole>
 *     </FortressAdmin>
 * </target>
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Delrole
{
    final private List<Role> roles = new ArrayList<>();

    /**
     * All Ant data entities must have a default constructor.
     */
    public Delrole()
    {
    }

    /**
     * This method name, 'addRole', is used for derived xml tag 'role' in the load script.
     * <pre>
     * {@code
     * <delrole>
     *     <role name="role1"/>
     *     <role name="role2"/>
     * </delrole>
     * }
     * </pre>
     *
     * @param role contains reference to data element targeted for removal.
     */
    public void addRole(Role role)
    {
        this.roles.add(role);
    }

    /**
     * Used by {@link FortressAntTask#deleteRoles()} to retrieve list of Roles as defined in input xml file.
     *
     * @return collection containing {@link org.apache.directory.fortress.core.model.Role}s targeted for removal.
     */
    public List<Role> getRoles()
    {
        return this.roles;
    }
}

