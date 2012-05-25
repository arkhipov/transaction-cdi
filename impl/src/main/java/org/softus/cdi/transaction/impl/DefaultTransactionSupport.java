/*
 *    Copyright 2012 The Softus Team.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.softus.cdi.transaction.impl;

import javax.enterprise.context.ApplicationScoped;

import javax.enterprise.inject.Typed;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.softus.cdi.transaction.TransactionSupport;

/**
 * <p>
 * Transaction support that uses most popular JNDI locations for
 * TransactionManager and TransactionSynchronizationRegistry to lookup them.
 * </p>
 *
 * <p>
 * JNDI locations for TransactionManager.
 * <ul>
 *   <li>java:appserver/TransactionManager</li>
 *   <li>java:jboss/TransactionManager</li>
 *   <li>java:/TransactionManager</li>
 *   <li>java:pm/TransactionManager</li>
 *   <li>java:comp/TransactionManager</li>
 * </ul>
 * </p>
 *
 * <p>
 * JNDI locations for TransactionSynchronizationRegistry.
 * <ul>
 *   <li>java:comp/TransactionSynchronizationRegistry</li>
 * </ul>
 * </p>
 *
 * @author Vlad Arkhipov
 */
@ApplicationScoped
@Typed(TransactionSupport.class)
class DefaultTransactionSupport
extends AbstractTransactionSupport
{
    private static final String[] TRANSACTION_MANAGER_JNDI_NAMES = {
        "java:appserver/TransactionManager", // Glassfish
        "java:jboss/TransactionManager", // JBoss
        "java:/TransactionManager", // JBoss
        "java:pm/TransactionManager", // TopLink
        "java:comp/TransactionManager" // Some servlet containers
    };

    private static final String TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME =
        "java:comp/TransactionSynchronizationRegistry";

    /**
     * Creates a new transaction support.
     */
    public DefaultTransactionSupport()
    {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionManager lookupTransactionManager()
    {
        InitialContext ctx;
        try
        {
            ctx = new InitialContext();
        }
        catch (NamingException e)
        {
            throw new RuntimeException(e);
        }

        for (String jndiName : TRANSACTION_MANAGER_JNDI_NAMES)
        {
            try
            {
                return (TransactionManager) ctx.lookup(jndiName);
            }
            catch (NamingException e)
            {
                // Try next.
            }
        }

        throw new RuntimeException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected TransactionSynchronizationRegistry lookupSynchronizationRegistry()
    {
        try
        {
            return (TransactionSynchronizationRegistry) new InitialContext()
                .lookup(TRANSACTION_SYNCHRONIZATION_REGISTRY_JNDI_NAME);
        }
        catch (NamingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
